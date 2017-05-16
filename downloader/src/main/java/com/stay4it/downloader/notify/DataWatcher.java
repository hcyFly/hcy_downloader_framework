package com.stay4it.downloader.notify;

import com.stay4it.downloader.entities.DownloadEntry;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by Stay on 4/8/15.
 * Powered by www.stay4it.com
 */
public abstract class DataWatcher implements Observer{
    @Override
    public void update(Observable observable, Object data) {
        if (data instanceof DownloadEntry){
            notifyUpdate((DownloadEntry)data);
        }
    }

    public abstract void notifyUpdate(DownloadEntry data);
}
