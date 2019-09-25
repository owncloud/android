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

package com.owncloud.android.data.sharing.shares

import androidx.lifecycle.LiveData
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.lib.resources.shares.ShareType

interface ShareRepository {

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    fun getPrivateSharesAsLiveData(filePath: String, accountName: String): LiveData<List<OCShareEntity>>

    suspend fun insertPrivateShare(
        filePath: String,
        shareType: ShareType?,
        shareeName: String,
        permissions: Int,
        accountName: String
    )

    fun updatePrivateShare(
        remoteId: Long,
        permissions: Int,
        accountName: String
    )

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    suspend fun insertPublicShare(
        filePath: String,
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        publicUpload: Boolean,
        accountName: String
    )

    fun updatePublicShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean,
        accountName: String
    )

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    fun getSharesAsLiveData(filePath: String, accountName: String): LiveData<List<OCShareEntity>>

    fun getShareAsLiveData(remoteId: Long): LiveData<OCShareEntity>

    suspend fun refreshSharesFromNetwork(filePath: String, accountName: String)

    fun deleteShare(
        remoteId: Long
    )
}
