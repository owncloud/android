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

package com.owncloud.android.data.sharing.shares.datasources.implementation

import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.sharing.shares.datasources.RemoteShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.mapper.RemoteShareMapper
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation
import com.owncloud.android.lib.resources.shares.RemoveRemoteShareOperation
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation

class OCRemoteShareDataSource(
    private val client: OwnCloudClient,
    private val remoteShareMapper: RemoteShareMapper
) : RemoteShareDataSource {

    override fun getShares(
        remoteFilePath: String,
        reshares: Boolean,
        subfiles: Boolean,
        accountName: String,
        getRemoteSharesForFileOperation: GetRemoteSharesForFileOperation
    ): List<OCShare> {
        executeRemoteOperation(
            getRemoteSharesForFileOperation, client
        ).let {
            return it.shares.map { remoteShare ->
                remoteShareMapper.toModel(remoteShare)!!.also {
                    it.accountOwner = accountName
                }
            }
        }
    }

    override fun insertShare(
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
    ): OCShare {
        createRemoteShareOperation.name = name
        createRemoteShareOperation.password = password
        createRemoteShareOperation.expirationDateInMillis = expirationDate
        createRemoteShareOperation.publicUpload = publicUpload
        createRemoteShareOperation.retrieveShareDetails = true

        executeRemoteOperation(
            createRemoteShareOperation, client
        ).let {
            return remoteShareMapper.toModel(it.shares.first())!!.also {
                it.accountOwner = accountName
            }
        }
    }

    override fun updateShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean,
        accountName: String,
        updateRemoteShareOperation: UpdateRemoteShareOperation
    ): OCShare {
        updateRemoteShareOperation.name = name
        updateRemoteShareOperation.password = password
        updateRemoteShareOperation.expirationDateInMillis = expirationDateInMillis
        updateRemoteShareOperation.permissions = permissions
        updateRemoteShareOperation.publicUpload = publicUpload
        updateRemoteShareOperation.retrieveShareDetails = true

        executeRemoteOperation(
            updateRemoteShareOperation, client
        ).shares.let {
            return remoteShareMapper.toModel(it.first())!!.also {
                it.accountOwner = accountName
            }
        }
    }

    override fun deleteShare(remoteId: Long, removeRemoteShareOperation: RemoveRemoteShareOperation) {
        executeRemoteOperation(
            removeRemoteShareOperation, client
        )
    }
}
