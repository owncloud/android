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
import android.hardware.SensorAdditionalInfo
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.db.OCShare

object TestUtil {
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
            path: String,
            isFolder: Boolean,
            accountOwner: String = "admin@server",
            name: String,
            shareLink: String
    ) = createShare(
            shareType = 3,
            path = path,
            isFolder = isFolder,
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
        remoteShare.remoteId = remoteId
        remoteShare.name = name
        remoteShare.shareLink = shareLink

        return remoteShare
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
}
