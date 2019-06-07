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

package com.owncloud.android.shares.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.AppExecutors
import com.owncloud.android.NetworkBoundResource
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.datasource.LocalSharesDataSource
import com.owncloud.android.shares.datasource.RemoteSharesDataSource
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.vo.Resource

class OCShareRepository(
    private val appExecutors: AppExecutors = AppExecutors(),
    private val localSharesDataSource: LocalSharesDataSource,
    private val remoteSharesDataSource: RemoteSharesDataSource,
    val filePath: String,
    val accountName: String,
    val shareTypes: List<ShareType>
) : ShareRepository {
    private val sharesForFile: MutableLiveData<Resource<List<OCShare>>> =
        object : NetworkBoundResource<List<OCShare>, ShareParserResult>(appExecutors) {
            override fun saveCallResult(item: ShareParserResult) {
                val sharesForFileFromServer = item.shares.map { remoteShare ->
                    OCShare.fromRemoteShare(remoteShare).also { it.accountOwner = accountName }
                }

                if (sharesForFileFromServer.isEmpty()) {
                    localSharesDataSource.deleteSharesForFile(filePath, accountName)
                }

                localSharesDataSource.replaceSharesForFile(sharesForFileFromServer)
            }

            override fun shouldFetchFromNetwork(data: List<OCShare>?) = true

            override fun loadFromDb(): LiveData<List<OCShare>> =
                localSharesDataSource.getSharesForFileAsLiveData(filePath, accountName, shareTypes)

            override fun createCall() =
                remoteSharesDataSource.getSharesForFile(filePath, reshares = true, subfiles = false)
        }.asMutableLiveData()

    override fun getSharesForFile(): LiveData<Resource<List<OCShare>>> {
        return sharesForFile
    }

    override fun insertPublicShareForFile(
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        publicUpload: Boolean
    ): LiveData<Resource<Void>> {
        val result = MutableLiveData<Resource<Void>>()
        result.postValue(Resource.loading())

        appExecutors.networkIO().execute() {
            // Perform network operation
            val remoteOperationResult = remoteSharesDataSource.insertShareForFile(
                filePath,
                ShareType.PUBLIC_LINK,
                "",
                permissions,
                name,
                password,
                expirationTimeInMillis,
                publicUpload
            )

            if (remoteOperationResult.isSuccess) {
                val newShareForFileFromServer = remoteOperationResult.data.shares.map { remoteShare ->
                    OCShare.fromRemoteShare(remoteShare).also { it.accountOwner = accountName }
                }
                localSharesDataSource.insert(newShareForFileFromServer)
                result.postValue(Resource.success()) // Used to close the share creation dialog
            } else {
                result.postValue(
                    Resource.error(
                        remoteOperationResult.code,
                        msg = remoteOperationResult.httpPhrase,
                        exception = remoteOperationResult.exception
                    )
                )
            }
        }
        return result
    }

    override fun updatePublicShareForFile(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean
    ): LiveData<Resource<Void>> {
        val result = MutableLiveData<Resource<Void>>()
        result.postValue(Resource.loading())

        appExecutors.networkIO().execute() {
            // Perform network operation
            val remoteOperationResult = remoteSharesDataSource.updateShareForFile(
                remoteId,
                name,
                password,
                expirationDateInMillis,
                permissions,
                publicUpload
            )

            if (remoteOperationResult.isSuccess) {
                val updatedShareForFileFromServer = remoteOperationResult.data.shares.map { remoteShare ->
                    OCShare.fromRemoteShare(remoteShare).also { it.accountOwner = accountName }
                }
                localSharesDataSource.update(updatedShareForFileFromServer.first())
                result.postValue(Resource.success()) // Used to close the share edition dialog
            } else {
                result.postValue(
                    Resource.error(
                        remoteOperationResult.code,
                        msg = remoteOperationResult.httpPhrase,
                        exception = remoteOperationResult.exception
                    )
                )
            }
        }
        return result
    }

    override fun deletePublicShare(
        remoteId: Long
    ): LiveData<Resource<Void>> {
        val result = MutableLiveData<Resource<Void>>()

        result.postValue(Resource.loading())

        // Perform network operation
        appExecutors.networkIO().execute() {
            // Perform network operation
            val remoteOperationResult = remoteSharesDataSource.deleteShare(remoteId)

            if (remoteOperationResult.isSuccess) {
                localSharesDataSource.deleteShare(remoteId)
                result.postValue(Resource.success()) // Used to close the share edition dialog
            } else {
                result.postValue(
                    Resource.error(
                        remoteOperationResult.code,
                        msg = remoteOperationResult.httpPhrase,
                        exception = remoteOperationResult.exception
                    )
                )
            }
        }
        return result
    }
}
