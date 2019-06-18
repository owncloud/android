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
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.RemoteCapability
import com.owncloud.android.shares.db.OCShare
import org.mockito.Mockito.`when`

object TestUtil {
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
        name: String,
        shareLink: String
    ) = OCShare(
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
        shareType: Int = 3, // Public share by default
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
        name: String,
        shareLink: String
    ): RemoteShare {
        val remoteShare = RemoteShare();

        remoteShare.fileSource = fileSource
        remoteShare.itemSource = itemSource
        remoteShare.shareType = ShareType.fromValue(shareType)
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
    ) = OCCapability(
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
        filesSharingApiEnabled = CapabilityBooleanType.fromValue(sharingApiEnabled)!!
        filesSharingPublicEnabled = CapabilityBooleanType.fromValue(sharingPublicEnabled)!!
        filesSharingPublicPasswordEnforced = CapabilityBooleanType.fromValue(sharingPublicPasswordEnforced)!!
        filesSharingPublicPasswordEnforcedReadOnly =
            CapabilityBooleanType.fromValue(sharingPublicPasswordEnforcedReadOnly)!!
        filesSharingPublicPasswordEnforcedReadWrite =
            CapabilityBooleanType.fromValue(sharingPublicPasswordEnforcedReadWrite)!!
        filesSharingPublicPasswordEnforcedUploadOnly =
            CapabilityBooleanType.fromValue(sharingPublicPasswordEnforcedUploadOnly)!!
        filesSharingPublicExpireDateEnabled = CapabilityBooleanType.fromValue(sharingPublicExpireDateEnabled)!!
        filesSharingPublicExpireDateDays = sharingPublicExpireDateDays
        filesSharingPublicExpireDateEnforced = CapabilityBooleanType.fromValue(sharingPublicExpireDateEnforced)!!
        filesSharingPublicSendMail = CapabilityBooleanType.fromValue(sharingPublicSendMail)!!
        filesSharingPublicUpload = CapabilityBooleanType.fromValue(sharingPublicUpload)!!
        filesSharingPublicMultiple = CapabilityBooleanType.fromValue(sharingPublicMultiple)!!
        filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.fromValue(sharingPublicSupportsUploadOnly)!!
        filesSharingUserSendMail = CapabilityBooleanType.fromValue(sharingUserSendMail)!!
        filesSharingResharing = CapabilityBooleanType.fromValue(sharingResharing)!!
        filesSharingFederationOutgoing = CapabilityBooleanType.fromValue(sharingFederationOutgoing)!!
        filesSharingFederationIncoming = CapabilityBooleanType.fromValue(sharingFederationIncoming)!!
        this.filesBigFileChunking = CapabilityBooleanType.fromValue(filesBigFileChunking)!!
        this.filesUndelete = CapabilityBooleanType.fromValue(filesUndelete)!!
        this.filesVersioning = CapabilityBooleanType.fromValue(filesVersioning)!!
    }

    fun createAccount(name: String, type: String): Account {
        val account = Account("MyAccount", "SomeType")
        val nameField = account.javaClass.getDeclaredField("name")
        nameField.isAccessible = true
        nameField.set(account, name)

        val typeField = account.javaClass.getDeclaredField("type")
        typeField.isAccessible = true
        typeField.set(account, type)

        return account
    }

    fun <T> createRemoteOperationResultMock(
        data: T,
        isSuccess: Boolean,
        httpPhrase: String? = null,
        resultCode: RemoteOperationResult.ResultCode? = null,
        exception: Exception? = null
    ): RemoteOperationResult<T> {
        val remoteOperationResult = mock<RemoteOperationResult<T>>()

        `when`(remoteOperationResult.data).thenReturn(data)
        `when`(remoteOperationResult.isSuccess).thenReturn(isSuccess)

        if (httpPhrase != null) {
            `when`(remoteOperationResult.httpPhrase).thenReturn(httpPhrase)
        }

        if (resultCode != null) {
            `when`(remoteOperationResult.code).thenReturn(resultCode)
        }

        if (exception != null) {
            `when`(remoteOperationResult.exception).thenReturn(exception)
        }

        return remoteOperationResult
    }
}
