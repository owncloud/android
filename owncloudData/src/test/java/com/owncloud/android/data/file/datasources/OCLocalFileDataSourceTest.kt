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

import com.owncloud.android.data.files.datasources.implementation.OCLocalFileDataSource
import com.owncloud.android.data.files.datasources.implementation.OCLocalFileDataSource.Companion.toEntity
import com.owncloud.android.data.files.db.FileDao
import com.owncloud.android.data.files.db.OCFileAndFileSync
import com.owncloud.android.data.files.db.OCFileEntity
import com.owncloud.android.data.files.db.OCFileSyncEntity
import com.owncloud.android.data.spaces.datasources.implementation.OCLocalSpacesDataSource.Companion.toEntity
import com.owncloud.android.domain.files.model.MIME_DIR
import com.owncloud.android.domain.files.model.MIME_PREFIX_IMAGE
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PARENT_ID
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FILE_WITH_SYNC_INFO_AND_SPACE
import com.owncloud.android.testutil.OC_SPACE_PERSONAL
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class OCLocalFileDataSourceTest {
    private lateinit var localDataSource: OCLocalFileDataSource
    private lateinit var dao: FileDao
    private val ocFileAndFileSync = OCFileAndFileSync(OC_FILE.toEntity(), OCFileSyncEntity(
        fileId = OC_FILE.id!!,
        uploadWorkerUuid = null,
        downloadWorkerUuid = null,
        isSynchronizing = false
    ), OC_SPACE_PERSONAL.toEntity())

    @Before
    fun init() {
        dao = spyk()
        localDataSource = OCLocalFileDataSource(dao)
    }

    @Test
    fun `getFileById returns ok`() {
        every { dao.getFileById(any()) } returns DUMMY_FILE_ENTITY

        val result = localDataSource.getFileById(OC_FILE.id!!)

        assertEquals(OC_FILE, result)

        verify (exactly = 1) { dao.getFileById(OC_FILE.id!!) }
    }

    @Test
    fun `getFileById returns null`() {
        every { dao.getFileById(any()) } returns null

        val result = localDataSource.getFileById(DUMMY_FILE_ENTITY.id)

        assertNull(result)

        verify (exactly = 1) { dao.getFileById(DUMMY_FILE_ENTITY.id) }
    }

    @Test(expected = Exception::class)
    fun `getFileById returns an Exception`() {
        every { dao.getFileById(any()) } throws Exception()

        localDataSource.getFileById(DUMMY_FILE_ENTITY.id)
    }

    @Test
    fun `getFileByIdAsFlow returns ok`() =  runBlocking {
        every { dao.getFileByIdAsFlow(any()) } returns flowOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getFileByIdAsFlow(OC_FILE.id!!)

        result.collect{ result ->
            assertEquals(OC_FILE, result)
        }

        verify (exactly = 1) { dao.getFileByIdAsFlow(OC_FILE.id!!) }
    }

    @Test
    fun `getFileByIdAsFlow returns null`() =  runBlocking {
        every { dao.getFileByIdAsFlow(any()) } returns flowOf(null)

        val result = localDataSource.getFileByIdAsFlow(DUMMY_FILE_ENTITY.id)

        result.collect { result->
            assertNull(result)
        }

        verify (exactly = 1) { dao.getFileByIdAsFlow(DUMMY_FILE_ENTITY.id) }
    }

    @Test(expected = Exception::class)
    fun `getFileByIdAsFlow returns an Exception`() {
        every { dao.getFileByIdAsFlow(any()) } throws Exception()

        localDataSource.getFileByIdAsFlow(DUMMY_FILE_ENTITY.id)
    }

    @Test
    fun `getFileWithSyncInfoByIdAsFlow returns a flow of OCFileWithSyncInfo object`() = runBlocking {

        every { dao.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) } returns flowOf(ocFileAndFileSync)

        val result = localDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!)

        result.collect { emittedFileWithSyncInfo ->
            assertEquals(OC_FILE_WITH_SYNC_INFO_AND_SPACE, emittedFileWithSyncInfo)
        }

        verify (exactly = 1) { dao.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) }
    }

    @Test
    fun `getFileWithSyncInfoByIdAsFlow returns null when DAO is null`() = runBlocking {

        every { dao.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) } returns flowOf(null)

        val result = localDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!)

        result.collect { emittedFileWithSyncInfo ->
            assertNull(emittedFileWithSyncInfo)
        }

        verify (exactly = 1) { dao.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) }
    }

    @Test(expected = Exception::class)
    fun `getFileWithSyncInfoByIdAsFlow returns an exception when DAO receive a Exception`() = runBlocking {

        every { dao.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) } throws Exception()

        val result = localDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!)

        result.collect { emittedFileWithSyncInfo ->
            assertEquals(OC_FILE_WITH_SYNC_INFO_AND_SPACE, emittedFileWithSyncInfo)
        }

        verify (exactly = 1) { dao.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) }
    }

    @Test
    fun `getFileByRemotePath returns the OCFIle`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any(), any()) } returns DUMMY_FILE_ENTITY

        val result = localDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner, OC_FILE.spaceId)

        assertEquals(OC_FILE, result)

        verify (exactly = 1) { dao.getFileByOwnerAndRemotePath(OC_FILE.owner, OC_FILE.remotePath, OC_FILE.spaceId) }
    }

    @Test
    fun `getFileByRemotePath returns null when DAO is null`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any(), any()) } returns null

        val result = localDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner, OC_FILE.spaceId)

        assertNull(result)

        verify (exactly = 1) { dao.getFileByOwnerAndRemotePath(OC_FILE.owner, OC_FILE.remotePath, OC_FILE.spaceId) }
    }

    @Test
    fun `getFileByRemotePath returns null when create root folder`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any(), any()) } returns null
        every { dao.mergeRemoteAndLocalFile(any()) } returns 1234
        every { dao.getFileById(1234) } returns DUMMY_FILE_ENTITY.copy(
            parentId = ROOT_PARENT_ID,
            mimeType = MIME_DIR,
            remotePath = ROOT_PATH
        )

        val result = localDataSource.getFileByRemotePath(ROOT_PATH, OC_FILE.owner, null)

        assertNotNull(result)
        assertEquals(ROOT_PARENT_ID, result!!.parentId)
        assertEquals(OC_FILE.owner, result.owner)
        assertEquals(MIME_DIR, result.mimeType)
        assertEquals(ROOT_PATH, result.remotePath)

        verify (exactly = 1) {
            dao.getFileByOwnerAndRemotePath(OC_FILE.owner, ROOT_PATH, null)
            dao.mergeRemoteAndLocalFile(any())
            dao.getFileById(1234)
        }
    }

    @Test(expected = Exception::class)
    fun `getFileByRemotePath returns an exception`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any(), any()) } throws Exception()

        localDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner, null)

        verify (exactly = 1) { dao.getFileByOwnerAndRemotePath(OC_FILE.owner, OC_FILE.remotePath, null) }
    }

    @Test
    fun `getFileByRemoteId returns OCFile`() {
        every { dao.getFileByRemoteId(any()) } returns DUMMY_FILE_ENTITY

        val result = localDataSource.getFileByRemoteId(DUMMY_FILE_ENTITY.remoteId.toString())

        assertEquals(OC_FILE, result)

        verify (exactly = 1) { dao.getFileByRemoteId(DUMMY_FILE_ENTITY.remoteId.toString()) }
    }

    @Test
    fun `getFileByRemoteId returns null when DAO is null`() {
        every { dao.getFileByRemoteId(any()) } returns null

        val result = localDataSource.getFileByRemoteId(DUMMY_FILE_ENTITY.remoteId.toString())

        assertEquals(null, result)

        verify (exactly = 1) { dao.getFileByRemoteId(DUMMY_FILE_ENTITY.remoteId.toString()) }
    }


    @Test(expected = Exception::class)
    fun `getFileByRemoteId returns an exception`() {
        every { dao.getFileByRemoteId(any()) } throws Exception()

        localDataSource.getFileByRemoteId(DUMMY_FILE_ENTITY.remoteId.toString())

    }


    @Test
    fun `getFolderContent returns a list of OCFile`() {
        every { dao.getFolderContent(any()) } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getFolderContent(DUMMY_FILE_ENTITY.id)

        assertEquals(listOf(OC_FILE), result)

        verify { dao.getFolderContent(DUMMY_FILE_ENTITY.id) }
    }

    @Test(expected = Exception::class)
    fun `getFolderContent returns an exception`() {
        every { dao.getFolderContent(any()) } throws Exception()

        localDataSource.getFolderContent(DUMMY_FILE_ENTITY.id)

        verify { dao.getFolderContent(DUMMY_FILE_ENTITY.id) }
    }

    @Test
    fun `getSearchFolderContent returns a list of OCFile`() {
        every { dao.getSearchFolderContent(any(), any()) } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getSearchFolderContent(OC_FILE.id!!, "test")

        assertEquals(listOf(OC_FILE) , result)

        verify (exactly = 1) {
            dao.getSearchFolderContent(DUMMY_FILE_ENTITY.id, "test")
        }
    }

    @Test(expected = Exception::class)
    fun `getSearchFolderContent returns an exception`() {
        every { dao.getSearchFolderContent(any(), any()) } throws Exception()

        localDataSource.getSearchFolderContent(OC_FILE.id!!, "test")

    }

    @Test
    fun `getSearchAvailableOfflineFolderContent returns a list of OCFile`() {
        every { dao.getSearchAvailableOfflineFolderContent(any(), any()) } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getSearchAvailableOfflineFolderContent(OC_FILE.id!!, "test")

        assertEquals(listOf(OC_FILE) , result)

        verify (exactly = 1) {
            dao.getSearchAvailableOfflineFolderContent(DUMMY_FILE_ENTITY.id, "test")
        }
    }

    @Test(expected = Exception::class)
    fun `getSearchAvailableOfflineFolderContent returns an exception`() {
        every { dao.getSearchAvailableOfflineFolderContent(any(), any()) } throws Exception()

        localDataSource.getSearchAvailableOfflineFolderContent(OC_FILE.id!!, "test")

    }

    @Test
    fun `getSearchSharedByLinkFolderContent returns a list of OCFile`() {

        every { dao.getSearchSharedByLinkFolderContent(any(), any()) } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getSearchSharedByLinkFolderContent(OC_FILE.id!!, "test")

        assertEquals(listOf(OC_FILE) , result)

        verify (exactly = 1) {
            dao.getSearchSharedByLinkFolderContent(DUMMY_FILE_ENTITY.id, "test")
        }
    }

    @Test(expected = Exception::class)
    fun `getSearchSharedByLinkFolderContent returns an exception`() {

        every { dao.getSearchSharedByLinkFolderContent(any(), any()) }  throws Exception()

        localDataSource.getSearchSharedByLinkFolderContent(OC_FILE.id!!, "test")

    }

    @Test
    fun `getFolderContentWithSyncInfoAsFlow returns a flow of list OcFileAndFileSync`() = runBlocking {

        every { dao.getFolderContentWithSyncInfoAsFlow(any()) } returns flowOf(listOf(ocFileAndFileSync))

        val result = localDataSource.getFolderContentWithSyncInfoAsFlow(OC_FILE.id!!)

        result.collect { result->
            assertEquals(listOf(OC_FILE_WITH_SYNC_INFO_AND_SPACE) , result)
        }

        verify (exactly = 1) {
            dao.getFolderContentWithSyncInfoAsFlow(OC_FILE.id!!)
        }
    }

    @Test(expected = Exception::class)
    fun `getFolderContentWithSyncInfoAsFlow returns an exception`() = runBlocking {

        every { dao.getFolderContentWithSyncInfoAsFlow(any()) } throws Exception()

        val result = localDataSource.getFolderContentWithSyncInfoAsFlow(OC_FILE.id!!)

    }

    @Test
    fun `getFolderImages returns a list of OCFile`() {
        every { dao.getFolderByMimeType(any(), any()) } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getFolderImages(DUMMY_FILE_ENTITY.id)

        assertEquals(listOf(OC_FILE), result)

        verify (exactly = 1) { dao.getFolderByMimeType(DUMMY_FILE_ENTITY.id, MIME_PREFIX_IMAGE) }
    }

    @Test(expected = Exception::class)
    fun `getFolderImages returns a exception`() {
        every { dao.getFolderByMimeType(any(), any()) } throws Exception()

        localDataSource.getFolderImages(DUMMY_FILE_ENTITY.id)

        verify (exactly = 1) { dao.getFolderByMimeType(DUMMY_FILE_ENTITY.id, MIME_PREFIX_IMAGE) }
    }

    @Test
    fun `getSharedByLinkWithSyncInfoForAccountAsFlow returns a flow of list of OCFileWithSyncInfo`() = runBlocking {
        every { dao.getFilesWithSyncInfoSharedByLinkAsFlow(any()) } returns flowOf(listOf(ocFileAndFileSync))

        val result = localDataSource.getSharedByLinkWithSyncInfoForAccountAsFlow(DUMMY_FILE_ENTITY.owner)

        result.collect { result ->
            assertEquals(listOf(OC_FILE_WITH_SYNC_INFO_AND_SPACE), result)
        }

        verify (exactly = 1) {
            dao.getFilesWithSyncInfoSharedByLinkAsFlow(DUMMY_FILE_ENTITY.owner)
        }
    }

    @Test(expected = Exception::class)
    fun `getSharedByLinkWithSyncInfoForAccountAsFlow returns a exception`() = runBlocking {
        every { dao.getFilesWithSyncInfoSharedByLinkAsFlow(any()) } throws Exception()

        val result = localDataSource.getSharedByLinkWithSyncInfoForAccountAsFlow(DUMMY_FILE_ENTITY.owner)

        result.collect { result ->
            assertEquals(listOf(OC_FILE_WITH_SYNC_INFO_AND_SPACE), result)
        }
    }

    @Test
    fun `getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow returns a flow of list of OCFileWithSyncInfo`() = runBlocking {
        every { dao.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(any()) } returns flowOf(listOf(ocFileAndFileSync))

        val result = localDataSource.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(DUMMY_FILE_ENTITY.owner)

        result.collect { result ->
            assertEquals(listOf(OC_FILE_WITH_SYNC_INFO_AND_SPACE), result)
        }

        verify (exactly = 1) {
            dao.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(DUMMY_FILE_ENTITY.owner)
        }
    }

    @Test(expected = Exception::class)
    fun `getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow returns an Exception`() = runBlocking {
        every { dao.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(any()) } throws Exception()

        val result = localDataSource.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(DUMMY_FILE_ENTITY.owner)

    }

    @Test
    fun `getFilesAvailableOfflineFromAccount returns a flow of list of OCFile`() {
        every { dao.getFilesAvailableOfflineFromAccount(any()) } returns listOf(OC_FILE.toEntity())

        val result = localDataSource.getFilesAvailableOfflineFromAccount(DUMMY_FILE_ENTITY.owner)

        assertEquals(listOf(OC_FILE), result)

        verify (exactly = 1) {
            dao.getFilesAvailableOfflineFromAccount(DUMMY_FILE_ENTITY.owner)
        }
    }

    @Test(expected = Exception::class)
    fun `getFilesAvailableOfflineFromAccount returns an exception`() {
        every { dao.getFilesAvailableOfflineFromAccount(any()) } throws Exception()

        val result = localDataSource.getFilesAvailableOfflineFromAccount(DUMMY_FILE_ENTITY.owner)

    }

    @Test
    fun `getFilesAvailableOfflineFromEveryAccount returns list of OCFile`() {
        every { dao.getFilesAvailableOfflineFromEveryAccount() } returns listOf(OC_FILE.toEntity())

        val result = localDataSource.getFilesAvailableOfflineFromEveryAccount()

        assertEquals(listOf(OC_FILE), result)

        verify (exactly = 1) {
            dao.getFilesAvailableOfflineFromEveryAccount()
        }
    }

    @Test(expected = Exception::class)
    fun `getFilesAvailableOfflineFromEveryAccount returns an exception`() {
        every { dao.getFilesAvailableOfflineFromEveryAccount() } throws Exception()

        val result = localDataSource.getFilesAvailableOfflineFromEveryAccount()

    }

    @Test
    fun `moveFile returns list of OCFile`() {
        every { dao.getFilesAvailableOfflineFromEveryAccount() } returns listOf(OC_FILE.toEntity())

        val result = localDataSource.getFilesAvailableOfflineFromEveryAccount()

        assertEquals(listOf(OC_FILE), result)

        verify (exactly = 1) {
            dao.getFilesAvailableOfflineFromEveryAccount()
        }
    }

    @Test
    fun `remove file - ok`() {
        every { dao.deleteFileById(any()) } returns Unit

        localDataSource.deleteFile(DUMMY_FILE_ENTITY.id)

        verify { dao.deleteFileById(DUMMY_FILE_ENTITY.id) }
    }

    @Test(expected = Exception::class)
    fun `remove file - ko`() {
        every { dao.deleteFileById(any()) } throws Exception()

        localDataSource.deleteFile(DUMMY_FILE_ENTITY.id)

        verify { dao.deleteFileById(DUMMY_FILE_ENTITY.id) }
    }

    companion object {
        private val DUMMY_FILE_ENTITY: OCFileEntity = OC_FILE.toEntity()
    }
}
