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
*/

package com.owncloud.android.lib.resources.files

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.HeadMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import timber.log.Timber
import java.net.URL

class GetRemotePathForFileIdOperation(val fileId: String) : RemoteOperation<RemoteMetaFile>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<RemoteMetaFile> {

        return try {
            val stringUrl = "${client.baseUri}${PATH}$fileId"
            val headMethod = HeadMethod(URL(stringUrl))
            val status = client.executeHttpMethod(headMethod)
            if (isSuccess(status)) {
                RemoteOperationResult<RemoteMetaFile>(RemoteOperationResult.ResultCode.OK).apply {
                    var fileName = headMethod.response.request.url.queryParameter(SCROLL_TO_QUERY)
                    fileName = if (fileName != null) {
                        "${headMethod.response.request.url.queryParameter(DIR_QUERY)}/$fileName"
                    } else {
                        headMethod.response.request.url.queryParameter(DIR_QUERY)
                    }
                    data = RemoteMetaFile(metaPathForUser = fileName)
                }
            } else {
                RemoteOperationResult<RemoteMetaFile>(headMethod)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while getting remote path head of get file method.")
            RemoteOperationResult<RemoteMetaFile>(e)
        }
    }

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_OK || status == HttpConstants.HTTP_MULTI_STATUS

    companion object {
        private const val PATH = "/f/"
        private const val DIR_QUERY = "dir"
        private const val SCROLL_TO_QUERY = "scrollto"
    }

}
