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

package com.owncloud.android.shares.data

import androidx.lifecycle.LiveData
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.domain.OCShare
import com.owncloud.android.vo.Resource

interface ShareRepository {

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    fun getPrivateShares(filePath: String): LiveData<Resource<List<OCShare>>>

    fun insertPrivateShare(
        filePath: String,
        shareType: ShareType?,
        shareeName: String,
        permissions: Int
    ): LiveData<Resource<Unit>>

    fun updatePrivateShare(
        remoteId: Long,
        permissions: Int
    ): LiveData<Resource<Unit>>

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    fun getPublicShares(filePath: String): LiveData<Resource<List<OCShare>>>

    fun insertPublicShare(
        filePath: String,
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        publicUpload: Boolean
    ): LiveData<Resource<Unit>>

    fun updatePublicShare(
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean
    ): LiveData<Resource<Unit>>

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    fun deleteShare(
        remoteId: Long
    ): LiveData<Resource<Unit>>

    fun getShare(remoteId: Long): LiveData<OCShare>
}
