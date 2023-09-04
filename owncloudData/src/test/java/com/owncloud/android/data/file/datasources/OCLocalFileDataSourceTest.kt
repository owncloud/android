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
import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus
import com.owncloud.android.domain.files.model.MIME_DIR
import com.owncloud.android.domain.files.model.MIME_PREFIX_IMAGE
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PARENT_ID
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FILE_AVAILABLE_OFFLINE
import com.owncloud.android.testutil.OC_FILE_WITH_SYNC_INFO_AND_SPACE
import com.owncloud.android.testutil.OC_SPACE_PERSONAL
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

@ExperimentalCoroutinesApi
class OCLocalFileDataSourceTest {
    private lateinit var localDataSource: OCLocalFileDataSource
    private lateinit var dao: FileDao
    private val ocFileAndFileSync = OCFileAndFileSync(
        DUMMY_FILE_ENTITY,
        OCFileSyncEntity(
            fileId = OC_FILE.id!!,
            uploadWorkerUuid = null,
            downloadWorkerUuid = null,
            isSynchronizing = false,
        ),
        OC_SPACE_PERSONAL.toEntity(),
    )

    @Before
    fun init() {
        dao = spyk()
        localDataSource = OCLocalFileDataSource(dao)
    }

    @Test
    fun `getFileById returns the same result as the localDataSource getFileById called in this method`() {
        every { dao.getFileById(any()) } returns DUMMY_FILE_ENTITY

        val result = localDataSource.getFileById(OC_FILE.id!!)

        assertEquals(OC_FILE, result)

        verify(exactly = 1) { dao.getFileById(OC_FILE.id!!) }
    }

    @Test
    fun `getFileById returns null when dao is null`() {
        every { dao.getFileById(any()) } returns null

        val result = localDataSource.getFileById(DUMMY_FILE_ENTITY.id)

        assertNull(result)

        verify(exactly = 1) { dao.getFileById(DUMMY_FILE_ENTITY.id) }
    }

    @Test(expected = Exception::class)
    fun `getFileById returns an Exception when getFileById receive an exception`() {
        every { dao.getFileById(any()) } throws Exception()

        localDataSource.getFileById(DUMMY_FILE_ENTITY.id)
    }

    @Test
    fun `getFileByIdAsFlow returns a flow of OCFile`() = runTest {
        every { dao.getFileByIdAsFlow(any()) } returns flowOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getFileByIdAsFlow(OC_FILE.id!!)

        result.collect { result ->
            assertEquals(OC_FILE, result)
        }

        verify(exactly = 1) { dao.getFileByIdAsFlow(OC_FILE.id!!) }
    }

    @Test
    fun `getFileByIdAsFlow returns null`() = runTest {
        every { dao.getFileByIdAsFlow(any()) } returns flowOf(null)

        val result = localDataSource.getFileByIdAsFlow(DUMMY_FILE_ENTITY.id)

        result.collect { result ->
            assertNull(result)
        }

        verify(exactly = 1) { dao.getFileByIdAsFlow(DUMMY_FILE_ENTITY.id) }
    }

    @Test(expected = Exception::class)
    fun `getFileByIdAsFlow returns an Exception when getFileByIdAsFlow receive an exception`() {
        every { dao.getFileByIdAsFlow(any()) } throws Exception()

        localDataSource.getFileByIdAsFlow(DUMMY_FILE_ENTITY.id)
    }

    @Test
    fun `getFileWithSyncInfoByIdAsFlow returns a flow of OCFileWithSyncInfo object`() = runTest {

        every { dao.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) } returns flowOf(ocFileAndFileSync)

        val result = localDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!)

        result.collect { emittedFileWithSyncInfo ->
            assertEquals(OC_FILE_WITH_SYNC_INFO_AND_SPACE, emittedFileWithSyncInfo)
        }

        verify(exactly = 1) { dao.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) }
    }

    @Test
    fun `getFileWithSyncInfoByIdAsFlow returns null when DAO is null`() = runTest {

        every { dao.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) } returns flowOf(null)

        val result = localDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!)

        result.collect { emittedFileWithSyncInfo ->
            assertNull(emittedFileWithSyncInfo)
        }

        verify(exactly = 1) { dao.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) }
    }

    @Test(expected = Exception::class)
    fun `getFileWithSyncInfoByIdAsFlow returns an exception when DAO receive a Exception`() = runTest {

        every { dao.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) } throws Exception()

        val result = localDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!)

        result.collect { emittedFileWithSyncInfo ->
            assertEquals(OC_FILE_WITH_SYNC_INFO_AND_SPACE, emittedFileWithSyncInfo)
        }

        verify(exactly = 1) { dao.getFileWithSyncInfoByIdAsFlow(OC_FILE.id!!) }
    }

    @Test
    fun `getFileByRemotePath returns the OCFIle`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any(), any()) } returns DUMMY_FILE_ENTITY

        val result = localDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner, OC_FILE.spaceId)

        assertEquals(OC_FILE, result)

        verify(exactly = 1) { dao.getFileByOwnerAndRemotePath(OC_FILE.owner, OC_FILE.remotePath, OC_FILE.spaceId) }
    }

    @Test
    fun `getFileByRemotePath returns null when DAO is null`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any(), any()) } returns null

        val result = localDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner, OC_FILE.spaceId)

        assertNull(result)

        verify(exactly = 1) { dao.getFileByOwnerAndRemotePath(OC_FILE.owner, OC_FILE.remotePath, OC_FILE.spaceId) }
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

        verify(exactly = 1) {
            dao.getFileByOwnerAndRemotePath(OC_FILE.owner, ROOT_PATH, null)
            dao.mergeRemoteAndLocalFile(any())
            dao.getFileById(1234)
        }
    }

    @Test(expected = Exception::class)
    fun `getFileByRemotePath returns an exception when getFileByRemotePath receive an exception`() {
        every { dao.getFileByOwnerAndRemotePath(any(), any(), any()) } throws Exception()

        localDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner, null)

        verify(exactly = 1) { dao.getFileByOwnerAndRemotePath(OC_FILE.owner, OC_FILE.remotePath, null) }
    }

    @Test
    fun `getFileByRemoteId returns OCFile`() {
        every { dao.getFileByRemoteId(any()) } returns DUMMY_FILE_ENTITY

        val result = localDataSource.getFileByRemoteId(DUMMY_FILE_ENTITY.remoteId.toString())

        assertEquals(OC_FILE, result)

        verify(exactly = 1) { dao.getFileByRemoteId(DUMMY_FILE_ENTITY.remoteId.toString()) }
    }

    @Test
    fun `getFileByRemoteId returns null when DAO is null`() {
        every { dao.getFileByRemoteId(any()) } returns null

        val result = localDataSource.getFileByRemoteId(DUMMY_FILE_ENTITY.remoteId.toString())

        assertEquals(null, result)

        verify(exactly = 1) { dao.getFileByRemoteId(DUMMY_FILE_ENTITY.remoteId.toString()) }
    }

    @Test(expected = Exception::class)
    fun `getFileByRemoteId returns an exception when getFileByRemoteId receive an exception`() {
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
    fun `getFolderContent returns an exception when getFolderContent receive an exception`() {
        every { dao.getFolderContent(any()) } throws Exception()

        localDataSource.getFolderContent(DUMMY_FILE_ENTITY.id)

    }

    @Test
    fun `getSearchFolderContent returns a list of OCFile`() {
        every { dao.getSearchFolderContent(any(), any()) } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getSearchFolderContent(OC_FILE.id!!, "test")

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) {
            dao.getSearchFolderContent(DUMMY_FILE_ENTITY.id, "test")
        }
    }

    @Test(expected = Exception::class)
    fun `getSearchFolderContent returns an exception when getSearchFolderContent receive an exception`() {
        every { dao.getSearchFolderContent(any(), any()) } throws Exception()

        localDataSource.getSearchFolderContent(OC_FILE.id!!, "test")

    }

    @Test
    fun `getSearchAvailableOfflineFolderContent returns a list of OCFile`() {
        every { dao.getSearchAvailableOfflineFolderContent(any(), any()) } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getSearchAvailableOfflineFolderContent(OC_FILE.id!!, "test")

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) {
            dao.getSearchAvailableOfflineFolderContent(DUMMY_FILE_ENTITY.id, "test")
        }
    }

    @Test(expected = Exception::class)
    fun `getSearchAvailableOfflineFolderContent returns an exception when getSearchAvailableOfflineFolderContent receive an exception`() {
        every { dao.getSearchAvailableOfflineFolderContent(any(), any()) } throws Exception()

        localDataSource.getSearchAvailableOfflineFolderContent(OC_FILE.id!!, "test")

    }

    @Test
    fun `getSearchSharedByLinkFolderContent returns a list of OCFile`() {

        every { dao.getSearchSharedByLinkFolderContent(any(), any()) } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getSearchSharedByLinkFolderContent(OC_FILE.id!!, "test")

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) {
            dao.getSearchSharedByLinkFolderContent(DUMMY_FILE_ENTITY.id, "test")
        }
    }

    @Test(expected = Exception::class)
    fun `getSearchSharedByLinkFolderContent returns an exception when getSearchSharedByLinkFolderContent receive an exception`() {

        every { dao.getSearchSharedByLinkFolderContent(any(), any()) } throws Exception()

        localDataSource.getSearchSharedByLinkFolderContent(OC_FILE.id!!, "test")

    }

    @Test
    fun `getFolderContentWithSyncInfoAsFlow returns a flow of list OcFileAndFileSync`() = runTest {

        every { dao.getFolderContentWithSyncInfoAsFlow(any()) } returns flowOf(listOf(ocFileAndFileSync))

        val result = localDataSource.getFolderContentWithSyncInfoAsFlow(OC_FILE.id!!)

        result.collect { result ->
            assertEquals(listOf(OC_FILE_WITH_SYNC_INFO_AND_SPACE), result)
        }

        verify(exactly = 1) {
            dao.getFolderContentWithSyncInfoAsFlow(OC_FILE.id!!)
        }
    }

    @Test(expected = Exception::class)
    fun `getFolderContentWithSyncInfoAsFlow returns an exception when getFolderContentWithSyncInfoAsFlow receive an exception`() {

        every { dao.getFolderContentWithSyncInfoAsFlow(any()) } throws Exception()

        localDataSource.getFolderContentWithSyncInfoAsFlow(OC_FILE.id!!)

    }

    @Test
    fun `getFolderImages returns a list of OCFile`() {
        every { dao.getFolderByMimeType(any(), any()) } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getFolderImages(DUMMY_FILE_ENTITY.id)

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) { dao.getFolderByMimeType(DUMMY_FILE_ENTITY.id, MIME_PREFIX_IMAGE) }
    }

    @Test(expected = Exception::class)
    fun `getFolderImages returns a exception when getFolderImages receive an exception`() {
        every { dao.getFolderByMimeType(any(), any()) } throws Exception()

        localDataSource.getFolderImages(DUMMY_FILE_ENTITY.id)

        verify(exactly = 1) { dao.getFolderByMimeType(DUMMY_FILE_ENTITY.id, MIME_PREFIX_IMAGE) }
    }

    @Test
    fun `getSharedByLinkWithSyncInfoForAccountAsFlow returns a flow of list of OCFileWithSyncInfo`() = runTest {
        every { dao.getFilesWithSyncInfoSharedByLinkAsFlow(any()) } returns flowOf(listOf(ocFileAndFileSync))

        val result = localDataSource.getSharedByLinkWithSyncInfoForAccountAsFlow(DUMMY_FILE_ENTITY.owner)

        result.collect { result ->
            assertEquals(listOf(OC_FILE_WITH_SYNC_INFO_AND_SPACE), result)
        }

        verify(exactly = 1) {
            dao.getFilesWithSyncInfoSharedByLinkAsFlow(DUMMY_FILE_ENTITY.owner)
        }
    }

    @Test(expected = Exception::class)
    fun `getSharedByLinkWithSyncInfoForAccountAsFlow returns a exception when getSharedByLinkWithSyncInfoForAccountAsFlow receive an exception`() {

        every { dao.getFilesWithSyncInfoSharedByLinkAsFlow(any()) } throws Exception()

        localDataSource.getSharedByLinkWithSyncInfoForAccountAsFlow(DUMMY_FILE_ENTITY.owner)

    }

    @Test
    fun `getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow returns a flow of list of OCFileWithSyncInfo`() = runTest {
        every { dao.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(any()) } returns flowOf(listOf(ocFileAndFileSync))

        val result = localDataSource.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(DUMMY_FILE_ENTITY.owner)

        result.collect { result ->
            assertEquals(listOf(OC_FILE_WITH_SYNC_INFO_AND_SPACE), result)
        }

        verify(exactly = 1) {
            dao.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(DUMMY_FILE_ENTITY.owner)
        }
    }

    @Test(expected = Exception::class)
    fun `getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow returns an Exception when getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow receive an exception`() {
        every { dao.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(any()) } throws Exception()

        localDataSource.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(DUMMY_FILE_ENTITY.owner)

    }

    @Test
    fun `getFilesAvailableOfflineFromAccount returns a flow of list of OCFile`() {
        every { dao.getFilesAvailableOfflineFromAccount(any()) } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getFilesAvailableOfflineFromAccount(DUMMY_FILE_ENTITY.owner)

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) {
            dao.getFilesAvailableOfflineFromAccount(DUMMY_FILE_ENTITY.owner)
        }
    }

    @Test(expected = Exception::class)
    fun `getFilesAvailableOfflineFromAccount returns an exception  when getFilesAvailableOfflineFromAccount receive an exception`() {
        every { dao.getFilesAvailableOfflineFromAccount(any()) } throws Exception()

        localDataSource.getFilesAvailableOfflineFromAccount(DUMMY_FILE_ENTITY.owner)

    }

    @Test
    fun `getFilesAvailableOfflineFromEveryAccount returns list of OCFile`() {
        every { dao.getFilesAvailableOfflineFromEveryAccount() } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.getFilesAvailableOfflineFromEveryAccount()

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) {
            dao.getFilesAvailableOfflineFromEveryAccount()
        }
    }

    @Test(expected = Exception::class)
    fun `getFilesAvailableOfflineFromEveryAccount returns an exception when getFilesAvailableOfflineFromEveryAccount receive an exception`() {
        every { dao.getFilesAvailableOfflineFromEveryAccount() } throws Exception()

        localDataSource.getFilesAvailableOfflineFromEveryAccount()

    }

    @Test
    fun `moveFile should move a file from source to target folder when filedao movefile returns ok`() {
        val finalRemotePath = "/final/path"
        val finalStoragePath = "final_storage"
        every { dao.moveFile(any(), any(), finalRemotePath, finalStoragePath) } returns Unit

        localDataSource.moveFile(OC_FILE, OC_FILE_AVAILABLE_OFFLINE, finalRemotePath, finalStoragePath)

        verify(exactly = 1) {
            dao.moveFile(DUMMY_FILE_ENTITY, OC_FILE_AVAILABLE_OFFLINE.toEntity(), finalRemotePath, finalStoragePath)
        }
    }

    @Test(expected = Exception::class)
    fun `moveFile returns an exception when moveFile receive an exception`() {
        val finalRemotePath = "/final/path"
        val finalStoragePath = "final_storage"

        every { dao.moveFile(any(), any(), finalRemotePath, finalStoragePath) } throws Exception()

        localDataSource.moveFile(OC_FILE, OC_FILE_AVAILABLE_OFFLINE, finalRemotePath, finalStoragePath)
    }

    @Test
    fun `saveFilesInFolderAndReturnThem should save a list of OCFile in a folder and return them`() {

        every { dao.insertFilesInFolderAndReturnThem(any(), any()) } returns listOf(DUMMY_FILE_ENTITY)

        val result = localDataSource.saveFilesInFolderAndReturnThem(listOf(OC_FILE), OC_FILE)

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) {
            dao.insertFilesInFolderAndReturnThem(DUMMY_FILE_ENTITY, listOf(DUMMY_FILE_ENTITY))
        }
    }

    @Test(expected = Exception::class)
    fun `saveFilesInFolderAndReturnThem returns exception when saveFilesInFolderAndReturnThem receive an exception`() {

        every { dao.insertFilesInFolderAndReturnThem(any(), any()) } throws Exception()

        localDataSource.saveFilesInFolderAndReturnThem(listOf(OC_FILE), OC_FILE)

    }

    @Test
    fun `saveFile should save a single file and returns unit`() {

        every { dao.upsert(any()) } returns Unit

        localDataSource.saveFile(OC_FILE)

        verify(exactly = 1) {
            dao.upsert(DUMMY_FILE_ENTITY)
        }
    }

    @Test(expected = Exception::class)
    fun `saveFile returns an exception when saveFile receive an exception`() {

        every { dao.upsert(any()) } throws Exception()

        localDataSource.saveFile(OC_FILE)

    }

    @Test
    fun `saveConflict should save conflict status for a file and returns unit`() {

        val etagInConflict = "error"

        every { dao.updateConflictStatusForFile(any(), any()) } returns Unit

        localDataSource.saveConflict(OC_FILE.id!!, etagInConflict)

        verify(exactly = 1) {
            dao.updateConflictStatusForFile(OC_FILE.id!!, etagInConflict)
        }
    }

    @Test(expected = Exception::class)
    fun `saveConflict returns exception when dao receive an exception`() {

        every { dao.updateConflictStatusForFile(any(), any()) } throws Exception()

        localDataSource.saveConflict(OC_FILE.id!!, OC_FILE.etagInConflict!!)

    }

    @Test
    fun `cleanConflict  should remove conflict status for a file and returns unit`() {

        every { dao.updateConflictStatusForFile(any(), null) } returns Unit

        localDataSource.cleanConflict(OC_FILE.id!!)

        verify(exactly = 1) {
            dao.updateConflictStatusForFile(OC_FILE.id!!, null)
        }
    }

    @Test(expected = Exception::class)
    fun `cleanConflict returns exception when dao receive an exception`() {

        every { dao.updateConflictStatusForFile(any(), null) } throws Exception()

        localDataSource.cleanConflict(OC_FILE.id!!)

    }

    @Test
    fun `deleteFile should delete a file by its ID and returns unit`() {
        every { dao.deleteFileById(any()) } returns Unit

        localDataSource.deleteFile(DUMMY_FILE_ENTITY.id)

        verify(exactly = 1) { dao.deleteFileById(DUMMY_FILE_ENTITY.id) }
    }

    @Test(expected = Exception::class)
    fun `deleteFile returns exception when dao receive an exception`() {
        every { dao.deleteFileById(any()) } throws Exception()

        localDataSource.deleteFile(DUMMY_FILE_ENTITY.id)
    }

    @Test
    fun `deleteFilesForAccount should delete files for a specific account and returns unit`() {
        every { dao.deleteFilesForAccount(any()) } returns Unit

        localDataSource.deleteFilesForAccount(DUMMY_FILE_ENTITY.name!!)

        verify(exactly = 1) { dao.deleteFilesForAccount(DUMMY_FILE_ENTITY.name!!) }
    }

    @Test(expected = Exception::class)
    fun `deleteFilesForAccount returns exception when dao receive an exception`() {
        every { dao.deleteFilesForAccount(any()) } throws Exception()

        localDataSource.deleteFilesForAccount(DUMMY_FILE_ENTITY.name!!)
    }

    @Test
    fun `disableThumbnailsForFile should disable thumbnails for a specific file and returns unit`() {
        every { dao.disableThumbnailsForFile(any()) } returns Unit

        localDataSource.disableThumbnailsForFile(DUMMY_FILE_ENTITY.id)

        verify(exactly = 1) { dao.disableThumbnailsForFile(DUMMY_FILE_ENTITY.id) }
    }

    @Test(expected = Exception::class)
    fun `disableThumbnailsForFile returns exception when dao receive an exception`() {
        every { dao.disableThumbnailsForFile(any()) } throws Exception()

        localDataSource.disableThumbnailsForFile(DUMMY_FILE_ENTITY.id)
    }

    @Test
    fun `updateAvailableOfflineStatusForFile should update available offline status for a file and returns unit`() {
        val newAvailableOfflineStatus: AvailableOfflineStatus = mockk(relaxed = true)

        every { dao.updateAvailableOfflineStatusForFile(any(), any()) } returns Unit

        localDataSource.updateAvailableOfflineStatusForFile(OC_FILE, newAvailableOfflineStatus)

        verify(exactly = 1) { dao.updateAvailableOfflineStatusForFile(OC_FILE, newAvailableOfflineStatus.ordinal) }
    }

    @Test(expected = Exception::class)
    fun `updateAvailableOfflineStatusForFile returns exception when dao receive an exception`() {
        val newAvailableOfflineStatus: AvailableOfflineStatus = mockk(relaxed = true)

        every { dao.updateAvailableOfflineStatusForFile(any(), any()) } throws Exception()

        localDataSource.updateAvailableOfflineStatusForFile(OC_FILE, newAvailableOfflineStatus)
    }

    @Test
    fun `saveDownloadWorkerUuid should save the worker UUID for a file and returns unit`() {
        val workerUuid: UUID = mockk(relaxed = true)

        every { dao.updateSyncStatusForFile(any(), any()) } returns Unit

        localDataSource.saveDownloadWorkerUuid(OC_FILE.id!!, workerUuid)

        verify(exactly = 1) { dao.updateSyncStatusForFile(OC_FILE.id!!, workerUuid) }
    }

    @Test(expected = Exception::class)
    fun `saveDownloadWorkerUuid returns exception when dao receive an exception`() {
        val workerUuid: UUID = mockk(relaxed = true)

        every { dao.updateSyncStatusForFile(any(), any()) } throws Exception()

        localDataSource.saveDownloadWorkerUuid(OC_FILE.id!!, workerUuid)
    }

    @Test
    fun `cleanWorkersUuid should clean the worker UUID for a file and returns unit`() {

        every { dao.updateSyncStatusForFile(any(), null) } returns Unit

        localDataSource.cleanWorkersUuid(OC_FILE.id!!)

        verify(exactly = 1) { dao.updateSyncStatusForFile(OC_FILE.id!!, null) }
    }

    @Test(expected = Exception::class)
    fun `cleanWorkersUuid returns an exception whe dao receive an exception`() {

        every { dao.updateSyncStatusForFile(any(), null) } throws Exception()

        localDataSource.cleanWorkersUuid(OC_FILE.id!!)

    }

    companion object {
        private val DUMMY_FILE_ENTITY: OCFileEntity = OC_FILE.toEntity()
    }
}
