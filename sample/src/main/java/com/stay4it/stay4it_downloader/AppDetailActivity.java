package com.stay4it.stay4it_downloader;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.stay4it.downloader.DownloadManager;
import com.stay4it.downloader.entities.DownloadEntry;
import com.stay4it.downloader.notify.DataWatcher;
import com.stay4it.downloader.utilities.Constants;
import com.stay4it.downloader.utilities.Trace;

public class AppDetailActivity extends ActionBarActivity implements View.OnClickListener {

    private Button mDownloadBtn;
    private DownloadManager mDownloadManager;
    private DownloadEntry entry;
    private DataWatcher watcher = new DataWatcher() {

        @Override
        public void notifyUpdate(DownloadEntry data) {
            if (data.id.equals(entry.id)) {
                entry = data;
                initializeData();
                Trace.e(data.toString());
            }
        }
    };
    private Button mDownloadPauseBtn;
    private Button mDownloadCancelBtn;
    private AppEntry app;
    private TextView mDownloadDetailLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);
        app = (AppEntry) getIntent().getSerializableExtra(Constants.KEY_APP_ENTRY);
        mDownloadManager = DownloadManager.getInstance(this);
        entry = mDownloadManager.containsDownloadEntry(app.url) ? mDownloadManager.queryDownloadEntry(app.url) : app.generateDownloadEntry();
        mDownloadBtn = (Button) findViewById(R.id.mDownloadBtn);
        mDownloadBtn.setOnClickListener(this);
        mDownloadPauseBtn = (Button) findViewById(R.id.mDownloadPauseBtn);
        mDownloadPauseBtn.setOnClickListener(this);
        mDownloadCancelBtn = (Button) findViewById(R.id.mDownloadCancelBtn);
        mDownloadCancelBtn.setOnClickListener(this);
        mDownloadDetailLabel = (TextView) findViewById(R.id.mDownloadDetailLabel);
        initializeData();
    }

    private void initializeData() {
        mDownloadDetailLabel.setText(app.name + "  " + app.size + "\n" + app.desc + "\n" + entry.status + "\n"
                + Formatter.formatShortFileSize(getApplicationContext(), entry.currentLength)
                + "/" + Formatter.formatShortFileSize(getApplicationContext(), entry.totalLength));
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
