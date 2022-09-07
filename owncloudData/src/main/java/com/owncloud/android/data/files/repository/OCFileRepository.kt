/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Christian Schabesberger
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

package com.owncloud.android.data.files.repository

import com.owncloud.android.data.files.datasources.LocalFileDataSource
import com.owncloud.android.data.files.datasources.RemoteFileDataSource
import com.owncloud.android.data.storage.LocalStorageProvider
import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus
import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT
import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
import com.owncloud.android.domain.exceptions.ConflictException
import com.owncloud.android.domain.exceptions.FileAlreadyExistsException
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.MIME_DIR
import com.owncloud.android.domain.files.model.OCFile
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.io.File

class OCFileRepository(
    private val localFileDataSource: LocalFileDataSource,
    private val remoteFileDataSource: RemoteFileDataSource,
    private val localStorageProvider: LocalStorageProvider
) : FileRepository {

    override fun createFolder(
        remotePath: String,
        parentFolder: OCFile
    ) {
        remoteFileDataSource.createFolder(
            remotePath = remotePath,
            createFullPath = false,
            isChunksFolder = false
        ).also {
            localFileDataSource.saveFilesInFolder(
                folder = parentFolder,
                listOfFiles = listOf(
                    OCFile(
                        remotePath = remotePath,
                        owner = parentFolder.owner,
                        modificationTimestamp = System.currentTimeMillis(),
                        length = 0,
                        mimeType = MIME_DIR
                    )
                )
            )
        }
    }

    override fun copyFile(listOfFilesToCopy: List<OCFile>, targetFolder: OCFile) {
        listOfFilesToCopy.forEach { ocFile ->

            // 1. Get the final remote path for this file.
            val expectedRemotePath: String = targetFolder.remotePath + ocFile.fileName
            val finalRemotePath: String = remoteFileDataSource.getAvailableRemotePath(expectedRemotePath).let {
                if (ocFile.isFolder) it.plus(File.separator) else it
            }

            // 2. Try to copy files in server
            val remoteId = try {
                remoteFileDataSource.copyFile(
                    sourceRemotePath = ocFile.remotePath,
                    targetRemotePath = finalRemotePath
                )
            } catch (targetNodeDoesNotExist: ConflictException) {
                // Target node does not exist anymore. Remove target folder from database and local storage and return
                removeLocalFolderRecursively(ocFile = targetFolder, onlyFromLocalStorage = false)
                return@copyFile
            } catch (sourceFileDoesNotExist: FileNotFoundException) {
                // Source file does not exist anymore. Remove file from database and local storage and continue
                if (ocFile.isFolder) {
                    removeLocalFolderRecursively(ocFile = ocFile, onlyFromLocalStorage = false)
                } else {
                    removeLocalFile(
                        ocFile = ocFile,
                        onlyFromLocalStorage = false
                    )
                }
                return@forEach
            }

            // 3. Update database with latest changes
            localFileDataSource.copyFile(
                sourceFile = ocFile,
                targetFolder = targetFolder,
                finalRemotePath = finalRemotePath,
                remoteId = remoteId
            )
        }
    }

    override fun getFileById(fileId: Long): OCFile? =
        localFileDataSource.getFileById(fileId)

    override fun getFileByRemotePath(remotePath: String, owner: String): OCFile? =
        localFileDataSource.getFileByRemotePath(remotePath, owner)

    override fun getSearchFolderContent(fileListOption: FileListOption, folderId: Long, search: String): List<OCFile> =
        when (fileListOption) {
            FileListOption.ALL_FILES -> localFileDataSource.getSearchFolderContent(folderId, search)
            FileListOption.AV_OFFLINE -> localFileDataSource.getSearchAvailableOfflineFolderContent(folderId, search)
            FileListOption.SHARED_BY_LINK -> localFileDataSource.getSearchSharedByLinkFolderContent(folderId, search)
        }

    override fun getFolderContent(folderId: Long): List<OCFile> =
        localFileDataSource.getFolderContent(folderId)

    override fun getFolderContentAsStream(folderId: Long): Flow<List<OCFile>> =
        localFileDataSource.getFolderContentAsStream(folderId)

    override fun getFolderImages(folderId: Long): List<OCFile> =
        localFileDataSource.getFolderImages(folderId)

    override fun getSharedByLinkForAccountAsStream(owner: String): Flow<List<OCFile>> =
        localFileDataSource.getSharedByLinkForAccountAsStream(owner)

    override fun getFilesAvailableOfflineFromAccountAsStream(owner: String): Flow<List<OCFile>> =
        localFileDataSource.getFilesAvailableOfflineFromAccountAsStream(owner)

    override fun getFilesAvailableOfflineFromAccount(owner: String): List<OCFile> =
        localFileDataSource.getFilesAvailableOfflineFromAccount(owner)

    override fun getFilesAvailableOfflineFromEveryAccount(): List<OCFile> =
        localFileDataSource.getFilesAvailableOfflineFromEveryAccount()

    override fun moveFile(listOfFilesToMove: List<OCFile>, targetFile: OCFile) {
        listOfFilesToMove.forEach { ocFile ->

            // 1. Get the final remote path for this file.
            val expectedRemotePath: String = targetFile.remotePath + ocFile.fileName
            val finalRemotePath: String = remoteFileDataSource.getAvailableRemotePath(expectedRemotePath).let {
                if (ocFile.isFolder) it.plus(File.separator) else it
            }
            val finalStoragePath: String = localStorageProvider.getDefaultSavePathFor(targetFile.owner, finalRemotePath)

            // 2. Try to move files in server
            try {
                remoteFileDataSource.moveFile(
                    sourceRemotePath = ocFile.remotePath,
                    targetRemotePath = finalRemotePath
                )
            } catch (targetNodeDoesNotExist: ConflictException) {
                // Target node does not exist anymore. Remove target folder from database and local storage and return
                removeLocalFolderRecursively(ocFile = targetFile, onlyFromLocalStorage = false)
                return@moveFile
            } catch (sourceFileDoesNotExist: FileNotFoundException) {
                // Source file does not exist anymore. Remove file from database and local storage and continue
                if (ocFile.isFolder) {
                    removeLocalFolderRecursively(ocFile = ocFile, onlyFromLocalStorage = false)
                } else {
                    removeLocalFile(
                        ocFile = ocFile,
                        onlyFromLocalStorage = false
                    )
                }
                return@forEach
            }

            // 3. Update database with latest changes
            localFileDataSource.moveFile(
                sourceFile = ocFile,
                targetFolder = targetFile,
                finalRemotePath = finalRemotePath,
                finalStoragePath = finalStoragePath
            )

            // 4. Update local storage
            localStorageProvider.moveLocalFile(ocFile, finalStoragePath)
        }
    }

    override fun readFile(remotePath: String): OCFile {
        return remoteFileDataSource.readFile(remotePath)
    }

    override fun refreshFolder(remotePath: String): List<OCFile> {
        val currentSyncTime = System.currentTimeMillis()

        // Retrieve remote folder data
        val fetchFolderResult = remoteFileDataSource.refreshFolder(remotePath)
        val remoteFolder = fetchFolderResult.first()
        val remoteFolderContent = fetchFolderResult.drop(1)

        // Final content for this folder, we will update the folder content all together
        val folderContentUpdated = mutableListOf<OCFile>()

        // Check if the folder already exists in database.
        val localFolderByRemotePath: OCFile? =
            localFileDataSource.getFileByRemotePath(remotePath = remoteFolder.remotePath, owner = remoteFolder.owner)

        // If folder doesn't exists in database, insert everything. Easy path
        if (localFolderByRemotePath == null) {
            folderContentUpdated.addAll(remoteFolderContent.map { it.apply { needsToUpdateThumbnail = !it.isFolder } })
        } else {
            // Keep the current local properties or we will miss relevant things.
            remoteFolder.copyLocalPropertiesFrom(localFolderByRemotePath)

            // Folder already exists in database, get database content to update files accordingly
            val localFolderContent = localFileDataSource.getFolderContent(folderId = localFolderByRemotePath.id!!)

            val localFilesMap = localFolderContent.associateBy { localFile -> localFile.remoteId ?: localFile.remotePath }.toMutableMap()

            // Loop to sync every child
            remoteFolderContent.forEach { remoteChild ->
                remoteChild.lastSyncDateForProperties = currentSyncTime

                // Let's try with remote path if the file does not have remote id yet
                val localChildToSync = localFilesMap.remove(remoteChild.remoteId) ?: localFilesMap.remove(remoteChild.remotePath)

                // If local child does not exists, just insert the new one.
                if (localChildToSync == null) {
                    folderContentUpdated.add(
                        remoteChild.apply {
                            parentId = localFolderByRemotePath.id
                            needsToUpdateThumbnail = !remoteChild.isFolder
                            // remote eTag will not be set unless file CONTENTS are synchronized
                            etag = ""
                            availableOfflineStatus =
                                if (remoteFolder.isAvailableOffline) AVAILABLE_OFFLINE_PARENT else NOT_AVAILABLE_OFFLINE

                        })
                } else {
                    // File exists in the database, we need to check several stuff.
                    folderContentUpdated.add(
                        remoteChild.apply {
                            copyLocalPropertiesFrom(localChildToSync)
                            // DO NOT update etag till contents are synced.
                            etag = localChildToSync.etag
                            needsToUpdateThumbnail =
                                (!remoteChild.isFolder && remoteChild.modificationTimestamp != localChildToSync.modificationTimestamp) || localChildToSync.needsToUpdateThumbnail
                            // Probably not needed, if the child was already in the database, the av offline status should be also there
                            if (remoteFolder.isAvailableOffline) {
                                availableOfflineStatus = AVAILABLE_OFFLINE_PARENT
                            }
                            // FIXME: What about renames? Need to fix storage path
                        })
                }
            }

            // Remaining items should be removed from the database and local storage. They do not exists in remote anymore.
            localFilesMap.map { it.value }.forEach { ocFile ->
                if (ocFile.isFolder) {
                    removeLocalFolderRecursively(ocFile = ocFile, onlyFromLocalStorage = false)
                } else {
                    removeLocalFile(ocFile = ocFile, onlyFromLocalStorage = false)
                }
            }
        }

        localFileDataSource.saveFilesInFolder(
            folder = remoteFolder,
            listOfFiles = folderContentUpdated
        )
        return folderContentUpdated
    }

    override fun removeFile(listOfFilesToRemove: List<OCFile>, removeOnlyLocalCopy: Boolean) {
        listOfFilesToRemove.forEach { ocFile ->
            if (!removeOnlyLocalCopy) {
                try {
                    remoteFileDataSource.removeFile(ocFile.remotePath)
                } catch (fileNotFoundException: FileNotFoundException) {
                    Timber.i("File ${ocFile.fileName} was not found in server. Let's remove it from local storage")
                }
            }
            if (ocFile.isFolder) {
                removeLocalFolderRecursively(ocFile = ocFile, onlyFromLocalStorage = removeOnlyLocalCopy)
            } else {
                removeLocalFile(ocFile = ocFile, onlyFromLocalStorage = removeOnlyLocalCopy)
            }
        }
    }

    override fun removeFilesForAccount(accountName: String) {
        localFileDataSource.removeFilesForAccount(accountName)
    }

    override fun renameFile(ocFile: OCFile, newName: String) {
        // 1. Compose new remote path
        val newRemotePath = localStorageProvider.getExpectedRemotePath(
            remotePath = ocFile.remotePath,
            newName = newName,
            isFolder = ocFile.isFolder
        )

        // 2. Check if file already exists in database
        if (localFileDataSource.getFileByRemotePath(newRemotePath, ocFile.owner) != null) {
            throw FileAlreadyExistsException()
        }

        // 3. Perform remote operation
        remoteFileDataSource.renameFile(
            oldName = ocFile.fileName,
            oldRemotePath = ocFile.remotePath,
            newName = newName,
            isFolder = ocFile.isFolder
        )

        // 4. Save new remote path in the local database
        localFileDataSource.renameFile(
            fileToRename = ocFile,
            finalRemotePath = newRemotePath,
            finalStoragePath = localStorageProvider.getDefaultSavePathFor(ocFile.owner, newRemotePath)
        )

        // 5. Update local storage
        localStorageProvider.moveLocalFile(
            ocFile = ocFile,
            finalStoragePath = localStorageProvider.getDefaultSavePathFor(ocFile.owner, newRemotePath)
        )
    }

    override fun saveFile(file: OCFile) {
        localFileDataSource.saveFile(file)
    }

    override fun disableThumbnailsForFile(fileId: Long) {
        localFileDataSource.disableThumbnailsForFile(fileId)
    }

    override fun updateFileWithNewAvailableOfflineStatus(ocFile: OCFile, newAvailableOfflineStatus: AvailableOfflineStatus) {
        localFileDataSource.updateAvailableOfflineStatusForFile(ocFile, newAvailableOfflineStatus)
    }

    override fun updateDownloadedFilesStorageDirectoryInStoragePath(oldDirectory: String, newDirectory: String) {
        localFileDataSource.updateDownloadedFilesStorageDirectoryInStoragePath(oldDirectory, newDirectory)
    }

    private fun removeLocalFolderRecursively(ocFile: OCFile, onlyFromLocalStorage: Boolean) {
        val folderContent = localFileDataSource.getFolderContent(ocFile.id!!)

        // 1. Remove folder content recursively
        folderContent.forEach { file ->
            if (file.isFolder) {
                removeLocalFolderRecursively(ocFile = file, onlyFromLocalStorage = onlyFromLocalStorage)
            } else {
                removeLocalFile(ocFile = file, onlyFromLocalStorage = onlyFromLocalStorage)
            }
        }

        // 2. Remove the folder itself
        removeLocalFile(ocFile = ocFile, onlyFromLocalStorage = onlyFromLocalStorage)
    }

    private fun removeLocalFile(ocFile: OCFile, onlyFromLocalStorage: Boolean) {
        localStorageProvider.deleteLocalFile(ocFile)
        if (onlyFromLocalStorage) {
            localFileDataSource.saveFile(ocFile.copy(storagePath = null, etagInConflict = null))
        } else {
            localFileDataSource.removeFile(ocFile.id!!)
        }
    }
}
