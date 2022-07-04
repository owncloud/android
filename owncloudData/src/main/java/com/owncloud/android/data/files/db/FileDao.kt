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
package com.owncloud.android.data.files.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.owncloud.android.data.ProviderMeta
import java.io.File.separatorChar

@Dao
abstract class FileDao {

    @Query(SELECT_FILE_WITH_ID)
    abstract fun getFileById(
        id: Long
    ): OCFileEntity?

    @Query(SELECT_FILE_FROM_OWNER_WITH_REMOTE_PATH)
    abstract fun getFileByOwnerAndRemotePath(
        owner: String,
        remotePath: String
    ): OCFileEntity?

    @Query(SELECT_FILE_WITH_REMOTE_ID)
    abstract fun getFileByRemoteId(
        remoteId: String
    ): OCFileEntity?

    @Query(SELECT_FILTERED_FOLDER_CONTENT)
    abstract fun getSearchFolderContent(
        folderId: Long,
        search: String
    ): List<OCFileEntity>

    @Query(SELECT_FILTERED_AVAILABLE_OFFLINE_FOLDER_CONTENT)
    abstract fun getSearchAvailableOfflineFolderContent(
        folderId: Long,
        search: String
    ): List<OCFileEntity>

    @Query(SELECT_FILTERED_SHARED_BY_LINK_FOLDER_CONTENT)
    abstract fun getSearchSharedByLinkFolderContent(
        folderId: Long,
        search: String
    ): List<OCFileEntity>

    @Query(SELECT_FOLDER_CONTENT)
    abstract fun getFolderContent(
        folderId: Long
    ): List<OCFileEntity>

    @Query(SELECT_FOLDER_CONTENT)
    abstract fun getFolderContentAsLiveData(
        folderId: Long
    ): LiveData<List<OCFileEntity>>

    @Query(SELECT_FOLDER_BY_MIMETYPE)
    abstract fun getFolderByMimeType(
        folderId: Long,
        mimeType: String
    ): List<OCFileEntity>

    @Query(SELECT_FILES_SHARED_BY_LINK)
    abstract fun getFilesSharedByLink(
        accountOwner: String
    ): List<OCFileEntity>

    @Query(SELECT_FILES_AVAILABLE_OFFLINE)
    abstract fun getFilesAvailableOffline(
        accountOwner: String
    ): List<OCFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(ocFileEntity: OCFileEntity): Long

    /**
     * Make sure that the ids are set properly. We don't take care of conflicts and that stuff here.
     */
    @Transaction
    open fun insertFilesInFolder(
        folder: OCFileEntity,
        folderContent: List<OCFileEntity>,
    ) {
        val folderId = insert(folder)

        folderContent.forEach { fileToInsert ->
            insert(fileToInsert.apply { parentId = folderId })
        }
    }

    @Transaction
    open fun mergeRemoteAndLocalFile(
        ocFileEntity: OCFileEntity
    ): Long {
        val localFile: OCFileEntity? = getFileByOwnerAndRemotePath(
            owner = ocFileEntity.owner,
            remotePath = ocFileEntity.remotePath
        )
        if (localFile == null) {
            return insert(ocFileEntity)
        } else {
            return insert(ocFileEntity.copy(
                parentId = localFile.parentId,
                lastSyncDateForData = localFile.lastSyncDateForData,
                modifiedAtLastSyncForData = localFile.modifiedAtLastSyncForData,
                storagePath = localFile.storagePath,
                treeEtag = localFile.treeEtag,
                etagInConflict = localFile.etagInConflict,
            ).apply {
                id = localFile.id
            })
        }
    }

    @Transaction
    open fun copy(
        sourceFile: OCFileEntity,
        targetFile: OCFileEntity,
        finalRemotePath: String,
        remoteId: String?
    ) {
        // 1. Update target size
        insert(
            targetFile.copy(
                length = targetFile.length + sourceFile.length
            ).apply { id = targetFile.id }
        )

        // 2. Insert a new file with common attributes and retrieved remote id
        insert(
            OCFileEntity(
                parentId = targetFile.id,
                owner = targetFile.owner,
                remotePath = finalRemotePath,
                remoteId = remoteId,
                length = sourceFile.length,
                modificationTimestamp = sourceFile.modificationTimestamp,
                mimeType = sourceFile.mimeType,
                name = null,
                needsToUpdateThumbnail = true,
                etag = "",
                creationTimestamp = null,
                permissions = null,
                treeEtag = ""
            )
        )
    }

    @Transaction
    open fun moveFile(
        sourceFile: OCFileEntity,
        targetFile: OCFileEntity,
        finalRemotePath: String,
        finalStoragePath: String?
    ) {
        // 1. Update target size
        insert(
            targetFile.copy(
                length = targetFile.length + sourceFile.length
            ).apply { id = targetFile.id }
        )

        // 2. Update source
        if (sourceFile.isFolder) {
            // Update remote path and storage path when moving a folder
            moveFolder(
                sourceFolder = sourceFile,
                targetFile = targetFile,
                targetRemotePath = finalRemotePath,
                targetStoragePath = finalStoragePath
            )
        } else {
            // Update remote path, storage path, parent file when moving a file
            moveSingleFile(
                sourceFile = sourceFile,
                targetFile = targetFile,
                finalRemotePath = finalRemotePath,
                finalStoragePath = finalStoragePath
            )
        }
    }

    @Query(DELETE_FILE_WITH_ID)
    abstract fun deleteFileWithId(id: Long)

    private fun moveSingleFile(
        sourceFile: OCFileEntity,
        targetFile: OCFileEntity,
        finalRemotePath: String,
        finalStoragePath: String?
    ) {
        insert(
            sourceFile.copy(
                parentId = targetFile.id,
                remotePath = finalRemotePath,
                storagePath = finalStoragePath
            ).apply { id = sourceFile.id }
        )
    }

    private fun moveFolder(
        sourceFolder: OCFileEntity,
        targetFile: OCFileEntity,
        targetRemotePath: String,
        targetStoragePath: String?
    ) {
        // 1. Move the folder
        val folderRemotePath =
            targetRemotePath.trimEnd(separatorChar).plus(separatorChar)
        val folderStoragePath =
            targetStoragePath?.trimEnd(separatorChar)?.plus(separatorChar)

        moveSingleFile(
            sourceFile = sourceFolder,
            targetFile = targetFile,
            finalRemotePath = folderRemotePath,
            finalStoragePath = folderStoragePath
        )

        // 2. Move its content
        val folderContent = getFolderContent(sourceFolder.id)

        folderContent.forEach { file ->
            val remotePathForChild = folderRemotePath.plus(file.name)
            val storagePathForChild = folderStoragePath?.plus(file.name)

            if (file.isFolder) {
                moveFolder(
                    sourceFolder = file,
                    targetFile = sourceFolder,
                    targetRemotePath = remotePathForChild,
                    targetStoragePath = storagePathForChild
                )
            } else {
                moveSingleFile(
                    sourceFile = file,
                    targetFile = sourceFolder,
                    finalRemotePath = remotePathForChild,
                    finalStoragePath = storagePathForChild
                )
            }
        }
    }

    companion object {
        private const val SELECT_FILE_WITH_ID =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE id = :id"

        private const val SELECT_FILE_WITH_REMOTE_ID =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE remoteId = :remoteId"

        private const val SELECT_FILE_FROM_OWNER_WITH_REMOTE_PATH =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE owner = :owner " +
                    "AND remotePath = :remotePath"

        private const val DELETE_FILE_WITH_ID =
            "DELETE FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE id = :id"

        private const val SELECT_FOLDER_CONTENT =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE parentId = :folderId"

        private const val SELECT_FILTERED_FOLDER_CONTENT =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE parentId = :folderId " +
                    "AND remotePath LIKE '%' || :search || '%'"

        private const val SELECT_FILTERED_AVAILABLE_OFFLINE_FOLDER_CONTENT =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE parentId = :folderId " +
                    "AND keepInSync = '1' " +
                    "AND remotePath LIKE '%' || :search || '%'"

        private const val SELECT_FILTERED_SHARED_BY_LINK_FOLDER_CONTENT =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE parentId = :folderId " +
                    "AND remotePath LIKE '%' || :search || '%'" +
                    "AND sharedByLink NOT LIKE '%0%' " +
                    "OR sharedWithSharee NOT LIKE '%0%' "

        private const val SELECT_FOLDER_BY_MIMETYPE =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE parentId = :folderId " +
                    "AND mimeType LIKE :mimeType || '%' "

        private const val SELECT_FILES_SHARED_BY_LINK =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE owner = :accountOwner " +
                    "AND sharedByLink NOT LIKE '%0%' " +
                    "OR sharedWithSharee NOT LIKE '%0%'"

        private const val SELECT_FILES_AVAILABLE_OFFLINE =
            "SELECT * " +
                    "FROM ${ProviderMeta.ProviderTableMeta.FILES_TABLE_NAME} " +
                    "WHERE owner = :accountOwner " +
                    "AND keepInSync = '1'"
    }
}
