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
package com.owncloud.android.lib.resources.files

import android.net.Uri
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.webdav.MoveMethod
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.isOneOf
import timber.log.Timber
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Remote operation moving a remote file or folder in the ownCloud server to a different folder
 * in the same account.
 *
 * Allows renaming the moving file/folder at the same time.
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Abel García de Prada
 */
open class MoveRemoteFileOperation(
    private val sourceRemotePath: String,
    private val targetRemotePath: String,
) : RemoteOperation<Unit>() {

    /**
     * Performs the rename operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    override fun run(client: OwnCloudClient): RemoteOperationResult<Unit> {
        if (targetRemotePath == sourceRemotePath) {
            // nothing to do!
            return RemoteOperationResult(ResultCode.OK)
        }

        if (targetRemotePath.startsWith(sourceRemotePath)) {
            return RemoteOperationResult(ResultCode.INVALID_MOVE_INTO_DESCENDANT)
        }

        /// perform remote operation
        var result: RemoteOperationResult<Unit>
        try {
            // After finishing a chunked upload, we have to move the resulting file from uploads folder to files one,
            // so this uri has to be customizable
            val srcWebDavUri = getSrcWebDavUriForClient(client)
            val moveMethod = MoveMethod(
                url = URL(srcWebDavUri.toString() + WebdavUtils.encodePath(sourceRemotePath)),
                destinationUrl = client.userFilesWebDavUri.toString() + WebdavUtils.encodePath(targetRemotePath),
            ).apply {
                addRequestHeaders(this)
                setReadTimeout(MOVE_READ_TIMEOUT, TimeUnit.SECONDS)
                setConnectionTimeout(MOVE_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            }

            val status = client.executeHttpMethod(moveMethod)

            when {
                isSuccess(status) -> {
                    result = RemoteOperationResult<Unit>(ResultCode.OK)
                }
                isPreconditionFailed(status) -> {
                    result = RemoteOperationResult<Unit>(ResultCode.INVALID_OVERWRITE)
                    client.exhaustResponse(moveMethod.getResponseBodyAsStream())

                    /// for other errors that could be explicitly handled, check first:
                    /// http://www.webdav.org/specs/rfc4918.html#rfc.section.9.9.4
                }
                else -> {
                    result = RemoteOperationResult<Unit>(moveMethod)
                    client.exhaustResponse(moveMethod.getResponseBodyAsStream())
                }
            }

            Timber.i("Move $sourceRemotePath to $targetRemotePath: ${result.logMessage}")
        } catch (e: Exception) {
            result = RemoteOperationResult<Unit>(e)
            Timber.e(e, "Move $sourceRemotePath to $targetRemotePath: ${result.logMessage}")

        }
        return result
    }

    /**
     * For standard moves, we will use [OwnCloudClient.getUserFilesWebDavUri].
     * In case we need a different source Uri, override this method.
     */
    open fun getSrcWebDavUriForClient(client: OwnCloudClient): Uri = client.userFilesWebDavUri

    /**
     * For standard moves, we won't need any special headers.
     * In case new headers are needed, override this method
     */
    open fun addRequestHeaders(moveMethod: MoveMethod) {
    }

    private fun isSuccess(status: Int) = status.isOneOf(HttpConstants.HTTP_CREATED, HttpConstants.HTTP_NO_CONTENT)

    private fun isPreconditionFailed(status: Int) = status == HttpConstants.HTTP_PRECONDITION_FAILED

    companion object {
        private const val MOVE_READ_TIMEOUT = 10L
        private const val MOVE_CONNECTION_TIMEOUT = 6L
    }
}
