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

package com.owncloud.android.data.file.datasources

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.files.datasources.implementation.OCRemoteFileDataSource
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.RemoteFile
import com.owncloud.android.lib.resources.files.services.FileService
import com.owncloud.android.lib.resources.files.services.implementation.OCFileService
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FOLDER
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_BASIC_AUTH
import com.owncloud.android.testutil.OC_SPACE_PROJECT_WITH_IMAGE
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class OCRemoteFileDataSourceTest {
    private lateinit var ocRemoteFileDataSource: OCRemoteFileDataSource

    private val clientManager: ClientManager = mockk(relaxed = true)
    private val ocFileService: OCFileService = mockk()
    private val sourceRemotePath = "source"
    private val remoteResult: RemoteOperationResult<Unit> =
        createRemoteOperationResultMock(data = Unit, isSuccess = true)

    private val remoteFileList = arrayListOf(
        RemoteFile(
            remotePath = OC_FILE.remotePath,
            mimeType = OC_FILE.mimeType,
            length = OC_FILE.length,
            creationTimestamp = OC_FILE.creationTimestamp!!,
            modifiedTimestamp = OC_FILE.modificationTimestamp,
            etag = OC_FILE.etag,
            permissions = OC_FILE.permissions,
            remoteId = OC_FILE.remoteId,
            privateLink = OC_FILE.privateLink,
            owner = OC_FILE.owner,
            sharedByLink = OC_FILE.sharedByLink,
            sharedWithSharee = OC_FILE.sharedWithSharee!!,
        )
    )

    private val remoteFile =
        RemoteFile(
            remotePath = OC_FILE.remotePath,
            mimeType = OC_FILE.mimeType,
            length = OC_FILE.length,
            creationTimestamp = OC_FILE.creationTimestamp!!,
            modifiedTimestamp = OC_FILE.modificationTimestamp,
            etag = OC_FILE.etag,
            permissions = OC_FILE.permissions,
            remoteId = OC_FILE.remoteId,
            privateLink = OC_FILE.privateLink,
            owner = OC_FILE.owner,
            sharedByLink = OC_FILE.sharedByLink,
            sharedWithSharee = OC_FILE.sharedWithSharee!!,
        )

    @Before
    fun init() {
        every { clientManager.getFileService(any()) } returns ocFileService

        ocRemoteFileDataSource = OCRemoteFileDataSource(clientManager)
    }

    @Test
    fun `checkPathExistence returns true when there is date`() {
        val checkPathExistenceRemoteResult: RemoteOperationResult<Boolean> =
            createRemoteOperationResultMock(data = true, isSuccess = true)

        every {
            ocFileService.checkPathExistence(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, true)
        } returns checkPathExistenceRemoteResult

        val checkPathExistence = ocRemoteFileDataSource.checkPathExistence(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, true, OC_ACCOUNT_NAME, null)

        assertNotNull(checkPathExistence)
        assertEquals(checkPathExistenceRemoteResult.data, checkPathExistence)

        verify(exactly = 1) { ocFileService.checkPathExistence(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, true) }
    }

    @Test
    fun `checkPathExistence returns false when there is not date`() {
        val checkPathExistenceRemoteResult: RemoteOperationResult<Boolean> =
            createRemoteOperationResultMock(data = false, isSuccess = true)

        every {
            ocFileService.checkPathExistence(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, true)
        } returns checkPathExistenceRemoteResult

        val checkPathExistence = ocRemoteFileDataSource.checkPathExistence(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, true, OC_ACCOUNT_NAME, null)

        assertNotNull(checkPathExistence)
        assertEquals(checkPathExistenceRemoteResult.data, checkPathExistence)

        verify(exactly = 1) { ocFileService.checkPathExistence(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, true) }
    }

    @Test(expected = Exception::class)
    fun `checkPathExistence returns an exception when checkPathExistence receive an exception`() {
        every {
            ocFileService.checkPathExistence(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, true)
        } throws Exception()

        ocRemoteFileDataSource.checkPathExistence(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, true, OC_ACCOUNT_NAME, null)
    }

    @Test
    fun `createFolder returns unit when createFolder is ok`() {

        every {
            ocFileService.createFolder(remotePath = OC_FOLDER.remotePath, createFullPath = false, isChunkFolder = false)
        } returns remoteResult

        val createFolderResult = ocRemoteFileDataSource.createFolder(OC_FOLDER.remotePath, false, false, OC_ACCOUNT_NAME, null)

        assertNotNull(createFolderResult)
        assertEquals(remoteResult.data, createFolderResult)

        verify(exactly = 1) { ocFileService.createFolder(any(), any(), any()) }
    }

    @Test(expected = Exception::class)
    fun `createFolder returns an exception when createFolder receive an exception`() {
        every {
            ocFileService.createFolder(OC_FOLDER.remotePath, false, false)
        } throws Exception()

        ocRemoteFileDataSource.createFolder(OC_FOLDER.remotePath, false, false, OC_ACCOUNT_NAME, null)
    }

    @Test
    fun `getAvailableRemotePath returns same path if file does not exist`() {
        every {
            clientManager.getFileService(OC_ACCOUNT_NAME).checkPathExistence(
                path = OC_FILE.remotePath,
                isUserLogged = false,
                spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
            ).data
        } returns false

        val firstCopyName = ocRemoteFileDataSource.getAvailableRemotePath(
            remotePath = OC_FILE.remotePath,
            accountName = OC_ACCOUNT_NAME,
            spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
            isUserLogged = false,
        )
        assertEquals(OC_FILE.remotePath, firstCopyName)

        verify(exactly = 1) {
            clientManager.getFileService(OC_ACCOUNT_NAME).checkPathExistence(
                path = OC_FILE.remotePath,
                isUserLogged = false,
                spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
            )
        }
    }

    @Test
    fun `getAvailableRemotePath returns path with one if file exists`() {
        val suffix = "(1)"
        val extension = "jpt"

        every {
            clientManager.getFileService(OC_ACCOUNT_NAME).checkPathExistence(
                path = any(),
                isUserLogged = true,
                spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
            ).data
        } returnsMany listOf(true, false)

        val firstCopyName = ocRemoteFileDataSource.getAvailableRemotePath(
            remotePath = OC_FILE.remotePath,
            accountName = OC_ACCOUNT_NAME,
            spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
            isUserLogged = true,
        )
        assertEquals("${OC_FILE.remotePath.substringBeforeLast('.', "")} $suffix.$extension", firstCopyName)
    }

    @Test
    fun `getAvailableRemotePath returns path with two if file exists and with one`() {
        val suffix = "(2)"
        val extension = "jpt"

        every {
            clientManager.getFileService(OC_ACCOUNT_NAME).checkPathExistence(
                path = any(),
                isUserLogged = true,
                spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
            ).data
        } returnsMany listOf(true, true, false)

        val firstCopyName = ocRemoteFileDataSource.getAvailableRemotePath(
            remotePath = OC_FILE.remotePath,
            accountName = OC_ACCOUNT_NAME,
            spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
            isUserLogged = true,
        )
        assertEquals("${OC_FILE.remotePath.substringBeforeLast('.', "")} $suffix.$extension", firstCopyName)
    }

    @Test
    fun `getAvailableRemotePath returns path with two ones if copying file with one`() {
        val suffix = "(1)"
        val extension = "jpt"

        every {
            clientManager.getFileService(OC_ACCOUNT_NAME).checkPathExistence(
                path = any(),
                isUserLogged = false,
                spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
            ).data
        } returnsMany listOf(true, false)

        val firstCopyName = ocRemoteFileDataSource.getAvailableRemotePath(
            remotePath = "${OC_FILE.remotePath.substringBeforeLast('.', "")} $suffix.$extension",
            accountName = OC_ACCOUNT_NAME,
            spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
            isUserLogged = false,
        )
        assertEquals("${OC_FILE.remotePath.substringBeforeLast('.', "")} $suffix $suffix.$extension", firstCopyName)

        verify(exactly = 2) {
            clientManager.getFileService(OC_ACCOUNT_NAME).checkPathExistence(
                path = any(),
                isUserLogged = false,
                spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
            )
        }
    }

    @Test
    fun `moveFile returns unit when replace is true`() {

        every {
            ocFileService.moveFile(
                sourceRemotePath = any(),
                targetRemotePath = OC_FILE.remotePath,
                spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
                replace = true,
            )
        } returns remoteResult

        ocRemoteFileDataSource.moveFile(
            sourceRemotePath,
            OC_FILE.remotePath,
            OC_ACCOUNT_NAME,
            OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
            true,
        )

        verify(exactly = 1) {
            ocFileService.moveFile(
                sourceRemotePath = any(),
                targetRemotePath = OC_FILE.remotePath,
                spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
                replace = true,
            )
        }

    }

    @Test
    fun `moveFile returns unit when replace is false`() {

        every {
            ocFileService.moveFile(any(), OC_FILE.remotePath, OC_SPACE_PROJECT_WITH_IMAGE.webUrl, false)
        } returns remoteResult

        ocRemoteFileDataSource.moveFile(
            sourceRemotePath, OC_FILE.remotePath, OC_ACCOUNT_NAME,
            OC_SPACE_PROJECT_WITH_IMAGE.webUrl, false
        )

        verify(exactly = 1) {
            ocFileService.moveFile(
                any(), OC_FILE.remotePath, OC_SPACE_PROJECT_WITH_IMAGE.webUrl, false
            )
        }
    }

    @Test(expected = Exception::class)
    fun `moveFile returns an exception when moveFile receive an exception`() {

        every {
            ocFileService.moveFile(
                sourceRemotePath = any(),
                targetRemotePath = OC_FILE.remotePath,
                spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
                replace = false
            )
        } throws Exception()

        ocRemoteFileDataSource.moveFile(
            sourceRemotePath,
            OC_FILE.remotePath,
            OC_ACCOUNT_NAME,
            OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
            false,
        )

    }

    @Test
    fun `readFile should call readFile and convert to model and returns OCFile object`() {
        val expectedOCFile = OC_FILE.copy(id = null, parentId = null, availableOfflineStatus = null) // Eliminar id y parentId

        val fileServiceMock = mockk<FileService>(relaxed = true)

        val clientManagerMock = mockk<ClientManager>()

        val remoteResult: RemoteOperationResult<RemoteFile> =
            createRemoteOperationResultMock(data = remoteFile, isSuccess = true)

        every {
            fileServiceMock.readFile(
                remotePath = OC_FILE.remotePath,
                spaceWebDavUrl = OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
            )
        } returns remoteResult

        every { clientManagerMock.getFileService(OC_ACCOUNT_NAME) } returns fileServiceMock

        val ocRemoteFileDataSource = OCRemoteFileDataSource(clientManagerMock)
        val result = ocRemoteFileDataSource.readFile(OC_FILE.remotePath, OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.webUrl)

        assertEquals(expectedOCFile, result)

        verify(exactly = 1) {
            fileServiceMock.readFile(
                OC_FILE.remotePath, OC_SPACE_PROJECT_WITH_IMAGE.webUrl
            )
        }
    }

    @Test
    fun `refreshFolder call refreshFolder, convert to model and  returns a list of OCFile`() {
        val expectedFile = arrayListOf(OC_FILE.copy(id = null, parentId = null, availableOfflineStatus = null)) // Eliminar id y parentId

        val remoteResult: RemoteOperationResult<ArrayList<RemoteFile>> =
            createRemoteOperationResultMock(data = remoteFileList, isSuccess = true)

        every {
            ocFileService.refreshFolder(OC_FILE.remotePath, OC_SPACE_PROJECT_WITH_IMAGE.webUrl)
        } returns remoteResult

        val result = ocRemoteFileDataSource.refreshFolder(
            OC_FILE.remotePath,
            OC_ACCOUNT_NAME,
            OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
        )
        assertEquals(expectedFile, result)

        verify(exactly = 1) {
            ocFileService.refreshFolder(OC_FILE.remotePath, OC_SPACE_PROJECT_WITH_IMAGE.webUrl)
        }
    }

    @Test
    fun `deleteFile returns Unit when deleteFile is ok`() {
        every {
            ocFileService.removeFile(OC_FILE.remotePath, OC_SPACE_PROJECT_WITH_IMAGE.webUrl)
        } returns remoteResult

        ocRemoteFileDataSource.deleteFile(
            OC_FILE.remotePath, OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.webUrl,
        )

        verify(exactly = 1) {
            ocFileService.removeFile(OC_FILE.remotePath, OC_SPACE_PROJECT_WITH_IMAGE.webUrl)
        }
    }

    @Test(expected = Exception::class)
    fun `deleteFile returns an exception when deleteFile receive an exception`() {
        every {
            ocFileService.removeFile(OC_FILE.remotePath, OC_SPACE_PROJECT_WITH_IMAGE.webUrl)
        } throws Exception()

        ocRemoteFileDataSource.deleteFile(OC_FILE.remotePath, OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.webUrl)
    }

    @Test
    fun `renameFile returns Unit when renameFile is ok`() {
        val oldName = "oldName"
        val oldRemotePath = "oldRemotePath"
        val newName = "newName"
        every {
            ocFileService.renameFile(oldName, oldRemotePath, newName, true, OC_SPACE_PROJECT_WITH_IMAGE.webUrl)
        } returns remoteResult

        ocRemoteFileDataSource.renameFile(oldName, oldRemotePath, newName, true, OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.webUrl)

        verify(exactly = 1) {
            ocFileService.renameFile(oldName, oldRemotePath, newName, true, OC_SPACE_PROJECT_WITH_IMAGE.webUrl)
        }
    }

    @Test(expected = Exception::class)
    fun `renameFile returns an exception when renameFile receive an exception`() {
        val oldName = "oldName"
        val oldRemotePath = "oldRemotePath"
        val newName = "newName"
        every {
            ocFileService.renameFile(oldName, oldRemotePath, newName, true, OC_SPACE_PROJECT_WITH_IMAGE.webUrl)
        } throws Exception()

        ocRemoteFileDataSource.renameFile(oldName, oldRemotePath, newName, true, OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.webUrl)

    }
}
