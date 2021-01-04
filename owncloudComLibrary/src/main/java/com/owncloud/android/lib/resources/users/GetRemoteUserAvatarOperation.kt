/* ownCloud Android Library is available under MIT license
 *
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
package com.owncloud.android.lib.resources.users

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL

/**
 * Gets avatar about the user logged in, if available
 *
 * @author David A. Velasco
 * @author David González Verdugo
 */
class GetRemoteUserAvatarOperation(private val avatarDimension: Int) : RemoteOperation<RemoteAvatarData>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<RemoteAvatarData> {
        var inputStream: InputStream? = null
        var result: RemoteOperationResult<RemoteAvatarData>

        try {
            val endPoint =
                client.baseUri.toString() + NON_OFFICIAL_AVATAR_PATH + client.credentials.username + File.separator + avatarDimension
            Timber.d("avatar URI: %s", endPoint)

            val getMethod = GetMethod(client, URL(endPoint))

            val status = client.executeHttpMethod(getMethod)

            if (isSuccess(status)) {
                // find out size of file to read
                val contentLength = getMethod.getResponseHeader(HttpConstants.CONTENT_LENGTH_HEADER)?.toInt()

                // find out MIME-type!
                val mimeType = getMethod.getResponseHeader(HttpConstants.CONTENT_TYPE_HEADER)

                if (mimeType == null || !mimeType.startsWith("image")) {
                    Timber.w("Not an image, failing with no avatar")
                    return RemoteOperationResult(RemoteOperationResult.ResultCode.FILE_NOT_FOUND)
                }

                /// download will be performed to a buffer
                inputStream = getMethod.getResponseBodyAsStream()
                val bytesArray = inputStream?.readBytes() ?: byteArrayOf()

                // TODO check total bytes transferred?
                Timber.d("Avatar size: Bytes received ${bytesArray.size} of $contentLength")

                // find out etag
                val etag = WebdavUtils.getEtagFromResponse(getMethod)
                if (etag.isEmpty()) {
                    Timber.w("Could not read Etag from avatar")
                }

                // Result
                result = RemoteOperationResult(RemoteOperationResult.ResultCode.OK)
                result.setData(RemoteAvatarData(bytesArray, mimeType, etag))

            } else {
                result = RemoteOperationResult(getMethod)
                client.exhaustResponse(getMethod.getResponseBodyAsStream())
            }

        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while getting OC user avatar")

        } finally {
            try {
                client.exhaustResponse(inputStream)
                inputStream?.close()
            } catch (i: IOException) {
                Timber.e(i, "Unexpected exception closing input stream")
            }
        }

        return result
    }

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_OK

    companion object {
        private const val NON_OFFICIAL_AVATAR_PATH = "/index.php/avatar/"
    }
}
