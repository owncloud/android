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
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/**
 * Checks if the server is valid
 *
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 * @author Abel García de Prada
 */
class GetRemoteStatusOperation : RemoteOperation<OwnCloudVersion>() {
    private lateinit var latestResult: RemoteOperationResult<OwnCloudVersion>

    override fun run(client: OwnCloudClient): RemoteOperationResult<OwnCloudVersion> {

        val baseUriStr = client.baseUri.toString()
        if (baseUriStr.startsWith(HTTP_PREFIX) || baseUriStr.startsWith(
                HTTPS_PREFIX
            )) {
            tryConnection(client)
        } else {
            client.baseUri = Uri.parse(HTTPS_PREFIX + baseUriStr)
            val httpsSuccess = tryConnection(client)
            if (!httpsSuccess && !latestResult.isSslRecoverableException) {
                Timber.d("Establishing secure connection failed, trying non secure connection")
                client.baseUri = Uri.parse(HTTP_PREFIX + baseUriStr)
                tryConnection(client)
            }
        }
        return latestResult
    }

    private fun tryConnection(client: OwnCloudClient): Boolean {
        var successfulConnection = false
        val baseUrlSt = client.baseUri.toString()
        try {
            var getMethod = GetMethod(URL(baseUrlSt + OwnCloudClient.STATUS_PATH)).apply {
                setReadTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                setConnectionTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            }
            client.setFollowRedirects(false)
            var isRedirectToNonSecureConnection = false
            var status: Int
            try {
                status = client.executeHttpMethod(getMethod)
                latestResult =
                    if (isSuccess(status)) RemoteOperationResult(ResultCode.OK)
                    else RemoteOperationResult(getMethod)

            } catch (sslE: SSLException) {
                latestResult = RemoteOperationResult(sslE)
                return successfulConnection
            }

            var redirectedLocation = latestResult.redirectedLocation
            while (!redirectedLocation.isNullOrEmpty() && !latestResult.isSuccess) {
                isRedirectToNonSecureConnection =
                    isRedirectToNonSecureConnection ||
                            (baseUrlSt.startsWith(HTTPS_PREFIX) && redirectedLocation.startsWith(
                                HTTP_PREFIX
                            ))

                getMethod = GetMethod(URL(redirectedLocation)).apply {
                    setReadTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                    setConnectionTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                }

                status = client.executeHttpMethod(getMethod)
                latestResult = RemoteOperationResult(getMethod)
                redirectedLocation = latestResult.redirectedLocation
            }

            if (isSuccess(status)) {
                val respJSON = JSONObject(getMethod.responseBodyAsString)
                if (!respJSON.getBoolean(NODE_INSTALLED)) {
                    latestResult = RemoteOperationResult(ResultCode.INSTANCE_NOT_CONFIGURED)
                } else {
                    val version = respJSON.getString(NODE_VERSION)
                    val ocVersion = OwnCloudVersion(version)
                    // the version object will be returned even if the version is invalid, no error code;
                    // every app will decide how to act if (ocVersion.isVersionValid() == false)
                    latestResult = if (isRedirectToNonSecureConnection) {
                        RemoteOperationResult(ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION)
                    } else {
                        if (baseUrlSt.startsWith(HTTPS_PREFIX)) RemoteOperationResult(ResultCode.OK_SSL)
                        else RemoteOperationResult(ResultCode.OK_NO_SSL)
                    }
                    latestResult.data = ocVersion
                    successfulConnection = true
                }
            } else {
                latestResult = RemoteOperationResult(getMethod)
            }
        } catch (e: JSONException) {
            latestResult = RemoteOperationResult(ResultCode.INSTANCE_NOT_CONFIGURED)
        } catch (e: Exception) {
            latestResult = RemoteOperationResult(e)
        }
        when {
            latestResult.isSuccess -> Timber.i("Connection check at $baseUrlSt successful: ${latestResult.logMessage}")

            latestResult.isException ->
                Timber.e(latestResult.exception, "Connection check at $baseUrlSt: ${latestResult.logMessage}")

            else -> Timber.e("Connection check at $baseUrlSt failed: ${latestResult.logMessage}")
        }
        return successfulConnection
    }

    private fun isSuccess(status: Int): Boolean = status == HttpConstants.HTTP_OK

    companion object {
        /**
         * Maximum time to wait for a response from the server when the connection is being tested,
         * in MILLISECONDs.
         */
        private const val TRY_CONNECTION_TIMEOUT: Long = 5000
        private const val NODE_INSTALLED = "installed"
        private const val NODE_VERSION = "version"
        private const val HTTPS_PREFIX = "https://"
        private const val HTTP_PREFIX = "http://"
    }
}
