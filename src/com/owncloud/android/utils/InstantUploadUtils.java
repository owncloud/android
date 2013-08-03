package com.owncloud.android.utils;

import android.content.Context;
import android.preference.PreferenceManager;

public class InstantUploadUtils {

    /**
     * Static methods to know the instant uploads preferences
     * 
     * @author masensio
     *
     */
    
    public static boolean instantUploadEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("instant_uploading", false);
    }

    public static boolean instantUploadViaWiFiOnly(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("instant_upload_on_wifi", false);
    }
    
 }
