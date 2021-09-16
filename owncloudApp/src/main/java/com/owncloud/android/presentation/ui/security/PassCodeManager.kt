/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
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

package com.owncloud.android.presentation.ui.security

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.preference.PreferenceManager
import com.owncloud.android.MainApp.Companion.appContext
import com.owncloud.android.authentication.BiometricManager

object PassCodeManager {

    private val exemptOfPasscodeActivites: MutableSet<Class<*>> = mutableSetOf(PassCodeActivity::class.java)

    private var timestamp = 0L
    private var visibleActivitiesCounter = 0

    // keeping a "low" positive value is the easiest way to prevent the pass code is requested on rotations
    private const val PASS_CODE_TIMEOUT = 1_000

    fun onActivityStarted(activity: Activity) {
        if (!exemptOfPasscodeActivites.contains(activity.javaClass) && passCodeShouldBeRequested()) {

            // Do not ask for passcode if biometric is enabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && BiometricManager.getBiometricManager(activity).isBiometricEnabled) {
                visibleActivitiesCounter++
                return
            }

            checkPasscode(activity)
        }

        visibleActivitiesCounter++ // keep it AFTER passCodeShouldBeRequested was checked
    }

    fun onActivityStopped(activity: Activity) {
        if (visibleActivitiesCounter > 0) visibleActivitiesCounter--

        setUnlockTimestamp()
        val powerMgr = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (isPassCodeEnabled() && !powerMgr.isScreenOn) {
            activity.moveTaskToBack(true)
        }
    }

    private fun passCodeShouldBeRequested(): Boolean {
        return if (SystemClock.elapsedRealtime() - timestamp > PASS_CODE_TIMEOUT && visibleActivitiesCounter <= 0) isPassCodeEnabled()
        else false
    }

    fun isPassCodeEnabled(): Boolean {
        val appPrefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        return appPrefs.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
    }

    private fun checkPasscode(activity: Activity) {
        val i = Intent(appContext, PassCodeActivity::class.java).apply {
            action = PassCodeActivity.ACTION_CHECK
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        activity.startActivity(i)
    }

    private fun setUnlockTimestamp() {
        timestamp = SystemClock.elapsedRealtime()
    }

    fun onBiometricCancelled(activity: Activity) {
        // Ask user for passcode
        checkPasscode(activity)
    }

    /**
     * This can be used for example for onActivityResult, where you don't want to re authenticate
     * again.
     *
     *
     * USE WITH CARE
     */
    fun bayPassUnlockOnce() {
        setUnlockTimestamp()
    }

}
