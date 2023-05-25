/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
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

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.sharing.shares.datasources.RemoteShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.mapper.RemoteShareMapper
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType

class OCRemoteShareDataSource(
    private val clientManager: ClientManager,
    private val remoteShareMapper: RemoteShareMapper
) : RemoteShareDataSource {

    override fun getShares(
        remoteFilePath: String,
        reshares: Boolean,
        subfiles: Boolean,
        accountName: String
    ): List<OCShare> {
        executeRemoteOperation {
            clientManager.getShareService(accountName).getShares(remoteFilePath, reshares, subfiles)
        }.let {
            return it.shares.map { remoteShare ->
                remoteShareMapper.toModel(remoteShare)!!.apply {
                    accountOwner = accountName
                }
            }
        }
    }

    override fun insert(
        remoteFilePath: String,
        shareType: ShareType,
        shareWith: String,
        permissions: Int,
        name: String,
        password: String,
        expirationDate: Long,
        accountName: String
    ): OCShare {
        executeRemoteOperation {
            clientManager.getShareService(accountName).insertShare(
                remoteFilePath,
                com.owncloud.android.lib.resources.shares.ShareType.fromValue(shareType.value)!!,
                shareWith,
                permissions,
                name,
                password,
                expirationDate,
            )
        }.let {
            return remoteShareMapper.toModel(it.shares.first())!!.apply {
                accountOwner = accountName
            }
        }
    }

    override fun updateShare(
        remoteId: String,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        accountName: String
    ): OCShare {
        executeRemoteOperation {
            clientManager.getShareService(accountName).updateShare(
                remoteId,
                name,
                password,
                expirationDateInMillis,
                permissions,
            )
        }.let {
            return remoteShareMapper.toModel(it.shares.first())!!.apply {
                accountOwner = accountName
            }
        }
    }

    override fun deleteShare(remoteId: String, accountName: String) {
        executeRemoteOperation {
            clientManager.getShareService(accountName).deleteShare(remoteId)
        }
    }
}
