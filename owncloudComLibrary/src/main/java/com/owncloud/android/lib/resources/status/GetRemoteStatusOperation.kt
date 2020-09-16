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

/**
 * Checks if the server is valid
 *
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 * @author Abel García de Prada
 */
class GetRemoteStatusOperation : RemoteOperation<OwnCloudVersion>() {
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

    fun updateLocationWithRedirectPath(oldLocation: String, redirectedLocation: String): String {
        if (!redirectedLocation.startsWith("/"))
            return redirectedLocation
        val oldLocation = URL(oldLocation)
        return URL(oldLocation.protocol, oldLocation.host, oldLocation.port, redirectedLocation).toString()
    }

    private fun checkIfConnectionIsRedirectedToNoneSecure(
        isConnectionSecure: Boolean,
        baseUrl: String,
        redirectedUrl: String
    ): Boolean {
        return isConnectionSecure ||
                (baseUrl.startsWith(HTTPS_SCHEME) && redirectedUrl.startsWith(HTTP_SCHEME))
    }

    private fun getGetMethod(url: String): GetMethod {
        return GetMethod(URL(url + OwnCloudClient.STATUS_PATH)).apply {
            setReadTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            setConnectionTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
        }
    }

    data class RequestResult(
        val getMethod: GetMethod,
        val status: Int,
        val result: RemoteOperationResult<OwnCloudVersion>,
        val redirectedToUnsecureLocation: Boolean
    )

    fun requestAndFollowRedirects(baseLocation: String): RequestResult {
        var currentLocation = baseLocation
        var redirectedToUnsecureLocation = false
        var status: Int

        while (true) {
            val getMethod = getGetMethod(currentLocation)

            status = client.executeHttpMethod(getMethod)
            val result =
                if (isSuccess(status)) RemoteOperationResult<OwnCloudVersion>(ResultCode.OK)
                else RemoteOperationResult(getMethod)

            if (result.redirectedLocation.isNullOrEmpty() || result.isSuccess) {
                return RequestResult(getMethod, status, result, redirectedToUnsecureLocation)
            } else {
                val nextLocation = updateLocationWithRedirectPath(currentLocation, result.redirectedLocation)
                redirectedToUnsecureLocation =
                    checkIfConnectionIsRedirectedToNoneSecure(
                        redirectedToUnsecureLocation,
                        currentLocation,
                        nextLocation
                    )
                currentLocation = nextLocation
            }
        }
    }

    private fun handleRequestResult(
        requestResult: RequestResult,
        baseUrl: String
    ): RemoteOperationResult<OwnCloudVersion> {
        if (!isSuccess(requestResult.status))
            return RemoteOperationResult(requestResult.getMethod)

        val respJSON = JSONObject(requestResult.getMethod.getResponseBodyAsString())
        if (!respJSON.getBoolean(NODE_INSTALLED))
            return RemoteOperationResult(ResultCode.INSTANCE_NOT_CONFIGURED)

        val version = respJSON.getString(NODE_VERSION)
        val ocVersion = OwnCloudVersion(version)
        // the version object will be returned even if the version is invalid, no error code;
        // every app will decide how to act if (ocVersion.isVersionValid() == false)
        val result =
            if (requestResult.redirectedToUnsecureLocation) {
                RemoteOperationResult<OwnCloudVersion>(ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION)
            } else {
                if (baseUrl.startsWith(HTTPS_SCHEME)) RemoteOperationResult(ResultCode.OK_SSL)
                else RemoteOperationResult(ResultCode.OK_NO_SSL)
            }
        result.data = ocVersion
        return result
    }

    private fun tryToConnect(client: OwnCloudClient): RemoteOperationResult<OwnCloudVersion> {
        val baseUrl = client.baseUri.toString()
        client.setFollowRedirects(false)
        return try {
            val requestResult = requestAndFollowRedirects(baseUrl)
            handleRequestResult(requestResult, baseUrl)
        } catch (e: JSONException) {
            RemoteOperationResult(ResultCode.INSTANCE_NOT_CONFIGURED)
        } catch (e: Exception) {
            RemoteOperationResult(e)
        }
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
        private const val HTTPS_SCHEME = "https"
        private const val HTTP_SCHEME = "http"
    }
}
