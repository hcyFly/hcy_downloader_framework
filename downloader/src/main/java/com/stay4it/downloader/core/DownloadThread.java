package com.stay4it.downloader.core;

import com.stay4it.downloader.entities.DownloadEntry;
import com.stay4it.downloader.utilities.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Stay on 4/8/15.
 * Powered by www.stay4it.com
 */
public class DownloadThread implements Runnable {
    private final String url;
    private final int startPos;
    private final int endPos;
    private final File destFile;
    private final DownloadListener listener;
    private final int index;
    private final boolean isSingleDownload;
    private volatile boolean isPaused;

    private DownloadEntry.DownloadStatus mStatus;
    private volatile boolean isCancelled;
    private volatile boolean isError;

    public DownloadThread(String url,File destFile, int index, int startPos, int endPos, DownloadListener listener) {
        this.url = url;
        this.index = index;
        this.startPos = startPos;
        this.endPos = endPos;
        this.destFile = destFile;
        if (startPos == 0 && endPos == 0) {
            isSingleDownload = true;
        } else {
            isSingleDownload = false;
        }

        this.listener = listener;
    }

    @Override
    public void run() {
        mStatus = DownloadEntry.DownloadStatus.downloading;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            if (!isSingleDownload) {
                connection.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);
            }
            connection.setConnectTimeout(Constants.CONNECT_TIME);
            connection.setReadTimeout(Constants.READ_TIME);
            int responseCode = connection.getResponseCode();
            int contentLength = connection.getContentLength();
            RandomAccessFile raf = null;
            FileOutputStream fos = null;
            InputStream is = null;
            if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                raf = new RandomAccessFile(destFile, "rw");
                raf.seek(startPos);
                is = connection.getInputStream();
                byte[] buffer = new byte[2048];
                int len = -1;
                while ((len = is.read(buffer)) != -1) {
                    raf.write(buffer, 0, len);
                    listener.onProgressChanged(index, len);
                    if (isPaused || isCancelled || isError) {
                        break;
                    }
                }
                raf.close();
                is.close();
            } else if (responseCode == HttpURLConnection.HTTP_OK) {
                fos = new FileOutputStream(destFile);
                is = connection.getInputStream();
                byte[] buffer = new byte[2048];
                int len = -1;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    listener.onProgressChanged(index, len);
                    if (isPaused || isCancelled || isError) {
                        break;
                    }
                }
                fos.close();
                is.close();
            } else {
                mStatus = DownloadEntry.DownloadStatus.error;
                listener.onDownloadError(index, "server error:" + responseCode);
                return;
            }
            if (isPaused) {
                mStatus = DownloadEntry.DownloadStatus.paused;
                listener.onDownloadPaused(index);
            } else if (isCancelled) {
                mStatus = DownloadEntry.DownloadStatus.cancelled;
                listener.onDownloadCancelled(index);
            } else if (isError) {
                mStatus = DownloadEntry.DownloadStatus.error;
                listener.onDownloadError(index, "cancel manually by error");
            } else {
                mStatus = DownloadEntry.DownloadStatus.completed;
                listener.onDownloadCompleted(index);
            }
        } catch (IOException e) {
//            listener.onConnectError(e.getMessage());
            e.printStackTrace();
            if (isPaused) {
                mStatus = DownloadEntry.DownloadStatus.paused;
                listener.onDownloadPaused(index);
            } else if (isCancelled) {
                mStatus = DownloadEntry.DownloadStatus.cancelled;
                listener.onDownloadCancelled(index);
            } else {
                mStatus = DownloadEntry.DownloadStatus.error;
                listener.onDownloadError(index, e.getMessage());
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public boolean isRunning() {
        return mStatus == DownloadEntry.DownloadStatus.downloading;
    }

    public void pause() {
        isPaused = true;
//        Thread.currentThread().interrupt();
    }

    public void cancel() {
        isCancelled = true;
//        Thread.currentThread().interrupt();
    }


    public void cancelByError() {
        isError = true;
//        Thread.currentThread().interrupt();
    }

    interface DownloadListener {
        void onProgressChanged(int index, int progress);

        void onDownloadCompleted(int index);

        void onDownloadError(int index, String message);

        void onDownloadPaused(int index);

        void onDownloadCancelled(int index);
    }
}
