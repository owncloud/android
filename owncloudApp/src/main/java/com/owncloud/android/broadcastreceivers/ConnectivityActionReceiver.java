/**
 * ownCloud Android client application
 *
 * @author LukeOwncloud
 * @author Christian Schabesberger
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.owncloud.android.MainApp;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.TransferRequester;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Receives all connectivity action from Android OS at all times and performs
 * required OC actions. For now that are: - Signal connectivity to
 * {@link FileUploader}.
 *
 * Later can be added: - Signal connectivity to download service, deletion
 * service, ... - Handle offline mode (cf.
 * https://github.com/owncloud/android/issues/162)
 *
 * Have fun with the comments :S
 */
public class ConnectivityActionReceiver extends BroadcastReceiver {
    private static final String TAG = ConnectivityActionReceiver.class.getSimpleName();

    /**
     * Magic keyword, by Google.
     *
     * {@See http://developer.android.com/intl/es/reference/android/net/wifi/WifiInfo.html#getSSID()}
     */
    private static final String UNKNOWN_SSID = "<unknown ssid>";

    @Override
    public void onReceive(final Context context, Intent intent) {
        // LOG ALL EVENTS:
        Log_OC.v(TAG, "action: " + intent.getAction());
        Log_OC.v(TAG, "component: " + intent.getComponent());
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Log_OC.v(TAG, "key [" + key + "]: " + extras.get(key));
            }
        } else {
            Log_OC.v(TAG, "no extras");
        }

        /**
         * There is an interesting mess to process WifiManager.NETWORK_STATE_CHANGED_ACTION and
         * ConnectivityManager.CONNECTIVITY_ACTION in a simple and reliable way.
         *
         * The former triggers much more events than what we really need to know about Wifi connection.
         *
         * But there are annoying uncertainties about ConnectivityManager.CONNECTIVITY_ACTION due
         * to the deprecation of ConnectivityManager.EXTRA_NETWORK_INFO in API level 14, and the absence
         * of ConnectivityManager.EXTRA_NETWORK_TYPE until API level 17. Dear Google, how should we
         * handle API levels 14 to 16?
         *
         * In the end maybe we need to keep in memory the current knowledge about connectivity
         * and update it taking into account several Intents received in a row
         *
         * But first let's try something "simple" to keep a basic retry of camera uploads in
         * version 1.9.2, similar to the existent until 1.9.1. To be improved.
         */
        if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo networkInfo =
                    intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            WifiInfo wifiInfo =
                    intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            String bssid =
                    intent.getStringExtra(WifiManager.EXTRA_BSSID);
            if (networkInfo.isConnected() &&      // not enough; see (*) right below
                    wifiInfo != null &&
                    !UNKNOWN_SSID.equals(wifiInfo.getSSID().toLowerCase()) &&
                    bssid != null
            ) {
                Log_OC.d(TAG, "WiFi connected");

                wifiConnected(context);
            } else {
                // TODO tons of things to check to conclude disconnection;
                // TODO maybe alternative commented below, based on CONNECTIVITY_ACTION is better
                Log_OC.d(TAG, "WiFi disconnected ... but don't know if right now");
            }
        }
        // (*) When WiFi is lost, an Intent with network state CONNECTED and SSID "<unknown ssid>" is
        //      received right before another Intent with network state DISCONNECTED; needs to
        //      be differentiated of a new Wifi connection.
        //
        //  Besides, with a new connection two Intents are received, having only the second the extra
        //  WifiManager.EXTRA_BSSID, with the BSSID of the access point accessed.
        //
        //  Not sure if this protocol is exact, since it's not documented. Only found mild references in
        //   - http://developer.android.com/intl/es/reference/android/net/wifi/WifiInfo.html#getSSID()
        //   - http://developer.android.com/intl/es/reference/android/net/wifi/WifiManager.html#EXTRA_BSSID
        //  and reproduced in Nexus 5 with Android 6.

        // TODO check if this helps us
        /**
         * Possible alternative attending ConnectivityManager.CONNECTIVITY_ACTION.
         *
         * Let's see what QA has to say
         *
         if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
         NetworkInfo networkInfo = intent.getParcelableExtra(
         ConnectivityManager.EXTRA_NETWORK_INFO      // deprecated in API 14
         );
         int networkType = intent.getIntExtra(
         ConnectivityManager.EXTRA_NETWORK_TYPE,     // only from API level 17
         -1
         );
         boolean couldBeWifiAction =
         (networkInfo == null && networkType < 0)    ||      // cases of lack of info
         networkInfo.getType() == ConnectivityManager.TYPE_WIFI  ||
         networkType == ConnectivityManager.TYPE_WIFI;

         if (couldBeWifiAction) {
         if (ConnectivityUtils.isAppConnectedViaWiFi(context)) {
         Log_OC.d(TAG, "WiFi connected");
         wifiConnected(context);
         } else {
         Log_OC.d(TAG, "WiFi disconnected");
         wifiDisconnected(context);
         }
         } /* else, CONNECTIVIY_ACTION is (probably) about other network interface (mobile, bluetooth, ...)
         }
         */
    }

    private void wifiConnected(Context context) {
        // for the moment, only recovery of camera uploads, similar to behaviour in release 1.9.1
        if (
                (PreferenceManager.cameraPictureUploadEnabled(context) &&
                        PreferenceManager.cameraPictureUploadViaWiFiOnly(context)) ||
                        (PreferenceManager.cameraVideoUploadEnabled(context) &&
                                PreferenceManager.cameraVideoUploadViaWiFiOnly(context))
        ) {

            Handler h = new Handler(Looper.getMainLooper());
            h.postDelayed(() -> {
                        Log_OC.d(TAG, "Requesting retry of camera uploads (& friends)");
                        TransferRequester requester = new TransferRequester();

                        //Avoid duplicate uploads, because uploads retry is also managed in FileUploader
                        //by using jobs in versions 5 or higher
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                            requester.retryFailedUploads(
                                    MainApp.Companion.getAppContext(),
                                    null,
                                    // for the interrupted when Wifi fell, if any
                                    // (side effect: any upload failed due to network error will be
                                    // retried too, instant or not)
                                    UploadResult.NETWORK_CONNECTION,
                                    true
                            );
                        }

                        requester.retryFailedUploads(
                                MainApp.Companion.getAppContext(),
                                null,
                                UploadResult.DELAYED_FOR_WIFI,       // for the rest of enqueued when Wifi fell
                                true
                        );
                    },
                    500
            );
        }
    }
}