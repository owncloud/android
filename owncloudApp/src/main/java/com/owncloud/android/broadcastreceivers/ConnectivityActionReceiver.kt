/**
 * ownCloud Android client application
 *
 * @author LukeOwncloud
 * @author Christian Schabesberger
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.owncloud.android.MainApp
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.db.UploadResult
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.files.services.TransferRequester
import com.owncloud.android.lib.common.utils.Log_OC

/**
 * Receives all connectivity action from Android OS at all times and performs
 * required OC actions. For now that are: - Signal connectivity to
 * [FileUploader].
 *
 * Later can be added: - Signal connectivity to download service, deletion
 * service, ... - Handle offline mode (cf.
 * https://github.com/owncloud/android/issues/162)
 *
 * Have fun with the comments :S
 */
class ConnectivityActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // LOG ALL EVENTS:
        Log_OC.v(TAG, "action: " + intent.action!!)
        Log_OC.v(TAG, "component: " + intent.component!!)
        intent.extras?.let {
            for (key in it.keySet()) {
                Log_OC.v(TAG, "key [" + key + "]: " + it.get(key))
            }
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
        if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
            val wifiInfo = intent.getParcelableExtra<WifiInfo>(WifiManager.EXTRA_WIFI_INFO)
            val bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID)
            if (networkInfo.isConnected &&      // not enough; see (*) right below

                wifiInfo != null &&
                UNKNOWN_SSID != wifiInfo.ssid.toLowerCase() &&
                bssid != null
            ) {
                Log_OC.d(TAG, "WiFi connected")

                wifiConnected(context)
            } else {
                // TODO tons of things to check to conclude disconnection;
                // TODO maybe alternative commented below, based on CONNECTIVITY_ACTION is better
                Log_OC.d(TAG, "WiFi disconnected ... but don't know if right now")
            }
        }
    }

    private fun wifiConnected(context: Context) {
        // for the moment, only recovery of camera uploads, similar to behaviour in release 1.9.1
        if (PreferenceManager.cameraPictureUploadEnabled(context) && PreferenceManager.cameraPictureUploadViaWiFiOnly(
                context
            ) || PreferenceManager.cameraVideoUploadEnabled(context) && PreferenceManager.cameraVideoUploadViaWiFiOnly(
                context
            )
        ) {

            val h = Handler(Looper.getMainLooper())
            h.postDelayed(
                {
                    Log_OC.d(TAG, "Requesting retry of camera uploads (& friends)")
                    val requester = TransferRequester()

                    //Avoid duplicate uploads, because uploads retry is also managed in FileUploader
                    //by using jobs in versions 5 or higher
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        requester.retryFailedUploads(
                            context, null,
                            // for the interrupted when Wifi fell, if any
                            // (side effect: any upload failed due to network error will be
                            // retried too, instant or not)
                            UploadResult.NETWORK_CONNECTION,
                            true
                        )
                    }

                    requester.retryFailedUploads(
                        context, null,
                        UploadResult.DELAYED_FOR_WIFI, // for the rest of enqueued when Wifi fell
                        true
                    )
                },
                500
            )
        }
    }

    companion object {
        private val TAG = ConnectivityActionReceiver::class.java.simpleName

        /**
         * Magic keyword, by Google.
         *
         * {@See http://developer.android.com/intl/es/reference/android/net/wifi/WifiInfo.html#getSSID()}
         */
        private const val UNKNOWN_SSID = "<unknown ssid>"
    }
}