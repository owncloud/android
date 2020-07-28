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

import com.owncloud.android.data.files.datasources.RemoteFileDataSource
import com.owncloud.android.data.files.datasources.mapper.RemoteFileMapper
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.lib.resources.files.services.FileService

class OCRemoteFileDataSource(
    private val fileService: FileService,
    private val remoteFileMapper: RemoteFileMapper
) : RemoteFileDataSource {
    override fun checkPathExistence(path: String, checkUserCredentials: Boolean): Boolean =
        fileService.checkPathExistence(path = path, isUserLogged = checkUserCredentials).data

    override fun createFolder(remotePath: String, createFullPath: Boolean, isChunksFolder: Boolean): Unit =
        fileService.createFolder(
            remotePath = remotePath,
            createFullPath = createFullPath,
            isChunkFolder = isChunksFolder
        ).data

    override fun refreshFolder(remotePath: String): List<OCFile> =
        // Assert not null, service should return an empty list if no files there.
        fileService.refreshFolder(remotePath).data.map { remoteFileMapper.toModel(it)!! }

}
