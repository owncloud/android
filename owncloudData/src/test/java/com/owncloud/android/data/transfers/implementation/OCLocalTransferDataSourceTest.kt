package com.owncloud.android.data.transfers.implementation

import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.transfers.datasources.implementation.OCLocalTransferDataSource
import com.owncloud.android.data.transfers.datasources.implementation.OCLocalTransferDataSource.Companion.toEntity
import com.owncloud.android.data.transfers.datasources.implementation.OCLocalTransferDataSource.Companion.toModel
import com.owncloud.android.data.transfers.db.OCTransferEntity
import com.owncloud.android.data.transfers.db.TransferDao
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferResult
import com.owncloud.android.domain.transfers.model.TransferStatus
import com.owncloud.android.domain.transfers.model.UploadEnqueuedBy
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCLocalTransferDataSourceTest {
    private lateinit var ocLocalTransferDataSource: OCLocalTransferDataSource
    private val transferDao = mockkClass(TransferDao::class)
    private val id = -1L
    private val ocTransfer = OCTransfer(
        id = id,
        localPath = "/local/path",
        remotePath = "/remote/path",
        accountName = OC_ACCOUNT_NAME,
        fileSize = 1024L,
        status = TransferStatus.TRANSFER_IN_PROGRESS,
        localBehaviour = UploadBehavior.MOVE,
        forceOverwrite = true,
        createdBy = UploadEnqueuedBy.ENQUEUED_BY_USER
    )
    private val transferEntity: OCTransferEntity = ocTransfer.toEntity()

    @Before
    fun init() {
        val db = mockkClass(OwncloudDatabase::class)

        every { db.transferDao() } returns transferDao

        ocLocalTransferDataSource = OCLocalTransferDataSource(transferDao)
    }

    @Test
    fun `saveTransfer returns a Long`() {
        val resultExpected = 1L
        every {
            transferDao.insertOrReplace(any())
        } returns resultExpected

        val resultActual = ocLocalTransferDataSource.saveTransfer(ocTransfer)

        assertEquals(resultExpected, resultActual)

        verify(exactly = 1) {
            transferDao.insertOrReplace(ocTransfer.toEntity())
        }
    }

    @Test(expected = Exception::class)
    fun `saveTransfer returns an exception when dao receive an exception`() {

        every {
            transferDao.insertOrReplace(any())
        } throws Exception()

        ocLocalTransferDataSource.saveTransfer(ocTransfer)
    }
    @Test
    fun `updateTransfer returns a Long`() {
        val resultExpected = 1L
        every {
            transferDao.insertOrReplace(any())
        } returns resultExpected

        ocLocalTransferDataSource.updateTransfer(ocTransfer)

        verify(exactly = 1) {
            transferDao.insertOrReplace(ocTransfer.toEntity())
        }
    }

    @Test(expected = Exception::class)
    fun `updateTransfer returns an exception when dao receive an exception`() {
        every {
            transferDao.insertOrReplace(any())
        } throws Exception()

        ocLocalTransferDataSource.updateTransfer(ocTransfer)
    }
    @Test
    fun `updateTransferStatusToInProgressById returns a unit`() {
        val resultExpected = 10L
        every {
            transferDao.updateTransferStatusWithId(resultExpected, TransferStatus.TRANSFER_IN_PROGRESS.value)
        } returns Unit

        ocLocalTransferDataSource.updateTransferStatusToInProgressById(resultExpected)

        verify(exactly = 1) {
            transferDao.updateTransferStatusWithId(resultExpected, TransferStatus.TRANSFER_IN_PROGRESS.value)
        }
    }

    @Test(expected = Exception::class)
    fun `updateTransferStatusToInProgressById returns an exception when dao receive an exception`() {
        val resultExpected = 10L
        every {
            transferDao.updateTransferStatusWithId(resultExpected, TransferStatus.TRANSFER_IN_PROGRESS.value)
        } throws Exception()

        ocLocalTransferDataSource.updateTransferStatusToInProgressById(resultExpected)

    }

    @Test
    fun `updateTransferStatusToEnqueuedById returns a unit`() {
        val resultExpected = 10L
        every {
            transferDao.updateTransferStatusWithId(resultExpected, TransferStatus.TRANSFER_QUEUED.value)
        } returns Unit

        ocLocalTransferDataSource.updateTransferStatusToEnqueuedById(resultExpected)

        verify(exactly = 1) {
            transferDao.updateTransferStatusWithId(resultExpected, TransferStatus.TRANSFER_QUEUED.value)
        }
    }

    @Test(expected = Exception::class)
    fun `updateTransferStatusToEnqueuedById returns an exception when dao receive an exception`() {
        val resultExpected = 10L
        every {
            transferDao.updateTransferStatusWithId(resultExpected, TransferStatus.TRANSFER_QUEUED.value)
        } throws Exception()

        ocLocalTransferDataSource.updateTransferStatusToEnqueuedById(resultExpected)

    }
    @Test
    fun `updateTransferWhenFinished returns a unit`() {
        val timestamp = System.currentTimeMillis()
        every {
            transferDao.updateTransferWhenFinished(id, TransferStatus.TRANSFER_SUCCEEDED.value, timestamp, TransferResult.UPLOADED.value)
        } returns Unit

        ocLocalTransferDataSource.updateTransferWhenFinished(id, TransferStatus.TRANSFER_SUCCEEDED, timestamp, TransferResult.UPLOADED)

        verify(exactly = 1) {
            transferDao.updateTransferWhenFinished(id, TransferStatus.TRANSFER_SUCCEEDED.value, timestamp, TransferResult.UPLOADED.value)
        }
    }

    @Test(expected = Exception::class)
    fun `updateTransferWhenFinished returns an exception when dao receive an exception`() {
        val timestamp = System.currentTimeMillis()
        every {
            transferDao.updateTransferWhenFinished(id, TransferStatus.TRANSFER_SUCCEEDED.value, timestamp, TransferResult.UPLOADED.value)
        } throws Exception()

        ocLocalTransferDataSource.updateTransferWhenFinished(id, TransferStatus.TRANSFER_SUCCEEDED, timestamp, TransferResult.UPLOADED)

    }

    @Test
    fun `updateTransferLocalPath returns a unit`() {
        every {
            transferDao.updateTransferLocalPath(id, ocTransfer.localPath)
        } returns Unit

        ocLocalTransferDataSource.updateTransferLocalPath(id, ocTransfer.localPath)

        verify(exactly = 1) {
            transferDao.updateTransferLocalPath(id, ocTransfer.localPath)
        }
    }

    @Test(expected = Exception::class)
    fun `updateTransferLocalPath returns an exception when dao receive an exception`() {
        every {
            transferDao.updateTransferLocalPath(id, ocTransfer.localPath)
        } throws Exception()

        ocLocalTransferDataSource.updateTransferLocalPath(id, ocTransfer.localPath)

    }

    @Test
    fun `updateTransferStorageDirectoryInLocalPath returns a unit`() {
        val oldDirectory = "oldDirectory"
        val newDirectory = "newDirectory"

        every {
            transferDao.updateTransferStorageDirectoryInLocalPath(id, oldDirectory, newDirectory)
        } returns Unit

        ocLocalTransferDataSource.updateTransferStorageDirectoryInLocalPath(id, oldDirectory, newDirectory)

        verify(exactly = 1) {
            transferDao.updateTransferStorageDirectoryInLocalPath(id, oldDirectory, newDirectory)
        }
    }

    @Test(expected = Exception::class)
    fun `updateTransferStorageDirectoryInLocalPath returns an exception when dao receive an exception`() {
        val oldDirectory = "oldDirectory"
        val newDirectory = "newDirectory"

        every {
            transferDao.updateTransferStorageDirectoryInLocalPath(id, oldDirectory, newDirectory)
        } throws Exception()

        ocLocalTransferDataSource.updateTransferStorageDirectoryInLocalPath(id, oldDirectory, newDirectory)

    }
    @Test
    fun `deleteTransferById returns a unit`() {
        every {
            transferDao.deleteTransferWithId(id)
        } returns Unit

        ocLocalTransferDataSource.deleteTransferById(id)

        verify(exactly = 1) {
            transferDao.deleteTransferWithId(id)
        }
    }

    @Test(expected = Exception::class)
    fun `deleteTransferById returns an exception when dao receive an exception`() {
        every {
            transferDao.deleteTransferWithId(id)
        } throws Exception()

        ocLocalTransferDataSource.deleteTransferById(id)

    }
    @Test
    fun `deleteAllTransfersFromAccount returns a unit`() {
        every {
            transferDao.deleteTransfersWithAccountName(OC_ACCOUNT_NAME)
        } returns Unit

        ocLocalTransferDataSource.deleteAllTransfersFromAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            transferDao.deleteTransfersWithAccountName(OC_ACCOUNT_NAME)
        }
    }

    @Test(expected = Exception::class)
    fun `deleteAllTransfersFromAccount returns an exception when dao receive an exception`() {
        every {
            transferDao.deleteTransfersWithAccountName(OC_ACCOUNT_NAME)
        } throws Exception()

        ocLocalTransferDataSource.deleteAllTransfersFromAccount(OC_ACCOUNT_NAME)

    }
    @Test
    fun `getTransferById returns a OCTransfer`() {
        every {
            transferDao.getTransferWithId(any())
        } returns ocTransfer.toEntity()

        val actualResult = ocLocalTransferDataSource.getTransferById(id)

        assertEquals(ocTransfer, actualResult)

        verify(exactly = 1) {
            transferDao.getTransferWithId(id)
        }
    }

    @Test(expected = Exception::class)
    fun `getTransferById returns an exception when dao receive an exception`() {
        every {
            transferDao.getTransferWithId(any())
        } throws Exception()

        ocLocalTransferDataSource.getTransferById(id)

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

    @Test(expected = Exception::class)
    fun `getAllTransfers returns an exception when dao receive an exception`() {

        every {
            transferDao.getAllTransfers()
        } throws Exception()

        ocLocalTransferDataSource.getAllTransfers()

    }

    @Test
    fun `getAllTransfersAsStream returns a flow of list of OCTransfer`() = runBlocking {

            every {
                transferDao.getAllTransfersAsStream()
            } returns flowOf(listOf(transferEntity))

            val actualResult = ocLocalTransferDataSource.getAllTransfersAsStream()

            actualResult.collect { result ->
                assertEquals(listOf(transferEntity.toModel()), result)
            }

            verify(exactly = 1) {
                transferDao.getAllTransfersAsStream()
            }
        }

    @Test(expected = Exception::class)
    fun `getAllTransfersAsStream returns an exception when dao receive an exception`() {

        every {
            transferDao.getAllTransfersAsStream()
        } throws Exception()

        ocLocalTransferDataSource.getAllTransfersAsStream()

    }

    @Test
    fun `getLastTransferFor returns a OCTransfer`() {

        every {
            transferDao.getLastTransferWithRemotePathAndAccountName(ocTransfer.remotePath, OC_ACCOUNT_NAME)
        } returns transferEntity

        val actualResult = ocLocalTransferDataSource.getLastTransferFor(ocTransfer.remotePath, OC_ACCOUNT_NAME)

        assertEquals(transferEntity.toModel(), actualResult)

        verify(exactly = 1) {
            transferDao.getLastTransferWithRemotePathAndAccountName(ocTransfer.remotePath, OC_ACCOUNT_NAME)
        }
    }

    @Test(expected = Exception::class)
    fun `getLastTransferFor returns an exception`() {

        every {
            transferDao.getLastTransferWithRemotePathAndAccountName(ocTransfer.remotePath, OC_ACCOUNT_NAME)
        } throws Exception()

        ocLocalTransferDataSource.getLastTransferFor(ocTransfer.remotePath, OC_ACCOUNT_NAME)

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

    @Test(expected = Exception::class)
    fun `getCurrentAndPendingTransfers returns an exception when dao receive an exception`() {

        every {
            transferDao.getTransfersWithStatus(listOf(TransferStatus.TRANSFER_IN_PROGRESS.value, TransferStatus.TRANSFER_QUEUED.value))
        } throws Exception()

        ocLocalTransferDataSource.getCurrentAndPendingTransfers()

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

    @Test(expected = Exception::class)
    fun `getFailedTransfers returns an Exception when dao receive an exception`() {

        every {
            transferDao.getTransfersWithStatus(listOf(TransferStatus.TRANSFER_FAILED.value))
        } throws Exception()

        ocLocalTransferDataSource.getFailedTransfers()

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

    @Test(expected = Exception::class)
    fun `getFinishedTransfers returns an exception when dao receive an exception`() {

        every {
            transferDao.getTransfersWithStatus(listOf(TransferStatus.TRANSFER_SUCCEEDED.value))
        } throws Exception()

        ocLocalTransferDataSource.getFinishedTransfers()

    }

    @Test
    fun `clearFailedTransfers returns unit`() {

        every {
            transferDao.deleteTransfersWithStatus(TransferStatus.TRANSFER_FAILED.value)
        } returns Unit

        ocLocalTransferDataSource.clearFailedTransfers()

        verify(exactly = 1) {
            transferDao.deleteTransfersWithStatus(TransferStatus.TRANSFER_FAILED.value)
        }
    }

    @Test(expected = Exception::class)
    fun `clearFailedTransfers returns an exception when dao receive an exception`() {

        every {
            transferDao.deleteTransfersWithStatus(TransferStatus.TRANSFER_FAILED.value)
        } throws Exception()

        ocLocalTransferDataSource.clearFailedTransfers()

    }

    @Test
    fun `clearSuccessfulTransfers returns unit`() {

        every {
            transferDao.deleteTransfersWithStatus(TransferStatus.TRANSFER_SUCCEEDED.value)
        } returns Unit

        ocLocalTransferDataSource.clearSuccessfulTransfers()

        verify(exactly = 1) {
            transferDao.deleteTransfersWithStatus(TransferStatus.TRANSFER_SUCCEEDED.value)
        }
    }

    @Test(expected = Exception::class)
    fun `clearSuccessfulTransfers returns an exception when dao receive an exception`() {

        every {
            transferDao.deleteTransfersWithStatus(TransferStatus.TRANSFER_SUCCEEDED.value)
        } throws Exception()

        ocLocalTransferDataSource.clearSuccessfulTransfers()

    }
}