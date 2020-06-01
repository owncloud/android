/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Roberto Bermejo
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
package com.owncloud.android.authentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;
import com.owncloud.android.MainApp;
import com.owncloud.android.ui.activity.BiometricActivity;

import java.util.HashSet;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.M)
/**
 * Handle biometric requests. Besides, is a facade to access some
 * {@link androidx.biometric.BiometricMananager} methods
 */
    public class BiometricManager {

    private static final Set<Class> sExemptOfBiometricActivites;

    private androidx.biometric.BiometricManager mBiometricManager;

    static {
        sExemptOfBiometricActivites = new HashSet<>();
        sExemptOfBiometricActivites.add(BiometricActivity.class);
        // other activities may be exempted, if needed
    }

    private static int BIOMETRIC_TIMEOUT = 1000;
    // keeping a "low" positive value is the easiest way to prevent the biometric is requested on rotations

    private static BiometricManager mBiometricManagerInstance = null;

    public static BiometricManager getBiometricManager(Context context) {

        if (mBiometricManagerInstance == null) {
            mBiometricManagerInstance = new BiometricManager();
            mBiometricManagerInstance.mBiometricManager = androidx.biometric.BiometricManager.from(context);
        }
        return mBiometricManagerInstance;
    }

    private Long mTimestamp = 0l;
    private int mVisibleActivitiesCounter = 0;

    private BiometricManager() {
    }

    public void onActivityStarted(Activity activity) {

        if (!sExemptOfBiometricActivites.contains(activity.getClass())) {

            if (biometricShouldBeRequested()) {

                if (isHardwareDetected() && hasEnrolledBiometric()) {
                    // Use biometric lock
                    Intent i = new Intent(MainApp.Companion.getAppContext(), BiometricActivity.class);
                    activity.startActivity(i);
                } else if (PassCodeManager.getPassCodeManager().isPassCodeEnabled()) {
                    // Cancel biometric lock and use passcode unlock method
                    PassCodeManager.getPassCodeManager().onBiometricCancelled(activity);
                    mVisibleActivitiesCounter++;
                } else if (PatternManager.getPatternManager().isPatternEnabled()) {
                    // Cancel biometric lock and use pattern unlock method
                    PatternManager.getPatternManager().onBiometricCancelled(activity);
                    mVisibleActivitiesCounter++;
                }

            }
        }

        mVisibleActivitiesCounter++;    // keep it AFTER biometricShouldBeRequested was checked
    }

    public void onActivityStopped(Activity activity) {
        if (mVisibleActivitiesCounter > 0) {
            mVisibleActivitiesCounter--;
        }
        setUnlockTimestamp();
        PowerManager powerMgr = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        if (isBiometricEnabled() && powerMgr != null && !powerMgr.isScreenOn()) {
            activity.moveTaskToBack(true);
        }
    }

    private void setUnlockTimestamp() {
        mTimestamp = SystemClock.elapsedRealtime();
    }

    private boolean biometricShouldBeRequested() {

        if ((SystemClock.elapsedRealtime() - mTimestamp) > BIOMETRIC_TIMEOUT && mVisibleActivitiesCounter <= 0) {
            return isBiometricEnabled();
        }

        return false;
    }

    protected boolean isBiometricEnabled() {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(MainApp.Companion.getAppContext());
        return (appPrefs.getBoolean(BiometricActivity.PREFERENCE_SET_BIOMETRIC, false));
    }

    public boolean isHardwareDetected() {
        return mBiometricManager.canAuthenticate() != androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE &&
                mBiometricManager.canAuthenticate() != androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE;
    }

    public boolean hasEnrolledBiometric() {
        return mBiometricManager.canAuthenticate() != androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED;
    }

    /**
     * This can be used for example for onActivityResult, where you don't want to re authenticate
     * again.
     *
     * USE WITH CARE
     */
    public void bayPassUnlockOnce() {
        setUnlockTimestamp();
    }
}
