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
import com.owncloud.android.data.files.datasources.mapper.RemoteFileMapper
import com.owncloud.android.domain.files.model.OCFile

class OCRemoteFileDataSource(
    private val clientManager: ClientManager,
    private val remoteFileMapper: RemoteFileMapper
) : RemoteFileDataSource {

    override fun checkPathExistence(
        path: String,
        checkUserCredentials: Boolean
    ): Boolean = executeRemoteOperation {
        clientManager.getFileService().checkPathExistence(
            path = path,
            isUserLogged = checkUserCredentials
        )
    }

    override fun createFolder(
        remotePath: String,
        createFullPath: Boolean,
        isChunksFolder: Boolean
    ): Unit = executeRemoteOperation {
        clientManager.getFileService().createFolder(
            remotePath = remotePath,
            createFullPath = createFullPath,
            isChunkFolder = isChunksFolder
        )
    }

    override fun refreshFolder(remotePath: String): List<OCFile> =
        // Assert not null, service should return an empty list if no files there.
        executeRemoteOperation {
            clientManager.getFileService().refreshFolder(
                remotePath = remotePath
            )
        }.let { listOfRemote ->
            listOfRemote.map { remoteFile -> remoteFileMapper.toModel(remoteFile)!! }
        }
}
