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

package com.owncloud.android.data.files.datasources

import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCMetaFile

interface RemoteFileDataSource {
    fun checkPathExistence(
        path: String,
        isUserLogged: Boolean,
        accountName: String,
        spaceWebDavUrl: String?,
    ): Boolean

    fun copyFile(
        sourceRemotePath: String,
        targetRemotePath: String,
        accountName: String,
        sourceSpaceWebDavUrl: String?,
        targetSpaceWebDavUrl: String?,
        replace: Boolean,
    ): String?

    fun createFolder(
        remotePath: String,
        createFullPath: Boolean,
        isChunksFolder: Boolean,
        accountName: String,
        spaceWebDavUrl: String?,
    )

    fun getAvailableRemotePath(
        remotePath: String,
        accountName: String,
        spaceWebDavUrl: String?,
        isUserLogged: Boolean,
    ): String

    fun moveFile(
        sourceRemotePath: String,
        targetRemotePath: String,
        accountName: String,
        spaceWebDavUrl: String?,
        replace: Boolean,
    )

    fun readFile(
        remotePath: String,
        accountName: String,
        spaceWebDavUrl: String? = null,
    ): OCFile

    fun refreshFolder(
        remotePath: String,
        accountName: String,
        spaceWebDavUrl: String? = null,
    ): List<OCFile>

    fun deleteFile(
        remotePath: String,
        accountName: String,
        spaceWebDavUrl: String? = null,
    )

    fun renameFile(
        oldName: String,
        oldRemotePath: String,
        newName: String,
        isFolder: Boolean,
        accountName: String,
        spaceWebDavUrl: String? = null,
    )

    fun getMetaFile(
        fileId: String,
        accountName: String,
    ): OCMetaFile

}
