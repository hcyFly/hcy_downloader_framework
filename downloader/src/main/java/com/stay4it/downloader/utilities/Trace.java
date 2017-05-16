package com.stay4it.downloader.utilities;

import android.util.Log;

/**
 * Created by Stay on 5/8/15.
 * Powered by www.stay4it.com
 */
public class Trace {

    public static final String TAG = "stay4it";
    private static final boolean DEBUG = true;

    public static void d(String msg) {
        if (DEBUG)
            Log.d(TAG, msg);
    }

    public static void e(String msg) {
        if (DEBUG)
            Log.e(TAG, msg);
    }
}
