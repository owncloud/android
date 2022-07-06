/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.data.transfers.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.TRANSFERS_TABLE_NAME
import com.owncloud.android.data.files.db.OCFileEntity

@Dao
abstract class TransferDao {
    @Query(SELECT_TRANSFER_WITH_ID)
    abstract fun getFileById(id: Long): OCFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(ocTransferEntity: OCTransferEntity): Long

    @Query(DELETE_TRANSFER_WITH_ID)
    abstract fun deleteTransferWithId(id: Long)

    companion object {
        private const val SELECT_TRANSFER_WITH_ID =
            "SELECT * " +
                    "FROM $TRANSFERS_TABLE_NAME " +
                    "WHERE id = :id"

        private const val DELETE_TRANSFER_WITH_ID =
            "DELETE FROM $TRANSFERS_TABLE_NAME " +
                    "WHERE id = :id"
    }
}
