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
import com.owncloud.android.testutil.OC_FOLDER
import com.owncloud.android.testutil.OC_SERVER_INFO
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OCFileRepositoryTest {

    private val remoteFileDataSource = mockk<RemoteFileDataSource>(relaxed = true)
    private val localFileDataSource = mockk<LocalFileDataSource>(relaxed = true)
    private val ocFileRepository: OCFileRepository = OCFileRepository(localFileDataSource, remoteFileDataSource)

    private val folderToFetch = OC_FOLDER
    private val listOfFilesRetrieved = listOf(
        folderToFetch,
        OC_FOLDER.copy(remoteId = "one"),
        OC_FOLDER.copy(remoteId = "two")
    )

    @Test
    fun `check path existence - ok`() {
        every { remoteFileDataSource.checkPathExistence(OC_SERVER_INFO.baseUrl, false) } returns true

        ocFileRepository.checkPathExistence(OC_SERVER_INFO.baseUrl, false)

        verify(exactly = 1) {
            remoteFileDataSource.checkPathExistence(OC_SERVER_INFO.baseUrl, false)
        }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun `check path existence - ko - no connection exception`() {
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
    fun `create folder - ok`() {
        every { remoteFileDataSource.createFolder(OC_FOLDER.remotePath, false, false) } returns Unit


        ocFileRepository.createFolder(OC_FOLDER.remotePath, OC_FOLDER)

        verify(exactly = 1) {
            remoteFileDataSource.createFolder(any(), false, false)
            localFileDataSource.saveFilesInFolder(any(), OC_FOLDER)
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
            localFileDataSource.saveFilesInFolder(any(), OC_FOLDER)
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
            localFileDataSource.saveFilesInFolder(
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
            localFileDataSource.saveFilesInFolder(any(), any())
        }
    }
}
