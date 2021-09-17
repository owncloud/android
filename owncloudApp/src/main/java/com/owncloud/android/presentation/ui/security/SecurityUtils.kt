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

import com.owncloud.android.MainApp
import com.owncloud.android.data.preferences.datasources.implementation.SharedPreferencesProviderImpl

const val PREFERENCE_LOCK_TIMEOUT = "lock_timeout"
const val PREFERENCE_LAST_UNLOCK_TIMESTAMP = "last_unlock_timestamp"

enum class LockTimeout {
    IMMEDIATELY, ONE_MINUTE, FIVE_MINUTES, THIRTY_MINUTES;

    fun toMilliseconds(): Int {
        return when (this) {
            ONE_MINUTE -> 60_000
            FIVE_MINUTES -> 300_000
            THIRTY_MINUTES -> 1_800_000
            else -> 0
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
    val preferencesProvider = SharedPreferencesProviderImpl(MainApp.appContext)
    val timeout = LockTimeout.valueOf(preferencesProvider.getString(PREFERENCE_LOCK_TIMEOUT, LockTimeout.IMMEDIATELY.name)!!).toMilliseconds()
    val lastUnlockTimestamp = preferencesProvider.getLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, 0)
    if (System.currentTimeMillis() - lastUnlockTimestamp > timeout) {
        val newLastUnlockTimestamp = System.currentTimeMillis() - timeout + 1_000
        preferencesProvider.putLong(PREFERENCE_LAST_UNLOCK_TIMESTAMP, newLastUnlockTimestamp)
    }
}