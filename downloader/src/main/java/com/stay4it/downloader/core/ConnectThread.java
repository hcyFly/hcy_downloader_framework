package com.stay4it.downloader.core;

import com.stay4it.downloader.utilities.Constants;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Stay on 4/8/15.
 * Powered by www.stay4it.com
 */
public class ConnectThread implements  Runnable{

    private final String url;
    private final ConnectListener listener;
    private volatile boolean isRunning;

    public ConnectThread(String url, ConnectListener listener){
        this.url = url;
        this.listener = listener;
    }


    @Override
    public void run() {
        isRunning = true;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setRequestMethod("GET");
//            connection.setRequestProperty("Range","bytes=0-"+Integer.MAX_VALUE);
            connection.setConnectTimeout(Constants.CONNECT_TIME);
            connection.setReadTimeout(Constants.READ_TIME);
            int responseCode = connection.getResponseCode();
            int contentLength = connection.getContentLength();
            boolean isSupportRange = false;
            if (responseCode == HttpURLConnection.HTTP_OK){
                String ranges = connection.getHeaderField("Accept-Ranges");
                if ("bytes".equals(ranges)){
                    isSupportRange = true;
                }
                listener.onConnected(isSupportRange, contentLength);
            }else {
                listener.onConnectError("server error:" + responseCode);
            }
//            if (responseCode == HttpURLConnection.HTTP_PARTIAL){
//                isSupportRange = true;
//            }else {
//
//            }

            isRunning = false;
        } catch (IOException e) {
            isRunning = false;
            listener.onConnectError(e.getMessage());
//            e.printStackTrace();
        }finally {
            if (connection != null){
                connection.disconnect();
            }
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void cancel() {
        Thread.currentThread().interrupt();
    }

    interface ConnectListener{
        void onConnected(boolean isSupportRange, int totalLength);

        void onConnectError(String message);
    }
}

