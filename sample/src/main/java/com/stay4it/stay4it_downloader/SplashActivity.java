package com.stay4it.stay4it_downloader;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;

import com.stay4it.downloader.DownloadManager;


/**
 * Created by Stay on 10/8/15.
 * Powered by www.stay4it.com
 */
public class SplashActivity extends ActionBarActivity {
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            jumpTo();
        }
    };

    private  void jumpTo() {
        Intent intent = new Intent(this, AppListActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DownloadManager.getInstance(getApplicationContext());
        mHandler.sendEmptyMessageDelayed(0,2000);
    }
}
