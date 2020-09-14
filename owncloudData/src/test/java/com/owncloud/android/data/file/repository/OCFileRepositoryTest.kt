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

package com.owncloud.android.data.file.repository

import com.owncloud.android.data.files.datasources.LocalFileDataSource
import com.owncloud.android.data.files.datasources.RemoteFileDataSource
import com.owncloud.android.data.files.repository.OCFileRepository
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_SERVER_INFO
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class OCFileRepositoryTest {

    private val remoteFileDataSource = mockk<RemoteFileDataSource>(relaxed = true)
    private val localFileDataSource = mockk<LocalFileDataSource>(relaxed = true)
    private val ocFileRepository: OCFileRepository = OCFileRepository(localFileDataSource, remoteFileDataSource)

    private val folderToFetch = OC_FILE
    private val listOfFilesRetrieved = listOf(
        folderToFetch,
        OC_FILE.copy(remoteId = "one"),
        OC_FILE.copy(remoteId = "two")
    )

    @Test
    fun checkPathExistenceExists() {
        every { remoteFileDataSource.checkPathExistence(OC_SERVER_INFO.baseUrl, false) } returns true

        ocFileRepository.checkPathExistence(OC_SERVER_INFO.baseUrl, false)

        verify(exactly = 1) {
            remoteFileDataSource.checkPathExistence(OC_SERVER_INFO.baseUrl, false)
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun checkPathExistenceExistsNoConnection() {
        every {
            remoteFileDataSource.checkPathExistence(
                OC_SERVER_INFO.baseUrl,
                false
            )
        } throws NoConnectionWithServerException()

        ocFileRepository.checkPathExistence(OC_SERVER_INFO.baseUrl, false)

        verify(exactly = 1) {
            remoteFileDataSource.checkPathExistence(OC_SERVER_INFO.baseUrl, false)
        }
    }

    @Test
    fun refreshFolderOk() {
        every {
            remoteFileDataSource.refreshFolder(folderToFetch.remotePath)
        } returns listOfFilesRetrieved

        ocFileRepository.refreshFolder(folderToFetch.remotePath)

        verify(exactly = 1) {
            remoteFileDataSource.refreshFolder(folderToFetch.remotePath)
            localFileDataSource.saveFilesInFolder(
                listOfFiles = listOfFilesRetrieved.drop(1),
                folder = listOfFilesRetrieved.first()
            )
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun refreshFolderNoConnection() {
        every {
            remoteFileDataSource.refreshFolder(folderToFetch.remotePath)
        } throws NoConnectionWithServerException()

        ocFileRepository.refreshFolder(folderToFetch.remotePath)

        verify(exactly = 1) {
            remoteFileDataSource.refreshFolder(OC_FILE.remotePath)
        }
        verify(exactly = 0) {
            localFileDataSource.saveFilesInFolder(any(), any())
        }
    }

    @Test
    fun createFolder() {
        every { remoteFileDataSource.createFolder(OC_FILE.remotePath, false, false) } returns Unit

        ocFileRepository.createFolder(OC_FILE.remotePath, OC_FILE)

        verify(exactly = 1) {
            remoteFileDataSource.createFolder(any(), false, false)
            localFileDataSource.saveFilesInFolder(any(), OC_FILE)
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun createFolderNoConnection() {
        every {
            remoteFileDataSource.createFolder(OC_FILE.remotePath, false, false)
        } throws NoConnectionWithServerException()

        ocFileRepository.createFolder(OC_FILE.remotePath, OC_FILE)

        verify(exactly = 1) {
            remoteFileDataSource.createFolder(any(), false, false)
        }
        verify(exactly = 0) {
            localFileDataSource.saveFilesInFolder(any(), OC_FILE)
        }
    }

    @Test
    fun getFileByIdSuccess() {
        every { localFileDataSource.getFileById(OC_FILE.id!!) } returns OC_FILE

        val ocFile = ocFileRepository.getFileById(OC_FILE.id!!)

        assertEquals(OC_FILE, ocFile)

        verify(exactly = 1) {
            localFileDataSource.getFileById(OC_FILE.id!!)
        }
    }

    @Test(expected = Exception::class)
    fun getFileByIdException() {
        every { localFileDataSource.getFileById(OC_FILE.id!!) } throws Exception()

        ocFileRepository.getFileById(OC_FILE.id!!)

        verify(exactly = 1) {
            localFileDataSource.getFileById(OC_FILE.id!!)
        }
    }

    @Test
    fun getFileByRemotePathAndOwnerSuccess() {
        every { localFileDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner) } returns OC_FILE

        ocFileRepository.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner)

        verify(exactly = 1) {
            localFileDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner)
        }
    }

    @Test(expected = Exception::class)
    fun getFileByRemotePathAndOwnerException() {
        every {
            localFileDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner)
        } throws Exception()

        ocFileRepository.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner)

        verify(exactly = 1) {
            localFileDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner)
        }
    }

    @Test
    fun getFolderContentSuccess() {
        every { localFileDataSource.getFolderContent(OC_FILE.parentId!!) } returns listOf(OC_FILE)

        val folderContent = ocFileRepository.getFolderContent(OC_FILE.parentId!!)

        assertEquals(listOf(OC_FILE), folderContent)

        verify(exactly = 1) {
            localFileDataSource.getFolderContent(OC_FILE.parentId!!)
        }
    }

    @Test(expected = Exception::class)
    fun getFolderContentException() {
        every { localFileDataSource.getFolderContent(OC_FILE.parentId!!) } throws Exception()

        ocFileRepository.getFolderContent(OC_FILE.parentId!!)

        verify(exactly = 1) {
            localFileDataSource.getFolderContent(OC_FILE.parentId!!)
        }
    }
}
