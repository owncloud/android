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
import androidx.lifecycle.ViewModel
import com.owncloud.android.data.common.Resource
import com.owncloud.android.data.sharing.shares.datasources.OCLocalSharesDataSource
import com.owncloud.android.data.sharing.shares.datasources.OCRemoteSharesDataSource
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.domain.sharing.shares.OCShareRepository
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.data.ShareRepository
import com.owncloud.android.shares.data.datasources.OCLocalShareDataSource
import com.owncloud.android.shares.data.datasources.OCRemoteShareDataSource
import com.owncloud.android.shares.domain.OCShare
import com.owncloud.android.shares.domain.OCShareRepository
import com.owncloud.android.testing.OpenForTesting

/**
 * View Model to keep a reference to the share repository and an up-to-date list of a shares
 */
@OpenForTesting
class OCShareViewModel(
    context: Context,
    account: Account,
    val shareRepository: ShareRepository = OCShareRepository(
        localShareDataSource = OCLocalShareDataSource(context),
        remoteShareDataSource = OCRemoteShareDataSource(
            OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(
                OwnCloudAccount(account, context),
                context
            )
        ),
        accountName = account.name
    )
) : ViewModel() {

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/
    fun getPrivateShares(filePath: String): LiveData<Resource<List<OCShareEntity>>> {
        return shareRepository.getPrivateShares(filePath)
    }

    fun getPrivateShare(remoteId: Long): LiveData<OCShare> = shareRepository.getShare(remoteId)

    fun insertPrivateShare(
        filePath: String,
        shareType: ShareType?,
        shareeName: String, // User or group name of the target sharee.
        permissions: Int
    ): LiveData<Resource<Unit>> = shareRepository.insertPrivateShare(
        filePath, shareType, shareeName, permissions
    )

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
        return shareRepository.getPublicShares(filePath)
    }

    fun insertPublicShare(
        filePath: String,
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        publicUpload: Boolean
    ): LiveData<Resource<Unit>> = shareRepository.insertPublicShare(
        filePath, permissions, name, password, expirationTimeInMillis, publicUpload
    )

    fun updatePublicShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean
    ): LiveData<Resource<Unit>> = shareRepository.updatePublicShare(
        remoteId, name, password, expirationDateInMillis, permissions, publicUpload
    )

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    fun deleteShare(
        remoteId: Long
    ): LiveData<Resource<Unit>> = shareRepository.deleteShare(remoteId)
}
