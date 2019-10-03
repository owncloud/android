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

package com.owncloud.android.data.utils

import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.GetRemoteShareesOperation
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.status.RemoteCapability
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject

object DataTestUtil {
    /**
     * Shares
     */
    private fun createShare(
        fileSource: Long = 7,
        itemSource: Long = 7,
        shareType: Int, // Public share by default
        shareWith: String = "",
        path: String,
        permissions: Int = 1,
        sharedDate: Long = 1542628397,
        expirationDate: Long = 0,
        token: String = "pwdasd12dasdWZ",
        sharedWithDisplayName: String = "",
        sharedWithAdditionalInfo: String = "",
        isFolder: Boolean,
        userId: Long = -1,
        remoteId: Long = 1,
        accountOwner: String = "admin@server",
        name: String = "",
        shareLink: String = ""
    ) = OCShare(
        0,
        fileSource,
        itemSource,
        ShareType.fromValue(shareType)!!,
        shareWith,
        path,
        permissions,
        sharedDate,
        expirationDate,
        token,
        sharedWithDisplayName,
        sharedWithAdditionalInfo,
        isFolder,
        userId,
        remoteId,
        accountOwner,
        name,
        shareLink
    )

    fun createPrivateShare(
        remoteId: Long = 1,
        shareType: Int = 0,
        shareWith: String,
        path: String,
        permissions: Int = -1,
        isFolder: Boolean,
        sharedWithDisplayName: String,
        accountOwner: String = "admin@server"
    ) = createShare(
        remoteId = remoteId,
        shareType = shareType,
        shareWith = shareWith,
        path = path,
        permissions = permissions,
        isFolder = isFolder,
        sharedWithDisplayName = sharedWithDisplayName,
        accountOwner = accountOwner
    )

    fun createPublicShare(
        shareWith: String = "",
        path: String,
        expirationDate: Long = 1000,
        isFolder: Boolean,
        permissions: Int = 1,
        remoteId: Long = 1,
        accountOwner: String = "admin@server",
        name: String,
        shareLink: String
    ) = createShare(
        shareWith = shareWith,
        shareType = 3,
        path = path,
        permissions = permissions,
        expirationDate = expirationDate,
        isFolder = isFolder,
        remoteId = remoteId,
        accountOwner = accountOwner,
        name = name,
        shareLink = shareLink
    )

    fun createRemoteShare(
        fileSource: Long = 7,
        itemSource: Long = 7,
        shareType: Int, // Public share by default
        shareWith: String = "",
        path: String,
        permissions: Int = 1,
        sharedDate: Long = 1542628397,
        expirationDate: Long = 0,
        token: String = "pwdasd12dasdWZ",
        sharedWithDisplayName: String = "",
        isFolder: Boolean,
        userId: Long = -1,
        remoteId: Long = 1,
        name: String = "",
        shareLink: String = ""
    ): RemoteShare {
        val remoteShare = RemoteShare();

        remoteShare.fileSource = fileSource
        remoteShare.itemSource = itemSource
        remoteShare.shareType = com.owncloud.android.lib.resources.shares.ShareType.fromValue(shareType)
        remoteShare.shareWith = shareWith
        remoteShare.path = path
        remoteShare.permissions = permissions
        remoteShare.sharedDate = sharedDate
        remoteShare.expirationDate = expirationDate
        remoteShare.token = token
        remoteShare.sharedWithDisplayName = sharedWithDisplayName
        remoteShare.isFolder = isFolder
        remoteShare.userId = userId
        remoteShare.id = remoteId
        remoteShare.name = name
        remoteShare.shareLink = shareLink

        return remoteShare
    }

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
     * Capability
     */
    fun createCapability(
        accountName: String = "user@server",
        versionMayor: Int = 2,
        versionMinor: Int = 1,
        versionMicro: Int = 0,
        versionString: String = "1.0.0",
        versionEdition: String = "1.0.0",
        corePollinterval: Int = 0,
        sharingApiEnabled: CapabilityBooleanType = CapabilityBooleanType.TRUE,
        sharingPublicEnabled: CapabilityBooleanType = CapabilityBooleanType.TRUE,
        sharingPublicPasswordEnforced: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        sharingPublicPasswordEnforcedReadOnly: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        sharingPublicPasswordEnforcedReadWrite: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        sharingPublicPasswordEnforcedUploadOnly: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        sharingPublicExpireDateEnabled: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        sharingPublicExpireDateDays: Int = 0,
        sharingPublicExpireDateEnforced: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        sharingPublicSendMail: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        sharingPublicUpload: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        sharingPublicMultiple: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        sharingPublicSupportsUploadOnly: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        sharingUserSendMail: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        sharingResharing: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        sharingFederationOutgoing: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        sharingFederationIncoming: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        filesBigFileChunking: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        filesUndelete: CapabilityBooleanType = CapabilityBooleanType.FALSE,
        filesVersioning: CapabilityBooleanType = CapabilityBooleanType.FALSE
    ) = OCCapability(
        0,
        accountName,
        versionMayor,
        versionMinor,
        versionMicro,
        versionString,
        versionEdition,
        corePollinterval,
        sharingApiEnabled,
        sharingPublicEnabled,
        sharingPublicPasswordEnforced,
        sharingPublicPasswordEnforcedReadOnly,
        sharingPublicPasswordEnforcedReadWrite,
        sharingPublicPasswordEnforcedUploadOnly,
        sharingPublicExpireDateEnabled,
        sharingPublicExpireDateDays,
        sharingPublicExpireDateEnforced,
        sharingPublicSendMail,
        sharingPublicUpload,
        sharingPublicMultiple,
        sharingPublicSupportsUploadOnly,
        sharingUserSendMail,
        sharingResharing,
        sharingFederationOutgoing,
        sharingFederationIncoming,
        filesBigFileChunking,
        filesUndelete,
        filesVersioning
    )

    fun createCapabilityEntity(
        accountName: String = "user@server",
        versionMayor: Int = 2,
        versionMinor: Int = 1,
        versionMicro: Int = 0,
        versionString: String = "1.0.0",
        versionEdition: String = "1.0.0",
        corePollinterval: Int = 0,
        sharingApiEnabled: Int = 1,
        sharingPublicEnabled: Int = 1,
        sharingPublicPasswordEnforced: Int = 0,
        sharingPublicPasswordEnforcedReadOnly: Int = 0,
        sharingPublicPasswordEnforcedReadWrite: Int = 0,
        sharingPublicPasswordEnforcedUploadOnly: Int = 0,
        sharingPublicExpireDateEnabled: Int = 0,
        sharingPublicExpireDateDays: Int = 0,
        sharingPublicExpireDateEnforced: Int = 0,
        sharingPublicSendMail: Int = 0,
        sharingPublicUpload: Int = 0,
        sharingPublicMultiple: Int = 0,
        sharingPublicSupportsUploadOnly: Int = 0,
        sharingUserSendMail: Int = 0,
        sharingResharing: Int = 0,
        sharingFederationOutgoing: Int = 0,
        sharingFederationIncoming: Int = 0,
        filesBigFileChunking: Int = 0,
        filesUndelete: Int = 0,
        filesVersioning: Int = 0
    ) = OCCapabilityEntity(
        accountName,
        versionMayor,
        versionMinor,
        versionMicro,
        versionString,
        versionEdition,
        corePollinterval,
        sharingApiEnabled,
        sharingPublicEnabled,
        sharingPublicPasswordEnforced,
        sharingPublicPasswordEnforcedReadOnly,
        sharingPublicPasswordEnforcedReadWrite,
        sharingPublicPasswordEnforcedUploadOnly,
        sharingPublicExpireDateEnabled,
        sharingPublicExpireDateDays,
        sharingPublicExpireDateEnforced,
        sharingPublicSendMail,
        sharingPublicUpload,
        sharingPublicMultiple,
        sharingPublicSupportsUploadOnly,
        sharingUserSendMail,
        sharingResharing,
        sharingFederationOutgoing,
        sharingFederationIncoming,
        filesBigFileChunking,
        filesUndelete,
        filesVersioning
    )

    fun createRemoteCapability(
        accountName: String = "user@server",
        versionMayor: Int = 2,
        versionMinor: Int = 1,
        versionMicro: Int = 0,
        versionString: String = "1.0.0",
        versionEdition: String = "1.0.0",
        corePollinterval: Int = 0,
        sharingApiEnabled: Int = 0,
        sharingPublicEnabled: Int = 1,
        sharingPublicPasswordEnforced: Int = 0,
        sharingPublicPasswordEnforcedReadOnly: Int = 0,
        sharingPublicPasswordEnforcedReadWrite: Int = 0,
        sharingPublicPasswordEnforcedUploadOnly: Int = 0,
        sharingPublicExpireDateEnabled: Int = 0,
        sharingPublicExpireDateDays: Int = 0,
        sharingPublicExpireDateEnforced: Int = 0,
        sharingPublicSendMail: Int = 0,
        sharingPublicUpload: Int = 0,
        sharingPublicMultiple: Int = 0,
        sharingPublicSupportsUploadOnly: Int = 0,
        sharingUserSendMail: Int = 0,
        sharingResharing: Int = 0,
        sharingFederationOutgoing: Int = 0,
        sharingFederationIncoming: Int = 0,
        filesBigFileChunking: Int = 0,
        filesUndelete: Int = 0,
        filesVersioning: Int = 0
    ): RemoteCapability = RemoteCapability().apply {
        this.accountName = accountName
        this.versionMayor = versionMayor
        this.versionMinor = versionMinor
        this.versionMicro = versionMicro
        this.versionString = versionString
        this.versionEdition = versionEdition
        this.corePollinterval = corePollinterval
        filesSharingApiEnabled =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(sharingApiEnabled)!!
        filesSharingPublicEnabled =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(sharingPublicEnabled)!!
        filesSharingPublicPasswordEnforced =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(sharingPublicPasswordEnforced)!!
        filesSharingPublicPasswordEnforcedReadOnly =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(
                sharingPublicPasswordEnforcedReadOnly
            )!!
        filesSharingPublicPasswordEnforcedReadWrite =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(
                sharingPublicPasswordEnforcedReadWrite
            )!!
        filesSharingPublicPasswordEnforcedUploadOnly =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(
                sharingPublicPasswordEnforcedUploadOnly
            )!!
        filesSharingPublicExpireDateEnabled =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(sharingPublicExpireDateEnabled)!!
        filesSharingPublicExpireDateDays = sharingPublicExpireDateDays
        filesSharingPublicExpireDateEnforced =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(sharingPublicExpireDateEnforced)!!
        filesSharingPublicSendMail =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(sharingPublicSendMail)!!
        filesSharingPublicUpload =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(sharingPublicUpload)!!
        filesSharingPublicMultiple =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(sharingPublicMultiple)!!
        filesSharingPublicSupportsUploadOnly =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(sharingPublicSupportsUploadOnly)!!
        filesSharingUserSendMail =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(sharingUserSendMail)!!
        filesSharingResharing =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(sharingResharing)!!
        filesSharingFederationOutgoing =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(sharingFederationOutgoing)!!
        filesSharingFederationIncoming =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(sharingFederationIncoming)!!
        this.filesBigFileChunking =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(filesBigFileChunking)!!
        this.filesUndelete = com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(filesUndelete)!!
        this.filesVersioning =
            com.owncloud.android.lib.resources.status.CapabilityBooleanType.fromValue(filesVersioning)!!
    }

    fun <T> createRemoteOperationResultMock(
        data: T,
        isSuccess: Boolean,
        httpPhrase: String? = null,
        resultCode: RemoteOperationResult.ResultCode? = null,
        exception: Exception? = null
    ): RemoteOperationResult<T> {
        val remoteOperationResult = mockk<RemoteOperationResult<T>>(relaxed = true)

        every {
            remoteOperationResult.data
        } returns data

        every {
            remoteOperationResult.isSuccess
        } returns isSuccess

        if (httpPhrase != null) {
            every {
                remoteOperationResult.httpPhrase
            } returns httpPhrase
        }

        if (resultCode != null) {
            every {
                remoteOperationResult.code
            } returns resultCode
        }

        if (exception != null) {
            every {
                remoteOperationResult.exception
            } returns exception
        }

        return remoteOperationResult
    }

//    fun createFile(name: String = "default") = OCFile("/Photos").apply {
//        availableOfflineStatus = OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
//        fileName = name
//        fileId = 9456985479
//        remoteId = "1"
//        privateLink = "private link"
//    }
}
