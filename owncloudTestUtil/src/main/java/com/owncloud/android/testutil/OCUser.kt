/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

import com.owncloud.android.domain.user.model.UserAvatar
import com.owncloud.android.domain.user.model.UserInfo
import com.owncloud.android.domain.user.model.UserQuota

val OC_USER_INFO = UserInfo(
    id = "admin",
    displayName = "adminOc",
    email = null
)

val OC_USER_QUOTA = UserQuota(
    used = 80_000,
    available = 200_000
)

val OC_USER_AVATAR = UserAvatar(
    byteArrayOf(1, 2, 3, 4, 5, 6),
    eTag = "edcdc7d39dc218d197c269c8f75ab0f4",
    mimeType = "image/png"
)
