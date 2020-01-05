/**
 * ownCloud Android client application
 *
 * @author Shashvat Kedia
 * @author Christian Schabesberger
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.authentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.owncloud.android.MainApp;
import com.owncloud.android.ui.activity.PatternLockActivity;

import java.util.HashSet;
import java.util.Set;

public class PatternManager {

    private static final Set<Class> sExemptOfPatternActivites;
    private Long timeStamp = 0L;
    private int mVisibleActivitiesCounter = 0;

    static {
        sExemptOfPatternActivites = new HashSet<Class>();
        sExemptOfPatternActivites.add(PatternLockActivity.class);
    }

    private static PatternManager mPatternManagerInstance = null;

    public static PatternManager getPatternManager() {
        if (mPatternManagerInstance == null) {
            mPatternManagerInstance = new PatternManager();
        }
        return mPatternManagerInstance;
    }

    private PatternManager() {
    }

    public void onActivityStarted(Activity activity) {
        if (!sExemptOfPatternActivites.contains(activity.getClass()) && patternShouldBeRequested()) {

            // Do not ask for pattern if fingerprint is enabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && FingerprintManager.getFingerprintManager(activity).
                    isFingerPrintEnabled()) {
                mVisibleActivitiesCounter++;
                return;
            }
            checkPattern(activity);
        }
        mVisibleActivitiesCounter++;
    }

    private void setUnlockTimestamp() {
        timeStamp = System.currentTimeMillis();
    }

    public void onActivityStopped(Activity activity) {
        if (mVisibleActivitiesCounter > 0) {
            mVisibleActivitiesCounter--;
        }
        setUnlockTimestamp();
        PowerManager powerMgr = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        if (isPatternEnabled() && powerMgr != null && !powerMgr.isScreenOn()) {
            activity.moveTaskToBack(true);
        }
    }

    public void onFingerprintCancelled(Activity activity) {
        // Ask user for pattern
        checkPattern(activity);
    }

    private void checkPattern(Activity activity) {
        Intent i = new Intent(MainApp.Companion.getAppContext(), PatternLockActivity.class);
        i.setAction(PatternLockActivity.ACTION_CHECK);
        activity.startActivity(i);
    }

    private boolean patternShouldBeRequested() {
        int PATTERN_TIMEOUT = 1000;
        if ((System.currentTimeMillis() - timeStamp) > PATTERN_TIMEOUT &&
                mVisibleActivitiesCounter <= 0
        ) {
            return isPatternEnabled();
        }
        return false;
    }

    public boolean isPatternEnabled() {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(MainApp.Companion.getAppContext());
        return appPrefs.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false);
    }

    /**
     * This can be used for example for onActivityResult, where you don't want to re authenticate
     * again.
     * <p>
     * USE WITH CARE
     */
    public void bayPassUnlockOnce() {
        setUnlockTimestamp();
    }
}
