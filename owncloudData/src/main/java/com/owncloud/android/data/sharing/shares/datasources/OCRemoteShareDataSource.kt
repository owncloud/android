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

import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.awaitToRemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation
import com.owncloud.android.lib.resources.shares.RemoveRemoteShareOperation
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation

class OCRemoteShareDataSource(
    private val client: OwnCloudClient
) : RemoteShareDataSource {

    override fun getShares(
        remoteFilePath: String,
        reshares: Boolean,
        subfiles: Boolean,
        getRemoteSharesForFileOperation: GetRemoteSharesForFileOperation
    ): RemoteOperationResult<ShareParserResult> {
        return getRemoteSharesForFileOperation.execute(client)
    }

    override suspend fun insertShare(
        remoteFilePath: String,
        shareType: ShareType,
        shareWith: String,
        permissions: Int,
        name: String,
        password: String,
        expirationDate: Long,
        publicUpload: Boolean,
        accountName: String,
        createRemoteShareOperation: CreateRemoteShareOperation
    ) : OCShareEntity {
        createRemoteShareOperation.name = name
        createRemoteShareOperation.password = password
        createRemoteShareOperation.expirationDateInMillis = expirationDate
        createRemoteShareOperation.publicUpload = publicUpload
        createRemoteShareOperation.retrieveShareDetails = true

        awaitToRemoteOperationResult {
            createRemoteShareOperation.execute(client)
        }.shares.run{
            return OCShareEntity.fromRemoteShare(this.first())
        }
    }

    override fun updateShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean,
        updateRemoteShareOperation: UpdateRemoteShareOperation
    ): RemoteOperationResult<ShareParserResult> {
        updateRemoteShareOperation.name = name
        updateRemoteShareOperation.password = password
        updateRemoteShareOperation.expirationDateInMillis = expirationDateInMillis
        updateRemoteShareOperation.permissions = permissions
        updateRemoteShareOperation.publicUpload = publicUpload
        updateRemoteShareOperation.retrieveShareDetails = true
        return updateRemoteShareOperation.execute(client)
    }

    override fun deleteShare(
        remoteId: Long,
        removeRemoteShareOperation: RemoveRemoteShareOperation
    ): RemoteOperationResult<ShareParserResult> {
        return removeRemoteShareOperation.execute(client)
    }
}
