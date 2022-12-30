/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.data.spaces.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.owncloud.android.data.ProviderMeta
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.DRIVE_TYPE_PERSONAL
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.DRIVE_TYPE_PROJECT
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ACCOUNT_NAME
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_DRIVE_TYPE
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ID
import kotlinx.coroutines.flow.Flow

@Dao
interface SpacesDao {
    @Transaction
    fun insertOrDeleteSpaces(
        listOfSpacesEntities: List<SpacesEntity>,
        listOfSpecialEntities: List<SpaceSpecialEntity>,
    ) {
        val currentAccountName = listOfSpacesEntities.first().accountName
        val currentSpaces = getAllSpacesForAccount(currentAccountName)

        // Delete spaces that are not attached to the current account anymore
        val spacesToDelete = currentSpaces.filterNot { oldSpace ->
            listOfSpacesEntities.any { it.id == oldSpace.id }
        }

        spacesToDelete.forEach { spaceToDelete ->
            deleteSpaceForAccountById(accountName = spaceToDelete.accountName, spaceId = spaceToDelete.id)
        }

        // Insert new spaces
        insertOrReplaceSpaces(listOfSpacesEntities)
        insertOrReplaceSpecials(listOfSpecialEntities)
    }

    // TODO: Use upsert instead of insert and replace
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplaceSpaces(listOfSpacesEntities: List<SpacesEntity>): List<Long>

    // TODO: Use upsert instead of insert and replace
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplaceSpecials(listOfSpecialEntities: List<SpaceSpecialEntity>): List<Long>

    @Query(SELECT_ALL_SPACES_FOR_ACCOUNT)
    fun getAllSpacesForAccount(
        accountName: String,
    ): List<SpacesEntity>

    @Query(SELECT_PERSONAL_SPACE_FOR_ACCOUNT)
    fun getPersonalSpacesForAccount(
        accountName: String,
    ): List<SpacesEntity>

    @Query(SELECT_PROJECT_SPACES_FOR_ACCOUNT)
    fun getProjectSpacesWithSpecialsForAccountAsFlow(
        accountName: String,
    ): Flow<List<SpacesWithSpecials>>

    @Query(DELETE_ALL_SPACES_FOR_ACCOUNT)
    fun deleteSpacesForAccount(accountName: String)

    @Query(DELETE_SPACE_FOR_ACCOUNT_BY_ID)
    fun deleteSpaceForAccountById(accountName: String, spaceId: String)

    companion object {
        private const val SELECT_ALL_SPACES_FOR_ACCOUNT = """
            SELECT *
            FROM ${ProviderMeta.ProviderTableMeta.SPACES_TABLE_NAME}
            WHERE $SPACES_ACCOUNT_NAME = :accountName
        """

        private const val SELECT_PERSONAL_SPACE_FOR_ACCOUNT = """
            SELECT *
            FROM ${ProviderMeta.ProviderTableMeta.SPACES_TABLE_NAME}
            WHERE $SPACES_ACCOUNT_NAME = :accountName AND $SPACES_DRIVE_TYPE LIKE '$DRIVE_TYPE_PERSONAL'
        """

        private const val SELECT_PROJECT_SPACES_FOR_ACCOUNT = """
            SELECT *
            FROM ${ProviderMeta.ProviderTableMeta.SPACES_TABLE_NAME}
            WHERE $SPACES_ACCOUNT_NAME = :accountName AND $SPACES_DRIVE_TYPE LIKE '$DRIVE_TYPE_PROJECT'
        """

        private const val DELETE_ALL_SPACES_FOR_ACCOUNT = """
            DELETE
            FROM ${ProviderMeta.ProviderTableMeta.SPACES_TABLE_NAME}
            WHERE $SPACES_ACCOUNT_NAME = :accountName
        """

        private const val DELETE_SPACE_FOR_ACCOUNT_BY_ID = """
            DELETE
            FROM ${ProviderMeta.ProviderTableMeta.SPACES_TABLE_NAME}
            WHERE $SPACES_ACCOUNT_NAME = :accountName AND $SPACES_ID LIKE :spaceId
        """
    }
}
