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

package com.owncloud.android.shares.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

@Dao
abstract class OCShareDao {
    @Query("SELECT * from " + ProviderTableMeta.OCSHARES_TABLE_NAME + " ORDER BY id")
    abstract fun getAllShares(): List<OCShare>

    @Query("SELECT * from " + ProviderTableMeta.OCSHARES_TABLE_NAME + " ORDER BY id")
    abstract fun getAllSharesAsLiveData(): LiveData<List<OCShare>>

    @Query(
        "SELECT * from " + ProviderTableMeta.OCSHARES_TABLE_NAME + " WHERE " +
                ProviderTableMeta.OCSHARES_PATH + " = :filePath AND " +
                ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " = :accountName AND " +
                ProviderTableMeta.OCSHARES_SHARE_TYPE + " IN (:shareTypes)"
    )
    abstract fun getSharesForFile(
        filePath: String, accountName: String, shareTypes: List<Int>
    ): List<OCShare>

    @Query(
        "SELECT * from " + ProviderTableMeta.OCSHARES_TABLE_NAME + " WHERE " +
                ProviderTableMeta.OCSHARES_PATH + " = :filePath AND " +
                ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " = :accountName AND " +
                ProviderTableMeta.OCSHARES_SHARE_TYPE + " IN (:shareTypes)"
    )
    abstract fun getSharesForFileAsLiveData(
        filePath: String, accountName: String, shareTypes: List<Int>
    ): LiveData<List<OCShare>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(ocShares: List<OCShare>): List<Long>

    @Query(
        "DELETE from " + ProviderTableMeta.OCSHARES_TABLE_NAME + " WHERE " +
                ProviderTableMeta.OCSHARES_PATH + " IN (:paths)"
    )
    abstract fun clear(paths: List<String>)

    @Transaction
    open fun replace(ocShares: List<OCShare>) {
        clear(ocShares.map { it.path })
        insert(ocShares)
    }
}
