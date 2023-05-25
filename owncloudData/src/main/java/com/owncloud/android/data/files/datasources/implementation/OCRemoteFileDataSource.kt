/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.files.datasources.RemoteFileDataSource
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.lib.resources.files.RemoteFile

class OCRemoteFileDataSource(
    private val clientManager: ClientManager,
) : RemoteFileDataSource {
    override fun checkPathExistence(
        path: String,
        checkUserCredentials: Boolean,
        accountName: String,
        spaceWebDavUrl: String?,
    ): Boolean = clientManager.getFileService(accountName).checkPathExistence(
        path = path,
        isUserLogged = checkUserCredentials,
        spaceWebDavUrl = spaceWebDavUrl,
    ).data

    override fun copyFile(
        sourceRemotePath: String,
        targetRemotePath: String,
        accountName: String,
        sourceSpaceWebDavUrl: String?,
        targetSpaceWebDavUrl: String?,
    ): String = executeRemoteOperation {
        clientManager.getFileService(accountName).copyFile(
            sourceRemotePath = sourceRemotePath,
            targetRemotePath = targetRemotePath,
            sourceSpaceWebDavUrl = sourceSpaceWebDavUrl,
            targetSpaceWebDavUrl = targetSpaceWebDavUrl,
        )
    }

    override fun createFolder(
        remotePath: String,
        createFullPath: Boolean,
        isChunksFolder: Boolean,
        accountName: String,
        spaceWebDavUrl: String?,
    ) = executeRemoteOperation {
        clientManager.getFileService(accountName).createFolder(
            remotePath = remotePath,
            createFullPath = createFullPath,
            isChunkFolder = isChunksFolder,
            spaceWebDavUrl = spaceWebDavUrl,
        )
    }

    /**
     * Checks if remotePath does not exist in the server and returns it, or adds
     * a suffix to it in order to avoid the server file is overwritten.
     *
     * @param remotePath
     * @return
     */
    override fun getAvailableRemotePath(
        remotePath: String,
        accountName: String,
        spaceWebDavUrl: String?,
    ): String {
        var checkExistsFile = checkPathExistence(remotePath, false, accountName, spaceWebDavUrl)
        if (!checkExistsFile) {
            return remotePath
        }

        val pos = remotePath.lastIndexOf(".")
        var suffix: String
        var extension = ""
        if (pos >= 0) {
            extension = remotePath.substring(pos + 1)
            remotePath.apply {
                substring(0, pos)
            }
        }
        var count = 2
        do {
            suffix = " ($count)"
            checkExistsFile = if (pos >= 0) {
                checkPathExistence("${remotePath.substringBeforeLast('.', "")}$suffix.$extension", false, accountName, spaceWebDavUrl)
            } else {
                checkPathExistence(remotePath + suffix, false, accountName, spaceWebDavUrl)
            }
            count++
        } while (checkExistsFile)
        return if (pos >= 0) {
            "${remotePath.substringBeforeLast('.', "")}$suffix.$extension"
        } else {
            remotePath + suffix
        }
    }

    override fun moveFile(
        sourceRemotePath: String,
        targetRemotePath: String,
        accountName: String,
        spaceWebDavUrl: String?,
    ) = executeRemoteOperation {
        clientManager.getFileService(accountName).moveFile(
            sourceRemotePath = sourceRemotePath,
            targetRemotePath = targetRemotePath,
            spaceWebDavUrl = spaceWebDavUrl,
        )
    }

    override fun readFile(
        remotePath: String,
        accountName: String,
        spaceWebDavUrl: String?,
    ): OCFile = executeRemoteOperation {
        clientManager.getFileService(accountName).readFile(
            remotePath = remotePath,
            spaceWebDavUrl = spaceWebDavUrl,
        )
    }.toModel()

    override fun refreshFolder(
        remotePath: String,
        accountName: String,
        spaceWebDavUrl: String?,
    ): List<OCFile> =
        // Assert not null, service should return an empty list if no files there.
        executeRemoteOperation {
            clientManager.getFileService(accountName).refreshFolder(
                remotePath = remotePath,
                spaceWebDavUrl = spaceWebDavUrl,
            )
        }.let { listOfRemote ->
            listOfRemote.map { remoteFile -> remoteFile.toModel() }
        }

    override fun deleteFile(
        remotePath: String,
        accountName: String,
        spaceWebDavUrl: String?,
    ) = executeRemoteOperation {
        clientManager.getFileService(accountName).removeFile(
            remotePath = remotePath,
            spaceWebDavUrl = spaceWebDavUrl,
        )
    }

    override fun renameFile(
        oldName: String,
        oldRemotePath: String,
        newName: String,
        isFolder: Boolean,
        accountName: String,
        spaceWebDavUrl: String?,
    ) = executeRemoteOperation {
        clientManager.getFileService(accountName).renameFile(
            oldName = oldName,
            oldRemotePath = oldRemotePath,
            newName = newName,
            isFolder = isFolder,
            spaceWebDavUrl = spaceWebDavUrl,
        )
    }

    private fun RemoteFile.toModel(): OCFile =
        OCFile(
            owner = owner,
            remoteId = remoteId,
            remotePath = remotePath,
            length = if (isFolder) {
                size
            } else {
                length
            },
            creationTimestamp = creationTimestamp,
            modificationTimestamp = modifiedTimestamp,
            mimeType = mimeType,
            etag = etag,
            permissions = permissions,
            privateLink = privateLink,
            sharedWithSharee = sharedWithSharee,
            sharedByLink = sharedByLink,
        )
}
