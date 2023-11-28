/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
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

package com.owncloud.android.data.files.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.files.datasources.implementation.OCRemoteFileDataSource.Companion.toModel
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.lib.resources.files.RemoteFile
import com.owncloud.android.lib.resources.files.services.implementation.OCFileService
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FOLDER
import com.owncloud.android.testutil.REMOTE_FILE
import com.owncloud.android.testutil.REMOTE_META_FILE
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OCRemoteFileDataSourceTest {

    private lateinit var ocRemoteFileDataSource: OCRemoteFileDataSource

    private val ocFileService: OCFileService = mockk()
    private val clientManager: ClientManager = mockk(relaxed = true)

    private val sourceRemotePath = "/source/remote/path/file.txt"
    private val targetRemotePath = "/target/remote/path/file.txt"

    private val remoteResult = createRemoteOperationResultMock(data = Unit, isSuccess = true)

    @Before
    fun setUp() {
        every { clientManager.getFileService(OC_ACCOUNT_NAME) } returns ocFileService

        ocRemoteFileDataSource = OCRemoteFileDataSource(clientManager)
    }

    @Test
    fun `checkPathExistence returns true when the path exists in remote`() {
        val checkPathExistenceRemoteResult = createRemoteOperationResultMock(data = true, isSuccess = true)

        every {
            ocFileService.checkPathExistence(sourceRemotePath, true)
        } returns checkPathExistenceRemoteResult

        val checkPathExistence = ocRemoteFileDataSource.checkPathExistence(sourceRemotePath, true, OC_ACCOUNT_NAME, null)

        assertTrue(checkPathExistence)

        verify(exactly = 1) {
            clientManager.getFileService(OC_ACCOUNT_NAME)
            ocFileService.checkPathExistence(sourceRemotePath, true)
        }
    }

    @Test
    fun `checkPathExistence returns false when the path does not exist in remote`() {
        val checkPathExistenceRemoteResult = createRemoteOperationResultMock(data = false, isSuccess = true)

        every {
            ocFileService.checkPathExistence(sourceRemotePath, true)
        } returns checkPathExistenceRemoteResult

        val checkPathExistence = ocRemoteFileDataSource.checkPathExistence(sourceRemotePath, true, OC_ACCOUNT_NAME, null)

        assertFalse(checkPathExistence)

        verify(exactly = 1) {
            clientManager.getFileService(OC_ACCOUNT_NAME)
            ocFileService.checkPathExistence(sourceRemotePath, true)
        }
    }

    @Test
    fun `copyFile copies a file and returns a fileRemoteId`() {
        val fileRemoteId = "fileRemoteId"

        val remoteResult = createRemoteOperationResultMock(data = fileRemoteId as String?, isSuccess = true)

        every {
            ocFileService.copyFile(
                sourceRemotePath = sourceRemotePath,
                targetRemotePath = targetRemotePath,
                sourceSpaceWebDavUrl = null,
                targetSpaceWebDavUrl = null,
                replace = any(),
            )
        } returns remoteResult

        val result = ocRemoteFileDataSource.copyFile(
            sourceRemotePath,
            targetRemotePath,
            OC_ACCOUNT_NAME,
            null,
            null,
            true,
        )

        assertEquals(fileRemoteId, result)

        verify(exactly = 1) {
            clientManager.getFileService(OC_ACCOUNT_NAME)
            ocFileService.copyFile(
                sourceRemotePath = sourceRemotePath,
                targetRemotePath = targetRemotePath,
                sourceSpaceWebDavUrl = null,
                targetSpaceWebDavUrl = null,
                replace = true,
            )
        }
    }

    @Test
    fun `createFolder creates folder in remote correctly`() {
        every {
            ocFileService.createFolder(remotePath = sourceRemotePath, createFullPath = false)
        } returns remoteResult

        ocRemoteFileDataSource.createFolder(
            remotePath = sourceRemotePath,
            createFullPath = false,
            isChunksFolder = false,
            accountName = OC_ACCOUNT_NAME,
            spaceWebDavUrl = null
        )

        verify(exactly = 1) {
            clientManager.getFileService(OC_ACCOUNT_NAME)
            ocFileService.createFolder(sourceRemotePath, false)
        }
    }

    @Test
    fun `getAvailableRemotePath returns same String path if file does not exist`() {
        val checkPathExistenceRemoteResult = createRemoteOperationResultMock(data = false, isSuccess = true)

        every {
            ocFileService.checkPathExistence(sourceRemotePath, true)
        } returns checkPathExistenceRemoteResult

        val firstCopyName = ocRemoteFileDataSource.getAvailableRemotePath(
            remotePath = sourceRemotePath,
            accountName = OC_ACCOUNT_NAME,
            spaceWebDavUrl = null,
            isUserLogged = true,
        )

        assertEquals(sourceRemotePath, firstCopyName)

        verify(exactly = 1) {
            clientManager.getFileService(OC_ACCOUNT_NAME)
            ocFileService.checkPathExistence(sourceRemotePath, true)
        }
    }

    @Test
    fun `getAvailableRemotePath returns String path with (1) if file already exists`() {
        val checkPathExistenceRemoteResultFirst = createRemoteOperationResultMock(data = true, isSuccess = true)
        val checkPathExistenceRemoteResultSecond = createRemoteOperationResultMock(data = false, isSuccess = true)
        val finalRemotePath = "/source/remote/path/file (1).txt"

        every {
            ocFileService.checkPathExistence(sourceRemotePath, true)
        } returns checkPathExistenceRemoteResultFirst
        every {
            ocFileService.checkPathExistence(finalRemotePath, true)
        } returns checkPathExistenceRemoteResultSecond

        val firstCopyName = ocRemoteFileDataSource.getAvailableRemotePath(
            remotePath = sourceRemotePath,
            accountName = OC_ACCOUNT_NAME,
            spaceWebDavUrl = null,
            isUserLogged = true,
        )

        assertEquals(finalRemotePath, firstCopyName)

        verify(exactly = 2) { clientManager.getFileService(OC_ACCOUNT_NAME) }
        verify(exactly = 1) {
            ocFileService.checkPathExistence(sourceRemotePath, true)
            ocFileService.checkPathExistence(finalRemotePath,  true)
        }
    }

    @Test
    fun `getAvailableRemotePath returns String path with two (1) if file with (1) already exists`() {
        val checkPathExistenceRemoteResultFirst = createRemoteOperationResultMock(data = true, isSuccess = true)
        val checkPathExistenceRemoteResultSecond = createRemoteOperationResultMock(data = false, isSuccess = true)
        val remotePath = "/remote/path/file (1).txt"
        val finalRemotePath = "/remote/path/file (1) (1).txt"

        every {
            ocFileService.checkPathExistence(remotePath, true)
        } returns checkPathExistenceRemoteResultFirst
        every {
            ocFileService.checkPathExistence(finalRemotePath, true)
        } returns checkPathExistenceRemoteResultSecond

        val firstCopyName = ocRemoteFileDataSource.getAvailableRemotePath(
            remotePath = remotePath,
            accountName = OC_ACCOUNT_NAME,
            spaceWebDavUrl = null,
            isUserLogged = true,
        )

        assertEquals(finalRemotePath, firstCopyName)

        verify(exactly = 2) { clientManager.getFileService(OC_ACCOUNT_NAME) }
        verify(exactly = 1) {
            ocFileService.checkPathExistence(remotePath, true)
            ocFileService.checkPathExistence(finalRemotePath, true)
        }
    }

    @Test
    fun `moveFile moves a file correctly`() {
        every {
            ocFileService.moveFile(
                sourceRemotePath = sourceRemotePath,
                targetRemotePath = targetRemotePath,
                spaceWebDavUrl = null,
                replace = any(),
            )
        } returns remoteResult

        ocRemoteFileDataSource.moveFile(
            sourceRemotePath,
            targetRemotePath,
            OC_ACCOUNT_NAME,
            null,
            true,
        )

        verify(exactly = 1) {
            clientManager.getFileService(OC_ACCOUNT_NAME)
            ocFileService.moveFile(
                sourceRemotePath = sourceRemotePath,
                targetRemotePath = targetRemotePath,
                spaceWebDavUrl = null,
                replace = true,
            )
        }
    }

    @Test
    fun `readFile returns a OCFile`() {
        val remoteResult = createRemoteOperationResultMock(data = REMOTE_FILE, isSuccess = true)

        every {
            ocFileService.readFile(
                remotePath = REMOTE_FILE.remotePath,
                spaceWebDavUrl = null
            )
        } returns remoteResult

        val result = ocRemoteFileDataSource.readFile(
            REMOTE_FILE.remotePath,
            OC_ACCOUNT_NAME,
            null,
        )

        assertEquals(REMOTE_FILE.toModel(), result)

        verify(exactly = 1) {
            clientManager.getFileService(OC_ACCOUNT_NAME)
            ocFileService.readFile(
                REMOTE_FILE.remotePath,
                null,
            )
        }
    }

    @Test
    fun `refreshFolder returns a list of OCFile`() {
        val remoteResult = createRemoteOperationResultMock(data = arrayListOf(REMOTE_FILE), isSuccess = true)

        every {
            ocFileService.refreshFolder(OC_FOLDER.remotePath, null)
        } returns remoteResult

        val result = ocRemoteFileDataSource.refreshFolder(
            OC_FOLDER.remotePath,
            OC_ACCOUNT_NAME,
            null,
        )
        assertEquals(arrayListOf(REMOTE_FILE.toModel()), result)

        verify(exactly = 1) {
            clientManager.getFileService(OC_ACCOUNT_NAME)
            ocFileService.refreshFolder(OC_FOLDER.remotePath, null)
        }
    }

    @Test
    fun `refreshFolder returns an empty list if service returns an empty list`() {
        val remoteResult = createRemoteOperationResultMock(data = arrayListOf<RemoteFile>(), isSuccess = true)

        every {
            ocFileService.refreshFolder(OC_FOLDER.remotePath, null)
        } returns remoteResult

        val result = ocRemoteFileDataSource.refreshFolder(
            OC_FOLDER.remotePath,
            OC_ACCOUNT_NAME,
            null,
        )
        assertEquals(emptyList<OCFile>(), result)

        verify(exactly = 1) {
            clientManager.getFileService(OC_ACCOUNT_NAME)
            ocFileService.refreshFolder(OC_FOLDER.remotePath, null)
        }
    }

    @Test
    fun `deleteFile deletes a file correctly`() {
        every {
            ocFileService.removeFile(OC_FILE.remotePath, null)
        } returns remoteResult

        ocRemoteFileDataSource.deleteFile(
            OC_FILE.remotePath,
            OC_ACCOUNT_NAME,
            null,
        )

        verify(exactly = 1) {
            clientManager.getFileService(OC_ACCOUNT_NAME)
            ocFileService.removeFile(OC_FILE.remotePath, null)
        }
    }

    @Test
    fun `renameFile renames a file correctly`() {
        val oldName = "oldName"
        val oldRemotePath = "/old/remote/path"
        val newName = "newName"

        every {
            ocFileService.renameFile(oldName, oldRemotePath, newName, false, null)
        } returns remoteResult

        ocRemoteFileDataSource.renameFile(
            oldName,
            oldRemotePath,
            newName,
            false,
            OC_ACCOUNT_NAME,
            null)

        verify(exactly = 1) {
            clientManager.getFileService(OC_ACCOUNT_NAME)
            ocFileService.renameFile(oldName, oldRemotePath, newName, false, null)
        }
    }

    @Test
    fun `getMetaFile returns a OCMetaFile`() {
        val remoteResult = createRemoteOperationResultMock(data = REMOTE_META_FILE, isSuccess = true)

        every {
            ocFileService.getMetaFileInfo(
                fileId = OC_FILE.remoteId!!,
            )
        } returns remoteResult

        val result = ocRemoteFileDataSource.getMetaFile(
            OC_FILE.remoteId!!,
            OC_ACCOUNT_NAME,
        )

        assertEquals(REMOTE_META_FILE.toModel(), result)

        verify(exactly = 1) {
            clientManager.getFileService(OC_ACCOUNT_NAME)
            ocFileService.getMetaFileInfo(OC_FILE.remoteId!!)
        }
    }
}
