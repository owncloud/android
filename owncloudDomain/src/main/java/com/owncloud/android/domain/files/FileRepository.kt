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

package com.owncloud.android.domain.files

import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.OCFile
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun createFolder(remotePath: String, parentFolder: OCFile)
    fun copyFile(listOfFilesToCopy: List<OCFile>, targetFolder: OCFile)
    fun getFileById(fileId: Long): OCFile?
    fun getFileByRemotePath(remotePath: String, owner: String): OCFile?
    fun getSearchFolderContent(fileListOption: FileListOption, folderId: Long, search: String): List<OCFile>
    fun getFolderContent(folderId: Long): List<OCFile>
    fun getFolderContentAsStream(folderId: Long): Flow<List<OCFile>>
    fun getFolderImages(folderId: Long): List<OCFile>
    fun getSharedByLinkForAccountAsStream(owner: String): Flow<List<OCFile>>
    fun getFilesAvailableOfflineFromAccountAsStream(owner: String): Flow<List<OCFile>>
    fun getFilesAvailableOfflineFromAccount(owner: String): List<OCFile>
    fun getFilesAvailableOfflineFromEveryAccount(): List<OCFile>
    fun moveFile(listOfFilesToMove: List<OCFile>, targetFile: OCFile)
    fun readFile(remotePath: String): OCFile
    fun refreshFolder(remotePath: String): List<OCFile>
    fun removeFile(listOfFilesToRemove: List<OCFile>, removeOnlyLocalCopy: Boolean)
    fun renameFile(ocFile: OCFile, newName: String)
    fun saveFile(file: OCFile)

    fun disableThumbnailsForFile(fileId: Long)
    fun updateFileWithNewAvailableOfflineStatus(ocFile: OCFile, newAvailableOfflineStatus: AvailableOfflineStatus)
}
