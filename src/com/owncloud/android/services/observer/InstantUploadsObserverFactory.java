/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2017 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.services.observer;

import android.content.Context;
import android.os.Build;

import com.owncloud.android.db.PreferenceManager.InstantUploadsConfiguration;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Builds new instances of {@link InstantUploadsObserver}, using the appropritate implementation.
 */
class InstantUploadsObserverFactory {

    private static final String TAG = InstantUploadsObserverFactory.class.getName();

    public static InstantUploadsObserver newObserver(InstantUploadsConfiguration config, Context context) {
        if (Build.MODEL.toLowerCase().contains("nexus") ||
            Build.MODEL.toLowerCase().contains("pixel") ) {
            Log_OC.d(TAG, "Creating observer based on iNotify");
            return new InstantUploadsObserverBasedOnINotify(config, context);
        } else {
            Log_OC.d(TAG, "Creating observer based on Commons IO");
            return new InstantUploadsObserverBasedOnCommonsIOFileMonitor(config, context);
        }
    }
}