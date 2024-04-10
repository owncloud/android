/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

import androidx.annotation.VisibleForTesting
import com.owncloud.android.data.files.datasources.LocalFileDataSource
import com.owncloud.android.data.files.db.FileDao
import com.owncloud.android.data.files.db.OCFileAndFileSync
import com.owncloud.android.data.files.db.OCFileEntity
import com.owncloud.android.data.spaces.datasources.implementation.OCLocalSpacesDataSource.Companion.toModel
import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus
import com.owncloud.android.domain.files.model.MIME_DIR
import com.owncloud.android.domain.files.model.MIME_PREFIX_IMAGE
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PARENT_ID
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class OCLocalFileDataSource(
    private val fileDao: FileDao,
) : LocalFileDataSource {
    override fun getFileById(fileId: Long): OCFile? =
        fileDao.getFileById(fileId)?.toModel()

    override fun getFileByIdAsFlow(fileId: Long): Flow<OCFile?> =
        fileDao.getFileByIdAsFlow(fileId).map { it?.toModel() }

    override fun getFileWithSyncInfoByIdAsFlow(id: Long): Flow<OCFileWithSyncInfo?> =
        fileDao.getFileWithSyncInfoByIdAsFlow(id).map { it?.toModel() }

    override fun getFileByRemotePath(remotePath: String, owner: String, spaceId: String?): OCFile? {
        fileDao.getFileByOwnerAndRemotePath(owner, remotePath, spaceId)?.let { return it.toModel() }

        // If root folder do not exists, create and return it.
        if (remotePath == ROOT_PATH) {
            val rootFolder = OCFile(
                parentId = ROOT_PARENT_ID,
                owner = owner,
                remotePath = ROOT_PATH,
                length = 0,
                mimeType = MIME_DIR,
                modificationTimestamp = 0,
                spaceId = spaceId,
                permissions = "CK",
            )
            fileDao.mergeRemoteAndLocalFile(rootFolder.toEntity()).also { return getFileById(it) }
        }

        return null
    }

    override fun getFileByRemoteId(remoteId: String): OCFile? =
        fileDao.getFileByRemoteId(remoteId)?.toModel()

    override fun getFolderContent(folderId: Long): List<OCFile> =
        fileDao.getFolderContent(folderId = folderId).map {
            it.toModel()
        }

    override fun getSearchFolderContent(folderId: Long, search: String): List<OCFile> =
        fileDao.getSearchFolderContent(folderId = folderId, search = search).map {
            it.toModel()
        }

    override fun getSearchAvailableOfflineFolderContent(folderId: Long, search: String): List<OCFile> =
        fileDao.getSearchAvailableOfflineFolderContent(folderId = folderId, search = search).map {
            it.toModel()
        }

    override fun getSearchSharedByLinkFolderContent(folderId: Long, search: String): List<OCFile> =
        fileDao.getSearchSharedByLinkFolderContent(folderId = folderId, search = search).map {
            it.toModel()
        }

    override fun getFolderContentWithSyncInfoAsFlow(folderId: Long): Flow<List<OCFileWithSyncInfo>> =
        fileDao.getFolderContentWithSyncInfoAsFlow(folderId = folderId).map { folderContent ->
            folderContent.map { it.toModel() }
        }

    override fun getFolderImages(folderId: Long): List<OCFile> =
        fileDao.getFolderByMimeType(folderId = folderId, mimeType = MIME_PREFIX_IMAGE).map {
            it.toModel()
        }

    override fun getSharedByLinkWithSyncInfoForAccountAsFlow(owner: String): Flow<List<OCFileWithSyncInfo>> =
        fileDao.getFilesWithSyncInfoSharedByLinkAsFlow(accountOwner = owner).map { fileList ->
            fileList.map { it.toModel() }
        }

    override fun getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(owner: String): Flow<List<OCFileWithSyncInfo>> =
        fileDao.getFilesWithSyncInfoAvailableOfflineFromAccountAsFlow(owner).map { fileList ->
            fileList.map { it.toModel() }
        }

    override fun getFilesAvailableOfflineFromAccount(owner: String): List<OCFile> =
        fileDao.getFilesAvailableOfflineFromAccount(accountOwner = owner).map {
            it.toModel()
        }

    override fun getFilesAvailableOfflineFromEveryAccount(): List<OCFile> =
        fileDao.getFilesAvailableOfflineFromEveryAccount().map {
            it.toModel()
        }

    override fun getDownloadedFilesForAccount(owner: String): List<OCFile> =
        fileDao.getDownloadedFilesForAccount(accountOwner = owner).map {
            it.toModel()
        }

    override fun getFilesWithLastUsageOlderThanGivenTime(milliseconds: Long): List<OCFile> =
        fileDao.getFilesWithLastUsageOlderThanGivenTime(milliseconds).map {
            it.toModel()
        }

    override fun moveFile(sourceFile: OCFile, targetFolder: OCFile, finalRemotePath: String, finalStoragePath: String) =
        fileDao.moveFile(
            sourceFile = sourceFile.toEntity(),
            targetFolder = targetFolder.toEntity(),
            finalRemotePath = finalRemotePath,
            finalStoragePath = finalStoragePath
        )

    override fun copyFile(sourceFile: OCFile, targetFolder: OCFile, finalRemotePath: String, remoteId: String, replace: Boolean?) {
        fileDao.copy(
            sourceFile = sourceFile.toEntity(),
            targetFolder = targetFolder.toEntity(),
            finalRemotePath = finalRemotePath,
            remoteId = remoteId,
            replace = replace,
        )
    }

    override fun saveFilesInFolderAndReturnTheFilesThatChanged(listOfFiles: List<OCFile>, folder: OCFile): List<OCFile> {
        // TODO: If it is root, add 0 as parent Id
        val folderContent = fileDao.insertFilesInFolderAndReturnTheFilesThatChanged(
            folder = folder.toEntity(),
            folderContent = listOfFiles.map { it.toEntity() }
        )
        return folderContent.map { it.toModel() }
    }

    override fun saveFile(file: OCFile) {
        fileDao.upsert(file.toEntity())
    }

    override fun saveConflict(fileId: Long, eTagInConflict: String) {
        fileDao.updateConflictStatusForFile(fileId, eTagInConflict)
    }

    override fun cleanConflict(fileId: Long) {
        fileDao.updateConflictStatusForFile(fileId, null)
    }

    override fun deleteFile(fileId: Long) {
        fileDao.deleteFileById(fileId)
    }

    override fun deleteFilesForAccount(accountName: String) {
        fileDao.deleteFilesForAccount(accountName)
    }

    override fun renameFile(fileToRename: OCFile, finalRemotePath: String, finalStoragePath: String) {
        fileDao.moveFile(
            sourceFile = fileToRename.toEntity(),
            targetFolder = fileDao.getFileById(fileToRename.parentId!!)!!,
            finalRemotePath = finalRemotePath,
            finalStoragePath = finalStoragePath
        )
    }

    override fun disableThumbnailsForFile(fileId: Long) {
        fileDao.disableThumbnailsForFile(fileId)
    }

    override fun updateAvailableOfflineStatusForFile(ocFile: OCFile, newAvailableOfflineStatus: AvailableOfflineStatus) {
        fileDao.updateAvailableOfflineStatusForFile(ocFile, newAvailableOfflineStatus.ordinal)
    }

    override fun updateDownloadedFilesStorageDirectoryInStoragePath(oldDirectory: String, newDirectory: String) {
        fileDao.updateDownloadedFilesStorageDirectoryInStoragePath(oldDirectory, newDirectory)
    }

    override fun updateFileWithLastUsage(fileId: Long, lastUsage: Long?) {
        fileDao.updateFileWithLastUsage(fileId, lastUsage)
    }

    override fun saveUploadWorkerUuid(fileId: Long, workerUuid: UUID) {
        TODO("Not yet implemented")
    }

    override fun saveDownloadWorkerUuid(fileId: Long, workerUuid: UUID) {
        fileDao.updateSyncStatusForFile(fileId, workerUuid)
    }

    override fun cleanWorkersUuid(fileId: Long) {
        fileDao.updateSyncStatusForFile(fileId, null)
    }

    companion object {
        @VisibleForTesting
        fun OCFileEntity.toModel(): OCFile =
            OCFile(
                id = id,
                parentId = parentId,
                remotePath = remotePath,
                owner = owner,
                permissions = permissions,
                remoteId = remoteId,
                privateLink = privateLink,
                creationTimestamp = creationTimestamp,
                modificationTimestamp = modificationTimestamp,
                etag = etag,
                mimeType = mimeType,
                length = length,
                sharedByLink = sharedByLink,
                sharedWithSharee = sharedWithSharee,
                storagePath = storagePath,
                availableOfflineStatus = AvailableOfflineStatus.fromValue(availableOfflineStatus),
                needsToUpdateThumbnail = needsToUpdateThumbnail,
                fileIsDownloading = fileIsDownloading,
                lastSyncDateForData = lastSyncDateForData,
                lastUsage = lastUsage,
                modifiedAtLastSyncForData = modifiedAtLastSyncForData,
                etagInConflict = etagInConflict,
                treeEtag = treeEtag,
                spaceId = spaceId,
            )

        @VisibleForTesting
        fun OCFile.toEntity(): OCFileEntity =
            OCFileEntity(
                parentId = parentId,
                remotePath = remotePath,
                owner = owner,
                permissions = permissions,
                remoteId = remoteId,
                privateLink = privateLink,
                creationTimestamp = creationTimestamp,
                modificationTimestamp = modificationTimestamp,
                etag = etag,
                mimeType = mimeType,
                length = length,
                sharedByLink = sharedByLink,
                sharedWithSharee = sharedWithSharee,
                storagePath = storagePath,
                availableOfflineStatus = availableOfflineStatus?.ordinal ?: AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE.ordinal,
                needsToUpdateThumbnail = needsToUpdateThumbnail,
                fileIsDownloading = fileIsDownloading,
                lastSyncDateForData = lastSyncDateForData,
                lastUsage = lastUsage,
                modifiedAtLastSyncForData = modifiedAtLastSyncForData,
                etagInConflict = etagInConflict,
                treeEtag = treeEtag,
                name = fileName,
                spaceId = spaceId,
            ).apply { this@toEntity.id?.let { modelId -> this.id = modelId } }
    }

    @VisibleForTesting
    fun OCFileAndFileSync.toModel(): OCFileWithSyncInfo =
        OCFileWithSyncInfo(
            file = file.toModel(),
            uploadWorkerUuid = fileSync?.uploadWorkerUuid,
            downloadWorkerUuid = fileSync?.downloadWorkerUuid,
            isSynchronizing = fileSync?.isSynchronizing == true,
            space = space?.toModel(),
        )
}
