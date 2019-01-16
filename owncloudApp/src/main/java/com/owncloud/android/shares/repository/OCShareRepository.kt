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

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.owncloud.android.Resource
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.datasources.LocalSharesDataSource
import com.owncloud.android.shares.datasources.RemoteSharesDataSource
import com.owncloud.android.shares.db.OCShare

class OCShareRepository(
    private val localSharesDataSource: LocalSharesDataSource,
    private val remoteShareSharesDataSource: RemoteSharesDataSource
) : ShareRepository {

    private val _errors = MutableLiveData<Resource<List<OCShare>>>()
    val errors: LiveData<Resource<List<OCShare>>>
        get() = _errors

    override fun loadSharesForFile(
        filePath: String,
        accountName: String,
        shareTypes: List<ShareType>,
        reshares: Boolean,
        subfiles: Boolean
    ) {
        try {
            remoteShareSharesDataSource.getSharesForFile(filePath, reshares, subfiles)
                .also { remoteOperationResult ->
                    if (remoteOperationResult.isSuccess) {
                        val localShares = remoteOperationResult.data.shares.map { remoteShare ->
                            val localShare = OCShare(remoteShare)
                            localShare.accountOwner = accountName
                            localShare
                        }
                        localSharesDataSource.insert(localShares)
                    } else {
                        _errors.postValue(
                            Resource.error(
                                remoteOperationResult.logMessage,
                                localSharesDataSource.getSharesForFile(filePath, accountName, shareTypes)
                            )
                        )
                    }
                }
        } catch (ex: Exception) {
            _errors.postValue(
                Resource.error(
                    ex.localizedMessage,
                    localSharesDataSource.getSharesForFile(filePath, accountName, shareTypes)
                )
            )
        }
    }

    override fun getSharesForFileAsLiveData(
        filePath: String,
        accountName: String,
        shareTypes: List<ShareType>
    ): LiveData<List<OCShare>> =
        localSharesDataSource.getSharesForFileAsLiveData(filePath, accountName, shareTypes)
}
