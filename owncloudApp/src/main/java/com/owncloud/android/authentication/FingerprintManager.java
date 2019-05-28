/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Christian Schabesberger
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
import android.preference.PreferenceManager;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;
import com.owncloud.android.MainApp;
import com.owncloud.android.lib.BuildConfig;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FingerprintActivity;

import java.util.HashSet;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.M)
/**
 * Handle fingerprint requests. Besides, is a facade to access some
 * {@link android.hardware.fingerprint.FingerprintManager} methods
 */
public class FingerprintManager {

    private static final Set<Class> sExemptOfFingerprintActivites;

    private android.hardware.fingerprint.FingerprintManager mHwFingerPrintManager;

    static {
        sExemptOfFingerprintActivites = new HashSet<Class>();
        sExemptOfFingerprintActivites.add(FingerprintActivity.class);
        // other activities may be exempted, if needed
    }

    private static int FINGERPRINT_TIMEOUT = 1000;
    // keeping a "low" positive value is the easiest way to prevent the fingerprint is requested on rotations

    public static FingerprintManager mFingerprintManagerInstance = null;

    public static FingerprintManager getFingerprintManager(Context context) {
        if (mFingerprintManagerInstance == null) {
            mFingerprintManagerInstance = new FingerprintManager();
            mFingerprintManagerInstance.mHwFingerPrintManager = context.getSystemService(android.hardware.fingerprint.
                    FingerprintManager.class);
        }
        return mFingerprintManagerInstance;
    }

    private Long mTimestamp = 0l;
    private int mVisibleActivitiesCounter = 0;

    protected FingerprintManager() {
    }

    public void onActivityCreated(Activity activity) {
        if (!BuildConfig.DEBUG) {
            if (isFingerPrintEnabled()) {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        } // else, let it go, or taking screenshots & testing will not be possible
    }

    public void onActivityStarted(Activity activity) {

        if (!sExemptOfFingerprintActivites.contains(activity.getClass())) {

            if (fingerprintShouldBeRequested()) {

                // There's no fingerprints registered in the device
                if (!hasEnrolledFingerprints() && PassCodeManager.getPassCodeManager().isPassCodeEnabled()) {

                    // Cancel fingerprint lock and use passcode unlock method
                    PassCodeManager.getPassCodeManager().onFingerprintCancelled(activity);
                    mVisibleActivitiesCounter++;
                    return;

                } else if (!hasEnrolledFingerprints() && PatternManager.getPatternManager().isPatternEnabled()) {

                    // Cancel fingerprint lock and use pattern unlock method
                    PatternManager.getPatternManager().onFingerprintCancelled(activity);
                    mVisibleActivitiesCounter++;
                    return;
                }

                Intent i = new Intent(MainApp.Companion.getAppContext(), FingerprintActivity.class);
                activity.startActivity(i);
            }
        }

        mVisibleActivitiesCounter++;    // keep it AFTER fingerprintShouldBeRequested was checked
    }

    public void onActivityStopped(Activity activity) {
        if (mVisibleActivitiesCounter > 0) {
            mVisibleActivitiesCounter--;
        }
        setUnlockTimestamp();
        PowerManager powerMgr = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        if (isFingerPrintEnabled() && powerMgr != null && !powerMgr.isScreenOn()) {
            activity.moveTaskToBack(true);
        }
    }

    private void setUnlockTimestamp() {
        mTimestamp = System.currentTimeMillis();
    }

    private boolean fingerprintShouldBeRequested() {

        if ((System.currentTimeMillis() - mTimestamp) > FINGERPRINT_TIMEOUT &&
                mVisibleActivitiesCounter <= 0) {
            return isFingerPrintEnabled();
        }

        return false;
    }

    public boolean isFingerPrintEnabled() {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(MainApp.Companion.getAppContext());
        return (appPrefs.getBoolean(FingerprintActivity.PREFERENCE_SET_FINGERPRINT, false));
    }

    public boolean isHardwareDetected() {
        return mHwFingerPrintManager.isHardwareDetected();
    }

    public boolean hasEnrolledFingerprints() {
        try {
            return mHwFingerPrintManager.hasEnrolledFingerprints();
        } catch (RuntimeException re) {
            Log_OC.e(FingerprintManager.class.toString(),
                    "Could find out if finger prints are enroded due to lack of android.permission" +
                            ".INTERACT_ACROSS_USERS");
            return false;
        }
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
