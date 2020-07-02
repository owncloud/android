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
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import timber.log.Timber
import java.net.URL

/**
 * Provide a list shares for a specific file.
 * The input is the full path of the desired file.
 * The output is a list of everyone who has the file shared with them.
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 */

/**
 * Constructor
 *
 * @param remoteFilePath Path to file or folder
 * @param reshares       If set to false (default), only shares owned by the current user are
 * returned.
 * If set to true, shares owned by any user from the given file are returned.
 * @param subfiles       If set to false (default), lists only the folder being shared
 * If set to true, all shared files within the folder are returned.
 */
class GetRemoteSharesForFileOperation(
    private val remoteFilePath: String,
    private val reshares: Boolean,
    private val subfiles: Boolean
) : RemoteOperation<ShareParserResult>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<ShareParserResult> {
        var result: RemoteOperationResult<ShareParserResult>

        try {

            val requestUri = client.baseUri
            val uriBuilder = requestUri.buildUpon()
            uriBuilder.appendEncodedPath(ShareUtils.SHARING_API_PATH)
            uriBuilder.appendQueryParameter(PARAM_PATH, remoteFilePath)
            uriBuilder.appendQueryParameter(PARAM_RESHARES, reshares.toString())
            uriBuilder.appendQueryParameter(PARAM_SUBFILES, subfiles.toString())

            val getMethod = GetMethod(URL(uriBuilder.build().toString()))

            getMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)

            val status = client.executeHttpMethod(getMethod)

            if (isSuccess(status)) {
                // Parse xml response and obtain the list of shares
                val parser = ShareToRemoteOperationResultParser(
                    ShareXMLParser()
                )
                parser.ownCloudVersion = client.ownCloudVersion
                parser.serverBaseUri = client.baseUri
                result = parser.parse(getMethod.getResponseBodyAsString())

                if (result.isSuccess) {
                    Timber.d("Got ${result.data.shares.size} shares")
                }
            } else {
                result = RemoteOperationResult(getMethod)
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while getting shares")
        }

        return result
    }

    private fun isSuccess(status: Int): Boolean = status == HttpConstants.HTTP_OK

    companion object {
        private const val PARAM_PATH = "path"
        private const val PARAM_RESHARES = "reshares"
        private const val PARAM_SUBFILES = "subfiles"
    }
}
