package com.stay4it.downloader.entities;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.stay4it.downloader.DownloadConfig;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by Stay on 5/8/15.
 * Powered by www.stay4it.com
 */
@DatabaseTable(tableName = "downloadentry")
public class DownloadEntry implements Serializable,Cloneable {
    @DatabaseField(id = true)
    public String id;
    @DatabaseField
    public String name;
    @DatabaseField
    public String url;
    @DatabaseField
    public int currentLength;
    @DatabaseField
    public int totalLength;
    @DatabaseField
    public DownloadStatus status = DownloadStatus.idle;
    @DatabaseField
    public boolean isSupportRange = false;
    @DatabaseField(dataType = DataType.SERIALIZABLE)
    public HashMap<Integer, Integer> ranges;
    @DatabaseField
    public int percent;

    public DownloadEntry() {

    }

    public DownloadEntry(String url) {
        this.url = url;
        this.id = url;
        this.name = url.substring(url.lastIndexOf("/") + 1);
    }

    public void reset() {
        currentLength = 0;
        ranges = null;
        percent = 0;
        File file = DownloadConfig.getConfig().getDownloadFile(url);
        if (file.exists()){
            file.delete();
        }
    }

    public enum DownloadStatus {
        idle, waiting,connecting, downloading, paused, resumed, cancelled, completed,error
    }

    @Override
    public String toString() {
        return name + " is " + status.name() + " with " + currentLength + "/" + totalLength;
    }

    @Override
    public boolean equals(Object o) {
        return o.hashCode() == this.hashCode();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
