/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Christian Schabesberger
 * @author Juan Carlos Garrote Gascón
 * @author Manuel Plazas Palacio
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

package com.owncloud.android.domain.files

import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface FileRepository {
    fun createFolder(remotePath: String, parentFolder: OCFile)

    // Returns files in conflict
    fun copyFile(listOfFilesToCopy: List<OCFile>, targetFolder: OCFile, replace: List<Boolean?> = emptyList(), isUserLogged: Boolean): List<OCFile>
    fun getFileById(fileId: Long): OCFile?
    fun getFileByIdAsFlow(fileId: Long): Flow<OCFile?>
    fun getFileWithSyncInfoByIdAsFlow(fileId: Long): Flow<OCFileWithSyncInfo?>
    fun getFileByRemotePath(remotePath: String, owner: String, spaceId: String? = null): OCFile?
    fun getFileFromRemoteId(fileId: String, accountName: String): OCFile?
    fun getPersonalRootFolderForAccount(owner: String): OCFile
    fun getSharesRootFolderForAccount(owner: String): OCFile?
    fun getSearchFolderContent(fileListOption: FileListOption, folderId: Long, search: String): List<OCFile>
    fun getFolderContent(folderId: Long): List<OCFile>
    fun getFolderContentWithSyncInfoAsFlow(folderId: Long): Flow<List<OCFileWithSyncInfo>>
    fun getFolderImages(folderId: Long): List<OCFile>
    fun getSharedByLinkWithSyncInfoForAccountAsFlow(owner: String): Flow<List<OCFileWithSyncInfo>>
    fun getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(owner: String): Flow<List<OCFileWithSyncInfo>>
    fun getFilesAvailableOfflineFromAccount(owner: String): List<OCFile>
    fun getFilesAvailableOfflineFromEveryAccount(): List<OCFile>
    fun getDownloadedFilesForAccount(owner: String): List<OCFile>
    fun getFilesWithLastUsageOlderThanGivenTime(milliseconds: Long): List<OCFile>

    // Returns files in conflict
    fun moveFile(listOfFilesToMove: List<OCFile>, targetFolder: OCFile, replace: List<Boolean?> = emptyList(), isUserLogged: Boolean): List<OCFile>
    fun readFile(remotePath: String, accountName: String, spaceId: String? = null): OCFile
    fun refreshFolder(
        remotePath: String,
        accountName: String,
        spaceId: String? = null,
        isActionSetFolderAvailableOfflineOrSynchronize: Boolean = false
    ): List<OCFile>
    fun deleteFiles(listOfFilesToDelete: List<OCFile>, removeOnlyLocalCopy: Boolean)
    fun renameFile(ocFile: OCFile, newName: String)
    fun saveFile(file: OCFile)
    fun saveConflict(fileId: Long, eTagInConflict: String)
    fun cleanConflict(fileId: Long)
    fun saveUploadWorkerUuid(fileId: Long, workerUuid: UUID)
    fun saveDownloadWorkerUuid(fileId: Long, workerUuid: UUID)
    fun cleanWorkersUuid(fileId: Long)

    fun disableThumbnailsForFile(fileId: Long)
    fun updateFileWithNewAvailableOfflineStatus(ocFile: OCFile, newAvailableOfflineStatus: AvailableOfflineStatus)
    fun updateDownloadedFilesStorageDirectoryInStoragePath(oldDirectory: String, newDirectory: String)
    fun updateFileWithLastUsage(fileId: Long, lastUsage: Long?)

}
