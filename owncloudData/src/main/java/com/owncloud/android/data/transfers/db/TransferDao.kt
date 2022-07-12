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

@Dao
abstract class TransferDao {
    @Query(SELECT_LAST_TRANSFER_WITH_REMOTE_PATH_AND_ACCOUNT_NAME)
    abstract fun getLastTransferWithRemotePathAndAccountName(remotePath: String, accountName: String): OCTransferEntity?

    @Query(SELECT_TRANSFERS_WITH_STATUS)
    abstract fun getTransfersWithStatus(status: List<Int>): List<OCTransferEntity>

    @Query(SELECT_ALL_TRANSFERS)
    abstract fun getAllTransfers(): List<OCTransferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(ocTransferEntity: OCTransferEntity): Long

    @Query(UPDATE_TRANSFER_STATUS_WITH_ID)
    abstract fun updateTransferStatusWithId(id: Long, newStatus: Int)

    abstract fun updateTransferWhenFinished(id: Long, status: Int, transferEndTimestamp: Long, lastResult: Int)

    @Query(DELETE_TRANSFER_WITH_ID)
    abstract fun deleteTransferWithId(id: Long)

    @Query(DELETE_TRANSFERS_WITH_ACCOUNT_NAME)
    abstract fun deleteTransfersWithAccountName(accountName: String)

    @Query(DELETE_TRANSFERS_WITH_STATUS)
    abstract fun deleteTransfersWithStatus(status: Int)

    companion object {
        private const val SELECT_LAST_TRANSFER_WITH_REMOTE_PATH_AND_ACCOUNT_NAME =
            "SELECT * " +
                    "FROM $TRANSFERS_TABLE_NAME " +
                    "WHERE remotePath = :remotePath AND accountName = :accountName " +
                    "ORDER BY transferEndTimestamp DESC " +
                    "LIMIT 1"

        private const val SELECT_TRANSFERS_WITH_STATUS =
            "SELECT * " +
                    "FROM $TRANSFERS_TABLE_NAME " +
                    "WHERE status IN (:status)"

        private const val SELECT_ALL_TRANSFERS =
            "SELECT * " +
                    "FROM $TRANSFERS_TABLE_NAME "

        private const val UPDATE_TRANSFER_STATUS_WITH_ID =
            "UPDATE $TRANSFERS_TABLE_NAME " +
                    "SET status = :newStatus " +
                    "WHERE id = :id"

        private const val UPDATE_TRANSFER_WHEN_FINISHED =
            "UPDATE $TRANSFERS_TABLE_NAME " +
                    "SET status = :status, " +
                    "transferEndTimestamp = :transferEndTimestamp," +
                    "lastResult = :lastResult " +
                    "WHERE id = :id"

        private const val DELETE_TRANSFER_WITH_ID =
            "DELETE FROM $TRANSFERS_TABLE_NAME " +
                    "WHERE id = :id"

        private const val DELETE_TRANSFERS_WITH_ACCOUNT_NAME =
            "DELETE FROM $TRANSFERS_TABLE_NAME " +
                    "WHERE accountName = :accountName"

        private const val DELETE_TRANSFERS_WITH_STATUS =
            "DELETE FROM $TRANSFERS_TABLE_NAME " +
                    "WHERE status = :status"
    }
}
