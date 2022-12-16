/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * Copyright (C) 2022 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.presentation.security

enum class LockEnforcedType {
    DISABLED, EITHER_ENFORCED, PASSCODE_ENFORCED, PATTERN_ENFORCED;

    companion object {
        fun parseFromInteger(int: Int): LockEnforcedType =
            when (int) {
                1 -> EITHER_ENFORCED
                2 -> PASSCODE_ENFORCED
                3 -> PATTERN_ENFORCED
                else -> DISABLED
            }
    }
}
