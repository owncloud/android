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
package com.owncloud.android.lib.resources.files

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.webdav.CopyMethod
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.isOneOf
import timber.log.Timber
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Remote operation copying a remote file or folder in the ownCloud server to a different folder
 * in the same account.
 *
 * Allows renaming the copying file/folder at the same time.
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González V.
 * @author Juan Carlos Garrote Gascón
 * @author Manuel Plazas Palacio
 *
 * @param sourceRemotePath    Remote path of the file/folder to copy.
 * @param targetRemotePath Remote path desired for the file/folder to copy it.
 */
class CopyRemoteFileOperation(
    private val sourceRemotePath: String,
    private val targetRemotePath: String,
    private val sourceSpaceWebDavUrl: String? = null,
    private val targetSpaceWebDavUrl: String? = null,
    private val forceOverride: Boolean = false,
) : RemoteOperation<String>() {

    /**
     * Performs the rename operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    override fun run(client: OwnCloudClient): RemoteOperationResult<String> {
        if (targetRemotePath == sourceRemotePath && sourceSpaceWebDavUrl == targetSpaceWebDavUrl) {
            // nothing to do!
            return RemoteOperationResult(ResultCode.OK)
        }

        /// perform remote operation
        var result: RemoteOperationResult<String>
        try {
            val copyMethod = CopyMethod(
                url = URL((sourceSpaceWebDavUrl ?: client.userFilesWebDavUri.toString()) + WebdavUtils.encodePath(sourceRemotePath)),
                destinationUrl = (targetSpaceWebDavUrl ?: client.userFilesWebDavUri.toString()) + WebdavUtils.encodePath(targetRemotePath),
                forceOverride = forceOverride,
            ).apply {
                addRequestHeaders(this)
                setReadTimeout(COPY_READ_TIMEOUT, TimeUnit.SECONDS)
                setConnectionTimeout(COPY_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            }
            val status = client.executeHttpMethod(copyMethod)
            when {
                isSuccess(status) -> {
                    val fileRemoteId = copyMethod.getResponseHeader(HttpConstants.OC_FILE_REMOTE_ID)
                    result = RemoteOperationResult(ResultCode.OK)
                    result.setData(fileRemoteId)
                }

                isPreconditionFailed(status) -> {
                    result = RemoteOperationResult(ResultCode.INVALID_OVERWRITE)
                    client.exhaustResponse(copyMethod.getResponseBodyAsStream())

                    /// for other errors that could be explicitly handled, check first:
                    /// http://www.webdav.org/specs/rfc4918.html#rfc.section.9.9.4
                }

                else -> {
                    result = RemoteOperationResult(copyMethod)
                    client.exhaustResponse(copyMethod.getResponseBodyAsStream())
                }
            }
            Timber.i("Copy $sourceRemotePath to $targetRemotePath - HTTP status code: $status")
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Copy $sourceRemotePath to $targetRemotePath: ${result.logMessage}")
        }
        return result
    }

    private fun addRequestHeaders(copyMethod: CopyMethod) {
        //Adding this because the library has an error with override
        if (copyMethod.forceOverride) {
            copyMethod.setRequestHeader(OVERWRITE, TRUE)
        }
    }

    private fun isSuccess(status: Int) = status.isOneOf(HttpConstants.HTTP_CREATED, HttpConstants.HTTP_NO_CONTENT)

    private fun isPreconditionFailed(status: Int) = status == HttpConstants.HTTP_PRECONDITION_FAILED

    companion object {
        private const val COPY_READ_TIMEOUT = 10L
        private const val COPY_CONNECTION_TIMEOUT = 6L
        private const val OVERWRITE = "overwrite"
        private const val TRUE = "T"
    }
}
