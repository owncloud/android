/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

import timber.log.Timber;

public class ConnectivityUtils {

    public static boolean isAppConnectedViaWiFi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean result =
                cm != null && cm.getActiveNetworkInfo() != null
                        && cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI
                        && cm.getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED;
        Timber.d("isAppConnectedViaWifi returns %s", result);
        return result;
    }

    public static boolean isAppConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    public static boolean isNetworkActive(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities activeNetwork = connectivityManager.getNetworkCapabilities(network);
        if (activeNetwork == null) return false;

        if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return true;
        } else if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return true;
        } else if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return true;
        } else {
            return false;
        }
    }
}
