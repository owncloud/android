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
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.vo.Resource

interface ShareRepository {
    fun loadSharesForFile(
        filePath: String,
        accountName: String,
        shareTypes: List<ShareType>,
        reshares: Boolean,
        subfiles: Boolean
    ): LiveData<Resource<List<OCShare>>>

    fun insertPublicShareForFile(
        filePath: String,
        accountName: String,
        permissions: Int,
        name: String,
        password: String,
        expirationTimeInMillis: Long,
        uploadToFolderPermission: Boolean
    ): LiveData<Resource<List<OCShare>>>

    fun updatePublicShareForFile(
        filePath: String,
        accountName: String,
        remoteId: Long,
        name: String,
        password: String?,
        expirationDateInMillis: Long,
        permissions: Int,
        publicUpload: Boolean
    ): LiveData<Resource<List<OCShare>>>
}
