/* ownCloud Android Library is available under MIT license
 *
 *   @author Abel García de Prada
 *
 *   Copyright (C) 2021 ownCloud GmbH.
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
import com.owncloud.android.lib.common.http.methods.nonwebdav.PostMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.oauth.params.ClientRegistrationParams
import com.owncloud.android.lib.resources.oauth.responses.ClientRegistrationResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import timber.log.Timber
import java.net.URL

class RegisterClientRemoteOperation(
    private val clientRegistrationParams: ClientRegistrationParams
) : RemoteOperation<ClientRegistrationResponse>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<ClientRegistrationResponse> {
        try {
            val requestBody = clientRegistrationParams.toRequestBody()

            val postMethod = PostMethod(
                httpClient = client,
                url = URL(clientRegistrationParams.registrationEndpoint),
                postRequestBody = requestBody
            )

            val status = client.executeHttpMethod(postMethod)

            val responseBody = postMethod.getResponseBodyAsString()

            if (status == HttpConstants.HTTP_CREATED && responseBody != null) {
                Timber.d("Successful response $responseBody")

                // Parse the response
                val moshi: Moshi = Moshi.Builder().build()
                val jsonAdapter: JsonAdapter<ClientRegistrationResponse> =
                    moshi.adapter(ClientRegistrationResponse::class.java)
                val clientRegistrationResponse: ClientRegistrationResponse? = jsonAdapter.fromJson(responseBody)
                Timber.d("Client registered and parsed to $clientRegistrationResponse")

                return RemoteOperationResult<ClientRegistrationResponse>(RemoteOperationResult.ResultCode.OK).apply {
                    data = clientRegistrationResponse
                }

            } else {
                Timber.e("Failed response while registering a new client. Status code: $status; response message: $responseBody")
                return RemoteOperationResult<ClientRegistrationResponse>(postMethod)
            }

        } catch (e: Exception) {
            Timber.e(e, "Exception while registering a new client.")
            return RemoteOperationResult<ClientRegistrationResponse>(e)

        }

    }
}
