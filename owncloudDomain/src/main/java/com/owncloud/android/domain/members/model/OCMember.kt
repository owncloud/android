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

package com.owncloud.android.domain.members.model

data class OCMember(
    val id: String,
    val displayName: String,
    val surname: String,
    val type: OCMemberType
)

enum class OCMemberType {
    USER, GROUP;

    companion object {
        const val USER_TYPE_STRING = "User"
        const val GROUP_TYPE_STRING = "Group"

        fun parseFromString(memberType: String): OCMemberType =
            when (memberType) {
                USER_TYPE_STRING -> USER
                GROUP_TYPE_STRING -> GROUP
                else -> USER
            }

        fun toString(type: OCMemberType): String =
            when (type) {
                USER -> USER_TYPE_STRING
                GROUP -> GROUP_TYPE_STRING
            }
    }
}
