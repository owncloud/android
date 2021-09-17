/**
 * ownCloud Android client application
 *
 * @author Shashvat Kedia
 * @author Christian Schabesberger
 * @author Juan Carlos Garrote Gascón
 * <p>
 * Copyright (C) 2021 ownCloud GmbH.
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
import android.os.Build;
import android.os.PowerManager;

import com.owncloud.android.MainApp;
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider;
import com.owncloud.android.data.preferences.datasources.implementation.SharedPreferencesProviderImpl;
import com.owncloud.android.presentation.ui.security.LockTimeout;
import com.owncloud.android.ui.activity.PatternLockActivity;

import java.util.HashSet;
import java.util.Set;

import static com.owncloud.android.presentation.ui.security.SecurityUtilsKt.PREFERENCE_LAST_UNLOCK_TIMESTAMP;
import static com.owncloud.android.presentation.ui.security.SecurityUtilsKt.PREFERENCE_LOCK_TIMEOUT;
import static com.owncloud.android.presentation.ui.security.SecurityUtilsKt.bayPassUnlockOnce;

public class PatternManager {

    private static final Set<Class> sExemptOfPatternActivites;
    private int mVisibleActivitiesCounter = 0;
    private final SharedPreferencesProvider preferencesProvider = new SharedPreferencesProviderImpl(MainApp.Companion.getAppContext());

    static {
        sExemptOfPatternActivites = new HashSet<>();
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

            // Do not ask for pattern if biometric is enabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && BiometricManager.getBiometricManager(activity).isBiometricEnabled()) {
                mVisibleActivitiesCounter++;
                return;
            }

            askUserForPattern(activity);
        }

        mVisibleActivitiesCounter++;
    }

    public void onActivityStopped(Activity activity) {
        if (mVisibleActivitiesCounter > 0) {
            mVisibleActivitiesCounter--;
        }
        bayPassUnlockOnce();
        PowerManager powerMgr = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        if (isPatternEnabled() && powerMgr != null && !powerMgr.isScreenOn()) {
            activity.moveTaskToBack(true);
        }
    }

    private boolean patternShouldBeRequested() {
        long lastUnlockTimestamp = preferencesProvider.getLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, 0);
        int timeout = LockTimeout.valueOf(preferencesProvider.getString(PREFERENCE_LOCK_TIMEOUT, LockTimeout.IMMEDIATELY.name())).toMilliseconds();
        if (System.currentTimeMillis() - lastUnlockTimestamp > timeout && mVisibleActivitiesCounter <= 0) {
            return isPatternEnabled();
        }
        return false;
    }

    public boolean isPatternEnabled() {
        return preferencesProvider.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false);
    }

    private void askUserForPattern(Activity activity) {
        Intent i = new Intent(MainApp.Companion.getAppContext(), PatternLockActivity.class);
        i.setAction(PatternLockActivity.ACTION_CHECK);
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(i);
    }

    public void onBiometricCancelled(Activity activity) {
        askUserForPattern(activity);
    }
}
