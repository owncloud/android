/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
 * @author Juan Carlos Garrote Gascón
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

package com.owncloud.android.data.files.repository

import com.owncloud.android.data.files.datasources.LocalFileDataSource
import com.owncloud.android.data.files.datasources.RemoteFileDataSource
import com.owncloud.android.data.providers.LocalStorageProvider
import com.owncloud.android.data.spaces.datasources.LocalSpacesDataSource
import com.owncloud.android.domain.exceptions.ConflictException
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.MIME_DIR
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import com.owncloud.android.testutil.OC_AVAILABLE_OFFLINE_FILES
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FILE_AVAILABLE_OFFLINE
import com.owncloud.android.testutil.OC_FILE_DOWNLOADED
import com.owncloud.android.testutil.OC_FILE_WITH_SPACE_ID
import com.owncloud.android.testutil.OC_FILE_WITH_SYNC_INFO
import com.owncloud.android.testutil.OC_FILE_WITH_SYNC_INFO_AND_SPACE
import com.owncloud.android.testutil.OC_FOLDER_WITH_SPACE_ID
import com.owncloud.android.testutil.OC_META_FILE
import com.owncloud.android.testutil.OC_META_FILE_ROOT_FOLDER
import com.owncloud.android.testutil.OC_PARENT_FOLDER_WITH_SPACE_ID
import com.owncloud.android.testutil.OC_ROOT_FOLDER
import com.owncloud.android.testutil.OC_SPACE_PERSONAL
import com.owncloud.android.testutil.OC_SPACE_SHARES
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class OCFileRepositoryTest {

    private val localFileDataSource = mockk<LocalFileDataSource>(relaxUnitFun = true)
    private val remoteFileDataSource = mockk<RemoteFileDataSource>(relaxUnitFun = true)
    private val localSpacesDataSource = mockk<LocalSpacesDataSource>(relaxUnitFun = true)
    private val localStorageProvider = mockk<LocalStorageProvider>(relaxUnitFun = true)
    private val ocFileRepository = OCFileRepository(
        localFileDataSource,
        remoteFileDataSource,
        localSpacesDataSource,
        localStorageProvider
    )
    private val ocFileRepositorySpy = spyk(ocFileRepository)

    private val expectedRemotePath = OC_FOLDER_WITH_SPACE_ID.remotePath + OC_FILE_WITH_SPACE_ID.fileName
    private val storagePath = "/local/storage/path/username@demo.owncloud.com/Folder/Photos/image2.jpt"
    private val remoteId = "remoteId"
    private val searchText = "image"

    /*private val listOfFilesRetrieved = listOf(
        OC_FOLDER,
        OC_FOLDER.copy(remoteId = "one"),
        OC_FOLDER.copy(remoteId = "two")
    )
    private val listOfFileToRemove = listOf(
        OC_FOLDER.copy(id = 1),
        OC_FILE.copy(id = 2),
        OC_FILE.copy(id = 3)
    )

    private val timeInMilliseconds = 3600000L*/

    @Before
    fun setUp() {
        every {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_FILE_WITH_SPACE_ID.spaceId,
                accountName = OC_FILE_WITH_SPACE_ID.owner
            )
        } returns OC_SPACE_PERSONAL.root.webDavUrl
        every {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner
            )
        } returns OC_SPACE_PERSONAL.root.webDavUrl
    }

    @Test
    fun `createFolder creates a new folder and saves it`() {
        every {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner
            )
        } returns OC_SPACE_PERSONAL.root.webDavUrl
        // The result of this method is not used, so it can be anything
        every { localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(any(), any()) } returns emptyList()

        ocFileRepository.createFolder(OC_FOLDER_WITH_SPACE_ID.remotePath, OC_PARENT_FOLDER_WITH_SPACE_ID)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.createFolder(
                remotePath = OC_FOLDER_WITH_SPACE_ID.remotePath,
                createFullPath = false,
                isChunksFolder = false,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(
                folder = OC_PARENT_FOLDER_WITH_SPACE_ID,
                listOfFiles = withArg<List<OCFile>> {
                    assertEquals(1, it.size)
                    assertEquals(OC_FOLDER_WITH_SPACE_ID.remotePath, it[0].remotePath)
                    assertEquals(OC_PARENT_FOLDER_WITH_SPACE_ID.owner, it[0].owner)
                    assertEquals(0, it[0].length)
                    assertEquals(MIME_DIR, it[0].mimeType)
                    assertEquals(OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId, it[0].spaceId)
                    assertEquals("CK", it[0].permissions)
                }
            )
        }
    }

    @Test
    fun `copyFile returns a list with the OCFile in conflict (the copied OCFile) when replace parameter is empty and expected path already exists`() {
        every {
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns true

        val filesNeedAction = ocFileRepository.copyFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, emptyList(), true)

        assertEquals(listOf(OC_FILE_WITH_SPACE_ID), filesNeedAction)

        val sourceAndTargetSpaceId = OC_FILE_WITH_SPACE_ID.spaceId
        val sourceAndTargetOwner = OC_FILE_WITH_SPACE_ID.owner
        verify(exactly = 2) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = sourceAndTargetSpaceId,
                accountName = sourceAndTargetOwner
            )
        }
        verify(exactly = 1) {
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        }
    }

    @Test
    fun `copyFile returns an empty list with no OCFiles in conflict when replace parameter is empty and expected path doesn't exist`() {
        every {
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns false
        every {
            remoteFileDataSource.copyFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                sourceSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                targetSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
        } returns remoteId

        val filesNeedAction = ocFileRepository.copyFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, emptyList(), true)

        assertEquals(emptyList<OCFile>(), filesNeedAction)

        val sourceAndTargetSpaceId = OC_FILE_WITH_SPACE_ID.spaceId
        val sourceAndTargetOwner = OC_FILE_WITH_SPACE_ID.owner
        verify(exactly = 2) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = sourceAndTargetSpaceId,
                accountName = sourceAndTargetOwner
            )
        }
        verify(exactly = 1) {
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
            )
            remoteFileDataSource.copyFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                sourceSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                targetSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
            localFileDataSource.copyFile(
                sourceFile = OC_FILE_WITH_SPACE_ID,
                targetFolder = OC_FOLDER_WITH_SPACE_ID,
                finalRemotePath = expectedRemotePath,
                remoteId = remoteId,
                replace = null
            )
        }
    }

    @Test
    fun `copyFile returns an empty list with no OCFiles in conflict when replace parameter is true`() {
        every {
            remoteFileDataSource.copyFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                sourceSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                targetSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = true
            )
        } returns remoteId

        val filesNeedAction = ocFileRepository.copyFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, listOf(true), true)

        assertEquals(emptyList<OCFile>(), filesNeedAction)

        val sourceAndTargetSpaceId = OC_FILE_WITH_SPACE_ID.spaceId
        val sourceAndTargetOwner = OC_FILE_WITH_SPACE_ID.owner
        verify(exactly = 2) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = sourceAndTargetSpaceId,
                accountName = sourceAndTargetOwner
            )
        }
        verify(exactly = 1) {
            remoteFileDataSource.copyFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                sourceSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                targetSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = true
            )
            localFileDataSource.copyFile(
                sourceFile = OC_FILE_WITH_SPACE_ID,
                targetFolder = OC_FOLDER_WITH_SPACE_ID,
                finalRemotePath = expectedRemotePath,
                remoteId = remoteId,
                replace = true
            )
        }
    }

    @Test
    fun `copyFile returns an empty list with no OCFiles in conflict when replace parameter is false`() {
        val availableRemotePath = "$expectedRemotePath (1)"
        every {
            remoteFileDataSource.getAvailableRemotePath(
                remotePath = expectedRemotePath,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                isUserLogged = true
            )
        } returns availableRemotePath
        every {
            remoteFileDataSource.copyFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = availableRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                sourceSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                targetSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
        } returns remoteId

        val filesNeedAction = ocFileRepository.copyFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, listOf(false), true)

        assertEquals(emptyList<OCFile>(), filesNeedAction)

        val sourceAndTargetSpaceId = OC_FILE_WITH_SPACE_ID.spaceId
        val sourceAndTargetOwner = OC_FILE_WITH_SPACE_ID.owner
        verify(exactly = 2) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = sourceAndTargetSpaceId,
                accountName = sourceAndTargetOwner
            )
        }
        verify(exactly = 1) {
            remoteFileDataSource.getAvailableRemotePath(
                remotePath = expectedRemotePath,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                isUserLogged = true
            )
            remoteFileDataSource.copyFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = availableRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                sourceSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                targetSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
            localFileDataSource.copyFile(
                sourceFile = OC_FILE_WITH_SPACE_ID,
                targetFolder = OC_FOLDER_WITH_SPACE_ID,
                finalRemotePath = availableRemotePath,
                remoteId = remoteId,
                replace = false
            )
        }
    }

    @Test
    fun `copyFile returns an empty list with no OCFiles in conflict when replace parameter is null`() {
        val filesNeedAction = ocFileRepository.copyFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, listOf(null), true)

        assertEquals(emptyList<OCFile>(), filesNeedAction)

        val sourceAndTargetSpaceId = OC_FILE_WITH_SPACE_ID.spaceId
        val sourceAndTargetOwner = OC_FILE_WITH_SPACE_ID.owner
        verify(exactly = 2) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = sourceAndTargetSpaceId,
                accountName = sourceAndTargetOwner
            )
        }
    }

    @Test
    fun `copyFile removes target folder locally and throws a ConflictException when replace parameter is empty and expected path doesn't exist but target folder doesn't exist anymore`() {
        every {
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns false
        every {
            remoteFileDataSource.copyFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                sourceSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                targetSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
        } throws ConflictException()
        every {
            localFileDataSource.getFolderContent(OC_FOLDER_WITH_SPACE_ID.id!!)
        } returns emptyList()

        assertThrows(ConflictException::class.java) {
            ocFileRepository.copyFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, emptyList(), true)
        }

        val sourceAndTargetSpaceId = OC_FILE_WITH_SPACE_ID.spaceId
        val sourceAndTargetOwner = OC_FILE_WITH_SPACE_ID.owner
        verify(exactly = 2) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = sourceAndTargetSpaceId,
                accountName = sourceAndTargetOwner
            )
        }
        verify(exactly = 1) {
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
            )
            remoteFileDataSource.copyFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                sourceSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                targetSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
            localFileDataSource.getFolderContent(OC_FOLDER_WITH_SPACE_ID.id!!)
            localStorageProvider.deleteLocalFolderIfItHasNoFilesInside(OC_FOLDER_WITH_SPACE_ID)
            localFileDataSource.deleteFile(OC_FOLDER_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `copyFile removes source file locally and throws a FileNotFoundException when replace parameter is empty and expected path doesn't exist but source file doesn't exist anymore`() {
        every {
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns false
        every {
            remoteFileDataSource.copyFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                sourceSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                targetSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
        } throws FileNotFoundException()
        every {
            localStorageProvider.deleteLocalFile(OC_FILE_WITH_SPACE_ID)
        } returns true

        assertThrows(FileNotFoundException::class.java) {
            ocFileRepository.copyFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, emptyList(), true)
        }

        val sourceAndTargetSpaceId = OC_FILE_WITH_SPACE_ID.spaceId
        val sourceAndTargetOwner = OC_FILE_WITH_SPACE_ID.owner
        verify(exactly = 2) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = sourceAndTargetSpaceId,
                accountName = sourceAndTargetOwner
            )
        }
        verify(exactly = 1) {
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
            )
            remoteFileDataSource.copyFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                sourceSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                targetSpaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
            localStorageProvider.deleteLocalFile(OC_FILE_WITH_SPACE_ID)
            localFileDataSource.deleteFile(OC_FILE_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `getFileById returns a OCFile`() {
        every {
            localFileDataSource.getFileById(OC_FILE_WITH_SPACE_ID.id!!)
        } returns OC_FILE_WITH_SPACE_ID

        val ocFile = ocFileRepository.getFileById(OC_FILE_WITH_SPACE_ID.id!!)
        assertEquals(OC_FILE_WITH_SPACE_ID, ocFile)

        verify(exactly = 1) {
            localFileDataSource.getFileById(OC_FILE_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `getFileById returns null when local datasource returns a null file`() {
        every {
            localFileDataSource.getFileById(OC_FILE_WITH_SPACE_ID.id!!)
        } returns null

        val ocFile = ocFileRepository.getFileById(OC_FILE_WITH_SPACE_ID.id!!)
        assertNull(ocFile)

        verify(exactly = 1) {
            localFileDataSource.getFileById(OC_FILE_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `getFileByIdAsFlow returns a Flow with an OCFile`() = runTest {
        every {
            localFileDataSource.getFileByIdAsFlow(OC_FILE_WITH_SPACE_ID.id!!)
        } returns flowOf(OC_FILE_WITH_SPACE_ID)

        val ocFile = ocFileRepository.getFileByIdAsFlow(OC_FILE_WITH_SPACE_ID.id!!).first()
        assertEquals(OC_FILE_WITH_SPACE_ID, ocFile)

        verify(exactly = 1) {
            localFileDataSource.getFileByIdAsFlow(OC_FILE_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `getFileByIdAsFlow returns a Flow with null when local datasource returns a Flow with null`() = runTest {
        every {
            localFileDataSource.getFileByIdAsFlow(OC_FILE_WITH_SPACE_ID.id!!)
        } returns flowOf(null)

        val ocFile = ocFileRepository.getFileByIdAsFlow(OC_FILE_WITH_SPACE_ID.id!!).first()
        assertNull(ocFile)

        verify(exactly = 1) {
            localFileDataSource.getFileByIdAsFlow(OC_FILE_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `getFileWithSyncInfoByIdAsFlow returns a Flow with an OCFileWithSyncInfo`() = runTest {
        every {
            localFileDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE_WITH_SYNC_INFO_AND_SPACE.file.id!!)
        } returns flowOf(OC_FILE_WITH_SYNC_INFO_AND_SPACE)

        val ocFile = ocFileRepository.getFileWithSyncInfoByIdAsFlow(OC_FILE_WITH_SYNC_INFO_AND_SPACE.file.id!!).first()
        assertEquals(OC_FILE_WITH_SYNC_INFO_AND_SPACE, ocFile)

        verify(exactly = 1) {
            localFileDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE_WITH_SYNC_INFO_AND_SPACE.file.id!!)
        }
    }

    @Test
    fun `getFileWithSyncInfoByIdAsFlow returns a Flow with null when local datasource returns a Flow with null`() = runTest {
        every {
            localFileDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE_WITH_SYNC_INFO_AND_SPACE.file.id!!)
        } returns flowOf(null)

        val ocFile = ocFileRepository.getFileWithSyncInfoByIdAsFlow(OC_FILE_WITH_SYNC_INFO_AND_SPACE.file.id!!).first()
        assertNull(ocFile)

        verify(exactly = 1) {
            localFileDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE_WITH_SYNC_INFO_AND_SPACE.file.id!!)
        }
    }

    @Test
    fun `getFileByRemotePath returns a OCFile`() {
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                owner = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
        } returns OC_FILE_WITH_SPACE_ID

        val ocFile = ocFileRepository.getFileByRemotePath(OC_FILE_WITH_SPACE_ID.remotePath, OC_FOLDER_WITH_SPACE_ID.owner, OC_FOLDER_WITH_SPACE_ID.spaceId)
        assertEquals(OC_FILE_WITH_SPACE_ID, ocFile)

        verify(exactly = 1) {
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                owner = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
        }
    }

    @Test
    fun `getFileByRemotePath returns null when local datasource returns a null file`() {
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                owner = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
        } returns null

        val ocFile = ocFileRepository.getFileByRemotePath(OC_FILE_WITH_SPACE_ID.remotePath, OC_FOLDER_WITH_SPACE_ID.owner, OC_FOLDER_WITH_SPACE_ID.spaceId)
        assertNull(ocFile)

        verify(exactly = 1) {
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                owner = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
        }
    }

    @Test
    fun `getFileFromRemoteId returns a OCFile when the remoteId belongs to a normal file`() {
        every {
            remoteFileDataSource.getMetaFile(OC_FILE.remoteId!!, OC_FILE.owner)
        } returns OC_META_FILE
        // The result of this method is not used, so it can be anything
        every {
            ocFileRepositorySpy.refreshFolder(
                remotePath = "",
                accountName = OC_FILE.owner,
                spaceId = null
            )
        } returns emptyList()
        every {
            ocFileRepositorySpy.refreshFolder(
                remotePath = "/Photos",
                accountName = OC_FILE.owner,
                spaceId = null
            )
        } returns listOf(OC_FILE)
        // The result of this method is not used, so it can be anything
        every {
            ocFileRepositorySpy.refreshFolder(
                remotePath = OC_FILE.remotePath,
                accountName = OC_FILE.owner,
                spaceId = null
            )
        } returns emptyList()

        val ocFile = ocFileRepositorySpy.getFileFromRemoteId(OC_FILE.remoteId!!, OC_FILE.owner)
        assertEquals(OC_FILE, ocFile)

        verify(exactly = 1) {
            remoteFileDataSource.getMetaFile(OC_FILE.remoteId!!, OC_FILE.owner)
            ocFileRepositorySpy.refreshFolder(
                remotePath = "",
                accountName = OC_FILE.owner,
                spaceId = null
            )
            ocFileRepositorySpy.refreshFolder(
                remotePath = "/Photos",
                accountName = OC_FILE.owner,
                spaceId = null
            )
            ocFileRepositorySpy.refreshFolder(
                remotePath = OC_FILE.remotePath,
                accountName = OC_FILE.owner,
                spaceId = null
            )
        }
    }

    @Test
    fun `getFileFromRemoteId returns root folder as OCFile when the remoteId belongs to a root folder`() {
        every {
            remoteFileDataSource.getMetaFile(OC_ROOT_FOLDER.remoteId!!, OC_ROOT_FOLDER.owner)
        } returns OC_META_FILE_ROOT_FOLDER
        // The result of this method is not used, so it can be anything
        every {
            ocFileRepositorySpy.refreshFolder(
                remotePath = "",
                accountName = OC_ROOT_FOLDER.owner,
                spaceId = null
            )
        } returns emptyList()
        // The result of this method is not used, so it can be anything
        every {
            ocFileRepositorySpy.refreshFolder(
                remotePath = OC_ROOT_FOLDER.remotePath,
                accountName = OC_ROOT_FOLDER.owner,
                spaceId = null
            )
        } returns emptyList()
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_ROOT_FOLDER.remotePath,
                owner = OC_ROOT_FOLDER.owner,
                spaceId = null
            )
        } returns OC_ROOT_FOLDER

        val ocFile = ocFileRepositorySpy.getFileFromRemoteId(OC_ROOT_FOLDER.remoteId!!, OC_ROOT_FOLDER.owner)
        assertEquals(OC_ROOT_FOLDER, ocFile)

        verify(exactly = 1) {
            remoteFileDataSource.getMetaFile(OC_ROOT_FOLDER.remoteId!!, OC_ROOT_FOLDER.owner)
            ocFileRepositorySpy.refreshFolder(
                remotePath = "",
                accountName = OC_ROOT_FOLDER.owner,
                spaceId = null
            )
            ocFileRepositorySpy.refreshFolder(
                remotePath = OC_ROOT_FOLDER.remotePath,
                accountName = OC_ROOT_FOLDER.owner,
                spaceId = null
            )
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_ROOT_FOLDER.remotePath,
                owner = OC_ROOT_FOLDER.owner,
                spaceId = null
            )
        }
    }

    @Test
    fun `getPersonalRootFolderForAccount returns root folder as OCFile when personal space is not null`() {
        every {
            localSpacesDataSource.getPersonalSpaceForAccount(OC_SPACE_PERSONAL.accountName)
        } returns OC_SPACE_PERSONAL
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = ROOT_PATH,
                owner = OC_SPACE_PERSONAL.accountName,
                spaceId = OC_SPACE_PERSONAL.root.id
            )
        } returns OC_ROOT_FOLDER

        val ocFolder = ocFileRepository.getPersonalRootFolderForAccount(OC_SPACE_PERSONAL.accountName)
        assertEquals(OC_ROOT_FOLDER, ocFolder)

        verify(exactly = 1) {
            localSpacesDataSource.getPersonalSpaceForAccount(OC_SPACE_PERSONAL.accountName)
            localFileDataSource.getFileByRemotePath(
                remotePath = ROOT_PATH,
                owner = OC_SPACE_PERSONAL.accountName,
                spaceId = OC_SPACE_PERSONAL.root.id
            )
        }
    }

    @Test
    fun `getPersonalRootFolderForAccount returns root folder as OCFile when personal space is null`() {
        every {
            localSpacesDataSource.getPersonalSpaceForAccount(OC_SPACE_PERSONAL.accountName)
        } returns null
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = ROOT_PATH,
                owner = OC_SPACE_PERSONAL.accountName,
                spaceId = null
            )
        } returns OC_ROOT_FOLDER

        val ocFolder = ocFileRepository.getPersonalRootFolderForAccount(OC_SPACE_PERSONAL.accountName)
        assertEquals(OC_ROOT_FOLDER, ocFolder)

        verify(exactly = 1) {
            localSpacesDataSource.getPersonalSpaceForAccount(OC_SPACE_PERSONAL.accountName)
            localFileDataSource.getFileByRemotePath(
                remotePath = ROOT_PATH,
                owner = OC_SPACE_PERSONAL.accountName,
                spaceId = null
            )
        }
    }

    @Test
    fun `getPersonalRootFolderForAccount throws NullPointerException when local datasource returns a null root folder`() {
        every {
            localSpacesDataSource.getPersonalSpaceForAccount(OC_SPACE_PERSONAL.accountName)
        } returns OC_SPACE_PERSONAL
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = ROOT_PATH,
                owner = OC_SPACE_PERSONAL.accountName,
                spaceId = OC_SPACE_PERSONAL.root.id
            )
        } returns null

        assertThrows(NullPointerException::class.java) {
            ocFileRepository.getPersonalRootFolderForAccount(OC_SPACE_PERSONAL.accountName)
        }

        verify(exactly = 1) {
            localSpacesDataSource.getPersonalSpaceForAccount(OC_SPACE_PERSONAL.accountName)
            localFileDataSource.getFileByRemotePath(
                remotePath = ROOT_PATH,
                owner = OC_SPACE_PERSONAL.accountName,
                spaceId = OC_SPACE_PERSONAL.root.id
            )
        }
    }

    @Test
    fun `getSharesRootFolderForAccount returns root folder as OCFile when shares space is not null`() {
        every {
            localSpacesDataSource.getSharesSpaceForAccount(OC_SPACE_SHARES.accountName)
        } returns OC_SPACE_SHARES
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = ROOT_PATH,
                owner = OC_SPACE_SHARES.accountName,
                spaceId = OC_SPACE_SHARES.root.id
            )
        } returns OC_ROOT_FOLDER

        val ocFolder = ocFileRepository.getSharesRootFolderForAccount(OC_SPACE_SHARES.accountName)
        assertEquals(OC_ROOT_FOLDER, ocFolder)

        verify(exactly = 1) {
            localSpacesDataSource.getSharesSpaceForAccount(OC_SPACE_SHARES.accountName)
            localFileDataSource.getFileByRemotePath(
                remotePath = ROOT_PATH,
                owner = OC_SPACE_SHARES.accountName,
                spaceId = OC_SPACE_SHARES.root.id
            )
        }
    }

    @Test
    fun `getSharesRootFolderForAccount returns null when shares space is null`() {
        every {
            localSpacesDataSource.getSharesSpaceForAccount(OC_SPACE_SHARES.accountName)
        } returns null

        val ocFolder = ocFileRepository.getSharesRootFolderForAccount(OC_SPACE_SHARES.accountName)
        assertNull(ocFolder)

        verify(exactly = 1) {
            localSpacesDataSource.getSharesSpaceForAccount(OC_SPACE_SHARES.accountName)
        }
    }

    @Test
    fun `getSharesRootFolderForAccount throws NullPointerException when local datasource returns a null root folder`() {
        every {
            localSpacesDataSource.getSharesSpaceForAccount(OC_SPACE_SHARES.accountName)
        } returns OC_SPACE_SHARES
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = ROOT_PATH,
                owner = OC_SPACE_SHARES.accountName,
                spaceId = OC_SPACE_SHARES.root.id
            )
        } returns null

        assertThrows(NullPointerException::class.java) {
            ocFileRepository.getSharesRootFolderForAccount(OC_SPACE_SHARES.accountName)
        }

        verify(exactly = 1) {
            localSpacesDataSource.getSharesSpaceForAccount(OC_SPACE_SHARES.accountName)
            localFileDataSource.getFileByRemotePath(
                remotePath = ROOT_PATH,
                owner = OC_SPACE_SHARES.accountName,
                spaceId = OC_SPACE_SHARES.root.id
            )
        }
    }

    @Test
    fun `getSearchFolderContent returns a list of OCFiles when the file list option is all files`() {
        every {
            localFileDataSource.getSearchFolderContent(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!, searchText)
        } returns listOf(OC_FILE_WITH_SPACE_ID)

        val listOfFiles = ocFileRepository.getSearchFolderContent(FileListOption.ALL_FILES, OC_PARENT_FOLDER_WITH_SPACE_ID.id!!, searchText)
        assertEquals(listOf(OC_FILE_WITH_SPACE_ID), listOfFiles)

        verify(exactly = 1) {
            localFileDataSource.getSearchFolderContent(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!, searchText)
        }
    }

    @Test
    fun `getSearchFolderContent returns an empty list with no OCFiles when the file list option is spaces list`() {
        val listOfFiles = ocFileRepository.getSearchFolderContent(FileListOption.SPACES_LIST, OC_PARENT_FOLDER_WITH_SPACE_ID.id!!, searchText)
        assertEquals(emptyList<OCFile>(), listOfFiles)
    }

    @Test
    fun `getSearchFolderContent returns a list of OCFiles when the file list option is available offline`() {
        every {
            localFileDataSource.getSearchAvailableOfflineFolderContent(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!, searchText)
        } returns listOf(OC_FILE_WITH_SPACE_ID)

        val listOfFiles = ocFileRepository.getSearchFolderContent(FileListOption.AV_OFFLINE, OC_PARENT_FOLDER_WITH_SPACE_ID.id!!, searchText)
        assertEquals(listOf(OC_FILE_WITH_SPACE_ID), listOfFiles)

        verify(exactly = 1) {
            localFileDataSource.getSearchAvailableOfflineFolderContent(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!, searchText)
        }
    }

    @Test
    fun `getSearchFolderContent returns a list of OCFiles when the file list option is shared by link`() {
        every {
            localFileDataSource.getSearchSharedByLinkFolderContent(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!, searchText)
        } returns listOf(OC_FILE_WITH_SPACE_ID)

        val listOfFiles = ocFileRepository.getSearchFolderContent(FileListOption.SHARED_BY_LINK, OC_PARENT_FOLDER_WITH_SPACE_ID.id!!, searchText)
        assertEquals(listOf(OC_FILE_WITH_SPACE_ID), listOfFiles)

        verify(exactly = 1) {
            localFileDataSource.getSearchSharedByLinkFolderContent(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!, searchText)
        }
    }

    @Test
    fun `getFolderContent returns a list of OCFiles`() {
        every {
            localFileDataSource.getFolderContent(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!)
        } returns listOf(OC_FILE_WITH_SPACE_ID)

        val listOfFiles = ocFileRepository.getFolderContent(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!)
        assertEquals(listOf(OC_FILE_WITH_SPACE_ID), listOfFiles)

        verify(exactly = 1) {
            localFileDataSource.getFolderContent(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `getFolderContentWithSyncInfoAsFlow returns a Flow with a list of OCFileWithSyncInfo`() = runTest {
        every {
            localFileDataSource.getFolderContentWithSyncInfoAsFlow(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!)
        } returns flowOf(listOf(OC_FILE_WITH_SYNC_INFO))

        val listOfFiles = ocFileRepository.getFolderContentWithSyncInfoAsFlow(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!).first()
        assertEquals(listOf(OC_FILE_WITH_SYNC_INFO), listOfFiles)

        verify(exactly = 1) {
            localFileDataSource.getFolderContentWithSyncInfoAsFlow(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `getFolderImages returns a list of OCFiles`() {
        every {
            localFileDataSource.getFolderImages(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!)
        } returns listOf(OC_FILE_WITH_SPACE_ID)

        val listOfFiles = ocFileRepository.getFolderImages(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!)
        assertEquals(listOf(OC_FILE_WITH_SPACE_ID), listOfFiles)

        verify(exactly = 1) {
            localFileDataSource.getFolderImages(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `getSharedByLinkWithSyncInfoForAccountAsFlow returns a Flow with a list of OCFileWithSyncInfo`() = runTest {
        every {
            localFileDataSource.getSharedByLinkWithSyncInfoForAccountAsFlow(OC_FILE_WITH_SYNC_INFO.file.owner)
        } returns flowOf(listOf(OC_FILE_WITH_SYNC_INFO))

        val listOfFiles = ocFileRepository.getSharedByLinkWithSyncInfoForAccountAsFlow(OC_FILE_WITH_SYNC_INFO.file.owner).first()
        assertEquals(listOf(OC_FILE_WITH_SYNC_INFO), listOfFiles)

        verify(exactly = 1) {
            localFileDataSource.getSharedByLinkWithSyncInfoForAccountAsFlow(OC_FILE_WITH_SYNC_INFO.file.owner)
        }
    }

    @Test
    fun `getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow returns a Flow with a list of OCFileWithSyncInfo`() = runTest {
        every {
            localFileDataSource.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(OC_FILE_WITH_SYNC_INFO.file.owner)
        } returns flowOf(listOf(OC_FILE_WITH_SYNC_INFO))

        val listOfFiles = ocFileRepository.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(OC_FILE_WITH_SYNC_INFO.file.owner).first()
        assertEquals(listOf(OC_FILE_WITH_SYNC_INFO), listOfFiles)

        verify(exactly = 1) {
            localFileDataSource.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(OC_FILE_WITH_SYNC_INFO.file.owner)
        }
    }

    @Test
    fun `getFilesAvailableOfflineFromAccount returns a list of OCFiles`() {
        every {
            localFileDataSource.getFilesAvailableOfflineFromAccount(OC_FILE_AVAILABLE_OFFLINE.owner)
        } returns listOf(OC_FILE_AVAILABLE_OFFLINE)

        val listOfFiles = ocFileRepository.getFilesAvailableOfflineFromAccount(OC_FILE_AVAILABLE_OFFLINE.owner)
        assertEquals(listOf(OC_FILE_AVAILABLE_OFFLINE), listOfFiles)

        verify(exactly = 1) {
            localFileDataSource.getFilesAvailableOfflineFromAccount(OC_FILE_AVAILABLE_OFFLINE.owner)
        }
    }

    @Test
    fun `getFilesAvailableOfflineFromEveryAccount returns a list of OCFiles`() {
        every {
            localFileDataSource.getFilesAvailableOfflineFromEveryAccount()
        } returns OC_AVAILABLE_OFFLINE_FILES

        val listOfFiles = ocFileRepository.getFilesAvailableOfflineFromEveryAccount()
        assertEquals(OC_AVAILABLE_OFFLINE_FILES, listOfFiles)

        verify(exactly = 1) {
            localFileDataSource.getFilesAvailableOfflineFromEveryAccount()
        }
    }

    @Test
    fun `getDownloadedFilesForAccount returns a list of OCFiles`() {
        every {
            localFileDataSource.getDownloadedFilesForAccount(OC_FILE_DOWNLOADED.owner)
        } returns listOf(OC_FILE_DOWNLOADED)

        val listOfFiles = ocFileRepository.getDownloadedFilesForAccount(OC_FILE_DOWNLOADED.owner)
        assertEquals(listOf(OC_FILE_DOWNLOADED), listOfFiles)

        verify(exactly = 1) {
            localFileDataSource.getDownloadedFilesForAccount(OC_FILE_DOWNLOADED.owner)
        }
    }

    @Test
    fun `getFilesWithLastUsageOlderThanGivenTime returns a list of OCFiles`() {
        every {
            localFileDataSource.getFilesWithLastUsageOlderThanGivenTime(0)
        } returns listOf(OC_FILE_WITH_SPACE_ID)

        val listOfFiles = ocFileRepository.getFilesWithLastUsageOlderThanGivenTime(0)
        assertEquals(listOf(OC_FILE_WITH_SPACE_ID), listOfFiles)

        verify(exactly = 1) {
            localFileDataSource.getFilesWithLastUsageOlderThanGivenTime(0)
        }
    }

    @Test
    fun `moveFile returns a list with the OCFile in conflict (the moved OCFile) when replace parameter is empty and expected path already exists`() {
        every {
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns true

        val filesNeedAction = ocFileRepository.moveFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, emptyList(), true)

        assertEquals(listOf(OC_FILE_WITH_SPACE_ID), filesNeedAction)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        }
    }

    @Test
    fun `moveFile returns an empty list with no OCFiles in conflict when replace parameter is empty and expected path doesn't exist`() {
        every {
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns false
        every {
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                remotePath = expectedRemotePath,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
        } returns storagePath

        val filesNeedAction = ocFileRepository.moveFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, emptyList(), true)

        assertEquals(emptyList<OCFile>(), filesNeedAction)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
            )
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                remotePath = expectedRemotePath,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
            remoteFileDataSource.moveFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
            localFileDataSource.moveFile(
                sourceFile = OC_FILE_WITH_SPACE_ID,
                targetFolder = OC_FOLDER_WITH_SPACE_ID,
                finalRemotePath = expectedRemotePath,
                finalStoragePath = storagePath
            )
            localStorageProvider.moveLocalFile(
                ocFile = OC_FILE_WITH_SPACE_ID,
                finalStoragePath = storagePath
            )
        }
    }

    @Test
    fun `moveFile returns an empty list with no OCFiles in conflict when replace parameter is empty, expected path doesn't exist and file has a conflict`() {
        val fileWithConflict = OC_FILE_WITH_SPACE_ID.copy(
            etagInConflict = "5efb0c13c688i"
        )
        every {
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns false
        every {
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                remotePath = expectedRemotePath,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
        } returns storagePath

        val filesNeedAction = ocFileRepository.moveFile(listOf(fileWithConflict), OC_FOLDER_WITH_SPACE_ID, emptyList(), true)

        assertEquals(emptyList<OCFile>(), filesNeedAction)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
            )
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                remotePath = expectedRemotePath,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
            remoteFileDataSource.moveFile(
                sourceRemotePath = fileWithConflict.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = fileWithConflict.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
            localFileDataSource.cleanConflict(fileWithConflict.id!!)
            localFileDataSource.moveFile(
                sourceFile = fileWithConflict,
                targetFolder = OC_FOLDER_WITH_SPACE_ID,
                finalRemotePath = expectedRemotePath,
                finalStoragePath = storagePath
            )
            localFileDataSource.saveConflict(fileWithConflict.id!!, fileWithConflict.etagInConflict!!)
            localStorageProvider.moveLocalFile(
                ocFile = fileWithConflict,
                finalStoragePath = storagePath
            )
        }
    }

    @Test
    fun `moveFile returns an empty list with no OCFiles in conflict when replace parameter is true`() {
        every {
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                remotePath = expectedRemotePath,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
        } returns storagePath

        val filesNeedAction = ocFileRepository.moveFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, listOf(true), true)

        assertEquals(emptyList<OCFile>(), filesNeedAction)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner
            )
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                remotePath = expectedRemotePath,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
            remoteFileDataSource.moveFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = true
            )
            localFileDataSource.moveFile(
                sourceFile = OC_FILE_WITH_SPACE_ID,
                targetFolder = OC_FOLDER_WITH_SPACE_ID,
                finalRemotePath = expectedRemotePath,
                finalStoragePath = storagePath
            )
            localStorageProvider.moveLocalFile(
                ocFile = OC_FILE_WITH_SPACE_ID,
                finalStoragePath = storagePath
            )
        }
    }

    @Test
    fun `moveFile returns an empty list with no OCFiles in conflict when replace parameter is false`() {
        val availableRemotePath = "$expectedRemotePath (1)"
        val actualStoragePath = "/local/storage/path/username@demo.owncloud.com/Folder/Photos/image2 (1).jpt"
        every {
            remoteFileDataSource.getAvailableRemotePath(
                remotePath = expectedRemotePath,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                isUserLogged = true
            )
        } returns availableRemotePath
        every {
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                remotePath = availableRemotePath,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
        } returns actualStoragePath

        val filesNeedAction = ocFileRepository.moveFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, listOf(false), true)

        assertEquals(emptyList<OCFile>(), filesNeedAction)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.getAvailableRemotePath(
                remotePath = expectedRemotePath,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                isUserLogged = true
            )
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                remotePath = availableRemotePath,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
            remoteFileDataSource.moveFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = availableRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
            localFileDataSource.moveFile(
                sourceFile = OC_FILE_WITH_SPACE_ID,
                targetFolder = OC_FOLDER_WITH_SPACE_ID,
                finalRemotePath = availableRemotePath,
                finalStoragePath = actualStoragePath
            )
            localStorageProvider.moveLocalFile(
                ocFile = OC_FILE_WITH_SPACE_ID,
                finalStoragePath = actualStoragePath
            )
        }
    }

    @Test
    fun `moveFile returns an empty list with no OCFiles in conflict when replace parameter is null`() {
        val filesNeedAction = ocFileRepository.moveFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, listOf(null), true)

        assertEquals(emptyList<OCFile>(), filesNeedAction)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner
            )
        }
    }

    @Test
    fun `moveFile removes target folder locally and throws a ConflictException when replace parameter is empty and expected path doesn't exist but target folder doesn't exist anymore`() {
        every {
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns false
        every {
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                remotePath = expectedRemotePath,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
        } returns storagePath
        every {
            remoteFileDataSource.moveFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
        } throws ConflictException()
        every {
            localFileDataSource.getFolderContent(OC_FOLDER_WITH_SPACE_ID.id!!)
        } returns emptyList()

        assertThrows(ConflictException::class.java) {
            ocFileRepository.moveFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, emptyList(), true)
        }

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
            )
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                remotePath = expectedRemotePath,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
            remoteFileDataSource.moveFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
            localFileDataSource.getFolderContent(OC_FOLDER_WITH_SPACE_ID.id!!)
            localStorageProvider.deleteLocalFolderIfItHasNoFilesInside(OC_FOLDER_WITH_SPACE_ID)
            localFileDataSource.deleteFile(OC_FOLDER_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `moveFile removes source file locally and throws a FileNotFoundException when replace parameter is empty and expected path doesn't exist but source file doesn't exist anymore`() {
        every {
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns false
        every {
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                remotePath = expectedRemotePath,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
        } returns storagePath
        every {
            remoteFileDataSource.moveFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
        } throws FileNotFoundException()
        every {
            localStorageProvider.deleteLocalFile(OC_FILE_WITH_SPACE_ID)
        } returns true

        assertThrows(FileNotFoundException::class.java) {
            ocFileRepository.moveFile(listOf(OC_FILE_WITH_SPACE_ID), OC_FOLDER_WITH_SPACE_ID, emptyList(), true)
        }

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.checkPathExistence(
                path = expectedRemotePath,
                isUserLogged = true,
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
            )
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FOLDER_WITH_SPACE_ID.owner,
                remotePath = expectedRemotePath,
                spaceId = OC_FOLDER_WITH_SPACE_ID.spaceId
            )
            remoteFileDataSource.moveFile(
                sourceRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                targetRemotePath = expectedRemotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl,
                replace = false
            )
            localStorageProvider.deleteLocalFile(OC_FILE_WITH_SPACE_ID)
            localFileDataSource.deleteFile(OC_FILE_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `readFile returns a OCFile`() {
        val ocFileWithoutSpaceId = OC_FILE_WITH_SPACE_ID.copy(spaceId = null)
        every {
            remoteFileDataSource.readFile(
                remotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns ocFileWithoutSpaceId

        val ocFile = ocFileRepository.readFile(OC_FILE_WITH_SPACE_ID.remotePath, OC_FILE_WITH_SPACE_ID.owner, OC_FILE_WITH_SPACE_ID.spaceId)
        assertEquals(OC_FILE_WITH_SPACE_ID, ocFile)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_FILE_WITH_SPACE_ID.spaceId,
                accountName = OC_FILE_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.readFile(
                remotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        }
    }

    /*

    @Test
    fun `get folder content - ok`() {
        every { localFileDataSource.getFolderContent(OC_FOLDER.parentId!!) } returns listOf(OC_FOLDER)

        val folderContent = ocFileRepository.getFolderContent(OC_FOLDER.parentId!!)

        assertEquals(listOf(OC_FOLDER), folderContent)

        verify(exactly = 1) {
            localFileDataSource.getFolderContent(OC_FOLDER.parentId!!)
        }
    }

    @Test(expected = Exception::class)
    fun `get folder content - ko`() {
        every { localFileDataSource.getFolderContent(OC_FOLDER.parentId!!) } throws Exception()

        ocFileRepository.getFolderContent(OC_FOLDER.parentId!!)

        verify(exactly = 1) {
            localFileDataSource.getFolderContent(OC_FOLDER.parentId!!)
        }
    }

    @Test
    fun `refresh folder - ok`() {
        every {
            remoteFileDataSource.refreshFolder(folderToFetch.remotePath, any())
        } returns listOfFilesRetrieved

        ocFileRepository.refreshFolder(folderToFetch.remotePath, OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteFileDataSource.refreshFolder(folderToFetch.remotePath, OC_ACCOUNT_NAME)
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(
                listOfFiles = listOfFilesRetrieved.drop(1),
                folder = listOfFilesRetrieved.first()
            )
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun `refresh folder - ko - no connection exception`() {
        every {
            remoteFileDataSource.refreshFolder(folderToFetch.remotePath, OC_ACCOUNT_NAME)
        } throws NoConnectionWithServerException()

        ocFileRepository.refreshFolder(folderToFetch.remotePath, OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            remoteFileDataSource.refreshFolder(OC_FOLDER.remotePath, OC_ACCOUNT_NAME)
        }
        verify(exactly = 0) {
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(any(), any())
        }
    }

    @Test
    fun `remove file - ok`() {
        every { remoteFileDataSource.deleteFile(any(), any()) } returns Unit
        every { localStorageProvider.deleteLocalFile(any()) } returns true

        ocFileRepository.deleteFiles(listOfFilesToDelete = listOfFileToRemove, removeOnlyLocalCopy = false)

        verify(exactly = listOfFilesRetrieved.size) {
            remoteFileDataSource.deleteFile(any(), any())
            localFileDataSource.deleteFile(any())
            localStorageProvider.deleteLocalFile(any())
        }
    }

    @Test
    fun `remove file - ok - only local copy`() {
        every { localStorageProvider.deleteLocalFile(any()) } returns true

        ocFileRepository.deleteFiles(listOfFilesToDelete = listOfFileToRemove, removeOnlyLocalCopy = true)

        verify(exactly = listOfFilesRetrieved.size) { localStorageProvider.deleteLocalFile(any()) }
        verify(exactly = listOfFilesRetrieved.size) { localFileDataSource.saveFile(any()) }
        verify(exactly = 0) {
            remoteFileDataSource.deleteFile(any(), any())
            localFileDataSource.deleteFile(any())
        }
    }

    @Test
    fun `remove file - ok - folder recursively`() {
        every { remoteFileDataSource.deleteFile(any(), any()) } returns Unit
        every { localFileDataSource.getFolderContent(0) } returns listOfFileToRemove
        every { localFileDataSource.getFolderContent(1) } returns listOf(OC_FILE)
        every { localStorageProvider.deleteLocalFile(any()) } returns true

        ocFileRepository.deleteFiles(listOfFilesToDelete = listOf(OC_FOLDER.copy(id = 0)), removeOnlyLocalCopy = false)

        verify(exactly = 1) { remoteFileDataSource.deleteFile(any(), any()) }
        verify(exactly = 2) { localFileDataSource.getFolderContent(any()) }
        // Removing initial folder + listOfFilesToRemove.size + file inside a folder in listOfFilesToRemove
        verify(exactly = listOfFileToRemove.size + 2) {
            localFileDataSource.deleteFile(any())
            localStorageProvider.deleteLocalFile(any())
        }
    }

    @Test
    fun `remove file - ko - file not found exception`() {
        every { remoteFileDataSource.deleteFile(any(), any()) } throws FileNotFoundException()
        every { localStorageProvider.deleteLocalFile(any()) } returns true

        ocFileRepository.deleteFiles(listOfFilesToDelete = listOf(OC_FILE), removeOnlyLocalCopy = false)

        verify(exactly = 1) {
            remoteFileDataSource.deleteFile(any(), any())
            localFileDataSource.deleteFile(any())
            localStorageProvider.deleteLocalFile(any())
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun `remove file - ko - no connection exception`() {
        every {
            remoteFileDataSource.deleteFile(any(), any())
        } throws NoConnectionWithServerException()

        ocFileRepository.deleteFiles(listOfFilesRetrieved, removeOnlyLocalCopy = false)

        verify(exactly = 1) { remoteFileDataSource.deleteFile(OC_FOLDER.remotePath, any()) }
        verify(exactly = 0) { localFileDataSource.deleteFile(any()) }
    }

    @Test
    fun `save file - ok`() {
        ocFileRepository.saveFile(OC_FILE)

        verify(exactly = 1) { localFileDataSource.saveFile(OC_FILE) }
    }
     */
}
