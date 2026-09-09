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
    CAN_VIEW, CAN_VIEW_SHARE, CAN_EDIT, CAN_EDIT_SHARE, CAN_EDIT_FOLDER_SHARE, CAN_EDIT_WITH_TRASHBIN_FOLDER_SHARE, CAN_MANAGE, UNKNOWN_ROLE;

    companion object {
        private const val CAN_VIEW_ID = "a8d5fe5e-96e3-418d-825b-534dbdf22b99"
        private const val CAN_VIEW_SHARE_ID = "b1e2218d-eef8-4d4c-b82d-0f1a1b48f3b5"
        private const val CAN_EDIT_ID = "58c63c02-1d89-4572-916a-870abc5a1b7d"
        private const val CAN_EDIT_SHARE_ID = "2d00ce52-1fc2-4dbc-8b95-a73b73395f5a"
        private const val CAN_EDIT_FOLDER_SHARE_ID = "1c996275-f1c9-4e71-abdf-a42f6495e960"
        private const val CAN_EDIT_WITH_TRASHBIN_FOLDER_SHARE_ID = "fb6c3e19-e378-47e5-b277-9732f9de6e21"
        private const val CAN_MANAGE_ID = "312c0871-5ef7-4b3a-85b6-0e4074c64049"
        private const val UNKNOWN_ROLE_ID = "unknown-role"

        fun parseFromId(roleId: String): OCRoleType =
            when (roleId) {
                CAN_VIEW_ID -> CAN_VIEW
                CAN_VIEW_SHARE_ID -> CAN_VIEW_SHARE
                CAN_EDIT_ID -> CAN_EDIT
                CAN_EDIT_SHARE_ID -> CAN_EDIT_SHARE
                CAN_EDIT_FOLDER_SHARE_ID -> CAN_EDIT_FOLDER_SHARE
                CAN_EDIT_WITH_TRASHBIN_FOLDER_SHARE_ID -> CAN_EDIT_WITH_TRASHBIN_FOLDER_SHARE
                CAN_MANAGE_ID -> CAN_MANAGE
                else -> UNKNOWN_ROLE
            }

        fun toString(roleType: OCRoleType): String =
            when (roleType) {
                CAN_VIEW -> CAN_VIEW_ID
                CAN_VIEW_SHARE -> CAN_VIEW_SHARE_ID
                CAN_EDIT -> CAN_EDIT_ID
                CAN_EDIT_SHARE -> CAN_EDIT_SHARE_ID
                CAN_EDIT_FOLDER_SHARE -> CAN_EDIT_FOLDER_SHARE_ID
                CAN_EDIT_WITH_TRASHBIN_FOLDER_SHARE -> CAN_EDIT_WITH_TRASHBIN_FOLDER_SHARE_ID
                CAN_MANAGE -> CAN_MANAGE_ID
                UNKNOWN_ROLE -> UNKNOWN_ROLE_ID
            }
    }
}
