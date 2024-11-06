/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.owncloud.android.data.user.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.owncloud.android.data.ProviderMeta
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query(SELECT_QUOTA)
    fun getQuotaForAccount(
        accountName: String
    ): UserQuotaEntity?

    @Query(SELECT_ALL_QUOTAS)
    fun getAllUserQuotas(): List<UserQuotaEntity>

    @Query(SELECT_ALL_QUOTAS)
    fun getAllUserQuotasAsStream(): Flow<List<UserQuotaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(userQuotaEntity: UserQuotaEntity)

    @Query(DELETE_QUOTA)
    fun deleteQuotaForAccount(accountName: String)

    companion object {
        private const val SELECT_QUOTA = """
            SELECT *
            FROM ${ProviderMeta.ProviderTableMeta.USER_QUOTAS_TABLE_NAME}
            WHERE accountName = :accountName
        """

        private const val SELECT_ALL_QUOTAS = """
            SELECT *
            FROM ${ProviderMeta.ProviderTableMeta.USER_QUOTAS_TABLE_NAME}
        """

        private const val DELETE_QUOTA = """
            DELETE
            FROM ${ProviderMeta.ProviderTableMeta.USER_QUOTAS_TABLE_NAME}
            WHERE accountName = :accountName
        """
    }
}
