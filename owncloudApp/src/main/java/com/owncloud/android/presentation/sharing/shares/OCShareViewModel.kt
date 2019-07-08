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

package com.owncloud.android.presentation.sharing.shares

import android.accounts.Account
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.data.Executors
import com.owncloud.android.data.Resource
import com.owncloud.android.data.sharing.shares.datasources.OCLocalSharesDataSource
import com.owncloud.android.data.sharing.shares.datasources.OCRemoteSharesDataSource
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.domain.sharing.shares.OCShareRepository
import com.owncloud.android.domain.sharing.shares.usecases.GetPublicSharesUsecase
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.resources.shares.ShareType

/**
 * View Model to keep a reference to the share repository and an up-to-date list of a shares
 */
class OCShareViewModel(
    private val getPublicSharesUseCase: GetPublicSharesUsecase,
    private val executors: Executors = Executors()
) : ViewModel() {

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/
    fun getPrivateShares(filePath: String): LiveData<Resource<List<OCShareEntity>>> {
//        return shareRepository.getPrivateShares(filePath)
        return MutableLiveData()
    }

//    fun insertPrivateShare(
//        filePath: String,
//        shareType: ShareType?,
//        shareeName: String, // User or group name of the target sharee.
//        permissions: Int
//    ): LiveData<Resource<Unit>> = shareRepository.insertPrivateShare(
//        filePath, shareType, shareeName, permissions
//    )

    fun updatePrivateShare(
        remoteId: Long,
        permissions: Int
    ): LiveData<Resource<Unit>> = shareRepository.updatePrivateShare(
        remoteId, permissions
    )

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/
    fun getPublicShares(filePath: String): LiveData<Resource<List<OCShareEntity>>> {
        val result = executors.diskIO().execute {
            getPublicSharesUseCase.execute()
        }
//        return shareRepository.getPublicShares(filePath)
        return MutableLiveData()
    }

//    fun insertPublicShare(
//        filePath: String,
//        permissions: Int,
//        name: String,
//        password: String,
//        expirationTimeInMillis: Long,
//        publicUpload: Boolean
//    ): LiveData<Resource<Unit>> = shareRepository.insertPublicShare(
//        filePath, permissions, name, password, expirationTimeInMillis, publicUpload
//    )
//
//    fun updatePublicShareForFile(
//        remoteId: Long,
//        name: String,
//        password: String?,
//        expirationDateInMillis: Long,
//        permissions: Int,
//        publicUpload: Boolean
//    ): LiveData<Resource<Unit>> = shareRepository.updatePublicShare(
//        remoteId, name, password, expirationDateInMillis, permissions, publicUpload
//    )
//
//    fun deletePublicShare(
//        remoteId: Long
//    ): LiveData<Resource<Unit>> = shareRepository.deletePublicShare(remoteId)
}
