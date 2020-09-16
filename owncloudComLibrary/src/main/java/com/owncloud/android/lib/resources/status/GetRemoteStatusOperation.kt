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
package com.owncloud.android.lib.resources.status

import android.net.Uri
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import org.json.JSONException
import timber.log.Timber


/**
 * Checks if the server is valid
 *
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 * @author Abel García de Prada
 */
class GetRemoteStatusOperation : RemoteOperation<OwnCloudVersion>() {
    companion object {
        const val HTTPS_SCHEME = "https"
        const val HTTP_SCHEME = "http"
    }

    override fun run(client: OwnCloudClient): RemoteOperationResult<OwnCloudVersion> {
        if (client.baseUri.scheme.isNullOrEmpty())
            client.baseUri = Uri.parse(HTTPS_SCHEME + "://" + client.baseUri.toString())

        var result = tryToConnect(client)
        if (result.code != ResultCode.OK_SSL && !result.isSslRecoverableException) {
            Timber.d("Establishing secure connection failed, trying non secure connection")
            client.baseUri = client.baseUri.buildUpon().scheme(HTTP_SCHEME).build()
            result = tryToConnect(client)
        }

        return result
    }

    private fun tryToConnect(client: OwnCloudClient): RemoteOperationResult<OwnCloudVersion> {
        val baseUrl = client.baseUri.toString()
        client.setFollowRedirects(false)
        return try {
            val requestor = StatusRequestor()
            val requestResult = requestor.requestAndFollowRedirects(baseUrl, client)
            requestor.handleRequestResult(requestResult, baseUrl)
        } catch (e: JSONException) {
            RemoteOperationResult(ResultCode.INSTANCE_NOT_CONFIGURED)
        } catch (e: Exception) {
            RemoteOperationResult(e)
        }
    }
}
