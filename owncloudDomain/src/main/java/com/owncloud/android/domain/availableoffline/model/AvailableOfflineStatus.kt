/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
 */
package com.owncloud.android.domain.availableoffline.model


enum class AvailableOfflineStatus {
    /**
     * File is not available offline
     */
    NOT_AVAILABLE_OFFLINE,

    /**
     * File is available offline
     */
    AVAILABLE_OFFLINE,

    /**
     * File belongs to an available offline folder
     */
    AVAILABLE_OFFLINE_PARENT;

    companion object {
        fun fromValue(value: Int?): AvailableOfflineStatus {
            return when (value) {
                AVAILABLE_OFFLINE.ordinal -> AVAILABLE_OFFLINE
                AVAILABLE_OFFLINE_PARENT.ordinal -> AVAILABLE_OFFLINE_PARENT
                else -> NOT_AVAILABLE_OFFLINE
            }
        }
    }
}
