/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.data.transfers.datasources.implementation

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.owncloud.android.data.transfers.datasources.LocalTransferDataSource
import com.owncloud.android.data.transfers.db.OCTransferEntity
import com.owncloud.android.data.transfers.db.TransferDao
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferResult
import com.owncloud.android.domain.transfers.model.TransferStatus
import com.owncloud.android.domain.transfers.model.UploadEnqueuedBy

class OCLocalTransferDataSource(
    private val transferDao: TransferDao
) : LocalTransferDataSource {
    override fun saveTransfer(transfer: OCTransfer): Long {
        return transferDao.insertOrReplace(transfer.toEntity())
    }

    override fun updateTransfer(transfer: OCTransfer) {
        transferDao.insertOrReplace(transfer.toEntity())
    }

    override fun updateTransferStatusToInProgressById(id: Long) {
        transferDao.updateTransferStatusWithId(id, TransferStatus.TRANSFER_IN_PROGRESS.value)
    }

    override fun updateTransferStatusToEnqueuedById(id: Long) {
        transferDao.updateTransferStatusWithId(id, TransferStatus.TRANSFER_QUEUED.value)
    }

    override fun updateTransferWhenFinished(
        id: Long,
        status: TransferStatus,
        transferEndTimestamp: Long,
        lastResult: TransferResult
    ) {
        transferDao.updateTransferWhenFinished(id, status.value, transferEndTimestamp, lastResult.value)
    }

    override fun updateTransferLocalPath(id: Long, localPath: String) {
        transferDao.updateTransferLocalPath(id, localPath)
    }

    override fun updateTransferStorageDirectoryInLocalPath(
        id: Long,
        oldDirectory: String,
        newDirectory: String
    ) {
        transferDao.updateTransferStorageDirectoryInLocalPath(id, oldDirectory, newDirectory)
    }

    override fun deleteTransferById(id: Long) {
        transferDao.deleteTransferWithId(id)
    }

    override fun deleteAllTransfersFromAccount(accountName: String) {
        transferDao.deleteTransfersWithAccountName(accountName)
    }

    override fun getTransferById(id: Long): OCTransfer? {
        return transferDao.getTransferWithId(id)?.toModel()
    }

    override fun getAllTransfers(): List<OCTransfer> {
        return transferDao.getAllTransfers().map { transferEntity ->
            transferEntity.toModel()
        }
    }

    override fun getAllTransfersAsLiveData(): LiveData<List<OCTransfer>> {
        return Transformations.map(transferDao.getAllTransfersAsLiveData()) { transferEntitiesList ->
            val transfers = transferEntitiesList.map { transferEntity ->
                transferEntity.toModel()
            }
            val transfersGroupedByStatus = transfers.groupBy { it.status }
            val transfersGroupedByStatusOrdered = Array<List<OCTransfer>>(4) { emptyList() }
            val newTransfersList = mutableListOf<OCTransfer>()
            transfersGroupedByStatus.forEach { transferMap ->
                val order = when (transferMap.key) {
                    TransferStatus.TRANSFER_IN_PROGRESS -> 0
                    TransferStatus.TRANSFER_QUEUED -> 1
                    TransferStatus.TRANSFER_FAILED -> 2
                    TransferStatus.TRANSFER_SUCCEEDED -> 3
                }
                transfersGroupedByStatusOrdered[order] = transferMap.value
            }
            for (items in transfersGroupedByStatusOrdered) {
                newTransfersList.addAll(items)
            }
            newTransfersList
        }
    }

    override fun getLastTransferFor(remotePath: String, accountName: String): OCTransfer? {
        return transferDao.getLastTransferWithRemotePathAndAccountName(remotePath, accountName)?.toModel()
    }

    override fun getCurrentAndPendingTransfers(): List<OCTransfer> {
        return transferDao.getTransfersWithStatus(
            listOf(TransferStatus.TRANSFER_IN_PROGRESS.value, TransferStatus.TRANSFER_QUEUED.value)
        ).map { it.toModel() }
    }

    override fun getFailedTransfers(): List<OCTransfer> {
        return transferDao.getTransfersWithStatus(
            listOf(TransferStatus.TRANSFER_FAILED.value)
        ).map { it.toModel() }
    }

    override fun getFinishedTransfers(): List<OCTransfer> {
        return transferDao.getTransfersWithStatus(
            listOf(TransferStatus.TRANSFER_SUCCEEDED.value)
        ).map { it.toModel() }
    }

    override fun clearFailedTransfers() {
        transferDao.deleteTransfersWithStatus(TransferStatus.TRANSFER_FAILED.value)
    }

    override fun clearSuccessfulTransfers() {
        transferDao.deleteTransfersWithStatus(TransferStatus.TRANSFER_SUCCEEDED.value)
    }

    private fun OCTransferEntity.toModel() = OCTransfer(
        id = id,
        localPath = localPath,
        remotePath = remotePath,
        accountName = accountName,
        fileSize = fileSize,
        status = TransferStatus.fromValue(status),
        localBehaviour = if (localBehaviour > 1) UploadBehavior.MOVE else UploadBehavior.values()[localBehaviour],
        forceOverwrite = forceOverwrite,
        transferEndTimestamp = transferEndTimestamp,
        lastResult = lastResult?.let { TransferResult.fromValue(it) },
        createdBy = UploadEnqueuedBy.values()[createdBy],
        transferId = transferId,
        spaceId = spaceId,
    )

    private fun OCTransfer.toEntity() = OCTransferEntity(
        localPath = localPath,
        remotePath = remotePath,
        accountName = accountName,
        fileSize = fileSize,
        status = status.value,
        localBehaviour = localBehaviour.ordinal,
        forceOverwrite = forceOverwrite,
        transferEndTimestamp = transferEndTimestamp,
        lastResult = lastResult?.value,
        createdBy = createdBy.ordinal,
        transferId = transferId,
        spaceId = spaceId,
    ).apply { this@toEntity.id?.let { this.id = it } }

}
