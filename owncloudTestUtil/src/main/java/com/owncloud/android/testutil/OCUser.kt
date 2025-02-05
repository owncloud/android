/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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
import com.owncloud.android.domain.user.model.UserQuotaState

val OC_USER_INFO = UserInfo(
    id = "admin",
    displayName = "adminOc",
    email = null
)

val OC_USER_QUOTA = UserQuota(
    accountName = OC_ACCOUNT_NAME,
    available = 200_000,
    used = 80_000,
    total = 280_000,
    state = null
)

val OC_USER_QUOTA_WITHOUT_PERSONAL = UserQuota(
    accountName = OC_ACCOUNT_NAME,
    available = -4L,
    used = 0,
    total = 0,
    state = UserQuotaState.NORMAL
)

val OC_USER_QUOTA_UNLIMITED = UserQuota(
    accountName = OC_ACCOUNT_NAME,
    available = -3L,
    used = 5_000,
    total = 0,
    state = UserQuotaState.NORMAL
)

val OC_USER_QUOTA_LIMITED = OC_USER_QUOTA.copy(
    state = UserQuotaState.NORMAL
)

val OC_USER_AVATAR = UserAvatar(
    byteArrayOf(1, 2, 3, 4, 5, 6),
    eTag = "edcdc7d39dc218d197c269c8f75ab0f4",
    mimeType = "image/png"
)
