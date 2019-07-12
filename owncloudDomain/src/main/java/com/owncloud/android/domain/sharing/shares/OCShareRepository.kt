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
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.data.DataResult
import com.owncloud.android.data.sharing.shares.ShareRepository
import com.owncloud.android.data.sharing.shares.datasources.LocalShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.RemoteShareDataSource
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType

class OCShareRepository(
    private val localSharesDataSource: com.owncloud.android.data.sharing.shares.datasources.LocalSharesDataSource,
    private val remoteSharesDataSource: com.owncloud.android.data.sharing.shares.datasources.RemoteSharesDataSource
) : ShareRepository {

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    override fun insertPrivateShare(
        filePath: String,
        shareType: ShareType?,
        shareeName: String,     // User or group name of the target sharee.
        permissions: Int        // See https://doc.owncloud.com/server/developer_manual/core/apis/ocs-share-api.html
    ): LiveData<DataResult<Unit>> {
        if (shareType != ShareType.USER && shareType != ShareType.GROUP && shareType != ShareType.FEDERATED) {
            throw IllegalArgumentException("Illegal share type $shareType");
        }

//        val result = MutableLiveData<DataResult<Unit>>()
//        result.postValue(DataResult.loading())
//
//        executors.networkIO().execute {
//            // Perform network operation
//            val remoteOperationResult = remoteSharesDataSource.insertShare(
//                filePath,
//                shareType,
//                shareeName,
//                permissions
//            )
//
//            if (remoteOperationResult.isSuccess) {
//                val newPrivateShareFromServer = remoteOperationResult.data.shares.map { remoteShare ->
//                    OCShareEntity.fromRemoteShare(remoteShare)
//                        .also { it.accountOwner = accountName }
//                }
//                localSharesDataSource.insert(newPrivateShareFromServer)
//            } else {
//                notifyError(result, remoteOperationResult)
//            }
//        }
        return MutableLiveData()
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    override fun insertPublicShare(
        filePath: String,
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        publicUpload: Boolean,
        accountName: String
    ): DataResult<Unit> {
        remoteSharesDataSource.insertShare(
            filePath,
            ShareType.PUBLIC_LINK,
            "",
            permissions,
            name,
            password,
            expirationTimeInMillis,
            publicUpload
        ).also { remoteOperationResult ->
            // Error
            if (!remoteOperationResult.isSuccess) {
                return DataResult.error(
                    code = remoteOperationResult.code,
                    msg = remoteOperationResult.httpPhrase,
                    exception = remoteOperationResult.exception
                )
            }

            // Success
            val newPublicShareFromServer = remoteOperationResult.data.shares.map { remoteShare ->
                OCShareEntity.fromRemoteShare(remoteShare)
                    .also { it.accountOwner = accountName }
            }

            localSharesDataSource.insert(newPublicShareFromServer)

            return DataResult.success()
        }
    }

    private fun updateShare(
        remoteId: Long,
        permissions: Int,
        publicUpload: Boolean
    ): LiveData<DataResult<Unit>> {
        val result = MutableLiveData<DataResult<Unit>>()
        result.postValue(DataResult.loading())

//        executors.networkIO().execute {
//            // Perform network operation
//            val remoteOperationResult = remoteSharesDataSource.updateShare(
//                remoteId,
//                name,
//                password,
//                expirationDateInMillis,
//                permissions,
//                publicUpload
//            )
//
//            if (remoteOperationResult.isSuccess) {
//                val updatedShareForFileFromServer = remoteOperationResult.data.shares.map { remoteShare ->
//                    OCShareEntity.fromRemoteShare(remoteShare)
//                        .also { it.accountOwner = accountName }
//                }
//                localSharesDataSource.update(updatedShareForFileFromServer.first())
//                result.postValue(DataResult.success()) // Used to close the share edition dialog
//            } else {
//                notifyError(result, remoteOperationResult)
//            }
//        }
        return result
    }

    override fun deleteShare(
        remoteId: Long
    ): LiveData<DataResult<Unit>> {
        val result = MutableLiveData<DataResult<Unit>>()

        result.postValue(DataResult.loading())

        // Perform network operation
//        executors.networkIO().execute {
//            // Perform network operation
//            val remoteOperationResult = remoteSharesDataSource.deleteShare(remoteId)
//
//            if (remoteOperationResult.isSuccess) {
//                localSharesDataSource.deleteShare(remoteId)
//                result.postValue(DataResult.success()) // Used to close the share edition dialog
//            } else {
//                result.postValue(
//                    DataResult.error(
//                        remoteOperationResult.code,
//                        msg = remoteOperationResult.httpPhrase,
//                        exception = remoteOperationResult.exception
//                    )
//                )
//            }
//        }
        return result
    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    override fun getSharesAsLiveData(filePath: String, accountName: String): LiveData<List<OCShareEntity>> {
        return localSharesDataSource.getSharesAsLiveData(
            filePath, accountName, listOf(
                ShareType.PUBLIC_LINK,
                ShareType.USER,
                ShareType.GROUP,
                ShareType.FEDERATED
            )
        )
    }

    override fun refreshShares(
        filePath: String,
        accountName: String
    ): DataResult<Unit> {
        remoteSharesDataSource.getShares(
            filePath,
            reshares = true,
            subfiles = false
        ).also { remoteOperationResult ->
            // Error
            if (!remoteOperationResult.isSuccess) {
                return DataResult.error(
                    code = remoteOperationResult.code,
                    msg = remoteOperationResult.httpPhrase,
                    exception = remoteOperationResult.exception
                )
            }

            // Success
            val sharesForFileFromServer = remoteOperationResult.data.shares.map { remoteShare ->
                OCShareEntity.fromRemoteShare(remoteShare)
                    .also { it.accountOwner = accountName }
            }

            if (sharesForFileFromServer.isEmpty()) {
                localSharesDataSource.deleteSharesForFile(filePath, accountName)
            }

            // Save shares
            localSharesDataSource.replaceShares(sharesForFileFromServer)

            return DataResult.success()
        }
    }

    /**
     * Notify error in the given LiveData
     *
     * @param result liveData in which notify the error
     * @param remoteOperationResult contains the information of the error
     */
    private fun notifyError(
        result: MutableLiveData<DataResult<Unit>> = MutableLiveData(),
        remoteOperationResult: RemoteOperationResult<ShareParserResult>
    ) {
        result.postValue(
            DataResult.error(
                remoteOperationResult.code,
                msg = remoteOperationResult.httpPhrase,
                exception = remoteOperationResult.exception
            )
        )
    }
}
