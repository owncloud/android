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

package com.owncloud.android.presentation.security

import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import com.owncloud.android.MainApp
import com.owncloud.android.data.providers.implementation.OCSharedPreferencesProvider

const val PREFERENCE_LOCK_TIMEOUT = "lock_timeout"
const val PREFERENCE_LAST_UNLOCK_TIMESTAMP = "last_unlock_timestamp"
const val PREFERENCE_LAST_UNLOCK_ATTEMPT_TIMESTAMP = "last_unlock_attempt_timestamp"

enum class LockTimeout {
    DISABLED, IMMEDIATELY, ONE_MINUTE, FIVE_MINUTES, THIRTY_MINUTES;

    fun toMilliseconds(): Int {
        return when (this) {
            DISABLED, IMMEDIATELY -> 1_000
            ONE_MINUTE -> 60_000
            FIVE_MINUTES -> 300_000
            THIRTY_MINUTES -> 1_800_000
        }
    }

    companion object {
        fun parseFromInteger(int: Int): LockTimeout {
            return when (int) {
                1 -> IMMEDIATELY
                2 -> ONE_MINUTE
                3 -> FIVE_MINUTES
                4 -> THIRTY_MINUTES
                else -> DISABLED
            }
        }
    }
}

/**
 * This can be used for example for onActivityResult, where you don't want to re authenticate
 * again.
 *
 * USE WITH CARE
 */
fun bayPassUnlockOnce() {
    val preferencesProvider = OCSharedPreferencesProvider(MainApp.appContext)
    val timeout = LockTimeout.valueOf(preferencesProvider.getString(PREFERENCE_LOCK_TIMEOUT, LockTimeout.IMMEDIATELY.name)!!).toMilliseconds()
    val lastUnlockTimestamp = preferencesProvider.getLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, 0)
    if (SystemClock.elapsedRealtime() - lastUnlockTimestamp > timeout) {
        val newLastUnlockTimestamp = SystemClock.elapsedRealtime() - timeout + 1_000
        preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, newLastUnlockTimestamp)
    }
}

fun isDeviceSecure() = (MainApp.appContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure
