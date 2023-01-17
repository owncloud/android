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

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.files.datasources.RemoteFileDataSource
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.lib.resources.files.RemoteFile

class OCRemoteFileDataSource(
    private val clientManager: ClientManager,
) : RemoteFileDataSource {

    override fun getUrlToOpenInWeb(openWebEndpoint: String, fileId: String): String =
        executeRemoteOperation { clientManager.getFileService().getUrlToOpenInWeb(openWebEndpoint = openWebEndpoint, fileId = fileId) }

    override fun checkPathExistence(
        path: String,
        checkUserCredentials: Boolean,
        accountName: String,
    ): Boolean = clientManager.getFileService(accountName).checkPathExistence(
        path = path,
        isUserLogged = checkUserCredentials
    ).data

    override fun copyFile(
        sourceRemotePath: String,
        targetRemotePath: String,
        accountName: String,
    ): String = executeRemoteOperation {
        clientManager.getFileService(accountName).copyFile(
            sourceRemotePath = sourceRemotePath,
            targetRemotePath = targetRemotePath
        )
    }

    override fun createFolder(
        remotePath: String,
        createFullPath: Boolean,
        isChunksFolder: Boolean,
        accountName: String,
    ) = executeRemoteOperation {
        clientManager.getFileService(accountName).createFolder(
            remotePath = remotePath,
            createFullPath = createFullPath,
            isChunkFolder = isChunksFolder
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
    ): String {
        var checkExistsFile = checkPathExistence(remotePath, false, accountName)
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
                checkPathExistence("${remotePath.substringBeforeLast('.', "")}$suffix.$extension", false, accountName)
            } else {
                checkPathExistence(remotePath + suffix, false, accountName)
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
    ) = executeRemoteOperation {
        clientManager.getFileService(accountName).moveFile(
            sourceRemotePath = sourceRemotePath,
            targetRemotePath = targetRemotePath
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
    ): List<OCFile> =
        // Assert not null, service should return an empty list if no files there.
        executeRemoteOperation {
            clientManager.getFileService(accountName).refreshFolder(
                remotePath = remotePath
            )
        }.let { listOfRemote ->
            listOfRemote.map { remoteFile -> remoteFile.toModel() }
        }

    override fun deleteFile(
        remotePath: String,
        accountName: String,
    ) = executeRemoteOperation {
        clientManager.getFileService(accountName).removeFile(
            remotePath = remotePath
        )
    }

    override fun renameFile(
        oldName: String,
        oldRemotePath: String,
        newName: String,
        isFolder: Boolean,
        accountName: String,
    ) = executeRemoteOperation {
        clientManager.getFileService(accountName).renameFile(
            oldName = oldName,
            oldRemotePath = oldRemotePath,
            newName = newName,
            isFolder = isFolder
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
