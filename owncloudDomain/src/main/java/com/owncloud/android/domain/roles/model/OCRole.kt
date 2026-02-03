/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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

package com.owncloud.android.domain.roles.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OCRole(
    val id: String,
    val displayName: String,
    val description: String
): Parcelable

enum class OCRoleType {
    CAN_VIEW, CAN_EDIT, CAN_MANAGE, UNKNOWN_ROLE;

    companion object {
        fun parseFromId(roleId: String): OCRoleType =
            when (roleId) {
                "a8d5fe5e-96e3-418d-825b-534dbdf22b99" -> CAN_VIEW
                "58c63c02-1d89-4572-916a-870abc5a1b7d" -> CAN_EDIT
                "312c0871-5ef7-4b3a-85b6-0e4074c64049" -> CAN_MANAGE
                else -> UNKNOWN_ROLE
            }
    }
}
