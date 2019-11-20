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

package com.owncloud.android.utils

import android.accounts.Account
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.lib.resources.shares.GetRemoteShareesOperation
import org.json.JSONObject

object AppTestUtil {
    /**
     * Shares
     */
    val DUMMY_SHARE = OCShare(
        fileSource = 7,
        itemSource = 7,
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
        userId = -1,
        remoteId = 1,
        accountOwner = "admin@server",
        name = "",
        shareLink = ""
    )

    /**
     * Sharees
     */
    fun createSharee(
        label: String,
        shareType: String,
        shareWith: String,
        shareWithAdditionalInfo: String
    ): JSONObject {
        val jsonObject = JSONObject()

        jsonObject.put(GetRemoteShareesOperation.PROPERTY_LABEL, label)

        val value = JSONObject()
        value.put(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE, shareType)
        value.put(GetRemoteShareesOperation.PROPERTY_SHARE_WITH, shareWith)
        value.put(GetRemoteShareesOperation.PROPERTY_SHARE_WITH_ADDITIONAL_INFO, shareWithAdditionalInfo)

        jsonObject.put(GetRemoteShareesOperation.NODE_VALUE, value)

        return jsonObject
    }

    /**
     * Capabilities
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

    /**
     * Accounts
     */
    val DUMMY_ACCOUNT = Account("test", "owncloud")

    /**
     * Files
     */
    val DUMMY_FILE = OCFile(
        "/Images/img.png"
    ).apply {
        fileId = 1
        fileName =  "img.png"
        mimetype = ".png"
        privateLink = "privateLink"
    }

    val DUMMY_FOLDER = OCFile(
        "/Images/img.png"
    ).apply {
        fileName =  "/Documents/"
        mimetype = "DIR"
    }
}
