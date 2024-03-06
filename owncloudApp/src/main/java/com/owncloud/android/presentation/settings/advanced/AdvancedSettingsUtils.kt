/**
 * ownCloud Android client application
 *
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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
 */

package com.owncloud.android.presentation.settings.advanced

const val PREFERENCE_REMOVE_LOCAL_FILES = "remove_local_files"

enum class RemoveLocalFiles {
    NEVER, ONE_HOUR, TWELVE_HOURS, ONE_DAY, THIRTY_DAYS;

    fun toMilliseconds(): Long {
        return when (this) {
            NEVER -> -1
            ONE_HOUR -> 3_600_000
            TWELVE_HOURS -> 43_200_000
            ONE_DAY -> 86_400_000
            THIRTY_DAYS -> 2_592_000_000
        }
    }
}
