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
import android.os.PowerManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import com.owncloud.android.MainApp
import com.owncloud.android.MainApp.Companion.appContext
import com.owncloud.android.data.preferences.datasources.implementation.SharedPreferencesProviderImpl
import com.owncloud.android.presentation.ui.security.PassCodeManager.isPassCodeEnabled
import com.owncloud.android.presentation.ui.security.PatternManager.isPatternEnabled

object BiometricManager {

    private val exemptOfBiometricActivities: MutableSet<Class<*>> = mutableSetOf(BiometricActivity::class.java)
    private var visibleActivitiesCounter = 0
    private val preferencesProvider = SharedPreferencesProviderImpl(appContext)
    private val biometricManager: BiometricManager = BiometricManager.from(appContext)

    fun onActivityStarted(activity: Activity) {
        if (!exemptOfBiometricActivities.contains(activity.javaClass) && biometricShouldBeRequested()) {

            if (isHardwareDetected() && hasEnrolledBiometric()) {
                // Use biometric lock
                val i = Intent(appContext, BiometricActivity::class.java)
                activity.startActivity(i)
            } else if (isPassCodeEnabled()) {
                // Cancel biometric lock and use passcode unlock method
                PassCodeManager.onBiometricCancelled(activity)
                visibleActivitiesCounter++
            } else if (isPatternEnabled()) {
                // Cancel biometric lock and use pattern unlock method
                PatternManager.onBiometricCancelled(activity)
                visibleActivitiesCounter++
            }

        }

        visibleActivitiesCounter++ // keep it AFTER biometricShouldBeRequested was checked

    }

    fun onActivityStopped(activity: Activity) {
        if (visibleActivitiesCounter > 0) visibleActivitiesCounter--

        bayPassUnlockOnce()
        val powerMgr = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (isBiometricEnabled() && !powerMgr.isScreenOn) {
            activity.moveTaskToBack(true)
        }
    }

    private fun biometricShouldBeRequested(): Boolean {
        val lastUnlockTimestamp = preferencesProvider.getLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, 0)
        val timeout = LockTimeout.valueOf(preferencesProvider.getString(PREFERENCE_LOCK_TIMEOUT, LockTimeout.IMMEDIATELY.name)!!).toMilliseconds()
        return if (System.currentTimeMillis() - lastUnlockTimestamp > timeout && visibleActivitiesCounter <= 0) isBiometricEnabled()
        else false
    }

    fun isBiometricEnabled(): Boolean {
        return preferencesProvider.getBoolean(BiometricActivity.PREFERENCE_SET_BIOMETRIC, false)
    }

    fun isHardwareDetected(): Boolean {
        return biometricManager.canAuthenticate(BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE &&
                biometricManager.canAuthenticate(BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    }

    fun hasEnrolledBiometric(): Boolean {
        return biometricManager.canAuthenticate(BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }
}
