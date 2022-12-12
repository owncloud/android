/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
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
package com.owncloud.android.lib.resources.files

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.webdav.MkColMethod
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import timber.log.Timber
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Remote operation performing the creation of a new folder in the ownCloud server.
 *
 * @author David A. Velasco
 * @author masensio
 *
 * @param remotePath     Full path to the new directory to create in the remote server.
 * @param createFullPath 'True' means that all the ancestor folders should be created.
 */
class CreateRemoteFolderOperation(
    val remotePath: String,
    private val createFullPath: Boolean,
    private val isChunksFolder: Boolean = false
) : RemoteOperation<Unit>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<Unit> {

        var result = createFolder(client)
        if (!result.isSuccess && createFullPath && result.code == ResultCode.CONFLICT) {
            result = createParentFolder(FileUtils.getParentPath(remotePath), client)

            if (result.isSuccess) {
                // Second and last try
                result = createFolder(client)
            }
        }
        return result
    }

    private fun createFolder(client: OwnCloudClient): RemoteOperationResult<Unit> {
        var result: RemoteOperationResult<Unit>
        try {
            val webDavUri = if (isChunksFolder) {
                client.uploadsWebDavUri
            } else {
                client.userFilesWebDavUri
            }

            val mkCol = MkColMethod(
                URL(webDavUri.toString() + WebdavUtils.encodePath(remotePath))
            ).apply {
                setReadTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                setConnectionTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            }

            val status = client.executeHttpMethod(mkCol)
            result =
                if (status == HttpConstants.HTTP_CREATED) {
                    RemoteOperationResult(ResultCode.OK)
                } else {
                    RemoteOperationResult(mkCol)
                }

            Timber.d("Create directory $remotePath: ${result.logMessage}")
            client.exhaustResponse(mkCol.getResponseBodyAsStream())

        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Create directory $remotePath: ${result.logMessage}")
        }
        return result
    }

    private fun createParentFolder(parentPath: String, client: OwnCloudClient): RemoteOperationResult<Unit> {
        val operation: RemoteOperation<Unit> = CreateRemoteFolderOperation(parentPath, createFullPath)
        return operation.execute(client)
    }

    companion object {
        private const val READ_TIMEOUT: Long = 30_000
        private const val CONNECTION_TIMEOUT: Long = 5_000
    }
}
