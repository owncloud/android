/* ownCloud Android Library is available under MIT license
 *
 *   @author Abel García de Prada
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
package com.owncloud.android.lib.resources.oauth

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.oauth.responses.OIDCDiscoveryResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import timber.log.Timber
import java.net.URL

/**
 * Get OIDC Discovery
 *
 * @author Abel García de Prada
 */
class GetOIDCDiscoveryRemoteOperation : RemoteOperation<OIDCDiscoveryResponse>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<OIDCDiscoveryResponse> {
        try {
            val uriBuilder = client.baseUri.buildUpon().apply {
                appendPath(WELL_KNOWN_PATH)    // avoid starting "/" in this method
                appendPath(OPENID_CONFIGURATION_RESOURCE)
            }.build()

            val getMethod = GetMethod(URL(uriBuilder.toString())).apply {
                addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)
            }

            val status = client.executeHttpMethod(getMethod)

            val responseBody = getMethod.getResponseBodyAsString()

            if (status == HttpConstants.HTTP_OK && responseBody != null) {
                Timber.d("Successful response $responseBody")

                // Parse the response
                val moshi: Moshi = Moshi.Builder().build()
                val jsonAdapter: JsonAdapter<OIDCDiscoveryResponse> = moshi.adapter(OIDCDiscoveryResponse::class.java)
                val oidcDiscoveryResponse: OIDCDiscoveryResponse? = jsonAdapter.fromJson(responseBody)
                Timber.d("Get OIDC Discovery completed and parsed to [$oidcDiscoveryResponse]")

                return RemoteOperationResult<OIDCDiscoveryResponse>(RemoteOperationResult.ResultCode.OK).apply {
                    data = oidcDiscoveryResponse
                }

            } else {
                Timber.e("Failed response while getting OIDC server discovery from the server status code: $status; response message: $responseBody")

                return RemoteOperationResult<OIDCDiscoveryResponse>(getMethod)
            }

        } catch (e: Exception) {
            Timber.e(e, "Exception while getting OIDC server discovery")

            return RemoteOperationResult<OIDCDiscoveryResponse>(e)
        }
    }

    companion object {
        private const val WELL_KNOWN_PATH = ".well-known"
        private const val OPENID_CONFIGURATION_RESOURCE = "openid-configuration"

    }
}
