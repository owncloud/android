/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.data.sharing.shares.datasources.implementation

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.owncloud.android.data.sharing.shares.datasources.LocalShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.mapper.OCShareMapper
import com.owncloud.android.data.sharing.shares.db.OCShareDao
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType

class OCLocalShareDataSource(
    private val ocShareDao: OCShareDao,
    private val ocShareMapper: OCShareMapper
) : LocalShareDataSource {

    override fun getSharesAsLiveData(
        filePath: String,
        accountName: String,
        shareTypes: List<ShareType>
    ): LiveData<List<OCShare>> =
        Transformations.map(
            ocShareDao.getSharesAsLiveData(
                filePath,
                accountName,
                shareTypes.map { it.value })
        ) { ocShareEntities ->
            ocShareEntities.map { ocShareEntity -> ocShareMapper.toModel(ocShareEntity)!! }
        }

    override fun getShareAsLiveData(remoteId: Long): LiveData<OCShare> =
        Transformations.map(ocShareDao.getShareAsLiveData(remoteId)) { ocShareEntity ->
            ocShareMapper.toModel(ocShareEntity)!!
        }

    override fun insert(ocShare: OCShare): Long =
        ocShareDao.insert(
            ocShareMapper.toEntity(ocShare)!!
        )

    override fun insert(ocShares: List<OCShare>): List<Long> =
        ocShareDao.insert(
            ocShares.map { ocShare -> ocShareMapper.toEntity(ocShare)!! }
        )

    override fun update(ocShare: OCShare): Long = ocShareDao.update(ocShareMapper.toEntity(ocShare)!!)

    override fun replaceShares(ocShares: List<OCShare>): List<Long> =
        ocShareDao.replaceShares(
            ocShares.map { ocShare -> ocShareMapper.toEntity(ocShare)!! }
        )

    override fun deleteShare(remoteId: Long): Int = ocShareDao.deleteShare(remoteId)

    override fun deleteSharesForFile(filePath: String, accountName: String) =
        ocShareDao.deleteSharesForFile(filePath, accountName)
}
