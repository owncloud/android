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

package com.owncloud.android.extensions

import com.owncloud.android.domain.members.model.OCMember
import com.owncloud.android.domain.members.model.OCMemberType
import com.owncloud.android.domain.spaces.model.SpaceMember

private const val GROUP_PREFIX = "g:"
private const val USER_PREFIX = "u:"

fun SpaceMember.toOCMember(): OCMember {
    val isGroup = id.startsWith(GROUP_PREFIX)
    val type = if (isGroup) OCMemberType.GROUP else OCMemberType.USER
    return OCMember(
        id = id.removePrefix(if (isGroup) GROUP_PREFIX else USER_PREFIX),
        displayName = displayName,
        surname = OCMemberType.toString(type),
        type = type
    )
}
