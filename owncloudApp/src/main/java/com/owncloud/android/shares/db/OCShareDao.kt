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

import androidx.lifecycle.LiveData
import androidx.room.*
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

@Dao
abstract class OCShareDao {
    @Query(
        "SELECT * from " + ProviderTableMeta.OCSHARES_TABLE_NAME + " WHERE " +
                ProviderTableMeta.OCSHARES_PATH + " = :filePath AND " +
                ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " = :accountOwner AND " +
                ProviderTableMeta.OCSHARES_SHARE_TYPE + " IN (:shareTypes)"
    )
    abstract fun getSharesForFileAsLiveData(
        filePath: String, accountOwner: String, shareTypes: List<Int>
    ): LiveData<List<OCShare>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(ocShares: List<OCShare>): List<Long>

    @Update
    abstract fun update(ocShare: OCShare): Int

    @Query(
        "DELETE from " + ProviderTableMeta.OCSHARES_TABLE_NAME + " WHERE " +
                ProviderTableMeta.OCSHARES_NAME + " = :name AND " +
                ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " = :accountOwner"
    )
    abstract fun delete(name: String, accountOwner: String)

    @Transaction
    open fun replace(ocShares: List<OCShare>) {
        for (ocShare in ocShares) {
            if (ocShare.name != null) {
                delete(ocShare.name, ocShare.accountOwner)
            }
        }
        insert(ocShares)
    }
}
