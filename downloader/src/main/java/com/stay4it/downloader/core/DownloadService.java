package com.stay4it.downloader.core;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.stay4it.downloader.DownloadConfig;
import com.stay4it.downloader.db.DBController;
import com.stay4it.downloader.entities.DownloadEntry;
import com.stay4it.downloader.entities.DownloadEntry.DownloadStatus;
import com.stay4it.downloader.notify.DataChanger;
import com.stay4it.downloader.utilities.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Stay on 4/8/15.
 * Powered by www.stay4it.com
 */
public class DownloadService extends Service {
    public static final int NOTIFY_DOWNLOADING = 1;
    public static final int NOTIFY_UPDATING = 2;
    public static final int NOTIFY_PAUSED_OR_CANCELLED = 3;
    public static final int NOTIFY_COMPLETED = 4;
    public static final int NOTIFY_CONNECTING = 5;
    //    1. net error 2. no sd 3. no memory
    public static final int NOTIFY_ERROR = 6;
    private HashMap<String, DownloadTask> mDownloadingTasks = new HashMap<>();
    private ExecutorService mExecutors;
    private LinkedBlockingDeque<DownloadEntry> mWaitingQueue = new LinkedBlockingDeque<>();
    private DataChanger mDataChanger;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case NOTIFY_PAUSED_OR_CANCELLED:
                case NOTIFY_COMPLETED:
                case NOTIFY_ERROR:
                    checkNext((DownloadEntry)msg.obj);
                    break;
            }
            mDataChanger.postStatus((DownloadEntry) msg.obj);
        }
    };
    private DBController mDBController;

    private void checkNext(DownloadEntry obj) {
        mDownloadingTasks.remove(obj.id);
        DownloadEntry newEntry = mWaitingQueue.poll();
        if (newEntry != null) {
            startDownload(newEntry);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mExecutors = Executors.newCachedThreadPool();
        mDataChanger = DataChanger.getInstance(getApplicationContext());
        mDBController = DBController.getInstance(getApplicationContext());
        intializeDownload();
    }

    private void intializeDownload() {
        ArrayList<DownloadEntry> mDownloadEtries = mDBController.queryAll();
        if (mDownloadEtries != null) {
            for (DownloadEntry entry : mDownloadEtries) {
                if (entry.status == DownloadStatus.downloading || entry.status == DownloadStatus.waiting) {
//                    TODO add a config if need to recover download
                    if (DownloadConfig.getConfig().isRecoverDownloadWhenStart()) {
                        if (entry.isSupportRange){
                            entry.status = DownloadStatus.paused;
                        }else {
                            entry.status = DownloadStatus.idle;
                            entry.reset();
                        }
                        addDownload(entry);
                    } else {
                        if (entry.isSupportRange){
                            entry.status = DownloadStatus.paused;
                        }else {
                            entry.status = DownloadStatus.idle;
                            entry.reset();
                        }
                        mDBController.newOrUpdate(entry);
                    }
                }
                mDataChanger.addToOperatedEntryMap(entry.id, entry);
            }
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            DownloadEntry entry = (DownloadEntry) intent.getSerializableExtra(Constants.KEY_DOWNLOAD_ENTRY);
            if (entry != null && mDataChanger.containsDownloadEntry(entry.id)) {
                entry = mDataChanger.queryDownloadEntryById(entry.id);
            }
            int action = intent.getIntExtra(Constants.KEY_DOWNLOAD_ACTION, -1);
            doAction(action, entry);
        }


        return super.onStartCommand(intent, flags, startId);
    }

    private void doAction(int action, DownloadEntry entry) {
//        check action, do related action
        switch (action) {
            case Constants.KEY_DOWNLOAD_ACTION_ADD:
                addDownload(entry);
                break;
            case Constants.KEY_DOWNLOAD_ACTION_PAUSE:
                pauseDownload(entry);
                break;
            case Constants.KEY_DOWNLOAD_ACTION_RESUME:
                resumeDownload(entry);
                break;
            case Constants.KEY_DOWNLOAD_ACTION_CANCEL:
                cancelDownload(entry);
                break;
            case Constants.KEY_DOWNLOAD_ACTION_PAUSE_ALL:
                pauseAll();
                break;
            case Constants.KEY_DOWNLOAD_ACTION_RECOVER_ALL:
                recoverAll();
                break;
        }
    }

    private void recoverAll() {
        ArrayList<DownloadEntry> mRecoverableEntries = mDataChanger.queryAllRecoverableEntries();
        if (mRecoverableEntries != null) {
            for (DownloadEntry entry : mRecoverableEntries) {
                addDownload(entry);
            }
        }
    }

    private void pauseAll() {
        while (mWaitingQueue.iterator().hasNext()) {
            DownloadEntry entry = mWaitingQueue.poll();
            entry.status = DownloadStatus.paused;
//            FIXME notify all once
            mDataChanger.postStatus(entry);
        }

        for (Map.Entry<String, DownloadTask> entry : mDownloadingTasks.entrySet()) {
            entry.getValue().pause();
        }

        mDownloadingTasks.clear();
    }

    private void addDownload(DownloadEntry entry) {
        if (mDownloadingTasks.size() >= DownloadConfig.getConfig().getMaxDownloadTasks()) {
            mWaitingQueue.offer(entry);
            entry.status = DownloadStatus.waiting;
            mDataChanger.postStatus(entry);
        } else {
            startDownload(entry);
        }
    }

    private void cancelDownload(DownloadEntry entry) {
        DownloadTask task = mDownloadingTasks.remove(entry.id);
        if (task != null) {
            task.cancel();
        } else {
            mWaitingQueue.remove(entry);
            entry.status = DownloadStatus.cancelled;
            mDataChanger.postStatus(entry);
        }
    }

    private void resumeDownload(DownloadEntry entry) {
        addDownload(entry);
    }

    private void pauseDownload(DownloadEntry entry) {
        DownloadTask task = mDownloadingTasks.remove(entry.id);
        if (task != null) {
            task.pause();
        } else {
            mWaitingQueue.remove(entry);
            entry.status = DownloadStatus.paused;
            mDataChanger.postStatus(entry);
        }

    }

    private void startDownload(DownloadEntry entry) {
//        TODO check if enable to download (3G, no memory, no sd)
        DownloadTask task = new DownloadTask(entry, mHandler, mExecutors);
        task.start();
        mDownloadingTasks.put(entry.id, task);
//        mExecutors.execute(task);
    }
}
