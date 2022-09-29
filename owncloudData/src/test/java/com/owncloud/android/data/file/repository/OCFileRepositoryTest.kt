/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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

package com.owncloud.android.data.file.repository

import com.owncloud.android.data.files.datasources.LocalFileDataSource
import com.owncloud.android.data.files.datasources.RemoteFileDataSource
import com.owncloud.android.data.files.repository.OCFileRepository
import com.owncloud.android.data.storage.LocalStorageProvider
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FOLDER
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Ignore
import org.junit.Test

@Ignore("Ignore temporary, pretty dependant on implementation... Will be reworked when finished")
class OCFileRepositoryTest {

    private val remoteFileDataSource = mockk<RemoteFileDataSource>(relaxed = true)
    private val localFileDataSource = mockk<LocalFileDataSource>(relaxed = true)
    private val localStorageProvider = mockk<LocalStorageProvider>()
    private val ocFileRepository: OCFileRepository = OCFileRepository(localFileDataSource, remoteFileDataSource, localStorageProvider)

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

    @Test
    fun `create folder - ok`() {
        every { remoteFileDataSource.createFolder(OC_FOLDER.remotePath, false, false) } returns Unit


        ocFileRepository.createFolder(OC_FOLDER.remotePath, OC_FOLDER)

        verify(exactly = 1) {
            remoteFileDataSource.createFolder(any(), false, false)
            localFileDataSource.saveFilesInFolderAndReturnThem(any(), OC_FOLDER)
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun `create folder - ko - no connection exception`() {
        every {
            remoteFileDataSource.createFolder(OC_FOLDER.remotePath, false, false)
        } throws NoConnectionWithServerException()

        ocFileRepository.createFolder(OC_FOLDER.remotePath, OC_FOLDER)

        verify(exactly = 1) {
            remoteFileDataSource.createFolder(any(), false, false)
        }
        verify(exactly = 0) {
            localFileDataSource.saveFilesInFolderAndReturnThem(any(), OC_FOLDER)
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
        every { localFileDataSource.getFileByRemotePath(OC_FOLDER.remotePath, OC_FOLDER.owner) } returns OC_FOLDER

        ocFileRepository.getFileByRemotePath(OC_FOLDER.remotePath, OC_FOLDER.owner)

        verify(exactly = 1) {
            localFileDataSource.getFileByRemotePath(OC_FOLDER.remotePath, OC_FOLDER.owner)
        }
    }

    @Test(expected = Exception::class)
    fun `get file by remote path - ko`() {
        every {
            localFileDataSource.getFileByRemotePath(OC_FOLDER.remotePath, OC_FOLDER.owner)
        } throws Exception()

        ocFileRepository.getFileByRemotePath(OC_FOLDER.remotePath, OC_FOLDER.owner)

        verify(exactly = 1) {
            localFileDataSource.getFileByRemotePath(OC_FOLDER.remotePath, OC_FOLDER.owner)
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
            remoteFileDataSource.refreshFolder(folderToFetch.remotePath)
        } returns listOfFilesRetrieved

        ocFileRepository.refreshFolder(folderToFetch.remotePath)

        verify(exactly = 1) {
            remoteFileDataSource.refreshFolder(folderToFetch.remotePath)
            localFileDataSource.saveFilesInFolderAndReturnThem(
                listOfFiles = listOfFilesRetrieved.drop(1),
                folder = listOfFilesRetrieved.first()
            )
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun `refresh folder - ko - no connection exception`() {
        every {
            remoteFileDataSource.refreshFolder(folderToFetch.remotePath)
        } throws NoConnectionWithServerException()

        ocFileRepository.refreshFolder(folderToFetch.remotePath)

        verify(exactly = 1) {
            remoteFileDataSource.refreshFolder(OC_FOLDER.remotePath)
        }
        verify(exactly = 0) {
            localFileDataSource.saveFilesInFolderAndReturnThem(any(), any())
        }
    }

    @Test
    fun `remove file - ok`() {
        every { remoteFileDataSource.removeFile(any()) } returns Unit
        every { localStorageProvider.deleteLocalFile(any()) } returns true

        ocFileRepository.removeFile(listOfFilesToRemove = listOfFileToRemove, removeOnlyLocalCopy = false)

        verify(exactly = listOfFilesRetrieved.size) {
            remoteFileDataSource.removeFile(any())
            localFileDataSource.removeFile(any())
            localStorageProvider.deleteLocalFile(any())
        }
    }

    @Test
    fun `remove file - ok - only local copy`() {
        every { localStorageProvider.deleteLocalFile(any()) } returns true

        ocFileRepository.removeFile(listOfFilesToRemove = listOfFileToRemove, removeOnlyLocalCopy = true)

        verify(exactly = listOfFilesRetrieved.size) { localStorageProvider.deleteLocalFile(any()) }
        verify(exactly = listOfFilesRetrieved.size) { localFileDataSource.saveFile(any()) }
        verify(exactly = 0) {
            remoteFileDataSource.removeFile(any())
            localFileDataSource.removeFile(any())
        }
    }

    @Test
    fun `remove file - ok - folder recursively`() {
        every { remoteFileDataSource.removeFile(any()) } returns Unit
        every { localFileDataSource.getFolderContent(0) } returns listOfFileToRemove
        every { localFileDataSource.getFolderContent(1) } returns listOf(OC_FILE)
        every { localStorageProvider.deleteLocalFile(any()) } returns true

        ocFileRepository.removeFile(listOfFilesToRemove = listOf(OC_FOLDER.copy(id = 0)), removeOnlyLocalCopy = false)

        verify(exactly = 1) { remoteFileDataSource.removeFile(any()) }
        verify(exactly = 2) { localFileDataSource.getFolderContent(any()) }
        // Removing initial folder + listOfFilesToRemove.size + file inside a folder in listOfFilesToRemove
        verify(exactly = listOfFileToRemove.size + 2) {
            localFileDataSource.removeFile(any())
            localStorageProvider.deleteLocalFile(any())
        }
    }

    @Test
    fun `remove file - ko - file not found exception`() {
        every { remoteFileDataSource.removeFile(any()) } throws FileNotFoundException()
        every { localStorageProvider.deleteLocalFile(any()) } returns true

        ocFileRepository.removeFile(listOfFilesToRemove = listOf(OC_FILE), removeOnlyLocalCopy = false)

        verify(exactly = 1) {
            remoteFileDataSource.removeFile(any())
            localFileDataSource.removeFile(any())
            localStorageProvider.deleteLocalFile(any())
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun `remove file - ko - no connection exception`() {
        every {
            remoteFileDataSource.removeFile(any())
        } throws NoConnectionWithServerException()

        ocFileRepository.removeFile(listOfFilesRetrieved, removeOnlyLocalCopy = false)

        verify(exactly = 1) { remoteFileDataSource.removeFile(OC_FOLDER.remotePath) }
        verify(exactly = 0) { localFileDataSource.removeFile(any()) }
    }

    @Test
    fun `save file - ok`() {
        ocFileRepository.saveFile(OC_FILE)

        verify(exactly = 1) { localFileDataSource.saveFile(OC_FILE) }
    }
}
