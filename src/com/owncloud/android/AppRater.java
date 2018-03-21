/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2018 ownCloud GmbH.
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

    public static final String APP_RATER_PREF_TITLE = "app_rater";
    public static final String APP_RATER_PREF_DONT_SHOW = "don't_show_again";
    private static final String APP_RATER_PREF_LAUNCH_COUNT = "launch_count";
    private static final String APP_RATER_PREF_DATE_FIRST_LAUNCH = "date_first_launch";
    public static final String APP_RATER_PREF_DATE_NEUTRAL = "date_neutral";

    public static void appLaunched(Context mContext, String packageName) {
        SharedPreferences prefs = mContext.getSharedPreferences(APP_RATER_PREF_TITLE, 0);
        if (prefs.getBoolean(APP_RATER_PREF_DONT_SHOW, false)) {
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();

        /// Increment launch counter
        long launchCount = prefs.getLong(APP_RATER_PREF_LAUNCH_COUNT, 0) + 1;
        editor.putLong(APP_RATER_PREF_LAUNCH_COUNT, launchCount);

        /// Get date of first launch
        Long dateFirstLaunch = prefs.getLong(APP_RATER_PREF_DATE_FIRST_LAUNCH, 0);
        if (dateFirstLaunch == 0) {
            dateFirstLaunch = System.currentTimeMillis();
            editor.putLong(APP_RATER_PREF_DATE_FIRST_LAUNCH, dateFirstLaunch);
        }

        /// Get date of neutral click
        Long dateNeutralClick = prefs.getLong(APP_RATER_PREF_DATE_NEUTRAL, 0);

        /// Wait at least n days before opening
        if (launchCount >= LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= Math.max(dateFirstLaunch
                    + daysToMilliseconds(DAYS_UNTIL_PROMPT), dateNeutralClick
                    + daysToMilliseconds(DAYS_UNTIL_NEUTRAL_CLICK))) {
                showRateDialog(mContext, packageName, false);
            }
        }

        editor.apply();
    }

    private static int daysToMilliseconds(int days){
            return days * 24 * 60 * 60 * 1000;
    }

    private static void showRateDialog(Context mContext, String packageName, Boolean cancelable) {
        RateMeDialog rateMeDialog = RateMeDialog.newInstance(packageName, cancelable);
        FragmentManager fm = ((FragmentActivity)mContext).getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        rateMeDialog.show(ft, DIALOG_RATE_ME_TAG);
    }
}
