/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Christian Schabesberger
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

package com.owncloud.android.data.files.datasources

import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface LocalFileDataSource {
    fun getFileById(fileId: Long): OCFile?
    fun getFileByIdAsFlow(fileId: Long): Flow<OCFile?>
    fun getFileByRemotePath(remotePath: String, owner: String, spaceId: String?): OCFile?
    fun getFileByRemoteId(remoteId: String): OCFile?
    fun getFolderContent(folderId: Long): List<OCFile>
    fun getSearchFolderContent(folderId: Long, search: String): List<OCFile>
    fun getSearchAvailableOfflineFolderContent(folderId: Long, search: String): List<OCFile>
    fun getSearchSharedByLinkFolderContent(folderId: Long, search: String): List<OCFile>
    fun getFolderContentWithSyncInfoAsFlow(folderId: Long): Flow<List<OCFileWithSyncInfo>>
    fun getFolderImages(folderId: Long): List<OCFile>
    fun getSharedByLinkWithSyncInfoForAccountAsFlow(owner: String): Flow<List<OCFileWithSyncInfo>>
    fun getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(owner: String): Flow<List<OCFileWithSyncInfo>>
    fun getFilesAvailableOfflineFromAccount(owner: String): List<OCFile>
    fun getFilesAvailableOfflineFromEveryAccount(): List<OCFile>
    fun getDownloadedFilesForAccount(owner: String): List<OCFile>
    fun getFileWithSyncInfoByIdAsFlow(id: Long): Flow<OCFileWithSyncInfo?>
    fun getFilesWithLastUsageOlderThanGivenTime(milliseconds: Long): List<OCFile>
    fun moveFile(sourceFile: OCFile, targetFolder: OCFile, finalRemotePath: String, finalStoragePath: String)
    fun copyFile(sourceFile: OCFile, targetFolder: OCFile, finalRemotePath: String, remoteId: String, replace: Boolean?)
    fun saveFilesInFolderAndReturnTheFilesThatChanged(listOfFiles: List<OCFile>, folder: OCFile): List<OCFile>
    fun saveFile(file: OCFile)
    fun saveConflict(fileId: Long, eTagInConflict: String)
    fun cleanConflict(fileId: Long)
    fun deleteFile(fileId: Long)
    fun deleteFilesForAccount(accountName: String)
    fun renameFile(fileToRename: OCFile, finalRemotePath: String, finalStoragePath: String)

    fun disableThumbnailsForFile(fileId: Long)
    fun updateAvailableOfflineStatusForFile(ocFile: OCFile, newAvailableOfflineStatus: AvailableOfflineStatus)
    fun updateDownloadedFilesStorageDirectoryInStoragePath(oldDirectory: String, newDirectory: String)
    fun saveUploadWorkerUuid(fileId: Long, workerUuid: UUID)
    fun saveDownloadWorkerUuid(fileId: Long, workerUuid: UUID)
    fun cleanWorkersUuid(fileId: Long)
    fun updateFileWithLastUsage(fileId: Long, lastUsage: Long?)
}
