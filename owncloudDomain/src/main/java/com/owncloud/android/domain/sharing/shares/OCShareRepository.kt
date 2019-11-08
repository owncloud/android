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

package com.owncloud.android.domain.sharing.shares

import androidx.lifecycle.LiveData
import com.owncloud.android.data.sharing.shares.ShareRepository
import com.owncloud.android.data.sharing.shares.datasources.LocalShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.RemoteShareDataSource
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.ShareType

class OCShareRepository(
    private val localShareDataSource: LocalShareDataSource,
    private val remoteShareDataSource: RemoteShareDataSource
) : ShareRepository {

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    override fun getPrivateSharesAsLiveData(filePath: String, accountName: String): LiveData<List<OCShareEntity>> {
        return localShareDataSource.getSharesAsLiveData(
            filePath, accountName, listOf(
                ShareType.USER,
                ShareType.GROUP,
                ShareType.FEDERATED
            )
        )
    }

    override suspend fun insertPrivateShare(
        filePath: String,
        shareType: ShareType?,
        shareeName: String,     // User or group name of the target sharee.
        permissions: Int,        // See https://doc.owncloud.com/server/developer_manual/core/apis/ocs-share-api.html
        accountName: String
    ) {
        if (!(shareType == ShareType.USER || shareType == ShareType.GROUP || shareType == ShareType.FEDERATED)) {
            throw IllegalArgumentException("Illegal share type $shareType");
        }

        insertShare(
            filePath = filePath,
            shareType = shareType,
            shareWith = shareeName,
            permissions = permissions,
            accountName = accountName
        )
    }

    override suspend fun updatePrivateShare(remoteId: Long, permissions: Int, accountName: String) {
        return updateShare(
            remoteId = remoteId,
            permissions = permissions,
            accountName = accountName
        )
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    override suspend fun insertPublicShare(
        filePath: String,
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        publicUpload: Boolean,
        accountName: String
    ) {
        insertShare(
            filePath = filePath,
            shareType = ShareType.PUBLIC_LINK,
            permissions = permissions,
            name = name,
            password = password,
            expirationTimeInMillis = expirationTimeInMillis,
            publicUpload = publicUpload,
            accountName = accountName
        )
    }

    override suspend fun updatePublicShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean,
        accountName: String
    ) {
        return updateShare(
            remoteId,
            permissions,
            name,
            password,
            expirationDateInMillis,
            publicUpload,
            accountName
        )
    }

    override suspend fun deleteShare(remoteId: Long) {
        remoteShareDataSource.deleteShare(remoteId)
        localShareDataSource.deleteShare(remoteId)
    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    override fun getSharesAsLiveData(filePath: String, accountName: String): LiveData<List<OCShareEntity>> {
        return localShareDataSource.getSharesAsLiveData(
            filePath, accountName, listOf(
                ShareType.PUBLIC_LINK,
                ShareType.USER,
                ShareType.GROUP,
                ShareType.FEDERATED
            )
        )
    }

    override fun getShareAsLiveData(remoteId: Long): LiveData<OCShareEntity> =
        localShareDataSource.getShareAsLiveData(remoteId)

    override suspend fun refreshSharesFromNetwork(
        filePath: String,
        accountName: String
    ) {
        remoteShareDataSource.getShares(
            filePath,
            reshares = true,
            subfiles = false,
            accountName = accountName
        ).also { remoteShares ->
            if (remoteShares.isEmpty()) {
                localShareDataSource.deleteSharesForFile(filePath, accountName)
            }
            // Save shares
            localShareDataSource.replaceShares(remoteShares)
        }
    }

    private suspend fun insertShare(
        filePath: String,
        shareType: ShareType,
        shareWith: String = "",
        permissions: Int,
        name: String = "",
        password: String = "",
        expirationTimeInMillis: Long = RemoteShare.INIT_EXPIRATION_DATE_IN_MILLIS,
        publicUpload: Boolean = false,
        accountName: String
    ) {
        remoteShareDataSource.insertShare(
            filePath,
            shareType,
            shareWith,
            permissions,
            name,
            password,
            expirationTimeInMillis,
            publicUpload,
            accountName
        ).also { remotelyInsertedShare ->
            localShareDataSource.insert(remotelyInsertedShare)
        }
    }

    private suspend fun updateShare(
        remoteId: Long,
        permissions: Int,
        name: String = "",
        password: String? = "",
        expirationDateInMillis: Long = RemoteShare.INIT_EXPIRATION_DATE_IN_MILLIS,
        publicUpload: Boolean = false,
        accountName: String
    ) {
        remoteShareDataSource.updateShare(
            remoteId,
            name,
            password,
            expirationDateInMillis,
            permissions,
            publicUpload,
            accountName
        ).also { remotelyUpdatedShare ->
            localShareDataSource.update(remotelyUpdatedShare)
        }
    }
}
