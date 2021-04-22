/* ownCloud Android Library is available under MIT license
 *   @author masensio
 *   @author Semih Serhat Karakaya <karakayasemi@itu.edu.tr>
 *   @author David González Verdugo
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
package com.owncloud.android.lib.resources.status

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK
import com.owncloud.android.lib.resources.CommonOcsResponse
import com.owncloud.android.lib.resources.status.responses.CapabilityResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import java.lang.reflect.Type
import java.net.URL

/**
 * Get the Capabilities from the server
 * Save in Result.getData in a RemoteCapability object
 *
 * @author masensio
 * @author David González Verdugo
 */
class GetRemoteCapabilitiesOperation : RemoteOperation<RemoteCapability>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<RemoteCapability> {
        var result: RemoteOperationResult<RemoteCapability>

        try {
            val uriBuilder = client.baseUri.buildUpon().apply {
                appendEncodedPath(OCS_ROUTE)    // avoid starting "/" in this method
                appendQueryParameter(PARAM_FORMAT, VALUE_FORMAT)
            }
            val getMethod = GetMethod(URL(uriBuilder.build().toString())).apply {
                addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)
            }
            val status = client.executeHttpMethod(getMethod)

            val response = getMethod.getResponseBodyAsString()

            if (status == HttpConstants.HTTP_OK) {
                Timber.d("Successful response $response")

                // Parse the response
                val moshi: Moshi = Moshi.Builder().build()
                val type: Type = Types.newParameterizedType(CommonOcsResponse::class.java, CapabilityResponse::class.java)
                val adapter: JsonAdapter<CommonOcsResponse<CapabilityResponse>> = moshi.adapter(type)
                val commonResponse: CommonOcsResponse<CapabilityResponse>? = adapter.fromJson(response)

                result = RemoteOperationResult(OK)
                result.data = commonResponse?.ocs?.data?.toRemoteCapability()

                Timber.d("Get Capabilities completed and parsed to ${result.data}")
            } else {
                result = RemoteOperationResult(getMethod)
                Timber.e("Failed response while getting capabilities from the server status code: $status; response message: $response")
            }

        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while getting capabilities")
        }

        return result
    }

    companion object {

        // OCS Routes
        private const val OCS_ROUTE = "ocs/v2.php/cloud/capabilities"

        // Arguments - names
        private const val PARAM_FORMAT = "format"

        // Arguments - constant values
        private const val VALUE_FORMAT = "json"
    }
}
