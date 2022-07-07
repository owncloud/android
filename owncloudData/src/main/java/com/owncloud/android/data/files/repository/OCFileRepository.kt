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

import androidx.lifecycle.LiveData
import com.owncloud.android.data.files.datasources.LocalFileDataSource
import com.owncloud.android.data.files.datasources.RemoteFileDataSource
import com.owncloud.android.data.storage.LocalStorageProvider
import com.owncloud.android.domain.exceptions.ConflictException
import com.owncloud.android.domain.exceptions.FileAlreadyExistsException
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.MIME_DIR
import com.owncloud.android.domain.files.model.OCFile
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
                targetFile = targetFolder,
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

    override fun getFolderContentAsLiveData(folderId: Long): LiveData<List<OCFile>> =
        localFileDataSource.getFolderContentAsLiveData(folderId)

    override fun getFolderImages(folderId: Long): List<OCFile> =
        localFileDataSource.getFolderImages(folderId)

    override fun getFilesSharedByLink(owner: String): List<OCFile> =
        localFileDataSource.getFilesSharedByLink(owner)

    override fun getFilesAvailableOffline(owner: String): List<OCFile> =
        localFileDataSource.getFilesAvailableOffline(owner)

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
                targetFile = targetFile,
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

    override fun refreshFolder(remotePath: String) {
        val currentSyncTime = System.currentTimeMillis()

        // Retrieve remote folder data
        val fetchFolderResult = remoteFileDataSource.refreshFolder(remotePath)
        val remoteFolder = fetchFolderResult.first()
        val remoteFolderContent = fetchFolderResult.drop(1)

        // Check if the folder already exists in database.
        val localFolderByRemotePath: OCFile? =
            localFileDataSource.getFileByRemotePath(remotePath = remoteFolder.remotePath, owner = remoteFolder.owner)

        // If folder doesn't exists in database, insert everything. Easy path
        if (localFolderByRemotePath == null) {
            localFileDataSource.saveFilesInFolder(
                folder = remoteFolder,
                listOfFiles = remoteFolderContent.map { it.apply { needsToUpdateThumbnail = !it.isFolder } }
            )
        } else {
            // Keep the current local properties or we will miss relevant things.
            remoteFolder.copyLocalPropertiesFrom(localFolderByRemotePath)

            // Folder already exists in database, we need to update data
            val localFolderContent = localFileDataSource.getFolderContent(folderId = localFolderByRemotePath.id!!)

            val localFilesMap = localFolderContent.associateBy { localFile -> localFile.remoteId ?: localFile.remotePath }.toMutableMap()

            // Final content for this folder, we will update the folder content all together
            val folderContentUpdated = mutableListOf<OCFile>()

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
                        })
                } else {
                    // File exists in the database, we need to check several stuff.
                    folderContentUpdated.add(
                        remoteChild.apply {
                            copyLocalPropertiesFrom(localChildToSync)
                            // DO NOT update etag till contents are synced.
                            etag = localChildToSync.etag
                            needsToUpdateThumbnail =
                                !remoteChild.isFolder && remoteChild.modificationTimestamp != localChildToSync.modificationTimestamp
                            // FIXME: What about renames? Need to fix storage path
                        })
                }
            }
            localFileDataSource.saveFilesInFolder(
                folder = remoteFolder,
                listOfFiles = folderContentUpdated
            )
            // Remaining items should be removed from the database and local storage. They do not exists in remote anymore.
            localFilesMap.map { it.value }.forEach { ocFile ->
                if (ocFile.isFolder) {
                    removeLocalFolderRecursively(ocFile = ocFile, onlyFromLocalStorage = false)
                } else {
                    removeLocalFile(ocFile = ocFile, onlyFromLocalStorage = false)
                }
            }
        }
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
