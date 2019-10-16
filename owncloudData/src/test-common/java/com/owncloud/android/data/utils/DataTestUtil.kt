/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
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
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.GetRemoteShareesOperation
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.status.CapabilityBooleanType as RemoteCapabilityBooleanType
import com.owncloud.android.lib.resources.status.RemoteCapability
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject

object DataTestUtil {
    /**
     * Shares
     */
    fun createShare(
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

    fun createShareEntity(
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
    ) = OCShareEntity(
        fileSource,
        itemSource,
        shareType,
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
        shareWith: String = "whoever",
        path: String = "/Photos/image.jpg",
        permissions: Int = -1,
        isFolder: Boolean = false,
        sharedWithDisplayName: String = "whatever",
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

    fun createPrivateShareEntity(
        remoteId: Long = 1,
        shareType: Int = 0,
        shareWith: String,
        path: String,
        permissions: Int = -1,
        isFolder: Boolean,
        sharedWithDisplayName: String,
        accountOwner: String = "admin@server"
    ) = createShareEntity(
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
        path: String = "/Photos/image.jpg",
        expirationDate: Long = 1000,
        isFolder: Boolean = false,
        permissions: Int = 1,
        remoteId: Long = 1,
        accountOwner: String = "admin@server",
        name: String = "Image link",
        shareLink: String = "link"
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

    fun createPublicShareEntity(
        shareWith: String = "",
        path: String,
        expirationDate: Long = 1000,
        isFolder: Boolean,
        permissions: Int = 1,
        remoteId: Long = 1,
        accountOwner: String = "admin@server",
        name: String,
        shareLink: String
    ) = createShareEntity(
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
    ): RemoteShare = RemoteShare().also {
        it.fileSource = fileSource
        it.itemSource = itemSource
        it.shareType = com.owncloud.android.lib.resources.shares.ShareType.fromValue(shareType)
        it.shareWith = shareWith
        it.path = path
        it.permissions = permissions
        it.sharedDate = sharedDate
        it.expirationDate = expirationDate
        it.token = token
        it.sharedWithDisplayName = sharedWithDisplayName
        it.isFolder = isFolder
        it.userId = userId
        it.id = remoteId
        it.name = name
        it.shareLink = shareLink
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

    val DUMMY_REMOTE_CAPABILITY =
        RemoteCapability().apply {
            accountName = "user@server"
            versionMayor = 2
            versionMinor = 1
            versionMicro = 0
            versionString = "1.0.0"
            versionEdition = "1.0.0"
            corePollinterval = 0
            filesSharingApiEnabled = RemoteCapabilityBooleanType.TRUE
            filesSharingPublicEnabled = RemoteCapabilityBooleanType.TRUE
            filesSharingPublicPasswordEnforced = RemoteCapabilityBooleanType.FALSE
            filesSharingPublicPasswordEnforcedReadOnly = RemoteCapabilityBooleanType.FALSE
            filesSharingPublicPasswordEnforcedReadWrite = RemoteCapabilityBooleanType.FALSE
            filesSharingPublicPasswordEnforcedUploadOnly = RemoteCapabilityBooleanType.FALSE
            filesSharingPublicExpireDateEnabled = RemoteCapabilityBooleanType.FALSE
            filesSharingPublicExpireDateDays = 0
            filesSharingPublicExpireDateEnforced = RemoteCapabilityBooleanType.FALSE
            filesSharingPublicSendMail = RemoteCapabilityBooleanType.FALSE
            filesSharingPublicUpload = RemoteCapabilityBooleanType.FALSE
            filesSharingPublicMultiple = RemoteCapabilityBooleanType.FALSE
            filesSharingPublicSupportsUploadOnly = RemoteCapabilityBooleanType.FALSE
            filesSharingUserSendMail = RemoteCapabilityBooleanType.FALSE
            filesSharingResharing = RemoteCapabilityBooleanType.FALSE
            filesSharingFederationOutgoing = RemoteCapabilityBooleanType.FALSE
            filesSharingFederationIncoming = RemoteCapabilityBooleanType.FALSE
            filesBigFileChunking = RemoteCapabilityBooleanType.FALSE
            filesUndelete = RemoteCapabilityBooleanType.FALSE
            filesVersioning = RemoteCapabilityBooleanType.FALSE
        }

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
        filesSharingApiEnabled = RemoteCapabilityBooleanType.fromValue(sharingApiEnabled)!!
        filesSharingPublicEnabled = RemoteCapabilityBooleanType.fromValue(sharingPublicEnabled)!!
        filesSharingPublicPasswordEnforced = RemoteCapabilityBooleanType.fromValue(sharingPublicPasswordEnforced)!!
        filesSharingPublicPasswordEnforcedReadOnly =
            RemoteCapabilityBooleanType.fromValue(sharingPublicPasswordEnforcedReadOnly)!!
        filesSharingPublicPasswordEnforcedReadWrite =
            RemoteCapabilityBooleanType.fromValue(sharingPublicPasswordEnforcedReadWrite)!!
        filesSharingPublicPasswordEnforcedUploadOnly =
            RemoteCapabilityBooleanType.fromValue(sharingPublicPasswordEnforcedUploadOnly)!!
        filesSharingPublicExpireDateEnabled = RemoteCapabilityBooleanType.fromValue(sharingPublicExpireDateEnabled)!!
        filesSharingPublicExpireDateDays = sharingPublicExpireDateDays
        filesSharingPublicExpireDateEnforced = RemoteCapabilityBooleanType.fromValue(sharingPublicExpireDateEnforced)!!
        filesSharingPublicSendMail = RemoteCapabilityBooleanType.fromValue(sharingPublicSendMail)!!
        filesSharingPublicUpload = RemoteCapabilityBooleanType.fromValue(sharingPublicUpload)!!
        filesSharingPublicMultiple = RemoteCapabilityBooleanType.fromValue(sharingPublicMultiple)!!
        filesSharingPublicSupportsUploadOnly = RemoteCapabilityBooleanType.fromValue(sharingPublicSupportsUploadOnly)!!
        filesSharingUserSendMail = RemoteCapabilityBooleanType.fromValue(sharingUserSendMail)!!
        filesSharingResharing = RemoteCapabilityBooleanType.fromValue(sharingResharing)!!
        filesSharingFederationOutgoing = RemoteCapabilityBooleanType.fromValue(sharingFederationOutgoing)!!
        filesSharingFederationIncoming = RemoteCapabilityBooleanType.fromValue(sharingFederationIncoming)!!
        this.filesBigFileChunking = RemoteCapabilityBooleanType.fromValue(filesBigFileChunking)!!
        this.filesUndelete = RemoteCapabilityBooleanType.fromValue(filesUndelete)!!
        this.filesVersioning = RemoteCapabilityBooleanType.fromValue(filesVersioning)!!
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
