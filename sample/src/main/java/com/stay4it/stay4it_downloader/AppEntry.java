package com.stay4it.stay4it_downloader;

import com.stay4it.downloader.entities.DownloadEntry;

import java.io.Serializable;

/**
 * Created by Stay on 18/8/15.
 * Powered by www.stay4it.com
 */
public class AppEntry implements Serializable{
    public String name;
    public String icon;
    public String size;
    public String desc;
    public String url;

    @Override
    public String toString() {
        return name + "-----" + desc + "-----" + url;
    }

    public DownloadEntry generateDownloadEntry() {
        DownloadEntry entry = new DownloadEntry();
        entry.id = url;
        entry.name = name;
        entry.url = url;
        return entry;
    }
}
