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

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation

class OCRemoteSharesDataSource(
    private val client: OwnCloudClient
) : RemoteSharesDataSource {
    override fun getSharesForFile(
        remoteFilePath: String,
        reshares: Boolean,
        subfiles: Boolean,
        getRemoteSharesForFileOperation: GetRemoteSharesForFileOperation
    ): RemoteOperationResult<ShareParserResult> {
        return getRemoteSharesForFileOperation.execute(client)
    }

    override fun insertShareForFile(
        remoteFilePath: String,
        shareType: ShareType,
        shareWith: String,
        permissions: Int,
        name: String,
        password: String,
        expirationDate: Long,
        publicUpload: Boolean,
        createRemoteShareOperation: CreateRemoteShareOperation
    ): RemoteOperationResult<ShareParserResult> {
        createRemoteShareOperation.name = name
        createRemoteShareOperation.password = password
        createRemoteShareOperation.expirationDateInMillis = expirationDate
        createRemoteShareOperation.publicUpload = publicUpload
        createRemoteShareOperation.retrieveShareDetails = true
        return createRemoteShareOperation.execute(client)
    }

    override fun updateShareForFile(
        remoteId: Long,
        password: String,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean,
        name: String,
        updateRemoteShareOperation: UpdateRemoteShareOperation
    ): RemoteOperationResult<ShareParserResult> {
        updateRemoteShareOperation.password = password
        updateRemoteShareOperation.expirationDateInMillis = expirationDateInMillis
        updateRemoteShareOperation.permissions = permissions
        updateRemoteShareOperation.publicUpload = publicUpload
        updateRemoteShareOperation.name = name
        return updateRemoteShareOperation.execute(client)
    }
}
