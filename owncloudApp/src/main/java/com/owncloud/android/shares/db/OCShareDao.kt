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

@Dao
abstract class OCShareDao {
    @Query("SELECT * from shares_table ORDER BY id")
    abstract fun shares(): LiveData<List<OCShare>>

    @Query(
        "SELECT * from shares_table " +
                "WHERE path = :filePath " +
                "AND accountOwner = :accountName AND shareType IN (:shareTypes)"
    )
    abstract fun getSharesForFile(
        filePath: String, accountName: String, shareTypes: List<Int>
    ): List<OCShare>

    @Query(
        "SELECT * from shares_table " +
                "WHERE path = :filePath " +
                "AND accountOwner = :accountName AND shareType IN (:shareTypes)"
    )
    abstract fun getSharesForFileAsLiveData(
        filePath: String, accountName: String, shareTypes: List<Int>
    ): LiveData<List<OCShare>>

    @Insert (onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(ocShares: List<OCShare>)

    @Query("DELETE from shares_table WHERE path IN (:paths)")
    abstract fun clear(paths: List<String>)

    @Transaction
    open fun replace(ocShares: List<OCShare>) {
        clear(ocShares.map { it.path })
        insert(ocShares)
    }
}
