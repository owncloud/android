/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2016 ownCloud GmbH.
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
package com.owncloud.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.owncloud.android.ui.dialog.RateMeDialog;

public class AppRater {

    private static final String DIALOG_RATE_ME_TAG = "DIALOG_RATE_ME";

    private final static int DAYS_UNTIL_PROMPT = 2;
    private final static int LAUNCHES_UNTIL_PROMPT = 2;
    private final static int DAYS_UNTIL_NEUTRAL_CLICK = 1;

    public static void appLaunched(Context mContext, String packageName) {
        SharedPreferences prefs = mContext.getSharedPreferences("app_rater", 0);
        if (prefs.getBoolean("don't_show_again", false)) {
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();

        /// Increment launch counter
        long launchCount = prefs.getLong("launch_count", 0) + 1;
        editor.putLong("launch_count", launchCount);

        /// Get date of first launch
        Long dateFirstLaunch = prefs.getLong("date_first_launch", 0);
        if (dateFirstLaunch == 0) {
            dateFirstLaunch = System.currentTimeMillis();
            editor.putLong("date_first_launch", dateFirstLaunch);
        }

        /// Get date of neutral click
        Long dateNeutralClick = prefs.getLong("date_neutral", 0);

        /// Wait at least n days before opening
        if (launchCount >= LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= Math.max(dateFirstLaunch
                    + (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000), dateNeutralClick
                    + (DAYS_UNTIL_NEUTRAL_CLICK * 24 * 60 * 60 * 1000))) {
                showRateDialog(mContext, packageName, false);
            }
        }

        editor.apply();
    }

    private static void showRateDialog(Context mContext, String packageName, Boolean cancelable) {
        RateMeDialog rateMeDialog = RateMeDialog.newInstance(packageName, cancelable);
        FragmentManager fm = ((FragmentActivity)mContext).getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        rateMeDialog.show(ft, DIALOG_RATE_ME_TAG);
    }
}
