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
package com.owncloud.android.data.files.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.owncloud.android.data.ProviderMeta

@Dao
abstract class FileDao {

    companion object {
        private const val SELECT_FILE_WITH_ID =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.OCFILES_TABLE_NAME} " +
                    "WHERE id = :id"
        private const val DELETE_FILE_WITH_ID =
            "DELETE FROM ${ProviderMeta.ProviderTableMeta.OCFILES_TABLE_NAME} " +
                    "WHERE id = :id"
    }

    @Query(SELECT_FILE_WITH_ID)
    abstract fun getFileWithId(
        id: Long
    ): OCFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(ocFileEntity: OCFileEntity)

    @Query(DELETE_FILE_WITH_ID)
    abstract fun deleteFileWithId(id: Long)
}
