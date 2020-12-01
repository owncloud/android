/* ownCloud Android Library is available under MIT license
 *   @author masensio
 *   @author David A. Velasco
 *   @author David González Verdugo
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

package com.owncloud.android.lib.resources.shares

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.DeleteMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import timber.log.Timber
import java.net.URL

/**
 * Remove a share
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 */

/**
 * Constructor
 *
 * @param remoteShareId Share ID
 */
class RemoveRemoteShareOperation(private val remoteShareId: String) : RemoteOperation<ShareParserResult>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<ShareParserResult> {
        var result: RemoteOperationResult<ShareParserResult>

        try {
            val requestUri = client.baseUri
            val uriBuilder = requestUri.buildUpon()
            uriBuilder.appendEncodedPath(ShareUtils.SHARING_API_PATH)
            uriBuilder.appendEncodedPath(remoteShareId)

            val deleteMethod = DeleteMethod(
                URL(uriBuilder.build().toString())
            )

            deleteMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)

            val status = client.executeHttpMethod(deleteMethod)

            if (isSuccess(status)) {

                // Parse xml response and obtain the list of shares
                val parser = ShareToRemoteOperationResultParser(
                    ShareXMLParser()
                )
                result = parser.parse(deleteMethod.getResponseBodyAsString())

                Timber.d("Unshare $remoteShareId: ${result.logMessage}")

            } else {
                result = RemoteOperationResult(deleteMethod)
            }

        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Unshare Link Exception ${result.logMessage}")
        }

        return result
    }

    private fun isSuccess(status: Int): Boolean = status == HttpConstants.HTTP_OK
}
