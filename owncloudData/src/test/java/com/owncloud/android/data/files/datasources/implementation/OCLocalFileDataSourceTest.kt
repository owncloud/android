/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
 * @author Juan Carlos Garrote Gascón
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

package com.owncloud.android.data.files.datasources.implementation

import com.owncloud.android.data.files.datasources.implementation.OCLocalFileDataSource.Companion.toEntity
import com.owncloud.android.data.files.db.FileDao
import com.owncloud.android.data.files.db.OCFileEntity
import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus
import com.owncloud.android.domain.files.model.MIME_DIR
import com.owncloud.android.domain.files.model.MIME_PREFIX_IMAGE
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PARENT_ID
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FILE_AND_FILE_SYNC
import com.owncloud.android.testutil.OC_FILE_AVAILABLE_OFFLINE
import com.owncloud.android.testutil.OC_FILE_AVAILABLE_OFFLINE_ENTITY
import com.owncloud.android.testutil.OC_FILE_ENTITY
import com.owncloud.android.testutil.OC_FILE_WITH_SYNC_INFO_AND_SPACE
import com.owncloud.android.testutil.OC_FOLDER
import com.owncloud.android.testutil.OC_FOLDER_ENTITY
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

@ExperimentalCoroutinesApi
class OCLocalFileDataSourceTest {

    private lateinit var ocLocalFileDataSource: OCLocalFileDataSource
    private val fileDao = mockk<FileDao>(relaxUnitFun = true)

    private val fileEntitySharedByLink = OC_FILE_ENTITY.copy(sharedByLink = true).apply { this.id = OC_FILE_ENTITY.id }
    private val fileSharedByLink = OC_FILE.copy(sharedByLink = true)
    private val timeInMilliseconds = 3600000L
    @Before
    fun setUp() {
        ocLocalFileDataSource = OCLocalFileDataSource(fileDao)
    }

    @Test
    fun `getFileById returns a OCFile`() {
        every { fileDao.getFileById(OC_FILE_ENTITY.id) } returns OC_FILE_ENTITY

        val result = ocLocalFileDataSource.getFileById(OC_FILE_ENTITY.id)

        assertEquals(OC_FILE, result)

        verify(exactly = 1) { fileDao.getFileById(OC_FILE_ENTITY.id) }
    }

    @Test
    fun `getFileById returns null when DAO returns a null file`() {
        every { fileDao.getFileById(OC_FILE_ENTITY.id) } returns null

        val result = ocLocalFileDataSource.getFileById(OC_FILE_ENTITY.id)

        assertNull(result)

        verify(exactly = 1) { fileDao.getFileById(OC_FILE_ENTITY.id) }
    }

    @Test
    fun `getFileByIdAsFlow returns a Flow with an OCFile`() = runTest {
        every { fileDao.getFileByIdAsFlow(OC_FILE_ENTITY.id) } returns flowOf(OC_FILE_ENTITY)

        val result = ocLocalFileDataSource.getFileByIdAsFlow(OC_FILE_ENTITY.id).first()

        assertEquals(OC_FILE, result)

        verify(exactly = 1) { fileDao.getFileByIdAsFlow(OC_FILE_ENTITY.id) }
    }

    @Test
    fun `getFileByIdAsFlow returns a Flow with null when DAO returns a Flow with null`() = runTest {
        every { fileDao.getFileByIdAsFlow(OC_FILE_ENTITY.id) } returns flowOf(null)

        val result = ocLocalFileDataSource.getFileByIdAsFlow(OC_FILE_ENTITY.id).first()

        assertNull(result)

        verify(exactly = 1) { fileDao.getFileByIdAsFlow(OC_FILE_ENTITY.id) }
    }

    @Test
    fun `getDownloadedFilesForAccount returns a list of OCFile`() {
        every { fileDao.getDownloadedFilesForAccount(OC_ACCOUNT_NAME) } returns listOf(OC_FILE_ENTITY)

        val result = ocLocalFileDataSource.getDownloadedFilesForAccount(OC_ACCOUNT_NAME)

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) { fileDao.getDownloadedFilesForAccount(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getDownloadedFilesForAccount returns an empty list when DAO returns an empty list`() {
        every { fileDao.getDownloadedFilesForAccount(OC_ACCOUNT_NAME) } returns emptyList()

        val result = ocLocalFileDataSource.getDownloadedFilesForAccount(OC_ACCOUNT_NAME)

        assertEquals(emptyList<OCFile>(), result)

        verify(exactly = 1) { fileDao.getDownloadedFilesForAccount(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getFileWithSyncInfoByIdAsFlow returns a Flow with an OCFileWithSyncInfo`() = runTest {
        every { fileDao.getFileWithSyncInfoByIdAsFlow(OC_FILE_ENTITY.id) } returns flowOf(OC_FILE_AND_FILE_SYNC)

        val result = ocLocalFileDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE_ENTITY.id).first()

        assertEquals(OC_FILE_WITH_SYNC_INFO_AND_SPACE, result)

        verify(exactly = 1) { fileDao.getFileWithSyncInfoByIdAsFlow(OC_FILE_ENTITY.id) }
    }

    @Test
    fun `getFileWithSyncInfoByIdAsFlow returns a Flow with null when DAO returns a Flow with null`() = runTest {
        every { fileDao.getFileWithSyncInfoByIdAsFlow(OC_FILE_ENTITY.id) } returns flowOf(null)

        val result = ocLocalFileDataSource.getFileWithSyncInfoByIdAsFlow(OC_FILE_ENTITY.id).first()

        assertNull(result)

        verify(exactly = 1) { fileDao.getFileWithSyncInfoByIdAsFlow(OC_FILE_ENTITY.id) }
    }

    @Test
    fun `getFileByRemotePath returns a OCFile`() {
        every { fileDao.getFileByOwnerAndRemotePath(OC_FILE.owner, OC_FILE.remotePath, OC_FILE.spaceId) } returns OC_FILE_ENTITY

        val result = ocLocalFileDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner, OC_FILE.spaceId)

        assertEquals(OC_FILE, result)

        verify(exactly = 1) { fileDao.getFileByOwnerAndRemotePath(OC_FILE.owner, OC_FILE.remotePath, OC_FILE.spaceId) }
    }

    @Test
    fun `getFileByRemotePath returns null when DAO returns a null file`() {
        every { fileDao.getFileByOwnerAndRemotePath(OC_FILE.owner, OC_FILE.remotePath, OC_FILE.spaceId) } returns null

        val result = ocLocalFileDataSource.getFileByRemotePath(OC_FILE.remotePath, OC_FILE.owner, OC_FILE.spaceId)

        assertNull(result)

        verify(exactly = 1) { fileDao.getFileByOwnerAndRemotePath(OC_FILE.owner, OC_FILE.remotePath, OC_FILE.spaceId) }
    }

    @Test
    fun `getFileByRemotePath returns OCFile of root folder when creating it`() {
        val rootFolder = OCFile(
            id = 0,
            parentId = ROOT_PARENT_ID,
            owner = OC_ACCOUNT_NAME,
            remotePath = ROOT_PATH,
            length = 0,
            mimeType = MIME_DIR,
            modificationTimestamp = 0,
            spaceId = OC_FILE.spaceId,
            permissions = "CK",
            availableOfflineStatus = AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE,
        )

        val rootFolderEntity = OCFileEntity(
            parentId = ROOT_PARENT_ID,
            remotePath = ROOT_PATH,
            owner = OC_ACCOUNT_NAME,
            permissions = "CK",
            remoteId = null,
            creationTimestamp = 0,
            modificationTimestamp = 0,
            etag = "",
            mimeType = MIME_DIR,
            length = 0,
            fileIsDownloading = false,
            modifiedAtLastSyncForData = 0,
            lastSyncDateForData = 0,
            treeEtag = "",
            privateLink = "",

        )

        every { fileDao.getFileByOwnerAndRemotePath(OC_FILE.owner, ROOT_PATH, OC_FILE.spaceId) } returns null
        every { fileDao.mergeRemoteAndLocalFile(rootFolder.toEntity()) } returns 0
        every { fileDao.getFileById(0) } returns rootFolderEntity

        val result = ocLocalFileDataSource.getFileByRemotePath(ROOT_PATH, OC_FILE.owner, OC_FILE.spaceId)

        assertEquals(rootFolder, result)

        verify(exactly = 1) {
            fileDao.getFileByOwnerAndRemotePath(OC_FILE.owner, ROOT_PATH, OC_FILE.spaceId)
            fileDao.mergeRemoteAndLocalFile(rootFolder.toEntity())
            fileDao.getFileById(0)
        }
    }

    @Test
    fun `getFileByRemoteId returns a OCFile`() {
        every { fileDao.getFileByRemoteId(OC_FILE_ENTITY.remoteId!!) } returns OC_FILE_ENTITY

        val result = ocLocalFileDataSource.getFileByRemoteId(OC_FILE_ENTITY.remoteId!!)

        assertEquals(OC_FILE, result)

        verify(exactly = 1) { fileDao.getFileByRemoteId(OC_FILE_ENTITY.remoteId!!) }
    }

    @Test
    fun `getFileByRemoteId returns null when DAO returns a null file`() {
        every { fileDao.getFileByRemoteId(OC_FILE_ENTITY.remoteId!!) } returns null

        val result = ocLocalFileDataSource.getFileByRemoteId(OC_FILE_ENTITY.remoteId!!)

        assertNull(result)

        verify(exactly = 1) { fileDao.getFileByRemoteId(OC_FILE_ENTITY.remoteId!!) }
    }

    @Test
    fun `getFolderContent returns a list of OCFile`() {
        every { fileDao.getFolderContent(OC_FILE_ENTITY.parentId!!) } returns listOf(OC_FILE_ENTITY)

        val result = ocLocalFileDataSource.getFolderContent(OC_FILE_ENTITY.parentId!!)

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) { fileDao.getFolderContent(OC_FILE_ENTITY.parentId!!) }
    }

    @Test
    fun `getFolderContent returns an empty list when DAO returns an empty list`() {
        every { fileDao.getFolderContent(OC_FILE_ENTITY.parentId!!) } returns emptyList()

        val result = ocLocalFileDataSource.getFolderContent(OC_FILE_ENTITY.parentId!!)

        assertEquals(emptyList<OCFile>(), result)

        verify(exactly = 1) { fileDao.getFolderContent(OC_FILE_ENTITY.parentId!!) }
    }

    @Test
    fun `getSearchFolderContent returns a list of OCFile`() {
        every { fileDao.getSearchFolderContent(OC_FILE_ENTITY.parentId!!, "test") } returns listOf(OC_FILE_ENTITY)

        val result = ocLocalFileDataSource.getSearchFolderContent(OC_FILE_ENTITY.parentId!!, "test")

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) { fileDao.getSearchFolderContent(OC_FILE_ENTITY.parentId!!, "test") }
    }

    @Test
    fun `getSearchFolderContent returns an empty list when DAO returns an empty list`() {
        every { fileDao.getSearchFolderContent(OC_FILE_ENTITY.parentId!!, "test") } returns emptyList()

        val result = ocLocalFileDataSource.getSearchFolderContent(OC_FILE_ENTITY.parentId!!, "test")

        assertEquals(emptyList<OCFile>(), result)

        verify(exactly = 1) { fileDao.getSearchFolderContent(OC_FILE_ENTITY.parentId!!, "test") }
    }

    @Test
    fun `getSearchAvailableOfflineFolderContent returns a list of OCFile`() {
        every { fileDao.getSearchAvailableOfflineFolderContent(OC_FILE_AVAILABLE_OFFLINE_ENTITY.parentId!!, "test") } returns listOf(OC_FILE_AVAILABLE_OFFLINE_ENTITY)

        val result = ocLocalFileDataSource.getSearchAvailableOfflineFolderContent(OC_FILE_AVAILABLE_OFFLINE_ENTITY.parentId!!, "test")

        assertEquals(listOf(OC_FILE_AVAILABLE_OFFLINE), result)

        verify(exactly = 1) { fileDao.getSearchAvailableOfflineFolderContent(OC_FILE_AVAILABLE_OFFLINE_ENTITY.parentId!!, "test") }
    }

    @Test
    fun `getSearchAvailableOfflineFolderContent returns an empty list when DAO returns an empty list`() {
        every { fileDao.getSearchAvailableOfflineFolderContent(OC_FILE_ENTITY.parentId!!, "test") } returns emptyList()

        val result = ocLocalFileDataSource.getSearchAvailableOfflineFolderContent(OC_FILE_ENTITY.parentId!!, "test")

        assertEquals(emptyList<OCFile>(), result)

        verify(exactly = 1) { fileDao.getSearchAvailableOfflineFolderContent(OC_FILE_ENTITY.parentId!!, "test") }
    }

    @Test
    fun `getSearchSharedByLinkFolderContent returns a list of OCFile`() {
        every { fileDao.getSearchSharedByLinkFolderContent(fileEntitySharedByLink.parentId!!, "test") } returns listOf(fileEntitySharedByLink)

        val result = ocLocalFileDataSource.getSearchSharedByLinkFolderContent(fileEntitySharedByLink.parentId!!, "test")

        assertEquals(listOf(fileSharedByLink), result)

        verify(exactly = 1) { fileDao.getSearchSharedByLinkFolderContent(fileEntitySharedByLink.parentId!!, "test") }
    }

    @Test
    fun `getSearchSharedByLinkFolderContent returns an empty list when DAO returns an empty list`() {
        every { fileDao.getSearchSharedByLinkFolderContent(OC_FILE_ENTITY.parentId!!, "test") } returns emptyList()

        val result = ocLocalFileDataSource.getSearchSharedByLinkFolderContent(OC_FILE_ENTITY.parentId!!, "test")

        assertEquals(emptyList<OCFile>(), result)

        verify(exactly = 1) { fileDao.getSearchSharedByLinkFolderContent(OC_FILE_ENTITY.parentId!!, "test") }
    }

    @Test
    fun `getFolderContentWithSyncInfoAsFlow returns a Flow with a list of OCFileWithSyncInfo`() = runTest {
        every { fileDao.getFolderContentWithSyncInfoAsFlow(OC_FILE_ENTITY.parentId!!) } returns flowOf(listOf(OC_FILE_AND_FILE_SYNC))

        val result = ocLocalFileDataSource.getFolderContentWithSyncInfoAsFlow(OC_FILE_ENTITY.parentId!!).first()

        assertEquals(listOf(OC_FILE_WITH_SYNC_INFO_AND_SPACE), result)

        verify(exactly = 1) { fileDao.getFolderContentWithSyncInfoAsFlow(OC_FILE_ENTITY.parentId!!) }
    }

    @Test
    fun `getFolderContentWithSyncInfoAsFlow returns a Flow with an empty list when DAO returns a Flow with an empty list`() = runTest {
        every { fileDao.getFolderContentWithSyncInfoAsFlow(OC_FILE_ENTITY.parentId!!) } returns flowOf(emptyList())

        val result = ocLocalFileDataSource.getFolderContentWithSyncInfoAsFlow(OC_FILE_ENTITY.parentId!!).first()

        assertEquals(emptyList<OCFileWithSyncInfo>(), result)

        verify(exactly = 1) { fileDao.getFolderContentWithSyncInfoAsFlow(OC_FILE_ENTITY.parentId!!) }
    }

    @Test
    fun `getFolderImages returns a list of OCFile`() {
        every { fileDao.getFolderByMimeType(OC_FILE_ENTITY.parentId!!, MIME_PREFIX_IMAGE) } returns listOf(OC_FILE_ENTITY)

        val result = ocLocalFileDataSource.getFolderImages(OC_FILE_ENTITY.parentId!!)

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) { fileDao.getFolderByMimeType(OC_FILE_ENTITY.parentId!!, MIME_PREFIX_IMAGE) }
    }

    @Test
    fun `getFolderImages returns an empty list when DAO returns an empty list`() {
        every { fileDao.getFolderByMimeType(OC_FILE_ENTITY.parentId!!, MIME_PREFIX_IMAGE) } returns emptyList()

        val result = ocLocalFileDataSource.getFolderImages(OC_FILE_ENTITY.parentId!!)

        assertEquals(emptyList<OCFile>(), result)

        verify(exactly = 1) { fileDao.getFolderByMimeType(OC_FILE_ENTITY.parentId!!, MIME_PREFIX_IMAGE) }
    }

    @Test
    fun `getSharedByLinkWithSyncInfoForAccountAsFlow returns a Flow with a list of OCFileWithSyncInfo`() = runTest {
        val fileAndFileSyncEntitySharedByLink = OC_FILE_AND_FILE_SYNC.copy(file = OC_FILE_ENTITY.copy(sharedByLink = true).apply { this.id = OC_FILE_ENTITY.id })

        val fileWithSyncInfoSharedByLink = OC_FILE_WITH_SYNC_INFO_AND_SPACE.copy(file = OC_FILE.copy(sharedByLink = true))

        every { fileDao.getFilesWithSyncInfoSharedByLinkAsFlow(OC_ACCOUNT_NAME) } returns flowOf(listOf(fileAndFileSyncEntitySharedByLink))

        val result = ocLocalFileDataSource.getSharedByLinkWithSyncInfoForAccountAsFlow(OC_ACCOUNT_NAME).first()

        assertEquals(listOf(fileWithSyncInfoSharedByLink), result)

        verify(exactly = 1) { fileDao.getFilesWithSyncInfoSharedByLinkAsFlow(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getSharedByLinkWithSyncInfoForAccountAsFlow returns a Flow with an empty list when DAO returns a Flow with an empty list`() = runTest {
        every { fileDao.getFilesWithSyncInfoSharedByLinkAsFlow(OC_ACCOUNT_NAME) } returns flowOf(emptyList())

        val result = ocLocalFileDataSource.getSharedByLinkWithSyncInfoForAccountAsFlow(OC_ACCOUNT_NAME).first()

        assertEquals(emptyList<OCFileWithSyncInfo>(), result)

        verify(exactly = 1) { fileDao.getFilesWithSyncInfoSharedByLinkAsFlow(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow returns a Flow with a list of OCFileWithSyncInfo`() = runTest {
        val fileAndFileSyncEntityAvailableOffline = OC_FILE_AND_FILE_SYNC.copy(
            file = OC_FILE_ENTITY.copy(availableOfflineStatus = AvailableOfflineStatus.AVAILABLE_OFFLINE.ordinal).apply { this.id = OC_FILE_ENTITY.id }
        )

        val fileWithSyncInfoAvailableOffline = OC_FILE_WITH_SYNC_INFO_AND_SPACE.copy(
            file = OC_FILE.copy(availableOfflineStatus = AvailableOfflineStatus.AVAILABLE_OFFLINE)
        )

        every { fileDao.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(OC_ACCOUNT_NAME) } returns flowOf(listOf(fileAndFileSyncEntityAvailableOffline))

        val result = ocLocalFileDataSource.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(OC_ACCOUNT_NAME).first()

        assertEquals(listOf(fileWithSyncInfoAvailableOffline), result)

        verify(exactly = 1) { fileDao.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow returns a Flow with an empty list when DAO returns a Flow with an empty list`() = runTest {
        every { fileDao.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(OC_ACCOUNT_NAME) } returns flowOf(emptyList())

        val result = ocLocalFileDataSource.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(OC_ACCOUNT_NAME).first()

        assertEquals(emptyList<OCFileWithSyncInfo>(), result)

        verify(exactly = 1) { fileDao.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getFilesAvailableOfflineFromAccount returns a list of OCFile`() {
        every { fileDao.getFilesAvailableOfflineFromAccount(OC_ACCOUNT_NAME) } returns listOf(OC_FILE_AVAILABLE_OFFLINE_ENTITY)

        val result = ocLocalFileDataSource.getFilesAvailableOfflineFromAccount(OC_ACCOUNT_NAME)

        assertEquals(listOf(OC_FILE_AVAILABLE_OFFLINE), result)

        verify(exactly = 1) { fileDao.getFilesAvailableOfflineFromAccount(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getFilesAvailableOfflineFromAccount returns an empty list when DAO returns an empty list`() {
        every { fileDao.getFilesAvailableOfflineFromAccount(OC_ACCOUNT_NAME) } returns emptyList()

        val result = ocLocalFileDataSource.getFilesAvailableOfflineFromAccount(OC_ACCOUNT_NAME)

        assertEquals(emptyList<OCFile>(), result)

        verify(exactly = 1) { fileDao.getFilesAvailableOfflineFromAccount(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `getFilesAvailableOfflineFromEveryAccount returns a list of OCFile`() {
        every { fileDao.getFilesAvailableOfflineFromEveryAccount() } returns listOf(OC_FILE_AVAILABLE_OFFLINE_ENTITY)

        val result = ocLocalFileDataSource.getFilesAvailableOfflineFromEveryAccount()

        assertEquals(listOf(OC_FILE_AVAILABLE_OFFLINE), result)

        verify(exactly = 1) { fileDao.getFilesAvailableOfflineFromEveryAccount() }
    }

    @Test
    fun `getFilesAvailableOfflineFromEveryAccount returns an empty list when DAO returns an empty list`() {
        every { fileDao.getFilesAvailableOfflineFromEveryAccount() } returns emptyList()

        val result = ocLocalFileDataSource.getFilesAvailableOfflineFromEveryAccount()

        assertEquals(emptyList<OCFile>(), result)

        verify(exactly = 1) { fileDao.getFilesAvailableOfflineFromEveryAccount() }
    }

    @Test
    fun `getFilesLastUsageIsOlderThanGivenTime returns a list of OCFile`() {
        every { fileDao.getFilesWithLastUsageOlderThanGivenTime(timeInMilliseconds) } returns listOf(OC_FILE_ENTITY)

        val result = ocLocalFileDataSource.getFilesWithLastUsageOlderThanGivenTime(timeInMilliseconds)

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) { fileDao.getFilesWithLastUsageOlderThanGivenTime(timeInMilliseconds) }
    }

    @Test
    fun `getFilesLastUsageIsOlderThanGivenTime returns an empty list when DAO returns an empty list`() {
        every { fileDao.getFilesWithLastUsageOlderThanGivenTime(timeInMilliseconds) } returns emptyList()

        val result = ocLocalFileDataSource.getFilesWithLastUsageOlderThanGivenTime(timeInMilliseconds)

        assertEquals(emptyList<OCFile>(), result)

        verify(exactly = 1) { fileDao.getFilesWithLastUsageOlderThanGivenTime(timeInMilliseconds) }
    }

    @Test
    fun `moveFile moves a file correctly`() {
        val finalRemotePath = "/final/remote/path"
        val finalStoragePath = "/final/storage/path"

        ocLocalFileDataSource.moveFile(OC_FILE, OC_FOLDER, finalRemotePath, finalStoragePath)

        verify(exactly = 1) { fileDao.moveFile(OC_FILE_ENTITY, OC_FOLDER_ENTITY, finalRemotePath, finalStoragePath) }
    }

    @Test
    fun `copyFile copies a file correctly`() {
        val finalRemotePath = "/final/remote/path"
        val remoteId = "testRemoteId"

        ocLocalFileDataSource.copyFile(OC_FILE, OC_FOLDER, finalRemotePath, remoteId, false)

        verify(exactly = 1) { fileDao.copy(OC_FILE_ENTITY, OC_FOLDER_ENTITY, finalRemotePath, remoteId, false) }
    }

    @Test
    fun `saveFilesInFolderAndReturnTheFilesThatChanged saves a list of OCFile and returns only the changed files`() {
        every { fileDao.insertFilesInFolderAndReturnTheFilesThatChanged(OC_FOLDER_ENTITY, listOf(OC_FILE_ENTITY)) } returns listOf(OC_FILE_ENTITY)

        val result = ocLocalFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(listOf(OC_FILE), OC_FOLDER)

        assertEquals(listOf(OC_FILE), result)

        verify(exactly = 1) { fileDao.insertFilesInFolderAndReturnTheFilesThatChanged(OC_FOLDER_ENTITY, listOf(OC_FILE_ENTITY)) }
    }

    @Test
    fun `saveFilesInFolderAndReturnTheFilesThatChanged returns an empty list when DAO returns an empty list`() {
        every { fileDao.insertFilesInFolderAndReturnTheFilesThatChanged(OC_FOLDER_ENTITY, emptyList()) } returns emptyList()

        val result = ocLocalFileDataSource.saveFilesInFolderAndReturnTheFilesThatChanged(emptyList(), OC_FOLDER)

        assertEquals(emptyList<OCFile>(), result)

        verify(exactly = 1) { fileDao.insertFilesInFolderAndReturnTheFilesThatChanged(OC_FOLDER_ENTITY, emptyList()) }
    }

    @Test
    fun `saveFile saves a file correctly`() {
        ocLocalFileDataSource.saveFile(OC_FILE)

        verify(exactly = 1) { fileDao.upsert(OC_FILE_ENTITY) }
    }

    @Test
    fun `saveConflict saves the etagInConflict related to a file correctly`() {
        val etagInConflict = "error"

        ocLocalFileDataSource.saveConflict(OC_FILE_ENTITY.id, etagInConflict)

        verify(exactly = 1) { fileDao.updateConflictStatusForFile(OC_FILE_ENTITY.id, etagInConflict) }
    }

    @Test
    fun `cleanConflict saves a null etagInConflict related to a file correctly`() {
        ocLocalFileDataSource.cleanConflict(OC_FILE_ENTITY.id)

        verify(exactly = 1) { fileDao.updateConflictStatusForFile(OC_FILE_ENTITY.id, null) }
    }

    @Test
    fun `deleteFile removes a file correctly`() {
        ocLocalFileDataSource.deleteFile(OC_FILE_ENTITY.id)

        verify(exactly = 1) { fileDao.deleteFileById(OC_FILE_ENTITY.id) }
    }

    @Test
    fun `deleteFilesForAccount removes files for an account correctly`() {
        ocLocalFileDataSource.deleteFilesForAccount(OC_ACCOUNT_NAME)

        verify(exactly = 1) { fileDao.deleteFilesForAccount(OC_ACCOUNT_NAME) }
    }

    @Test
    fun `renameFile performs a move in the file to change its name`() {
        every { fileDao.getFileById(OC_FILE.parentId!!) } returns OC_FOLDER_ENTITY

        val finalRemotePath = "/final/remote/path"
        val finalStoragePath = "/final/storage/path"

        ocLocalFileDataSource.renameFile(OC_FILE, finalRemotePath, finalStoragePath)

        verify(exactly = 1) {
            fileDao.getFileById(OC_FILE.parentId!!)
            fileDao.moveFile(OC_FILE_ENTITY, OC_FOLDER_ENTITY, finalRemotePath, finalStoragePath)
        }
    }

    @Test
    fun `disableThumbnailsForFile disables thumbnails for a file correctly`() {
        ocLocalFileDataSource.disableThumbnailsForFile(OC_FILE_ENTITY.id)

        verify(exactly = 1) { fileDao.disableThumbnailsForFile(OC_FILE_ENTITY.id) }
    }

    @Test
    fun `updateAvailableOfflineStatusForFile updates available offline status for a file correctly`() {
        ocLocalFileDataSource.updateAvailableOfflineStatusForFile(OC_FILE, AvailableOfflineStatus.AVAILABLE_OFFLINE)

        verify(exactly = 1) { fileDao.updateAvailableOfflineStatusForFile(OC_FILE, AvailableOfflineStatus.AVAILABLE_OFFLINE.ordinal) }
    }

    @Test
    fun `updateDownloadedFilesStorageDirectoryInStoragePath updates storage path for downloaded files correctly`() {
        val oldDirectory = "/old/directory"
        val newDirectory = "/new/directory"

        ocLocalFileDataSource.updateDownloadedFilesStorageDirectoryInStoragePath(oldDirectory, newDirectory)

        verify(exactly = 1) { fileDao.updateDownloadedFilesStorageDirectoryInStoragePath(oldDirectory, newDirectory) }
    }

    @Test
    fun `updateFileWithLastUsage updates last usage for a file correctly`() {
        val lastUsage = 12345L

        ocLocalFileDataSource.updateFileWithLastUsage(OC_FILE_ENTITY.id, lastUsage)

        verify(exactly = 1) { fileDao.updateFileWithLastUsage(OC_FILE_ENTITY.id, lastUsage) }
    }

    @Test
    fun `saveDownloadWorkerUuid saves the worker UUID for a file correctly`() {
        val workerUuid = UUID.randomUUID()

        ocLocalFileDataSource.saveDownloadWorkerUuid(OC_FILE_ENTITY.id, workerUuid)

        verify(exactly = 1) { fileDao.updateSyncStatusForFile(OC_FILE_ENTITY.id, workerUuid) }
    }

    @Test
    fun `cleanWorkersUuid saves a null worker UUID for a file correctly`() {
        ocLocalFileDataSource.cleanWorkersUuid(OC_FILE_ENTITY.id)

        verify(exactly = 1) { fileDao.updateSyncStatusForFile(OC_FILE_ENTITY.id, null) }
    }
}
