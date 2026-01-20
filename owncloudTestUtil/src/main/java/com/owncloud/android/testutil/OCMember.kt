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

package com.owncloud.android.testutil

import com.owncloud.android.domain.members.model.OCMember
import com.owncloud.android.lib.resources.members.responses.MemberResponse

val OC_USER_MEMBER = OCMember(
    id = "a9F3kL2mP7Qx",
    displayName = "ownCloud Developer",
    surname = "ownCloudDeveloper"
)

val OC_GROUP_MEMBER = OCMember(
    id = "G7pQ2M9xL4A8",
    displayName = "ownCloud Developers",
    surname = "Group"
)

val USER_MEMBER_RESPONSE = MemberResponse(
    id = "a9F3kL2mP7Qx",
    displayName = "ownCloud Developer",
    surname = "ownCloudDeveloper"
)

val GROUP_MEMBER_RESPONSE = MemberResponse(
    id = "G7pQ2M9xL4A8",
    displayName = "ownCloud Developers",
    surname = null
)
