/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.owncloud.android.data.files.repository

import com.owncloud.android.data.files.datasources.LocalFileDataSource
import com.owncloud.android.data.files.datasources.RemoteFileDataSource
import com.owncloud.android.data.providers.LocalStorageProvider
import com.owncloud.android.data.spaces.datasources.LocalSpacesDataSource
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FILE_WITH_SYNC_INFO_AND_SPACE
import com.owncloud.android.testutil.OC_FOLDER
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Ignore
import org.junit.Test

@Ignore("Ignore temporary, pretty dependant on implementation... Will be reworked when finished")
@ExperimentalCoroutinesApi
class OCFileRepositoryTest {

    private val remoteFileDataSource = mockk<RemoteFileDataSource>(relaxed = true)
    private val localFileDataSource = mockk<LocalFileDataSource>(relaxed = true)
    private val localSpacesDataSource = mockk<LocalSpacesDataSource>(relaxed = true)
    private val localStorageProvider = mockk<LocalStorageProvider>()
    private val ocFileRepository: OCFileRepository =
        OCFileRepository(localFileDataSource, remoteFileDataSource, localSpacesDataSource, localStorageProvider)

    private val folderToFetch = OC_FOLDER
    private val listOfFilesRetrieved = listOf(
        folderToFetch,
        OC_FOLDER.copy(remoteId = "one"),
        OC_FOLDER.copy(remoteId = "two")
    )
    private val listOfFileToRemove = listOf(
        OC_FOLDER.copy(id = 1),
        OC_FILE.copy(id = 2),
        OC_FILE.copy(id = 3)
    )

    private val timeInMilliseconds = 3600000L

    @Test
    fun `create folder - ok`() {
        every { remoteFileDataSource.createFolder(OC_FOLDER.remotePath, false, false, OC_ACCOUNT_NAME, null) } returns Unit


        ocFileRepository.createFolder(OC_FOLDER.remotePath, OC_FOLDER)

        verify(exactly = 1) {
            remoteFileDataSource.createFolder(any(), false, false, OC_ACCOUNT_NAME, null)
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(any(), OC_FOLDER)
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun `create folder - ko - no connection exception`() {
        every {
            remoteFileDataSource.createFolder(OC_FOLDER.remotePath, false, false, OC_ACCOUNT_NAME, null)
        } throws NoConnectionWithServerException()

        ocFileRepository.createFolder(OC_FOLDER.remotePath, OC_FOLDER)

        verify(exactly = 1) {
            remoteFileDataSource.createFolder(any(), false, false, OC_ACCOUNT_NAME, null)
        }
        verify(exactly = 0) {
            localFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(any(), OC_FOLDER)
        }
    }

    @Test
    fun `get file by id - ok`() {
        every { localFileDataSource.getFileById(OC_FOLDER.id!!) } returns OC_FOLDER

        val ocFile = ocFileRepository.getFileById(OC_FOLDER.id!!)

        assertEquals(OC_FOLDER, ocFile)

        verify(exactly = 1) {
            localFileDataSource.getFileById(OC_FOLDER.id!!)
        }
    }

    @Test
    fun `getFileWithSyncInfoByIdAsFlow returns OCFileWithSyncInfo`() = runTest {
        every { localFileDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) } returns flowOf(OC_FILE_WITH_SYNC_INFO_AND_SPACE)

        val ocFile = ocFileRepository.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!)

        ocFile.collect { result ->
            assertEquals(OC_FILE_WITH_SYNC_INFO_AND_SPACE, result)
        }

        verify(exactly = 1) {
            localFileDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!)
        }
    }

    @Test
    fun `getFileWithSyncInfoByIdAsFlow returns null`() = runTest {
        every { localFileDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) } returns flowOf(null)

        val ocFile = ocFileRepository.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!)

        ocFile.collect { result ->
            assertNull(result)
        }

        verify(exactly = 1) {
            localFileDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!)
        }
    }

    @Test(expected = Exception::class)
    fun `getFileWithSyncInfoByIdAsFlow returns an exception`() = runTest {
        every { localFileDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) } throws Exception()

        ocFileRepository.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!)

        verify(exactly = 1) {
            localFileDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!)
        }
    }

    @Test
    fun `get file by id - ok - null`() {
        every { localFileDataSource.getFileById(OC_FOLDER.id!!) } returns null

        val ocFile = ocFileRepository.getFileById(OC_FOLDER.id!!)

        assertNull(ocFile)

        verify(exactly = 1) {
            localFileDataSource.getFileById(OC_FOLDER.id!!)
        }
    }

    @Test(expected = Exception::class)
    fun `get file by id - ko`() {
        every { localFileDataSource.getFileById(OC_FOLDER.id!!) } throws Exception()

        ocFileRepository.getFileById(OC_FOLDER.id!!)

        verify(exactly = 1) {
            localFileDataSource.getFileById(OC_FOLDER.id!!)
        }
    }

    @Test
    fun `get file by remote path - ok`() {
        every { localFileDataSource.getFileByRemotePath(OC_FOLDER.remotePath, OC_FOLDER.owner, null) } returns OC_FOLDER

        ocFileRepository.getFileByRemotePath(OC_FOLDER.remotePath, OC_FOLDER.owner)

        verify(exactly = 1) {
            localFileDataSource.getFileByRemotePath(OC_FOLDER.remotePath, OC_FOLDER.owner, null)
        }
    }

    @Test(expected = Exception::class)
    fun `get file by remote path - ko`() {
        every {
            localFileDataSource.getFileByRemotePath(OC_FOLDER.remotePath, OC_FOLDER.owner, null)
        } throws Exception()

        ocFileRepository.getFileByRemotePath(OC_FOLDER.remotePath, OC_FOLDER.owner)

        verify(exactly = 1) {
            localFileDataSource.getFileByRemotePath(OC_FOLDER.remotePath, OC_FOLDER.owner, null)
        }
    }

    @Test
    fun `getDownloadedFilesForAccount returns a list of OCFile`() {
        every {
            localFileDataSource.getDownloadedFilesForAccount(OC_ACCOUNT_NAME)
        } returns listOf(OC_FILE)

        val result = ocFileRepository.getDownloadedFilesForAccount(OC_ACCOUNT_NAME)

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) {
            localFileDataSource.getDownloadedFilesForAccount(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `get folder content - ok`() {
        every { localFileDataSource.getFolderContent(OC_FOLDER.parentId!!) } returns listOf(OC_FOLDER)

        val folderContent = ocFileRepository.getFolderContent(OC_FOLDER.parentId!!)

        assertEquals(listOf(OC_FOLDER), folderContent)

        verify(exactly = 1) {
            localFileDataSource.getFolderContent(OC_FOLDER.parentId!!)
        }
    }

    @Test
    fun `getFilesLastUsageIsOlderThanGivenTime returns a list of OCFile`() {
        every {
            localFileDataSource.getFilesWithLastUsageOlderThanGivenTime(timeInMilliseconds)
        } returns listOf(OC_FILE)

        val result = ocFileRepository.getFilesWithLastUsageOlderThanGivenTime(timeInMilliseconds)

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) {
            localFileDataSource.getFilesWithLastUsageOlderThanGivenTime(timeInMilliseconds)
        }
    }

    @Test
    fun `getFilesLastUsageIsOlderThanGivenTime returns an empty list when datasource returns an empty list`() {
        every {
            localFileDataSource.getFilesWithLastUsageOlderThanGivenTime(timeInMilliseconds)
        } returns emptyList()

        val result = ocFileRepository.getFilesWithLastUsageOlderThanGivenTime(timeInMilliseconds)

        assertEquals(emptyList<OCFile>(), result)

        verify(exactly = 1) {
            localFileDataSource.getFilesWithLastUsageOlderThanGivenTime(timeInMilliseconds)
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
    fun `get folder images - ok`() {
        every { localFileDataSource.getFolderImages(OC_FOLDER.parentId!!) } returns listOf(OC_FOLDER)

        val folderContent = ocFileRepository.getFolderImages(OC_FOLDER.parentId!!)

        assertEquals(listOf(OC_FOLDER), folderContent)

        verify(exactly = 1) {
            localFileDataSource.getFolderImages(OC_FOLDER.parentId!!)
        }
    }

    @Test(expected = Exception::class)
    fun `get folder images - ko`() {
        every { localFileDataSource.getFolderImages(OC_FOLDER.parentId!!) } throws Exception()

        ocFileRepository.getFolderImages(OC_FOLDER.parentId!!)

        verify(exactly = 1) {
            localFileDataSource.getFolderImages(OC_FOLDER.parentId!!)
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
}
