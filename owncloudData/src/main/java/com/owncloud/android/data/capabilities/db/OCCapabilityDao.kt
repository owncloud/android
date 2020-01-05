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

package com.owncloud.android.data.capabilities.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta

@Dao
abstract class OCCapabilityDao {

    companion object {
        private const val SELECT =
            "SELECT * " +
                    "FROM ${ProviderTableMeta.CAPABILITIES_TABLE_NAME} " +
                    "WHERE ${ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME} = :accountName"
        private const val DELETE =
            "DELETE FROM ${ProviderTableMeta.CAPABILITIES_TABLE_NAME} " +
                    "WHERE ${ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME} = :accountName"
    }

    @Query(SELECT)
    abstract fun getCapabilitiesForAccountAsLiveData(
        accountName: String
    ): LiveData<OCCapabilityEntity>

    @Query(SELECT)
    abstract fun getCapabilitiesForAccount(
        accountName: String
    ): OCCapabilityEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(ocCapability: OCCapabilityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(ocCapabilities: List<OCCapabilityEntity>): List<Long>

    @Query(DELETE)
    abstract fun delete(accountName: String)

    @Transaction
    open fun replace(ocCapabilities: List<OCCapabilityEntity>) {
        ocCapabilities.forEach { ocCapability ->
            ocCapability.accountName?.run {
                delete(this)
            }
        }
        insert(ocCapabilities)
    }
}
