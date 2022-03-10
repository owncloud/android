/**
 * ownCloud Android client application
 *
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


package com.owncloud.android.testutil

import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType

val OC_SHARE = OCShare(
    shareType = ShareType.USER, // Private share by default
    shareWith = "",
    path = "/Photos/image.jpg",
    permissions = 1,
    sharedDate = 1542628397,
    expirationDate = 0,
    token = "AnyToken",
    sharedWithDisplayName = "",
    sharedWithAdditionalInfo = "",
    isFolder = false,
    remoteId = "1",
    name = "",
    shareLink = ""
)

val OC_PRIVATE_SHARE = OC_SHARE.copy(
    shareWith = "WhoEver",
    permissions = -1,
    sharedWithDisplayName = "anyDisplayName"
)

val OC_PUBLIC_SHARE = OC_SHARE.copy(
    shareType = ShareType.PUBLIC_LINK,
    expirationDate = 1000,
    name = "Image link",
    shareLink = "link"
)
