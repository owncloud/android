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
package com.owncloud.android.lib.resources.files.chunks

import android.net.Uri
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.webdav.MoveMethod
import com.owncloud.android.lib.resources.files.MoveRemoteFileOperation

/**
 * Remote operation to move the file built from chunks after uploading it
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 */
class MoveRemoteChunksFileOperation(
    sourceRemotePath: String,
    targetRemotePath: String,
    overwrite: Boolean,
    private val fileLastModificationTimestamp: String,
    private val fileLength: Long
) : MoveRemoteFileOperation(
    sourceRemotePath = sourceRemotePath,
    targetRemotePath = targetRemotePath,
    forceOverwrite = overwrite
) {

    override fun getSrcWebDavUriForClient(client: OwnCloudClient): Uri = client.uploadsWebDavUri

    override fun addRequestHeaders(moveMethod: MoveMethod) {
        super.addRequestHeaders(moveMethod)

        moveMethod.apply {
            addRequestHeader(HttpConstants.OC_X_OC_MTIME_HEADER, fileLastModificationTimestamp)
            addRequestHeader(HttpConstants.OC_TOTAL_LENGTH_HEADER, fileLength.toString())
        }
    }
}
