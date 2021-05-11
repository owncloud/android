/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2021 ownCloud GmbH.
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
import com.owncloud.android.lib.resources.files.chunks.MoveRemoteChunksFileOperation
import com.owncloud.android.lib.resources.files.chunks.RemoveRemoteChunksFolderOperation
import com.owncloud.android.lib.resources.files.services.ChunkService

class OCChunkService(override val client: OwnCloudClient) : ChunkService {

    override fun removeFile(remotePath: String): RemoteOperationResult<Unit> =
        RemoveRemoteChunksFolderOperation(remotePath = remotePath).execute(client)

    override fun moveFile(
        sourceRemotePath: String,
        targetRemotePath: String,
        overwrite: Boolean,
        fileLastModificationTimestamp: String,
        fileLength: Long
    ): RemoteOperationResult<Unit> =
        MoveRemoteChunksFileOperation(
            sourceRemotePath = sourceRemotePath,
            targetRemotePath = targetRemotePath,
            overwrite = overwrite,
            fileLastModificationTimestamp = fileLastModificationTimestamp,
            fileLength = fileLength
        ).execute(client)
}
