/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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

package com.owncloud.android.data.folderbackup.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.owncloud.android.data.ProviderMeta
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderBackupDao {
    @Query(SELECT)
    fun getFolderBackUpConfigurationByName(
        name: String
    ): FolderBackUpEntity?

    @Query(SELECT)
    fun getFolderBackUpConfigurationByNameAsFlow(
        name: String
    ): Flow<FolderBackUpEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(folderBackUpEntity: FolderBackUpEntity): Long

    @Query(DELETE)
    fun delete(name: String): Int

    @Transaction
    fun update(folderBackUpEntity: FolderBackUpEntity): Long {
        delete(folderBackUpEntity.name)
        return insertOrReplace(folderBackUpEntity)
    }

    companion object {
        private const val SELECT = """
            SELECT *
            FROM ${ProviderMeta.ProviderTableMeta.FOLDER_BACKUP_TABLE_NAME}
            WHERE name = :name
        """

        private const val DELETE = """
            DELETE
            FROM ${ProviderMeta.ProviderTableMeta.FOLDER_BACKUP_TABLE_NAME}
            WHERE name = :name
        """
    }
}
