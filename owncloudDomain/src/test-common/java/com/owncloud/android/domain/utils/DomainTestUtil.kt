/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.domain.utils

import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType

object DomainTestUtil {
    /**
     * Shares
     */

    val DUMMY_SHARE = OCShare(
        fileSource = 7,
        itemSource = 7,
        shareType = ShareType.USER, // Public share by default
        shareWith = "",
        path = "/Photos/image.jpg",
        permissions = 1,
        sharedDate = 1542628397,
        expirationDate = 0,
        token = "AnyToken",
        sharedWithDisplayName = "",
        sharedWithAdditionalInfo = "",
        isFolder = false,
        userId = -1,
        remoteId = 1,
        accountOwner = "admin@server",
        name = "",
        shareLink = ""
    )

    /**
     * Capability
     */
    val DUMMY_CAPABILITY =
        OCCapability(
            accountName = "user@server",
            versionMayor = 2,
            versionMinor = 1,
            versionMicro = 0,
            versionString = "1.0.0",
            versionEdition = "1.0.0",
            corePollInterval = 0,
            filesSharingApiEnabled = CapabilityBooleanType.TRUE,
            filesSharingSearchMinLength = 3,
            filesSharingPublicEnabled = CapabilityBooleanType.TRUE,
            filesSharingPublicPasswordEnforced = CapabilityBooleanType.FALSE,
            filesSharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.FALSE,
            filesSharingPublicPasswordEnforcedReadWrite = CapabilityBooleanType.FALSE,
            filesSharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.FALSE,
            filesSharingPublicExpireDateEnabled = CapabilityBooleanType.FALSE,
            filesSharingPublicExpireDateDays = 0,
            filesSharingPublicExpireDateEnforced = CapabilityBooleanType.FALSE,
            filesSharingPublicSendMail = CapabilityBooleanType.FALSE,
            filesSharingPublicUpload = CapabilityBooleanType.FALSE,
            filesSharingPublicMultiple = CapabilityBooleanType.FALSE,
            filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.FALSE,
            filesSharingUserSendMail = CapabilityBooleanType.FALSE,
            filesSharingResharing = CapabilityBooleanType.FALSE,
            filesSharingFederationOutgoing = CapabilityBooleanType.FALSE,
            filesSharingFederationIncoming = CapabilityBooleanType.FALSE,
            filesBigFileChunking = CapabilityBooleanType.FALSE,
            filesUndelete = CapabilityBooleanType.FALSE,
            filesVersioning = CapabilityBooleanType.FALSE
        )
}
