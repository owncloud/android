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
package com.owncloud.android.data.files.datasources.implementation

import androidx.annotation.VisibleForTesting
import com.owncloud.android.data.files.datasources.LocalFileDataSource
import com.owncloud.android.data.files.db.FileDao
import com.owncloud.android.data.files.db.OCFileEntity
import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus
import com.owncloud.android.domain.files.model.MIME_DIR
import com.owncloud.android.domain.files.model.MIME_PREFIX_IMAGE
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PARENT_ID
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OCLocalFileDataSource(
    private val fileDao: FileDao,
) : LocalFileDataSource {
    override fun copyFile(sourceFile: OCFile, targetFolder: OCFile, finalRemotePath: String, remoteId: String) {
        fileDao.copy(
            sourceFile = sourceFile.toEntity(),
            targetFolder = targetFolder.toEntity(),
            finalRemotePath = finalRemotePath,
            remoteId = remoteId
        )
    }

    override fun getFileById(fileId: Long): OCFile? =
        fileDao.getFileById(fileId)?.toModel()

    override fun getFileByIdAsStream(fileId: Long): Flow<OCFile?> =
        fileDao.getFileByIdAsStream(fileId).map { it?.toModel() }

    override fun getFileByRemotePath(remotePath: String, owner: String): OCFile? {
        fileDao.getFileByOwnerAndRemotePath(owner, remotePath)?.let { return it.toModel() }

        // If root folder do not exists, create and return it.
        if (remotePath == ROOT_PATH) {
            val rootFolder = OCFile(
                parentId = ROOT_PARENT_ID,
                owner = owner,
                remotePath = ROOT_PATH,
                length = 0,
                mimeType = MIME_DIR,
                modificationTimestamp = 0
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

    override fun getFolderContentAsStream(folderId: Long): Flow<List<OCFile>> =
        fileDao.getFolderContentAsStream(folderId = folderId).map { folderContent ->
            folderContent.map { it.toModel() }
        }

    override fun getFolderImages(folderId: Long): List<OCFile> =
        fileDao.getFolderByMimeType(folderId = folderId, mimeType = MIME_PREFIX_IMAGE).map {
            it.toModel()
        }

    override fun getSharedByLinkForAccountAsStream(owner: String): Flow<List<OCFile>> =
        fileDao.getFilesSharedByLink(accountOwner = owner).map { fileList ->
            fileList.map { it.toModel() }
        }

    override fun getFilesAvailableOfflineFromAccountAsStream(owner: String): Flow<List<OCFile>> =
        fileDao.getFilesAvailableOfflineFromAccountAsStream(owner).map { fileList ->
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

    override fun moveFile(sourceFile: OCFile, targetFolder: OCFile, finalRemotePath: String, finalStoragePath: String) =
        fileDao.moveFile(
            sourceFile = sourceFile.toEntity(),
            targetFolder = targetFolder.toEntity(),
            finalRemotePath = finalRemotePath,
            finalStoragePath = sourceFile.storagePath?.let { finalStoragePath }
        )

    override fun saveFilesInFolder(listOfFiles: List<OCFile>, folder: OCFile) {
        // TODO: If it is root, add 0 as parent Id
        fileDao.insertFilesInFolder(
            folder = folder.toEntity(),
            folderContent = listOfFiles.map { it.toEntity() }
        )
    }

    override fun saveFile(file: OCFile) {
        fileDao.insert(file.toEntity())
    }

    override fun removeFile(fileId: Long) {
        fileDao.deleteFileWithId(fileId)
    }

    override fun removeFilesForAccount(accountName: String) {
        fileDao.deleteFilesForAccount(accountName)
    }

    override fun renameFile(fileToRename: OCFile, finalRemotePath: String, finalStoragePath: String) {
        fileDao.moveFile(
            sourceFile = fileToRename.toEntity(),
            targetFolder = fileDao.getFileById(fileToRename.parentId!!)!!,
            finalRemotePath = finalRemotePath,
            finalStoragePath = fileToRename.storagePath?.let { finalStoragePath }
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
                lastSyncDateForProperties = lastSyncDateForProperties,
                modifiedAtLastSyncForData = modifiedAtLastSyncForData,
                etagInConflict = etagInConflict,
                treeEtag = treeEtag
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
                lastSyncDateForProperties = lastSyncDateForProperties,
                modifiedAtLastSyncForData = modifiedAtLastSyncForData,
                etagInConflict = etagInConflict,
                treeEtag = treeEtag,
                name = fileName
            ).apply { this@toEntity.id?.let { modelId -> this.id = modelId } }
    }
}
