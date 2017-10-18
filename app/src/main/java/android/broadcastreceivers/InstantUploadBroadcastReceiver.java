/**
 *  ownCloud Android client application
 *
 *  @author Bartek Przybylski
 *  @author David A. Velasco
 *  Copyright (C) 2012  Bartek Przybylski
 *  Copyright (C) 2016 ownCloud GmbH.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.observer.InstantUploadsHandler;


public class InstantUploadBroadcastReceiver extends BroadcastReceiver {

    private static String TAG = InstantUploadBroadcastReceiver.class.getName();
    // Image action
    // Officially supported action since SDK 14:
    // http://developer.android.com/reference/android/hardware/Camera.html#ACTION_NEW_PICTURE
    private static String NEW_PHOTO_ACTION = "android.hardware.action.NEW_PICTURE";
    // Video action
    // Officially supported action since SDK 14:
    // http://developer.android.com/reference/android/hardware/Camera.html#ACTION_NEW_VIDEO
    private static String NEW_VIDEO_ACTION = "android.hardware.action.NEW_VIDEO";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log_OC.d(TAG, "Received: " + intent.getAction());
        if (intent.getAction().equals(NEW_PHOTO_ACTION)) {
            InstantUploadsHandler handler = new InstantUploadsHandler();
            handler.handleNewPictureAction(
                intent,
                PreferenceManager.getInstantUploadsConfiguration(context),
                context
            );
            Log_OC.d(TAG, "processed: android.hardware.action.NEW_PICTURE");

        } else if (intent.getAction().equals(NEW_VIDEO_ACTION)) {
            InstantUploadsHandler handler = new InstantUploadsHandler();
            handler.handleNewVideoAction(
                intent,
                PreferenceManager.getInstantUploadsConfiguration(context),
                context
            );
            Log_OC.d(TAG, "processed: android.hardware.action.NEW_VIDEO");

        } else {
            Log_OC.e(TAG, "Incorrect intent received: " + intent.getAction());
        }
    }

}
