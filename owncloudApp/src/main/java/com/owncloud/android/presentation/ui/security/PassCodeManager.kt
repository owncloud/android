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
import com.owncloud.android.MainApp.Companion.appContext
import com.owncloud.android.authentication.BiometricManager
import com.owncloud.android.data.preferences.datasources.implementation.SharedPreferencesProviderImpl

object PassCodeManager {

    private const val PASS_CODE_TIMEOUT = "PASS_CODE_TIMEOUT"
    const val LAST_UNLOCK_TIMESTAMP = "LAST_UNLOCK_TIMESTAMP"

    private val exemptOfPasscodeActivities: MutableSet<Class<*>> = mutableSetOf(PassCodeActivity::class.java)

    private var visibleActivitiesCounter = 0

    private val preferencesProvider = SharedPreferencesProviderImpl(appContext)

    fun onActivityStarted(activity: Activity) {
        if (!exemptOfPasscodeActivities.contains(activity.javaClass) && passCodeShouldBeRequested()) {

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

        bayPassUnlockOnce()
        val powerMgr = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (isPassCodeEnabled() && !powerMgr.isScreenOn) {
            activity.moveTaskToBack(true)
        }
    }

    private fun passCodeShouldBeRequested(): Boolean {
        val lastUnlockTimestamp = preferencesProvider.getLong(LAST_UNLOCK_TIMESTAMP, 0)
        val timeout = 15000  //preferencesProvider.getInt(PASS_CODE_TIMEOUT, 1_000)
        return if (System.currentTimeMillis() - lastUnlockTimestamp > timeout && visibleActivitiesCounter <= 0) isPassCodeEnabled()
        else false
    }

    fun isPassCodeEnabled(): Boolean {
        return preferencesProvider.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
    }

    private fun checkPasscode(activity: Activity) {
        val i = Intent(appContext, PassCodeActivity::class.java).apply {
            action = PassCodeActivity.ACTION_CHECK
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        activity.startActivity(i)
    }

    fun onBiometricCancelled(activity: Activity) {
        // Ask user for passcode
        checkPasscode(activity)
    }

    /**
     * This can be used for example for onActivityResult, where you don't want to re authenticate
     * again.
     *
     * USE WITH CARE
     */
    fun bayPassUnlockOnce() {
        val timeout = 15000  //preferencesProvider.getInt(PASS_CODE_TIMEOUT, 1_000)
        val lastUnlockTimestamp = preferencesProvider.getLong(LAST_UNLOCK_TIMESTAMP, 0)
        if (System.currentTimeMillis() - lastUnlockTimestamp > timeout) {
            val newLastUnlockTimestamp = System.currentTimeMillis() - timeout + 1_000
            preferencesProvider.putLong(LAST_UNLOCK_TIMESTAMP, newLastUnlockTimestamp)
        }
    }

}
