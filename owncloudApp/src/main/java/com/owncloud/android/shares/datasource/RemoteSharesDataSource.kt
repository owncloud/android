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

package com.owncloud.android.shares.datasource

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation

interface RemoteSharesDataSource {
    fun getSharesForFile(
        remoteFilePath: String,
        reshares: Boolean,
        subfiles: Boolean,
        getRemoteSharesForFileOperation: GetRemoteSharesForFileOperation =
            GetRemoteSharesForFileOperation(remoteFilePath, reshares, subfiles)
    ): RemoteOperationResult<ShareParserResult>

    fun insertShareForFile(
        remoteFilePath: String,
        shareType: ShareType,
        shareWith: String,
        permissions: Int,
        name: String,
        password: String,
        expirationDate: Long,
        publicUpload: Boolean,
        createRemoteShareOperation: CreateRemoteShareOperation =
            CreateRemoteShareOperation(remoteFilePath, shareType, shareWith, permissions)
    ): RemoteOperationResult<ShareParserResult>

    fun updateShareForFile(
        remoteId: Long,
        password: String,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean,
        name: String,
        updateRemoteShareOperation: UpdateRemoteShareOperation =
            UpdateRemoteShareOperation(
                remoteId
            )
    ): RemoteOperationResult<ShareParserResult>
}
