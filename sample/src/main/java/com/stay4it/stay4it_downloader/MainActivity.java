package com.stay4it.stay4it_downloader;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;

import com.stay4it.downloader.notify.DataWatcher;
import com.stay4it.downloader.entities.DownloadEntry;
import com.stay4it.downloader.DownloadManager;
import com.stay4it.downloader.utilities.Trace;

public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private Button mDownloadBtn;
    private DownloadManager mDownloadManager;
    private DownloadEntry entry;
    private DataWatcher watcher = new DataWatcher() {
        @Override
        public void notifyUpdate(DownloadEntry data) {
            entry = data;
//            if (entry.status == DownloadEntry.DownloadStatus.cancelled){
//                entry = null;
//            }
            Trace.e(data.toString());
        }
    };
    private Button mDownloadPauseBtn;
    private Button mDownloadCancelBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDownloadBtn = (Button) findViewById(R.id.mDownloadBtn);
        mDownloadBtn.setOnClickListener(this);
        mDownloadPauseBtn = (Button) findViewById(R.id.mDownloadPauseBtn);
        mDownloadPauseBtn.setOnClickListener(this);
        mDownloadCancelBtn = (Button) findViewById(R.id.mDownloadCancelBtn);
        mDownloadCancelBtn.setOnClickListener(this);
        mDownloadManager = DownloadManager.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDownloadManager.addObserver(watcher);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mDownloadManager.removeObserver(watcher);
    }

    @Override
    public void onClick(View v) {
        if (entry == null) {
            entry = new DownloadEntry("http://shouji.360tpcdn.com/150723/de6fd89a346e304f66535b6d97907563/com.sina.weibo_2057.apk");
//            entry = new DownloadEntry("http://f.hiphotos.baidu.com/image/pic/item/0bd162d9f2d3572ce46b99dd8813632762d0c322.jpg");
        }
        switch (v.getId()) {
            case R.id.mDownloadBtn:
                mDownloadManager.add(entry);
                break;
            case R.id.mDownloadPauseBtn:
                if (entry.status == DownloadEntry.DownloadStatus.downloading) {
                    mDownloadManager.pause(entry);
                } else if (entry.status == DownloadEntry.DownloadStatus.paused) {
                    mDownloadManager.resume(entry);
                }
                break;
            case R.id.mDownloadCancelBtn:
                mDownloadManager.cancel(entry);
                break;
        }

    }
}
