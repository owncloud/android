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
import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus
import com.owncloud.android.domain.exceptions.ConflictException
import com.owncloud.android.domain.exceptions.FileAlreadyExistsException
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
import java.util.UUID

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
    private val etagInConflict = "5efb0c13c688i"
    private val fileWithConflict = OC_FILE_WITH_SPACE_ID.copy(etagInConflict = etagInConflict)

    @Before
    fun setUp() {
        val commonSpaceId = OC_FILE_WITH_SPACE_ID.spaceId
        val commonAccountName = OC_FILE_WITH_SPACE_ID.owner
        every {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = commonSpaceId,
                accountName = commonAccountName
            )
        } returns OC_SPACE_PERSONAL.root.webDavUrl
    }

    @Test
    fun `createFolder creates a new folder and saves it`() {
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

    @Suppress("MaxLineLength")
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

    @Suppress("MaxLineLength")
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

    @Suppress("MaxLineLength")
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

        val ocFile = ocFileRepository.getFileByRemotePath(
            OC_FILE_WITH_SPACE_ID.remotePath,
            OC_FOLDER_WITH_SPACE_ID.owner,
            OC_FOLDER_WITH_SPACE_ID.spaceId
        )
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

        val ocFile = ocFileRepository.getFileByRemotePath(
            OC_FILE_WITH_SPACE_ID.remotePath,
            OC_FOLDER_WITH_SPACE_ID.owner,
            OC_FOLDER_WITH_SPACE_ID.spaceId
        )
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

    @Suppress("MaxLineLength")
    @Test
    fun `moveFile returns an empty list with no OCFiles in conflict when replace parameter is empty, expected path doesn't exist and file has a conflict`() {
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

    @Suppress("MaxLineLength")
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

    @Suppress("MaxLineLength")
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

    @Test
    fun `refreshFolder returns an empty list of OCFiles when folder doesn't exist in database`() {
        val ocParentFolderWithoutSpaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.copy(spaceId = null)
        val ocFileWithoutSpaceId = OC_FILE_WITH_SPACE_ID.copy(spaceId = null)
        val ocFileWithSpaceIdAndNeedsThumbnailUpdate = OC_FILE_WITH_SPACE_ID.copy(needsToUpdateThumbnail = true)
        every {
            remoteFileDataSource.refreshFolder(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns listOf(ocParentFolderWithoutSpaceId, ocFileWithoutSpaceId)
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                owner = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId
            )
        } returns null
        every {
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(
                listOfFiles = listOf(ocFileWithSpaceIdAndNeedsThumbnailUpdate),
                folder = OC_PARENT_FOLDER_WITH_SPACE_ID
            )
        } returns emptyList()

        val listOfFiles = ocFileRepository.refreshFolder(
            OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
            OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
            OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId,
            false
        )
        assertEquals(emptyList<OCFile>(), listOfFiles)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.refreshFolder(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                owner = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId
            )
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(
                listOfFiles = listOf(ocFileWithSpaceIdAndNeedsThumbnailUpdate),
                folder = OC_PARENT_FOLDER_WITH_SPACE_ID
            )
        }
    }

    @Test
    fun `refreshFolder returns an empty list of OCFiles when folder already exists in database but not its content`() {
        val ocParentFolderWithoutSpaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.copy(spaceId = null)
        val ocFileWithoutSpaceId = OC_FILE_WITH_SPACE_ID.copy(spaceId = null)
        val ocFileWithSpaceIdAndNoEtagAndNeedsThumbnailUpdate = OC_FILE_WITH_SPACE_ID.copy(needsToUpdateThumbnail = true, etag = "")
        every {
            remoteFileDataSource.refreshFolder(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns listOf(ocParentFolderWithoutSpaceId, ocFileWithoutSpaceId)
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                owner = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId
            )
        } returns OC_PARENT_FOLDER_WITH_SPACE_ID
        every {
            localFileDataSource.getFolderContent(
                folderId = OC_PARENT_FOLDER_WITH_SPACE_ID.id!!
            )
        } returns emptyList()
        every {
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(
                listOfFiles = listOf(ocFileWithSpaceIdAndNoEtagAndNeedsThumbnailUpdate),
                folder = OC_PARENT_FOLDER_WITH_SPACE_ID,
            )
        } returns emptyList()

        val listOfFiles = ocFileRepository.refreshFolder(
            OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
            OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
            OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId,
            false
        )
        assertEquals(emptyList<OCFile>(), listOfFiles)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.refreshFolder(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                owner = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId
            )
            localFileDataSource.getFolderContent(
                folderId = OC_PARENT_FOLDER_WITH_SPACE_ID.id!!
            )
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(
                listOfFiles = listOf(ocFileWithSpaceIdAndNoEtagAndNeedsThumbnailUpdate),
                folder = OC_PARENT_FOLDER_WITH_SPACE_ID,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `refreshFolder returns a list with the OCFile that changed when folder and its content already exists in database but needs to be updated`() {
        val ocParentFolderWithoutSpaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.copy(spaceId = null)
        val ocFileWithoutSpaceIdAndDifferentEtag = OC_FILE_WITH_SPACE_ID.copy(spaceId = null, etag = "5efb0c13c688i2")
        every {
            remoteFileDataSource.refreshFolder(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns listOf(ocParentFolderWithoutSpaceId, ocFileWithoutSpaceIdAndDifferentEtag)
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                owner = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId
            )
        } returns OC_PARENT_FOLDER_WITH_SPACE_ID
        every {
            localFileDataSource.getFolderContent(
                folderId = OC_PARENT_FOLDER_WITH_SPACE_ID.id!!
            )
        } returns listOf(OC_FILE_WITH_SPACE_ID)
        every {
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(
                listOfFiles = listOf(OC_FILE_WITH_SPACE_ID),
                folder = OC_PARENT_FOLDER_WITH_SPACE_ID,
            )
        } returns listOf(OC_FILE_WITH_SPACE_ID)

        val listOfFiles = ocFileRepository.refreshFolder(
            OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
            OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
            OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId,
            true
        )
        assertEquals(listOf(OC_FILE_WITH_SPACE_ID), listOfFiles)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.refreshFolder(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                owner = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId
            )
            localFileDataSource.getFolderContent(
                folderId = OC_PARENT_FOLDER_WITH_SPACE_ID.id!!
            )
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(
                listOfFiles = listOf(OC_FILE_WITH_SPACE_ID),
                folder = OC_PARENT_FOLDER_WITH_SPACE_ID,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `refreshFolder returns an empty list of OCFiles when folder and its content already exists in database updated and the action is not set folder available offline or synchronize`() {
        val ocParentFolderWithoutSpaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.copy(spaceId = null)
        val ocFileWithoutSpaceId = OC_FILE_WITH_SPACE_ID.copy(spaceId = null)
        every {
            remoteFileDataSource.refreshFolder(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns listOf(ocParentFolderWithoutSpaceId, ocFileWithoutSpaceId)
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                owner = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId
            )
        } returns OC_PARENT_FOLDER_WITH_SPACE_ID
        every {
            localFileDataSource.getFolderContent(
                folderId = OC_PARENT_FOLDER_WITH_SPACE_ID.id!!
            )
        } returns listOf(OC_FILE_WITH_SPACE_ID)
        every {
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(
                listOfFiles = emptyList(),
                folder = OC_PARENT_FOLDER_WITH_SPACE_ID,
            )
        } returns emptyList()

        val listOfFiles = ocFileRepository.refreshFolder(
            OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
            OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
            OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId,
            false
        )
        assertEquals(emptyList<OCFile>(), listOfFiles)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.refreshFolder(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                owner = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId
            )
            localFileDataSource.getFolderContent(
                folderId = OC_PARENT_FOLDER_WITH_SPACE_ID.id!!
            )
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(
                listOfFiles = emptyList(),
                folder = OC_PARENT_FOLDER_WITH_SPACE_ID,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `refreshFolder returns an empty list of OCFiles when folder and its content already exists in database but there are additional files in conflict in local to be removed`() {
        val ocParentFolderWithoutSpaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.copy(spaceId = null)
        val ocFileWithoutSpaceId = OC_FILE_WITH_SPACE_ID.copy(spaceId = null)
        val additionalOcFile = OC_FILE_WITH_SPACE_ID.copy(id = 300, remotePath = "/Folder/image3.jpt",
            remoteId = "00000003oci9p7er2hox2", privateLink = "http://server.url/f/70", etagInConflict = etagInConflict)
        every {
            remoteFileDataSource.refreshFolder(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } returns listOf(ocParentFolderWithoutSpaceId, ocFileWithoutSpaceId)
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                owner = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId
            )
        } returns OC_PARENT_FOLDER_WITH_SPACE_ID
        every {
            localFileDataSource.getFolderContent(
                folderId = OC_PARENT_FOLDER_WITH_SPACE_ID.id!!
            )
        } returns listOf(OC_FILE_WITH_SPACE_ID, additionalOcFile)
        every {
            localStorageProvider.deleteLocalFile(additionalOcFile)
        } returns true
        every {
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(
                listOfFiles = emptyList(),
                folder = OC_PARENT_FOLDER_WITH_SPACE_ID,
            )
        } returns emptyList()

        val listOfFiles = ocFileRepository.refreshFolder(
            OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
            OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
            OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId,
            false)
        assertEquals(emptyList<OCFile>(), listOfFiles)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.refreshFolder(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
            localFileDataSource.getFileByRemotePath(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                owner = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId
            )
            localFileDataSource.getFolderContent(
                folderId = OC_PARENT_FOLDER_WITH_SPACE_ID.id!!
            )
            localFileDataSource.cleanConflict(additionalOcFile.id!!)
            localStorageProvider.deleteLocalFile(additionalOcFile)
            localFileDataSource.deleteFile(additionalOcFile.id!!)
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(
                listOfFiles = emptyList(),
                folder = OC_PARENT_FOLDER_WITH_SPACE_ID,
            )
        }
    }

    @Test
    fun `deleteFiles removes a file and its conflict from local and remote correctly`() {
        every {
            localStorageProvider.deleteLocalFile(fileWithConflict)
        } returns true

        ocFileRepository.deleteFiles(listOf(fileWithConflict), false)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = fileWithConflict.spaceId,
                accountName = fileWithConflict.owner
            )
            remoteFileDataSource.deleteFile(
                remotePath = fileWithConflict.remotePath,
                accountName = fileWithConflict.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
            localFileDataSource.cleanConflict(fileWithConflict.id!!)
            localStorageProvider.deleteLocalFile(fileWithConflict)
            localFileDataSource.deleteFile(fileWithConflict.id!!)
        }
    }

    @Test
    fun `deleteFiles removes a file and its conflict from local although it doesn't exist in remote because it throws a FileNotFoundException`() {
        every {
            remoteFileDataSource.deleteFile(
                remotePath = fileWithConflict.remotePath,
                accountName = fileWithConflict.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
        } throws FileNotFoundException()
        every {
            localStorageProvider.deleteLocalFile(fileWithConflict)
        } returns true

        ocFileRepository.deleteFiles(listOf(fileWithConflict), false)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = fileWithConflict.spaceId,
                accountName = fileWithConflict.owner
            )
            remoteFileDataSource.deleteFile(
                remotePath = fileWithConflict.remotePath,
                accountName = fileWithConflict.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
            localFileDataSource.cleanConflict(fileWithConflict.id!!)
            localStorageProvider.deleteLocalFile(fileWithConflict)
            localFileDataSource.deleteFile(fileWithConflict.id!!)
        }
    }

    @Test
    fun `deleteFiles removes a file and its conflict but only from local correctly`() {
        every {
            localStorageProvider.deleteLocalFile(fileWithConflict)
        } returns true

        ocFileRepository.deleteFiles(listOf(fileWithConflict), true)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = fileWithConflict.spaceId,
                accountName = fileWithConflict.owner
            )
            localFileDataSource.cleanConflict(fileWithConflict.id!!)
            localStorageProvider.deleteLocalFile(fileWithConflict)
            localFileDataSource.saveFile(fileWithConflict.copy(storagePath = null, etagInConflict = null, lastUsage = null, etag = null))
        }
    }

    @Test
    fun `deleteFiles removes a folder recursively from local and remote correctly`() {
        every {
            localFileDataSource.getFolderContent(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!)
        } returns listOf(OC_FILE_WITH_SPACE_ID)
        every {
            localStorageProvider.deleteLocalFile(OC_FILE_WITH_SPACE_ID)
        } returns true

        ocFileRepository.deleteFiles(listOf(OC_PARENT_FOLDER_WITH_SPACE_ID), false)

        verify(exactly = 1) {
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_PARENT_FOLDER_WITH_SPACE_ID.spaceId,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.deleteFile(
                remotePath = OC_PARENT_FOLDER_WITH_SPACE_ID.remotePath,
                accountName = OC_PARENT_FOLDER_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
            localFileDataSource.getFolderContent(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!)
            localStorageProvider.deleteLocalFile(OC_FILE_WITH_SPACE_ID)
            localFileDataSource.deleteFile(OC_FILE_WITH_SPACE_ID.id!!)
            localStorageProvider.deleteLocalFolderIfItHasNoFilesInside(OC_PARENT_FOLDER_WITH_SPACE_ID)
            localFileDataSource.deleteFile(OC_PARENT_FOLDER_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `renameFile renames a file correctly`() {
        val newName = "image3.jpt"
        val newRemotePath = "/Folder/image3.jpt"
        val newStoragePath = "/local/storage/path/username@demo.owncloud.com/Folder/image3.jpt"
        every {
            localStorageProvider.getExpectedRemotePath(
                remotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                newName = newName,
                isFolder = OC_FILE_WITH_SPACE_ID.isFolder
            )
        } returns newRemotePath
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = newRemotePath,
                owner = OC_FILE_WITH_SPACE_ID.owner,
                spaceId = OC_FILE_WITH_SPACE_ID.spaceId
            )
        } returns null
        every {
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                remotePath = newRemotePath,
                spaceId = OC_FILE_WITH_SPACE_ID.spaceId
            )
        } returns newStoragePath

        ocFileRepository.renameFile(OC_FILE_WITH_SPACE_ID, newName)

        verify(exactly = 1) {
            localStorageProvider.getExpectedRemotePath(
                remotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                newName = newName,
                isFolder = OC_FILE_WITH_SPACE_ID.isFolder
            )
            localFileDataSource.getFileByRemotePath(
                remotePath = newRemotePath,
                owner = OC_FILE_WITH_SPACE_ID.owner,
                spaceId = OC_FILE_WITH_SPACE_ID.spaceId
            )
            localSpacesDataSource.getWebDavUrlForSpace(
                spaceId = OC_FILE_WITH_SPACE_ID.spaceId,
                accountName = OC_FILE_WITH_SPACE_ID.owner
            )
            remoteFileDataSource.renameFile(
                oldName = OC_FILE_WITH_SPACE_ID.fileName,
                oldRemotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                newName = newName,
                isFolder = OC_FILE_WITH_SPACE_ID.isFolder,
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                spaceWebDavUrl = OC_SPACE_PERSONAL.root.webDavUrl
            )
            localFileDataSource.renameFile(
                fileToRename = OC_FILE_WITH_SPACE_ID,
                finalRemotePath = newRemotePath,
                finalStoragePath = newStoragePath
            )
            localStorageProvider.moveLocalFile(
                ocFile = OC_FILE_WITH_SPACE_ID,
                finalStoragePath = newStoragePath
            )
        }
        verify(exactly = 2) {
            localStorageProvider.getDefaultSavePathFor(
                accountName = OC_FILE_WITH_SPACE_ID.owner,
                remotePath = newRemotePath,
                spaceId = OC_FILE_WITH_SPACE_ID.spaceId
            )
        }
    }

    @Test
    fun `renameFile throws FileAlreadyExistsException when new name is already taken by other file`() {
        val newName = "image2.jpt"
        val newRemotePath = "/Folder/image2.jpt"
        every {
            localStorageProvider.getExpectedRemotePath(
                remotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                newName = newName,
                isFolder = OC_FILE_WITH_SPACE_ID.isFolder
            )
        } returns newRemotePath
        every {
            localFileDataSource.getFileByRemotePath(
                remotePath = newRemotePath,
                owner = OC_FILE_WITH_SPACE_ID.owner,
                spaceId = OC_FILE_WITH_SPACE_ID.spaceId
            )
        } returns OC_FILE_WITH_SPACE_ID

        assertThrows(FileAlreadyExistsException::class.java) {
            ocFileRepository.renameFile(OC_FILE_WITH_SPACE_ID, newName)
        }

        verify(exactly = 1) {
            localStorageProvider.getExpectedRemotePath(
                remotePath = OC_FILE_WITH_SPACE_ID.remotePath,
                newName = newName,
                isFolder = OC_FILE_WITH_SPACE_ID.isFolder
            )
            localFileDataSource.getFileByRemotePath(
                remotePath = newRemotePath,
                owner = OC_FILE_WITH_SPACE_ID.owner,
                spaceId = OC_FILE_WITH_SPACE_ID.spaceId
            )
        }
    }

    @Test
    fun `saveFile saves a file correctly`() {
        ocFileRepository.saveFile(OC_FILE_WITH_SPACE_ID)

        verify(exactly = 1) {
            localFileDataSource.saveFile(OC_FILE_WITH_SPACE_ID)
        }
    }

    @Test
    fun `saveConflict saves the etagInConflict related to a file correctly`() {
        ocFileRepository.saveConflict(OC_FILE_WITH_SPACE_ID.id!!, etagInConflict)

        verify(exactly = 1) {
            localFileDataSource.saveConflict(
                fileId = OC_FILE_WITH_SPACE_ID.id!!,
                eTagInConflict = etagInConflict
            )
        }
    }

    @Test
    fun `cleanConflict removes conflict for a file correctly`() {
        ocFileRepository.cleanConflict(OC_FILE_WITH_SPACE_ID.id!!)

        verify(exactly = 1) {
            localFileDataSource.cleanConflict(OC_FILE_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `disableThumbnailsForFile disables thumbnails for a file correctly`() {
        ocFileRepository.disableThumbnailsForFile(OC_FILE_WITH_SPACE_ID.id!!)

        verify(exactly = 1) {
            localFileDataSource.disableThumbnailsForFile(OC_FILE_WITH_SPACE_ID.id!!)
        }
    }

    @Test
    fun `updateFileWithNewAvailableOfflineStatus updates available offline status for a file correctly`() {
        ocFileRepository.updateFileWithNewAvailableOfflineStatus(OC_FILE_WITH_SPACE_ID, AvailableOfflineStatus.AVAILABLE_OFFLINE)

        verify(exactly = 1) {
            localFileDataSource.updateAvailableOfflineStatusForFile(
                ocFile = OC_FILE_WITH_SPACE_ID,
                newAvailableOfflineStatus = AvailableOfflineStatus.AVAILABLE_OFFLINE
            )
        }
    }

    @Test
    fun `updateFileWithLastUsage updates last usage for a file correctly`() {
        val lastUsage = 12345L

        ocFileRepository.updateFileWithLastUsage(OC_FILE_WITH_SPACE_ID.id!!, lastUsage)

        verify(exactly = 1) {
            localFileDataSource.updateFileWithLastUsage(
                fileId = OC_FILE_WITH_SPACE_ID.id!!,
                lastUsage = lastUsage
            )
        }
    }

    @Test
    fun `updateDownloadedFilesStorageDirectoryInStoragePath updates storage path for downloaded files correctly`() {
        val oldDirectory = "/old/directory"

        ocFileRepository.updateDownloadedFilesStorageDirectoryInStoragePath(oldDirectory, storagePath)

        verify(exactly = 1) {
            localFileDataSource.updateDownloadedFilesStorageDirectoryInStoragePath(
                oldDirectory = oldDirectory,
                newDirectory = storagePath
            )
        }
    }

    @Test
    fun `saveDownloadWorkerUuid saves the worker UUID for a file correctly`() {
        val workerUuid = UUID.randomUUID()

        ocFileRepository.saveDownloadWorkerUuid(OC_FILE_WITH_SPACE_ID.id!!, workerUuid)

        verify(exactly = 1) {
            localFileDataSource.saveDownloadWorkerUuid(
                fileId = OC_FILE_WITH_SPACE_ID.id!!,
                workerUuid = workerUuid
            )
        }
    }

    @Test
    fun `cleanWorkersUuid cleans workers UUID for a file correctly`() {
        ocFileRepository.cleanWorkersUuid(OC_FILE_WITH_SPACE_ID.id!!)

        verify(exactly = 1) {
            localFileDataSource.cleanWorkersUuid(OC_FILE_WITH_SPACE_ID.id!!)
        }
    }
}
