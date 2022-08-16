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

package com.owncloud.android.data.transfers.repository

import androidx.lifecycle.LiveData
import com.owncloud.android.data.transfers.datasources.LocalTransferDataSource
import com.owncloud.android.domain.transfers.TransferRepository
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferResult
import com.owncloud.android.domain.transfers.model.TransferStatus

class OCTransferRepository(
    private val localTransferDataSource: LocalTransferDataSource
) : TransferRepository {
    override fun storeTransfer(transfer: OCTransfer) =
        localTransferDataSource.storeTransfer(transfer = transfer)

    override fun updateTransfer(transfer: OCTransfer) =
        localTransferDataSource.updateTransfer(transfer = transfer)

    override fun updateTransferStatusToInProgressById(id: Long) {
        localTransferDataSource.updateTransferStatusToInProgressById(id = id)
    }

    override fun updateTransferStatusToEnqueuedById(id: Long) {
        localTransferDataSource.updateTransferStatusToEnqueuedById(id = id)
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

    override fun removeTransferById(id: Long) =
        localTransferDataSource.removeTransferById(id = id)

    override fun removeAllTransfersFromAccount(accountName: String) =
        localTransferDataSource.removeAllTransfersFromAccount(accountName = accountName)

    override fun getTransferById(id: Long): OCTransfer? =
        localTransferDataSource.getTransferById(id = id)

    override fun getAllTransfersAsLiveData(): LiveData<List<OCTransfer>> =
        localTransferDataSource.getAllTransfersAsLiveData()

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
