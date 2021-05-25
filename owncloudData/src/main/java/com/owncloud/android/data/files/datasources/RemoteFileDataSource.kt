/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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

package com.owncloud.android.data.files.datasources

import com.owncloud.android.domain.files.model.OCFile

interface RemoteFileDataSource {
    fun checkPathExistence(
        path: String,
        checkUserCredentials: Boolean
    ): Boolean

    fun copyFile(sourceRemotePath: String, targetRemotePath: String)

    fun createFolder(
        remotePath: String,
        createFullPath: Boolean,
        isChunksFolder: Boolean
    )

    fun getAvailableRemotePath(remotePath: String): String

    fun moveFile(sourceRemotePath: String, targetRemotePath: String)

    fun refreshFolder(
        remotePath: String
    ): List<OCFile>

    fun removeFile(
        remotePath: String
    )

    fun renameFile(
        oldName: String,
        oldRemotePath: String,
        newName: String,
        isFolder: Boolean,
    )
}
