/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.data.transfers.repository

import com.owncloud.android.data.transfers.datasources.LocalTransferDataSource
import com.owncloud.android.domain.transfers.TransferRepository
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferResult
import com.owncloud.android.domain.transfers.model.TransferStatus
import kotlinx.coroutines.flow.Flow

class OCTransferRepository(
    private val localTransferDataSource: LocalTransferDataSource
) : TransferRepository {
    override fun saveTransfer(transfer: OCTransfer) =
        localTransferDataSource.saveTransfer(transfer = transfer)

    override fun updateTransfer(transfer: OCTransfer) =
        localTransferDataSource.updateTransfer(transfer = transfer)

    override fun updateTransferStatusToInProgressById(id: Long) {
        localTransferDataSource.updateTransferStatusToInProgressById(id = id)
    }

    override fun updateTransferStatusToEnqueuedById(id: Long) {
        localTransferDataSource.updateTransferStatusToEnqueuedById(id = id)
    }

    override fun updateTransferLocalPath(id: Long, localPath: String) {
        localTransferDataSource.updateTransferLocalPath(id = id, localPath = localPath)
    }

    override fun updateTransferSourcePath(id: Long, sourcePath: String) {
        localTransferDataSource.updateTransferSourcePath(id = id, sourcePath = sourcePath)
    }

    override fun updateTransferWhenFinished(
        id: Long,
        status: TransferStatus,
        transferEndTimestamp: Long,
        lastResult: TransferResult
    ) {
        localTransferDataSource.updateTransferWhenFinished(
            id = id,
            status = status,
            transferEndTimestamp = transferEndTimestamp,
            lastResult = lastResult
        )
    }

    override fun updateTransferStorageDirectoryInLocalPath(
        id: Long,
        oldDirectory: String,
        newDirectory: String
    ) {
        localTransferDataSource.updateTransferStorageDirectoryInLocalPath(
            id = id,
            oldDirectory = oldDirectory,
            newDirectory = newDirectory
        )
    }

    override fun deleteTransferById(id: Long) =
        localTransferDataSource.deleteTransferById(id = id)

    override fun deleteAllTransfersFromAccount(accountName: String) =
        localTransferDataSource.deleteAllTransfersFromAccount(accountName = accountName)

    override fun getTransferById(id: Long): OCTransfer? =
        localTransferDataSource.getTransferById(id = id)

    override fun getAllTransfers(): List<OCTransfer> =
        localTransferDataSource.getAllTransfers()

    override fun getAllTransfersAsStream(): Flow<List<OCTransfer>> =
        localTransferDataSource.getAllTransfersAsStream()

    override fun getLastTransferFor(remotePath: String, accountName: String) =
        localTransferDataSource.getLastTransferFor(remotePath = remotePath, accountName = accountName)

    override fun getCurrentAndPendingTransfers() =
        localTransferDataSource.getCurrentAndPendingTransfers()

    override fun getFailedTransfers() =
        localTransferDataSource.getFailedTransfers()

    override fun getFinishedTransfers() =
        localTransferDataSource.getFinishedTransfers()

    override fun clearFailedTransfers() =
        localTransferDataSource.clearFailedTransfers()

    override fun clearSuccessfulTransfers() =
        localTransferDataSource.clearSuccessfulTransfers()
}
