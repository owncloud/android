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

package com.owncloud.android.data.sharing.shares.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta

@Dao
interface OCShareDao {
    @Query(SELECT_SHARE_BY_ID)
    fun getShareAsLiveData(
        remoteId: String
    ): LiveData<OCShareEntity>

    @Query(SELECT_SHARES_BY_FILEPATH_ACCOUNTOWNER_AND_TYPE)
    fun getSharesAsLiveData(
        filePath: String, accountOwner: String, shareTypes: List<Int>
    ): LiveData<List<OCShareEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(ocShare: OCShareEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(ocShares: List<OCShareEntity>): List<Long>

    @Transaction
    fun update(ocShare: OCShareEntity): Long {
        deleteShare(ocShare.remoteId)
        return insertOrReplace(ocShare)
    }

    @Transaction
    fun replaceShares(ocShares: List<OCShareEntity>): List<Long> {
        for (ocShare in ocShares) {
            deleteSharesForFile(ocShare.path, ocShare.accountOwner)
        }
        return insertOrReplace(ocShares)
    }

    @Query(DELETE_SHARE_BY_ID)
    fun deleteShare(remoteId: String): Int

    @Query(DELETE_SHARES_FOR_FILE)
    fun deleteSharesForFile(filePath: String, accountOwner: String)

    @Query(DELETE_SHARES_FOR_ACCOUNT)
    fun deleteSharesForAccount(accountName: String)

    companion object {
        private const val SELECT_SHARE_BY_ID = """
            SELECT *
            FROM ${ProviderTableMeta.OCSHARES_TABLE_NAME}
            WHERE ${ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED} = :remoteId
        """

        private const val SELECT_SHARES_BY_FILEPATH_ACCOUNTOWNER_AND_TYPE = """
            SELECT *
            FROM ${ProviderTableMeta.OCSHARES_TABLE_NAME}
            WHERE ${ProviderTableMeta.OCSHARES_PATH} = :filePath AND
                ${ProviderTableMeta.OCSHARES_ACCOUNT_OWNER} = :accountOwner AND
                ${ProviderTableMeta.OCSHARES_SHARE_TYPE} IN (:shareTypes)
        """

        private const val DELETE_SHARE_BY_ID = """
            DELETE
            FROM ${ProviderTableMeta.OCSHARES_TABLE_NAME}
            WHERE ${ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED} = :remoteId
        """

        private const val DELETE_SHARES_FOR_FILE = """
            DELETE
            FROM ${ProviderTableMeta.OCSHARES_TABLE_NAME}
            WHERE ${ProviderTableMeta.OCSHARES_PATH} = :filePath AND
                ${ProviderTableMeta.OCSHARES_ACCOUNT_OWNER} = :accountOwner
        """

        private const val DELETE_SHARES_FOR_ACCOUNT = """
            DELETE
            FROM ${ProviderTableMeta.OCSHARES_TABLE_NAME}
            WHERE ${ProviderTableMeta.OCSHARES_ACCOUNT_OWNER} = :accountName
        """
    }
}
