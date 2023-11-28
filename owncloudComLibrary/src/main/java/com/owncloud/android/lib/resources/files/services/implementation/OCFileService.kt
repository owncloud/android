/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2023 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */
package com.owncloud.android.lib.resources.files.services.implementation

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.CheckPathExistenceRemoteOperation
import com.owncloud.android.lib.resources.files.CopyRemoteFileOperation
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation
import com.owncloud.android.lib.resources.files.DownloadRemoteFileOperation
import com.owncloud.android.lib.resources.files.GetRemoteMetaFileOperation
import com.owncloud.android.lib.resources.files.MoveRemoteFileOperation
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation
import com.owncloud.android.lib.resources.files.RemoteFile
import com.owncloud.android.lib.resources.files.RemoteMetaFile
import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation
import com.owncloud.android.lib.resources.files.RenameRemoteFileOperation
import com.owncloud.android.lib.resources.files.services.FileService

class OCFileService(override val client: OwnCloudClient) : FileService {
    override fun checkPathExistence(
        path: String,
        isUserLogged: Boolean,
        spaceWebDavUrl: String?,
    ): RemoteOperationResult<Boolean> =
        CheckPathExistenceRemoteOperation(
            remotePath = path,
            isUserLoggedIn = isUserLogged,
            spaceWebDavUrl = spaceWebDavUrl,
        ).execute(client)

    override fun copyFile(
        sourceRemotePath: String,
        targetRemotePath: String,
        sourceSpaceWebDavUrl: String?,
        targetSpaceWebDavUrl: String?,
        replace: Boolean,
    ): RemoteOperationResult<String?> =
        CopyRemoteFileOperation(
            sourceRemotePath = sourceRemotePath,
            targetRemotePath = targetRemotePath,
            sourceSpaceWebDavUrl = sourceSpaceWebDavUrl,
            targetSpaceWebDavUrl = targetSpaceWebDavUrl,
            forceOverride = replace,
        ).execute(client)

    override fun createFolder(
        remotePath: String,
        createFullPath: Boolean,
        isChunkFolder: Boolean,
        spaceWebDavUrl: String?,
    ): RemoteOperationResult<Unit> =
        CreateRemoteFolderOperation(
            remotePath = remotePath,
            createFullPath = createFullPath,
            isChunksFolder = isChunkFolder,
            spaceWebDavUrl = spaceWebDavUrl,
        ).execute(client)

    override fun downloadFile(
        remotePath: String,
        localTempPath: String
    ): RemoteOperationResult<Unit> =
        DownloadRemoteFileOperation(
            remotePath = remotePath,
            localFolderPath = localTempPath
        ).execute(client)

    override fun moveFile(
        sourceRemotePath: String,
        targetRemotePath: String,
        spaceWebDavUrl: String?,
        replace: Boolean,
    ): RemoteOperationResult<Unit> =
        MoveRemoteFileOperation(
            sourceRemotePath = sourceRemotePath,
            targetRemotePath = targetRemotePath,
            spaceWebDavUrl = spaceWebDavUrl,
            forceOverride = replace,
        ).execute(client)

    override fun readFile(
        remotePath: String,
        spaceWebDavUrl: String?,
    ): RemoteOperationResult<RemoteFile> =
        ReadRemoteFileOperation(
            remotePath = remotePath,
            spaceWebDavUrl = spaceWebDavUrl,
        ).execute(client)

    override fun refreshFolder(
        remotePath: String,
        spaceWebDavUrl: String?,
    ): RemoteOperationResult<ArrayList<RemoteFile>> =
        ReadRemoteFolderOperation(
            remotePath = remotePath,
            spaceWebDavUrl = spaceWebDavUrl,
        ).execute(client)

    override fun removeFile(
        remotePath: String,
        spaceWebDavUrl: String?,
    ): RemoteOperationResult<Unit> =
        RemoveRemoteFileOperation(
            remotePath = remotePath,
            spaceWebDavUrl = spaceWebDavUrl,
        ).execute(client)

    override fun renameFile(
        oldName: String,
        oldRemotePath: String,
        newName: String,
        isFolder: Boolean,
        spaceWebDavUrl: String?,
    ): RemoteOperationResult<Unit> =
        RenameRemoteFileOperation(
            oldName = oldName,
            oldRemotePath = oldRemotePath,
            newName = newName,
            isFolder = isFolder,
            spaceWebDavUrl = spaceWebDavUrl,
        ).execute(client)

    override fun getMetaFileInfo(
        fileId: String,
    ): RemoteOperationResult<RemoteMetaFile> =
        GetRemoteMetaFileOperation(fileId).execute(client)
}
