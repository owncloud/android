package com.owncloud.android.utils;

import android.content.Context;
import android.net.ConnectivityManager;

/**
 * Static methods to know the connectivity device
 * 
 * @author masensio
 *
 */

public class ConnectivityUtils {

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}
