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

object PatternManager {

    private val exemptOfPatternActivities: MutableSet<Class<*>> = mutableSetOf(PatternActivity::class.java)
    private var visibleActivitiesCounter = 0
    private val preferencesProvider = SharedPreferencesProviderImpl(appContext)

    fun onActivityStarted(activity: Activity) {
        if (!exemptOfPatternActivities.contains(activity.javaClass) && patternShouldBeRequested()) {

            // Do not ask for pattern if biometric is enabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && BiometricManager.getBiometricManager(activity).isBiometricEnabled) {
                visibleActivitiesCounter++
                return
            }

            askUserForPattern(activity)
        }

        visibleActivitiesCounter++
    }

    fun onActivityStopped(activity: Activity) {
        if (visibleActivitiesCounter > 0) visibleActivitiesCounter--

        bayPassUnlockOnce()
        val powerMgr = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (isPatternEnabled() && !powerMgr.isScreenOn) {
            activity.moveTaskToBack(true)
        }
    }

    private fun patternShouldBeRequested(): Boolean {
        val lastUnlockTimestamp = preferencesProvider.getLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, 0)
        val timeout = LockTimeout.valueOf(preferencesProvider.getString(PREFERENCE_LOCK_TIMEOUT, LockTimeout.IMMEDIATELY.name)!!).toMilliseconds()
        return if (System.currentTimeMillis() - lastUnlockTimestamp > timeout && visibleActivitiesCounter <= 0) isPatternEnabled()
        else false
    }

    fun isPatternEnabled(): Boolean {
        return preferencesProvider.getBoolean(PatternActivity.PREFERENCE_SET_PATTERN, false)
    }

    private fun askUserForPattern(activity: Activity) {
        val i = Intent(appContext, PatternActivity::class.java).apply {
            action = PatternActivity.ACTION_CHECK
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        activity.startActivity(i)
    }

    fun onBiometricCancelled(activity: Activity) {
        askUserForPattern(activity)
    }
}
