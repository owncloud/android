package com.owncloud.android.lib.common.utils;

import timber.log.Timber;

import java.io.File;

public class Log_OC {

    private static String mOwncloudDataFolderLog;

    public static void setLogDataFolder(String logFolder) {
        mOwncloudDataFolderLog = logFolder;
    }

    public static void i(String tag, String message) {
        Timber.i(message);
    }

    public static void d(String TAG, String message) {
        Timber.d(message);
    }

    public static void d(String TAG, String message, Exception e) {
        Timber.d(e, message);
    }

    public static void e(String TAG, String message) {
        Timber.d(message);
    }

    public static void e(String TAG, String message, Throwable e) {
        Timber.e(e, message);
    }

    public static void v(String TAG, String message) {
        Timber.v(message);
    }

    public static void w(String TAG, String message) {
        Timber.w(message);
    }

    public static void startLogging(String storagePath) {
        LoggingHelper.INSTANCE.startLogging(
                new File(storagePath+ File.separator + mOwncloudDataFolderLog), mOwncloudDataFolderLog);
    }

    public static void stopLogging() {
        LoggingHelper.INSTANCE.stopLogging();
    }

}
