package com.stay4it.downloader.core;

import android.os.Handler;
import android.os.Message;

import com.stay4it.downloader.DownloadConfig;
import com.stay4it.downloader.entities.DownloadEntry;
import com.stay4it.downloader.utilities.TickTack;
import com.stay4it.downloader.utilities.Trace;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;


/**
 * Created by Stay on 4/8/15.
 * Powered by www.stay4it.com
 * <p/>
 * 1.check if support range, get content-length
 * 2.if not, single thread to download. can't be paused|resumed
 * 3.if support, multiple threads to download
 * 3.1 compute the block size per thread
 * 3.2 execute sub-threads
 * 3.3 combine the progress and notify
 */
public class DownloadTask implements ConnectThread.ConnectListener, DownloadThread.DownloadListener {
    private final DownloadEntry entry;
    private final Handler mHandler;
    private final ExecutorService mExecutor;
    private volatile boolean isPaused;
    private volatile boolean isCancelled;
    private ConnectThread mConnectThread;
    private DownloadThread[] mDownloadThreads;
    private DownloadEntry.DownloadStatus[] mDownloadStatus;
    private long mLastStamp;
    private File destFile;


    public DownloadTask(DownloadEntry entry, Handler mHandler, ExecutorService mExecutor) {
        this.entry = entry;
        this.mHandler = mHandler;
        this.mExecutor = mExecutor;
        this.destFile = DownloadConfig.getConfig().getDownloadFile(entry.url);
    }

    public void pause() {
        Trace.e("download paused");
        isPaused = true;
        if (mConnectThread != null && mConnectThread.isRunning()) {
            mConnectThread.cancel();
        }
        if (mDownloadThreads != null && mDownloadThreads.length > 0) {
            for (int i = 0; i < mDownloadThreads.length; i++) {
                if (mDownloadThreads[i] != null && mDownloadThreads[i].isRunning()) {
                    if (entry.isSupportRange) {
                        mDownloadThreads[i].pause();
                    } else {
                        mDownloadThreads[i].cancel();
                    }
                }
            }
        }
    }

    public void cancel() {
        Trace.e("download cancelled");
        isCancelled = true;
        if (mConnectThread != null && mConnectThread.isRunning()) {
            mConnectThread.cancel();
        }

        if (mDownloadThreads != null && mDownloadThreads.length > 0) {
            for (int i = 0; i < mDownloadThreads.length; i++) {
                if (mDownloadThreads[i] != null && mDownloadThreads[i].isRunning()) {
                    mDownloadThreads[i].cancel();
                }
            }
        }
    }

    public void start() {
        if (entry.totalLength > 0) {
            Trace.e("no need to check if support range and totalLength");
            startDownload();
        } else {
            entry.status = DownloadEntry.DownloadStatus.connecting;
            notifyUpdate(entry, DownloadService.NOTIFY_CONNECTING);
            mConnectThread = new ConnectThread(entry.url, this);
            mExecutor.execute(mConnectThread);
        }
    }

    private void startDownload() {
//        entry.isSupportRange = false;
//        entry.totalLength = -1;
        Trace.e("startDownload: isSupportRange" + entry.isSupportRange + entry.totalLength);
        if (entry.isSupportRange) {
            startMultiDownload();
        } else {
            startSingleDownload();
        }
    }

    private void startMultiDownload() {
        Trace.e("startMultiDownload");
        entry.status = DownloadEntry.DownloadStatus.downloading;
        notifyUpdate(entry, DownloadService.NOTIFY_DOWNLOADING);
        int block = entry.totalLength / DownloadConfig.getConfig().getMaxDownloadThreads();
        int startPos = 0;
        int endPos = 0;
        if (entry.ranges == null) {
            entry.ranges = new HashMap<>();
            for (int i = 0; i < DownloadConfig.getConfig().getMaxDownloadThreads(); i++) {
                entry.ranges.put(i, 0);
            }
        }
        mDownloadThreads = new DownloadThread[DownloadConfig.getConfig().getMaxDownloadThreads()];
        mDownloadStatus = new DownloadEntry.DownloadStatus[DownloadConfig.getConfig().getMaxDownloadThreads()];
        for (int i = 0; i < DownloadConfig.getConfig().getMaxDownloadThreads(); i++) {
            startPos = i * block + entry.ranges.get(i);
            if (i == DownloadConfig.getConfig().getMaxDownloadThreads() - 1) {
                endPos = entry.totalLength;
            } else {
                endPos = (i + 1) * block - 1;
            }
            if (startPos < endPos) {
                mDownloadThreads[i] = new DownloadThread(entry.url,destFile, i, startPos, endPos, this);
                mDownloadStatus[i] = DownloadEntry.DownloadStatus.downloading;
                mExecutor.execute(mDownloadThreads[i]);
            } else {
                mDownloadStatus[i] = DownloadEntry.DownloadStatus.completed;
            }
        }
    }

    private void startSingleDownload() {
        Trace.e("startSingleDownload");
        entry.status = DownloadEntry.DownloadStatus.downloading;
        notifyUpdate(entry, DownloadService.NOTIFY_DOWNLOADING);
        mDownloadStatus = new DownloadEntry.DownloadStatus[1];
        mDownloadStatus[0] = entry.status;
        mDownloadThreads = new DownloadThread[1];
        mDownloadThreads[0] = new DownloadThread(entry.url,destFile, 0, 0, 0, this);
        mExecutor.execute(mDownloadThreads[0]);
    }

    private void notifyUpdate(DownloadEntry entry, int what) {
        Trace.e("notifyUpdate:" + what + ":" + entry.currentLength);
//        mHandler.removeMessages(what);
        Message msg = mHandler.obtainMessage();
        msg.what = what;
        msg.obj = entry;
        mHandler.sendMessage(msg);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected(boolean isSupportRange, int totalLength) {
        entry.isSupportRange = isSupportRange;
        entry.totalLength = totalLength;

        startDownload();
    }

    @Override
    public void onConnectError(String message) {
        if (isPaused || isCancelled) {
            entry.status = isPaused ? DownloadEntry.DownloadStatus.paused : DownloadEntry.DownloadStatus.cancelled;
            notifyUpdate(entry, DownloadService.NOTIFY_PAUSED_OR_CANCELLED);
        } else {
            entry.status = DownloadEntry.DownloadStatus.error;
            notifyUpdate(entry, DownloadService.NOTIFY_ERROR);
        }
    }

    @Override
    public synchronized void onProgressChanged(int index, int progress) {
        if (entry.isSupportRange) {
            int range = entry.ranges.get(index) + progress;
            entry.ranges.put(index, range);
        }
        entry.currentLength += progress;
//        long stamp = System.currentTimeMillis();
//        if (stamp - mLastStamp > 1000) {
//            mLastStamp = stamp;
//        }
        if (TickTack.getInstance().needToNotify()){
            notifyUpdate(entry, DownloadService.NOTIFY_UPDATING);
        }
    }

    @Override
    public synchronized void onDownloadCompleted(int index) {
        mDownloadStatus[index] = DownloadEntry.DownloadStatus.completed;

        for (int i = 0; i < mDownloadStatus.length; i++) {
            if (mDownloadStatus[i] != DownloadEntry.DownloadStatus.completed) {
                return;
            }
        }

        if (entry.totalLength > 0 && entry.currentLength != entry.totalLength) {
            entry.status = DownloadEntry.DownloadStatus.error;
            resetDownload();
            notifyUpdate(entry, DownloadService.NOTIFY_ERROR);
        } else {
            entry.status = DownloadEntry.DownloadStatus.completed;
            notifyUpdate(entry, DownloadService.NOTIFY_COMPLETED);
        }
    }

    private void resetDownload() {
        entry.reset();
    }

    @Override
    public synchronized void onDownloadError(int index, String message) {
        Trace.e("onDownloadError:" + message);
        mDownloadStatus[index] = DownloadEntry.DownloadStatus.error;

        for (int i = 0; i < mDownloadStatus.length; i++) {
            if (mDownloadStatus[i] != DownloadEntry.DownloadStatus.completed && mDownloadStatus[i] != DownloadEntry.DownloadStatus.error) {
                mDownloadThreads[i].cancelByError();
                return;
            }
        }

        entry.status = DownloadEntry.DownloadStatus.error;
        notifyUpdate(entry, DownloadService.NOTIFY_ERROR);
    }

    @Override
    public synchronized void onDownloadPaused(int index) {
        mDownloadStatus[index] = DownloadEntry.DownloadStatus.paused;

        for (int i = 0; i < mDownloadStatus.length; i++) {
            if (mDownloadStatus[i] != DownloadEntry.DownloadStatus.completed && mDownloadStatus[i] != DownloadEntry.DownloadStatus.paused) {
                return;
            }
        }

        entry.status = DownloadEntry.DownloadStatus.paused;
        notifyUpdate(entry, DownloadService.NOTIFY_PAUSED_OR_CANCELLED);
    }

    @Override
    public synchronized void onDownloadCancelled(int index) {
        mDownloadStatus[index] = DownloadEntry.DownloadStatus.cancelled;

        for (int i = 0; i < mDownloadStatus.length; i++) {
            if (mDownloadStatus[i] != DownloadEntry.DownloadStatus.completed && mDownloadStatus[i] != DownloadEntry.DownloadStatus.cancelled) {
                return;
            }
        }

        entry.status = DownloadEntry.DownloadStatus.cancelled;
        resetDownload();
        notifyUpdate(entry, DownloadService.NOTIFY_PAUSED_OR_CANCELLED);
    }

}
