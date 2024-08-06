/**
 * ownCloud Android client application
 *
 * @author Aitor Ballesteros Pavón
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

import com.owncloud.android.data.transfers.datasources.implementation.OCLocalTransferDataSource.Companion.toEntity
import com.owncloud.android.data.transfers.datasources.implementation.OCLocalTransferDataSource.Companion.toModel
import com.owncloud.android.data.transfers.db.OCTransferEntity
import com.owncloud.android.data.transfers.db.TransferDao
import com.owncloud.android.domain.transfers.model.TransferResult
import com.owncloud.android.domain.transfers.model.TransferStatus
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_TRANSFER
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCLocalTransferDataSourceTest {
    private lateinit var ocLocalTransferDataSource: OCLocalTransferDataSource
    private val transferDao = mockk<TransferDao>(relaxUnitFun = true)

    private var transferEntity: OCTransferEntity = OC_TRANSFER.toEntity()

    @Before
    fun setUp() {
        ocLocalTransferDataSource = OCLocalTransferDataSource(transferDao)
    }

    @Test
    fun `saveTransfer inserts a transfer correctly`() {
        val resultExpected = 1L
        every {
            transferDao.insertOrReplace(any())
        } returns resultExpected

        val resultActual = ocLocalTransferDataSource.saveTransfer(OC_TRANSFER)

        assertEquals(resultExpected, resultActual)

        verify(exactly = 1) {
            transferDao.insertOrReplace(OC_TRANSFER.toEntity())
        }
    }

    @Test
    fun `updateTransfer updates the transfer correctly`() {
        val resultExpected = 1L
        every {
            transferDao.insertOrReplace(any())
        } returns resultExpected

        ocLocalTransferDataSource.updateTransfer(OC_TRANSFER)

        verify(exactly = 1) {
            transferDao.insertOrReplace(OC_TRANSFER.toEntity())
        }
    }

    @Test
    fun `updateTransferStatusToInProgressById changes transfer status correctly`() {
        val resultExpected = 10L

        ocLocalTransferDataSource.updateTransferStatusToInProgressById(resultExpected)

        verify(exactly = 1) {
            transferDao.updateTransferStatusWithId(resultExpected, TransferStatus.TRANSFER_IN_PROGRESS.value)
        }
    }

    @Test
    fun `updateTransferStatusToEnqueuedById changes transfer status correctly`() {
        val resultExpected = 10L

        ocLocalTransferDataSource.updateTransferStatusToEnqueuedById(resultExpected)

        verify(exactly = 1) {
            transferDao.updateTransferStatusWithId(resultExpected, TransferStatus.TRANSFER_QUEUED.value)
        }
    }

    @Test
    fun `updateTransferWhenFinished changes transfer status correctly`() {
        val timestamp = System.currentTimeMillis()

        ocLocalTransferDataSource.updateTransferWhenFinished(OC_TRANSFER.id!!, TransferStatus.TRANSFER_SUCCEEDED, timestamp, TransferResult.UPLOADED)

        verify(exactly = 1) {
            transferDao.updateTransferWhenFinished(OC_TRANSFER.id!!, TransferStatus.TRANSFER_SUCCEEDED.value, timestamp, TransferResult.UPLOADED.value)
        }
    }

    @Test
    fun `updateTransferLocalPath changes transfer local path correctly`() {

        ocLocalTransferDataSource.updateTransferLocalPath(OC_TRANSFER.id!!, OC_TRANSFER.localPath)

        verify(exactly = 1) {
            transferDao.updateTransferLocalPath(OC_TRANSFER.id!!, OC_TRANSFER.localPath)
        }
    }

    @Test
    fun `updateTransferSourcePath changes transfer source path correctly`() {

        ocLocalTransferDataSource.updateTransferSourcePath(OC_TRANSFER.id!!, OC_TRANSFER.sourcePath!!)

        verify(exactly = 1) {
            transferDao.updateTransferSourcePath(OC_TRANSFER.id!!, OC_TRANSFER.sourcePath!!)
        }
    }

    @Test
    fun `updateTransferStorageDirectoryInLocalPath changes directory correctly`() {
        val oldDirectory = "oldDirectory"
        val newDirectory = "newDirectory"

        ocLocalTransferDataSource.updateTransferStorageDirectoryInLocalPath(OC_TRANSFER.id!!, oldDirectory, newDirectory)

        verify(exactly = 1) {
            transferDao.updateTransferStorageDirectoryInLocalPath(OC_TRANSFER.id!!, oldDirectory, newDirectory)
        }
    }

    @Test
    fun `deleteTransferById removes a transfer correctly`() {

        ocLocalTransferDataSource.deleteTransferById(OC_TRANSFER.id!!)

        verify(exactly = 1) {
            transferDao.deleteTransferWithId(OC_TRANSFER.id!!)
        }
    }

    @Test
    fun `deleteAllTransfersFromAccount removes all transfers for an account correctly`() {

        ocLocalTransferDataSource.deleteAllTransfersFromAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            transferDao.deleteTransfersWithAccountName(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getTransferById returns a OCTransfer`() {
        every {
            transferDao.getTransferWithId(any())
        } returns OC_TRANSFER.toEntity()

        val actualResult = ocLocalTransferDataSource.getTransferById(OC_TRANSFER.id!!)

        assertEquals(OC_TRANSFER, actualResult)

        verify(exactly = 1) {
            transferDao.getTransferWithId(OC_TRANSFER.id!!)
        }
    }

    @Test
    fun `getAllTransfers returns a list of OCTransfer`() {

        every {
            transferDao.getAllTransfers()
        } returns listOf(transferEntity)

        val actualResult = ocLocalTransferDataSource.getAllTransfers()

        assertEquals(listOf(transferEntity.toModel()), actualResult)

        verify(exactly = 1) {
            transferDao.getAllTransfers()
        }
    }

    @Test
    fun `getAllTransfersAsStream returns a Flow with a list of OCTransfer ordered by status`() = runBlocking {

        val transferEntityInProgress: OCTransferEntity = OC_TRANSFER.copy(status = TransferStatus.TRANSFER_IN_PROGRESS).toEntity()

        val transferEntityQueue: OCTransferEntity = OC_TRANSFER.copy(status = TransferStatus.TRANSFER_QUEUED).toEntity()

        val transferEntityFailed: OCTransferEntity = OC_TRANSFER.copy(status = TransferStatus.TRANSFER_FAILED).toEntity()

        val transferEntitySucceeded: OCTransferEntity = OC_TRANSFER.copy(status = TransferStatus.TRANSFER_SUCCEEDED).toEntity()

        val transferListRandom = listOf(transferEntityQueue, transferEntityFailed, transferEntityInProgress, transferEntitySucceeded)

        val transferQueue = OC_TRANSFER.copy()
        transferQueue.status = TransferStatus.TRANSFER_QUEUED

        val transferFailed = OC_TRANSFER.copy()
        transferFailed.status = TransferStatus.TRANSFER_FAILED

        val transferSucceeded = OC_TRANSFER.copy()
        transferSucceeded.status = TransferStatus.TRANSFER_SUCCEEDED

        val transferListOrdered = listOf(OC_TRANSFER, transferQueue, transferFailed, transferSucceeded)

        every {
            transferDao.getAllTransfersAsStream()
        } returns flowOf(transferListRandom)

        val actualResult = ocLocalTransferDataSource.getAllTransfersAsStream().first()

        assertEquals(transferListOrdered, actualResult)

        verify(exactly = 1) {
            transferDao.getAllTransfersAsStream()
        }
    }

    @Test
    fun `getLastTransferFor returns a OCTransfer`() {

        every {
            transferDao.getLastTransferWithRemotePathAndAccountName(OC_TRANSFER.remotePath, OC_ACCOUNT_NAME)
        } returns transferEntity

        val actualResult = ocLocalTransferDataSource.getLastTransferFor(OC_TRANSFER.remotePath, OC_ACCOUNT_NAME)

        assertEquals(transferEntity.toModel(), actualResult)

        verify(exactly = 1) {
            transferDao.getLastTransferWithRemotePathAndAccountName(OC_TRANSFER.remotePath, OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getCurrentAndPendingTransfers returns a list of OCTransfer`() {

        every {
            transferDao.getTransfersWithStatus(listOf(TransferStatus.TRANSFER_IN_PROGRESS.value, TransferStatus.TRANSFER_QUEUED.value))
        } returns listOf(transferEntity)

        val actualResult = ocLocalTransferDataSource.getCurrentAndPendingTransfers()

        assertEquals(listOf(transferEntity.toModel()), actualResult)

        verify(exactly = 1) {
            transferDao.getTransfersWithStatus(listOf(TransferStatus.TRANSFER_IN_PROGRESS.value, TransferStatus.TRANSFER_QUEUED.value))
        }
    }

    @Test
    fun `getFailedTransfers returns a list of OCTransfer`() {

        every {
            transferDao.getTransfersWithStatus(listOf(TransferStatus.TRANSFER_FAILED.value))
        } returns listOf(transferEntity)

        val actualResult = ocLocalTransferDataSource.getFailedTransfers()

        assertEquals(listOf(transferEntity.toModel()), actualResult)

        verify(exactly = 1) {
            transferDao.getTransfersWithStatus(listOf(TransferStatus.TRANSFER_FAILED.value))
        }
    }

    @Test
    fun `getFinishedTransfers returns a list of OCTransfer`() {

        every {
            transferDao.getTransfersWithStatus(listOf(TransferStatus.TRANSFER_SUCCEEDED.value))
        } returns listOf(transferEntity)

        val actualResult = ocLocalTransferDataSource.getFinishedTransfers()

        assertEquals(listOf(transferEntity.toModel()), actualResult)

        verify(exactly = 1) {
            transferDao.getTransfersWithStatus(listOf(TransferStatus.TRANSFER_SUCCEEDED.value))
        }
    }

    @Test
    fun `clearFailedTransfers removes failed transfers correctly`() {

        ocLocalTransferDataSource.clearFailedTransfers()

        verify(exactly = 1) {
            transferDao.deleteTransfersWithStatus(TransferStatus.TRANSFER_FAILED.value)
        }
    }

    @Test
    fun `clearSuccessfulTransfers removes successful transfers correctly`() {

        ocLocalTransferDataSource.clearSuccessfulTransfers()

        verify(exactly = 1) {
            transferDao.deleteTransfersWithStatus(TransferStatus.TRANSFER_SUCCEEDED.value)
        }
    }
}
