/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.owncloud.android.data.ProviderMeta
import com.owncloud.android.data.spaces.db.SpaceSpecialEntity.Companion.SPACES_SPECIAL_ACCOUNT_NAME
import com.owncloud.android.data.spaces.db.SpaceSpecialEntity.Companion.SPACES_SPECIAL_ID
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ACCOUNT_NAME
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_DRIVE_TYPE
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ID
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ROOT_WEB_DAV_URL
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

        // Delete specials that are not attached to the current spaces of the account anymore
        val currentSpecials = getAllSpecialsForAccount(currentAccountName)

        val specialsToDelete = currentSpecials.filterNot { oldSpecial ->
            listOfSpecialEntities.any { it.id == oldSpecial.id }
        }

        specialsToDelete.forEach { specialToDelete ->
            deleteSpecialForAccountById(accountName = specialToDelete.accountName, specialId = specialToDelete.id)
        }

        // Upsert new spaces and specials
        upsertSpaces(listOfSpacesEntities)
        upsertSpecials(listOfSpecialEntities)
    }

    @Upsert
    fun upsertSpaces(listOfSpacesEntities: List<SpacesEntity>)

    @Upsert
    fun upsertSpecials(listOfSpecialEntities: List<SpaceSpecialEntity>)

    @Query(SELECT_SPACES_BY_DRIVE_TYPE)
    fun getSpacesByDriveTypeFromEveryAccountAsStream(
        filterDriveTypes: Set<String>,
    ): Flow<List<SpacesEntity>>

    @Query(SELECT_ALL_SPACES_FOR_ACCOUNT)
    fun getAllSpacesForAccount(
        accountName: String,
    ): List<SpacesEntity>

    @Query(SELECT_SPACES_BY_DRIVE_TYPE_FOR_ACCOUNT)
    fun getSpacesByDriveTypeForAccount(
        accountName: String,
        filterDriveTypes: Set<String>,
    ): List<SpacesEntity>

    @Transaction
    @Query(SELECT_SPACES_BY_DRIVE_TYPE_FOR_ACCOUNT)
    fun getSpacesByDriveTypeWithSpecialsForAccountAsFlow(
        accountName: String,
        filterDriveTypes: Set<String>,
    ): Flow<List<SpacesWithSpecials>>

    @Transaction
    @Query(SELECT_SPACE_BY_ID_FOR_ACCOUNT)
    fun getSpaceWithSpecialsByIdForAccount(
        spaceId: String?,
        accountName: String,
    ): SpacesWithSpecials

    @Query(SELECT_SPACE_BY_ID_FOR_ACCOUNT)
    fun getSpaceByIdForAccount(
        spaceId: String?,
        accountName: String,
    ): SpacesEntity?

    @Query(SELECT_ALL_SPECIALS_FOR_ACCOUNT)
    fun getAllSpecialsForAccount(
        accountName: String,
    ): List<SpaceSpecialEntity>

    @Query(SELECT_WEB_DAV_URL_FOR_SPACE)
    fun getWebDavUrlForSpace(
        spaceId: String?,
        accountName: String,
    ): String?

    @Query(DELETE_ALL_SPACES_FOR_ACCOUNT)
    fun deleteSpacesForAccount(accountName: String)

    @Query(DELETE_SPACE_FOR_ACCOUNT_BY_ID)
    fun deleteSpaceForAccountById(accountName: String, spaceId: String)

    @Query(DELETE_SPECIAL_FOR_ACCOUNT_BY_ID)
    fun deleteSpecialForAccountById(accountName: String, specialId: String)

    companion object {
        private const val SELECT_SPACES_BY_DRIVE_TYPE = """
            SELECT *
            FROM ${ProviderMeta.ProviderTableMeta.SPACES_TABLE_NAME}
            WHERE $SPACES_DRIVE_TYPE IN (:filterDriveTypes)
        """

        private const val SELECT_ALL_SPACES_FOR_ACCOUNT = """
            SELECT *
            FROM ${ProviderMeta.ProviderTableMeta.SPACES_TABLE_NAME}
            WHERE $SPACES_ACCOUNT_NAME = :accountName
        """

        private const val SELECT_SPACES_BY_DRIVE_TYPE_FOR_ACCOUNT = """
            SELECT *
            FROM ${ProviderMeta.ProviderTableMeta.SPACES_TABLE_NAME}
            WHERE $SPACES_ACCOUNT_NAME = :accountName AND ($SPACES_DRIVE_TYPE IN (:filterDriveTypes))
            ORDER BY $SPACES_DRIVE_TYPE ASC, name COLLATE NOCASE ASC
        """

        private const val SELECT_SPACE_BY_ID_FOR_ACCOUNT = """
            SELECT *
            FROM ${ProviderMeta.ProviderTableMeta.SPACES_TABLE_NAME}
            WHERE $SPACES_ID = :spaceId AND $SPACES_ACCOUNT_NAME = :accountName
        """

        private const val SELECT_ALL_SPECIALS_FOR_ACCOUNT = """
            SELECT *
            FROM ${ProviderMeta.ProviderTableMeta.SPACES_SPECIAL_TABLE_NAME}
            WHERE $SPACES_SPECIAL_ACCOUNT_NAME = :accountName
        """

        private const val SELECT_WEB_DAV_URL_FOR_SPACE = """
            SELECT $SPACES_ROOT_WEB_DAV_URL
            FROM ${ProviderMeta.ProviderTableMeta.SPACES_TABLE_NAME}
            WHERE $SPACES_ID = :spaceId AND $SPACES_ACCOUNT_NAME = :accountName
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

        private const val DELETE_SPECIAL_FOR_ACCOUNT_BY_ID = """
            DELETE
            FROM ${ProviderMeta.ProviderTableMeta.SPACES_SPECIAL_TABLE_NAME}
            WHERE $SPACES_SPECIAL_ACCOUNT_NAME = :accountName AND $SPACES_SPECIAL_ID LIKE :specialId
        """
    }
}
