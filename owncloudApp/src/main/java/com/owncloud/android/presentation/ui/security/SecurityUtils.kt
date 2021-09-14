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

const val LOCK_TIMEOUT = "lock_timeout"
const val LAST_UNLOCK_TIMESTAMP = "last_unlock_timestamp"

enum class LockTimeout {
    IMMEDIATELY, ONE_MINUTE, FIVE_MINUTES, THIRTY_MINUTES;

    companion object {
        fun fromStringToMilliseconds(string: String?): Int {
            return when {
                string.equals("IMMEDIATELY", ignoreCase = true) -> 0
                string.equals("ONE_MINUTE", ignoreCase = true) -> 60000
                string.equals("FIVE_MINUTES", ignoreCase = true) -> 300000
                else -> 1800000
            }
        }
    }
}