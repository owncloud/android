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
import com.owncloud.android.domain.exceptions.ConflictException
import com.owncloud.android.domain.exceptions.FileAlreadyExistsException
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.files.FileRepository
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

    override fun getFileById(fileId: Long): OCFile? =
        localFileDataSource.getFileById(fileId)

    override fun getFileByRemotePath(remotePath: String, owner: String): OCFile? =
        localFileDataSource.getFileByRemotePath(remotePath, owner)

    override fun getFolderContent(folderId: Long): List<OCFile> =
        localFileDataSource.getFolderContent(folderId)

    override fun getFolderImages(folderId: Long): List<OCFile> =
        localFileDataSource.getFolderImages(folderId)

    override fun moveFile(listOfFilesToMove: List<OCFile>, targetFile: OCFile) {
        listOfFilesToMove.forEach { ocFile ->

            // 1. Get the final remote path for this file.
            val expectedRemotePath: String = targetFile.remotePath + ocFile.fileName
            val finalRemotePath: String = remoteFileDataSource.getAvailableRemotePath(expectedRemotePath).apply {
                if (ocFile.isFolder) {
                    plus(File.separator)
                }
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
                removeFolderRecursively(ocFile = targetFile, removeOnlyLocalCopy = false)
                return@moveFile
            } catch (sourceFileDoesNotExist: FileNotFoundException) {
                // Source file does not exist anymore. Remove file from database and local storage and continue
                if (ocFile.isFolder) {
                    removeFolderRecursively(ocFile = ocFile, removeOnlyLocalCopy = false)
                } else {
                    removeFile(
                        ocFile = ocFile,
                        onlyLocalCopy = false
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

    override fun refreshFolder(remotePath: String) {
        remoteFileDataSource.refreshFolder(remotePath).also {
            localFileDataSource.saveFilesInFolder(
                folder = it.first(),
                listOfFiles = it.drop(1)
            )
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
                removeFolderRecursively(ocFile, removeOnlyLocalCopy)
            } else {
                removeFile(ocFile, removeOnlyLocalCopy)
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
        localFileDataSource.saveFile(ocFile.copy(remotePath = newRemotePath))
    }

    override fun saveFile(file: OCFile) {
        localFileDataSource.saveFile(file)
    }

    private fun removeFolderRecursively(ocFile: OCFile, removeOnlyLocalCopy: Boolean) {
        val folderContent = localFileDataSource.getFolderContent(ocFile.id!!)

        // 1. Remove folder content recursively
        folderContent.forEach { file ->
            if (file.isFolder) {
                removeFolderRecursively(file, removeOnlyLocalCopy)
            } else {
                removeFile(file, removeOnlyLocalCopy)
            }
        }

        // 2. Remove the folder itself
        removeFile(ocFile, removeOnlyLocalCopy)
    }

    private fun removeFile(ocFile: OCFile, onlyLocalCopy: Boolean) {
        localStorageProvider.deleteLocalFile(ocFile)
        if (onlyLocalCopy) {
            localFileDataSource.saveFile(ocFile.copy(storagePath = null))
        } else {
            localFileDataSource.removeFile(ocFile.id!!)
        }
    }
}
