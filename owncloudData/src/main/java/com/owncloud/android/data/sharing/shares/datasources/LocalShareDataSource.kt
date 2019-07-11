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

package com.owncloud.android.data.sharing.shares.datasources

import androidx.lifecycle.LiveData
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.data.sharing.shares.db.OCShareEntity

interface LocalShareDataSource {
    fun getSharesAsLiveData(
        filePath: String,
        accountName: String,
        shareTypes: List<ShareType>
    ): LiveData<List<OCShareEntity>>

    fun insert(ocShare: OCShareEntity): Long

    fun insert(ocShares: List<OCShareEntity>): List<Long>

    fun update(ocShare: OCShareEntity): Long

    fun replaceShares(ocShares: List<OCShareEntity>): List<Long>

    fun deleteShare(remoteId: Long): Int

    fun deleteSharesForFile(filePath: String, accountName: String)
}
