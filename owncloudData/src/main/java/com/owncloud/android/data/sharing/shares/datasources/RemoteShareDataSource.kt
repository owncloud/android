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

package com.owncloud.android.data.sharing.shares.datasources

import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation
import com.owncloud.android.lib.resources.shares.RemoteShare.Companion.INIT_EXPIRATION_DATE_IN_MILLIS
import com.owncloud.android.lib.resources.shares.RemoveRemoteShareOperation
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation

interface RemoteShareDataSource {
    fun getShares(
        remoteFilePath: String,
        reshares: Boolean,
        subfiles: Boolean,
        accountName: String,
        getRemoteSharesForFileOperation: GetRemoteSharesForFileOperation =
            GetRemoteSharesForFileOperation(remoteFilePath, reshares, subfiles)
    ): List<OCShare>

    fun insertShare(
        remoteFilePath: String,
        shareType: ShareType,
        shareWith: String,
        permissions: Int,
        name: String = "",
        password: String = "",
        expirationDate: Long = INIT_EXPIRATION_DATE_IN_MILLIS,
        publicUpload: Boolean = false,
        accountName: String,
        createRemoteShareOperation: CreateRemoteShareOperation =
            CreateRemoteShareOperation(
                remoteFilePath,
                com.owncloud.android.lib.resources.shares.ShareType.fromValue(shareType.value)!!,
                shareWith,
                permissions
            )
    ): OCShare

    fun updateShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean,
        accountName: String,
        updateRemoteShareOperation: UpdateRemoteShareOperation =
            UpdateRemoteShareOperation(
                remoteId
            )
    ): OCShare

    fun deleteShare(
        remoteId: Long,
        removeRemoteShareOperation: RemoveRemoteShareOperation =
            RemoveRemoteShareOperation(
                remoteId
            )
    ): RemoteOperationResult<ShareParserResult>
}
