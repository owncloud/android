/* ownCloud Android Library is available under MIT license
*   Copyright (C) 2022 ownCloud GmbH.
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
import com.owncloud.android.lib.common.http.methods.webdav.DavUtils
import com.owncloud.android.lib.common.http.methods.webdav.PropfindMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import timber.log.Timber
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Operation to get the base url, which might differ in case of a redirect.
 *
 * @author Christian Schabesberger
 */

class GetBaseUrlRemoteOperation : RemoteOperation<String?>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<String?> {
        return try {
            val stringUrl = client.baseFilesWebDavUri.toString()

            val propFindMethod = PropfindMethod(URL(stringUrl), 0, DavUtils.allPropSet).apply {
                setReadTimeout(TIMEOUT, TimeUnit.SECONDS)
                setConnectionTimeout(TIMEOUT, TimeUnit.SECONDS)
            }

            val status = client.executeHttpMethod(propFindMethod)

            if (isSuccess(status)) {
                RemoteOperationResult<String?>(RemoteOperationResult.ResultCode.OK).apply {
                    data = propFindMethod.getFinalUrl().toString()
                }
            } else {
                RemoteOperationResult<String?>(propFindMethod).apply {
                    data = null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Could not get actuall (or redirected) base URL from base url (/).")
            RemoteOperationResult<String?>(e)
        }
    }

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_OK || status == HttpConstants.HTTP_MULTI_STATUS

    companion object {
        /**
         * Maximum time to wait for a response from the server in milliseconds.
         */
        private const val TIMEOUT = 10_000L
    }
}
