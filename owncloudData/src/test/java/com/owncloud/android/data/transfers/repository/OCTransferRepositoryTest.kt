/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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
import com.owncloud.android.domain.transfers.model.TransferResult
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_FAILED_TRANSFER
import com.owncloud.android.testutil.OC_FINISHED_TRANSFER
import com.owncloud.android.testutil.OC_PENDING_TRANSFER
import com.owncloud.android.testutil.OC_TRANSFER
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OCTransferRepositoryTest {

    private val localTransferDataSource = mockk<LocalTransferDataSource>(relaxUnitFun = true)
    private val ocTransferRepository = OCTransferRepository(localTransferDataSource)

    @Test
    fun `saveTransfer inserts a transfer correctly`() {
        // The result of this method is not used, so it can be anything
        every {
            localTransferDataSource.saveTransfer(OC_TRANSFER)
        } returns 1L

        val result = ocTransferRepository.saveTransfer(OC_TRANSFER)
        assertEquals(1L, result)

        verify(exactly = 1) {
            localTransferDataSource.saveTransfer(OC_TRANSFER)
        }
    }

    @Test
    fun `updateTransfer updates a transfer correctly`() {
        ocTransferRepository.updateTransfer(OC_TRANSFER)

        verify(exactly = 1) {
            localTransferDataSource.updateTransfer(OC_TRANSFER)
        }
    }

    @Test
    fun `updateTransferStatusToInProgressById changes transfer status correctly`() {
        ocTransferRepository.updateTransferStatusToInProgressById(OC_TRANSFER.id!!)

        verify(exactly = 1) {
            localTransferDataSource.updateTransferStatusToInProgressById(OC_TRANSFER.id!!)
        }
    }

    @Test
    fun `updateTransferStatusToEnqueuedById changes transfer status correctly`() {
        ocTransferRepository.updateTransferStatusToEnqueuedById(OC_TRANSFER.id!!)

        verify(exactly = 1) {
            localTransferDataSource.updateTransferStatusToEnqueuedById(OC_TRANSFER.id!!)
        }
    }

    @Test
    fun `updateTransferLocalPath updates transfer local path correctly`() {
        ocTransferRepository.updateTransferLocalPath(OC_TRANSFER.id!!, OC_TRANSFER.localPath)

        verify(exactly = 1) {
            localTransferDataSource.updateTransferLocalPath(OC_TRANSFER.id!!, OC_TRANSFER.localPath)
        }
    }

    @Test
    fun `updateTransferSourcePath updates transfer source path correctly`() {
        ocTransferRepository.updateTransferSourcePath(OC_TRANSFER.id!!, OC_TRANSFER.sourcePath!!)

        verify(exactly = 1) {
            localTransferDataSource.updateTransferSourcePath(OC_TRANSFER.id!!, OC_TRANSFER.sourcePath!!)
        }
    }

    @Test
    fun `updateTransferWhenFinished changes transfer status correctly`() {
        ocTransferRepository.updateTransferWhenFinished(OC_TRANSFER.id!!, OC_FINISHED_TRANSFER.status, 1_000, TransferResult.UPLOADED)

        verify(exactly = 1) {
            localTransferDataSource.updateTransferWhenFinished(OC_TRANSFER.id!!, OC_FINISHED_TRANSFER.status, 1_000, TransferResult.UPLOADED)
        }
    }

    @Test
    fun `updateTransferStorageDirectoryInLocalPath updates transfer storage directory correctly`() {
        val oldDirectory = "/oldDirectory/path"
        val newDirectory = "/newDirectory/path"

        ocTransferRepository.updateTransferStorageDirectoryInLocalPath(OC_TRANSFER.id!!, oldDirectory, newDirectory)

        verify(exactly = 1) {
         localTransferDataSource.updateTransferStorageDirectoryInLocalPath(OC_TRANSFER.id!!, oldDirectory, newDirectory)
        }
    }

    @Test
    fun `deleteTransferById removes a transfer correctly`() {
        ocTransferRepository.deleteTransferById(OC_TRANSFER.id!!)

        verify(exactly = 1) {
            localTransferDataSource.deleteTransferById(OC_TRANSFER.id!!)
        }
    }

    @Test
    fun `deleteAllTransfersFromAccount removes all transfers correctly`() {
        ocTransferRepository.deleteAllTransfersFromAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            localTransferDataSource.deleteAllTransfersFromAccount(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getTransferById returns a OCTransfer`() {
        every {
            localTransferDataSource.getTransferById(OC_TRANSFER.id!!)
        } returns OC_TRANSFER

        val transfer = ocTransferRepository.getTransferById(OC_TRANSFER.id!!)
        assertEquals(OC_TRANSFER, transfer)

        verify(exactly = 1) {
            localTransferDataSource.getTransferById(OC_TRANSFER.id!!)
        }
    }

    @Test
    fun `getTransferById returns null when local datasource returns null`() {
        every {
            localTransferDataSource.getTransferById(OC_TRANSFER.id!!)
        } returns null

        val transfer = ocTransferRepository.getTransferById(OC_TRANSFER.id!!)
        assertNull(transfer)

        verify(exactly = 1) {
            localTransferDataSource.getTransferById(OC_TRANSFER.id!!)
        }
    }

    @Test
    fun `getAllTransfers returns a list of OCTransfer`() {
        every {
            localTransferDataSource.getAllTransfers()
        } returns listOf(OC_TRANSFER)

        val listOfTransfers = ocTransferRepository.getAllTransfers()
        assertEquals(listOf(OC_TRANSFER), listOfTransfers)

        verify(exactly = 1) {
            localTransferDataSource.getAllTransfers()
        }
    }

    @Test
    fun `getAllTransfersAsStream returns a Flow with a list of OCTransfer`() = runTest {
        every {
            localTransferDataSource.getAllTransfersAsStream()
        } returns flowOf(listOf(OC_TRANSFER))

        val listOfTransfers = ocTransferRepository.getAllTransfersAsStream().first()
        assertEquals(listOf(OC_TRANSFER), listOfTransfers)

        verify(exactly = 1) {
            localTransferDataSource.getAllTransfersAsStream()
        }
    }

    @Test
    fun `getLastTransferFor returns a OCTransfer`() {
        every {
            localTransferDataSource.getLastTransferFor(OC_TRANSFER.remotePath, OC_ACCOUNT_NAME)
        } returns OC_TRANSFER

        val lastTransfer = ocTransferRepository.getLastTransferFor(OC_TRANSFER.remotePath, OC_ACCOUNT_NAME)
        assertEquals(OC_TRANSFER, lastTransfer)

        verify(exactly = 1) {
            localTransferDataSource.getLastTransferFor(OC_TRANSFER.remotePath, OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getLastTransferFor returns null when local datasource returns null`() {
        every {
            localTransferDataSource.getLastTransferFor(OC_TRANSFER.remotePath, OC_ACCOUNT_NAME)
        } returns null

        val lastTransfer = ocTransferRepository.getLastTransferFor(OC_TRANSFER.remotePath, OC_ACCOUNT_NAME)
        assertNull(lastTransfer)

        verify(exactly = 1) {
            localTransferDataSource.getLastTransferFor(OC_TRANSFER.remotePath, OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getCurrentAndPendingTransfers returns a list of OCTransfer`() {
        every {
            localTransferDataSource.getCurrentAndPendingTransfers()
        } returns listOf(OC_TRANSFER, OC_PENDING_TRANSFER)

        val listOfCurrentAndPendingTransfers = ocTransferRepository.getCurrentAndPendingTransfers()
        assertEquals(listOf(OC_TRANSFER, OC_PENDING_TRANSFER), listOfCurrentAndPendingTransfers)

        verify(exactly = 1) {
            localTransferDataSource.getCurrentAndPendingTransfers()
        }
    }

    @Test
    fun `getFailedTransfers returns a list of OCTransfer`() {
        every {
            localTransferDataSource.getFailedTransfers()
        } returns listOf(OC_FAILED_TRANSFER)

        val listOfFailedTransfers = ocTransferRepository.getFailedTransfers()
        assertEquals(listOf(OC_FAILED_TRANSFER), listOfFailedTransfers)

        verify(exactly = 1) {
            localTransferDataSource.getFailedTransfers()
        }
    }

    @Test
    fun `getFinishedTransfers returns a list of OCTransfer`() {
        every {
            localTransferDataSource.getFinishedTransfers()
        } returns listOf(OC_FINISHED_TRANSFER)

        val listOfFinishedTransfers = ocTransferRepository.getFinishedTransfers()
        assertEquals(listOf(OC_FINISHED_TRANSFER), listOfFinishedTransfers)

        verify(exactly = 1) {
            localTransferDataSource.getFinishedTransfers()
        }
    }

    @Test
    fun `clearFailedTransfers removes failed transfers correctly`() {
        ocTransferRepository.clearFailedTransfers()

        verify(exactly = 1) {
            localTransferDataSource.clearFailedTransfers()
        }
    }

    @Test
    fun `clearSuccessfulTransfers removes successful transfers correctly`() {
        ocTransferRepository.clearSuccessfulTransfers()

        verify(exactly = 1) {
            localTransferDataSource.clearSuccessfulTransfers()
        }
    }

}
