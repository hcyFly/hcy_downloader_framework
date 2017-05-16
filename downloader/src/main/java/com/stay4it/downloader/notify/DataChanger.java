package com.stay4it.downloader.notify;

import android.content.Context;

import com.stay4it.downloader.db.DBController;
import com.stay4it.downloader.entities.DownloadEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Observable;

/**
 * Created by Stay on 4/8/15.
 * Powered by www.stay4it.com
 */
public class DataChanger extends Observable {
    private static DataChanger mInstance;
    private final Context context;
    private LinkedHashMap<String, DownloadEntry> mOperatedEntries;

    private DataChanger(Context context) {
        this.context = context;
        mOperatedEntries = new LinkedHashMap<>();
    }

    public synchronized static DataChanger getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DataChanger(context);
        }
        return mInstance;
    }


    public void postStatus(DownloadEntry entry) {
        mOperatedEntries.put(entry.id, entry);
        DBController.getInstance(context).newOrUpdate(entry);
        setChanged();
        notifyObservers(entry);
    }

    public ArrayList<DownloadEntry> queryAllRecoverableEntries() {
        ArrayList<DownloadEntry> mRecoverableEntries = null;
        for (Map.Entry<String, DownloadEntry> entry : mOperatedEntries.entrySet()) {
            if (entry.getValue().status == DownloadEntry.DownloadStatus.paused) {
                if (mRecoverableEntries == null) {
                    mRecoverableEntries = new ArrayList<>();
                }
                mRecoverableEntries.add(entry.getValue());
            }
        }
        return mRecoverableEntries;
    }

    public DownloadEntry queryDownloadEntryById(String id) {
        return mOperatedEntries.get(id);
    }

    public void addToOperatedEntryMap(String key, DownloadEntry value){
        mOperatedEntries.put(key, value);
    }

    public boolean containsDownloadEntry(String id) {
        return mOperatedEntries.containsKey(id);
    }

    public void deleteDownloadEntry(String id){
        mOperatedEntries.remove(id);
        DBController.getInstance(context).deleteById(id);
    }
}
