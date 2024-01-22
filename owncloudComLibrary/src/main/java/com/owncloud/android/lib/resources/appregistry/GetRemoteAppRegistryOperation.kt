/* ownCloud Android Library is available under MIT license
 *   @author Abel García de Prada
 *
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
 *
 */
package com.owncloud.android.lib.resources.appregistry

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK
import com.owncloud.android.lib.resources.appregistry.responses.AppRegistryResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import timber.log.Timber
import java.net.URL

class GetRemoteAppRegistryOperation(private val appUrl: String?) : RemoteOperation<AppRegistryResponse>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<AppRegistryResponse> {
        var result: RemoteOperationResult<AppRegistryResponse>

        try {
            val urlFormatted = removeSubfolder(client.baseUri.toString()) + appUrl
            val getMethod = GetMethod(URL(urlFormatted))

            val status = client.executeHttpMethod(getMethod)

            val response = getMethod.getResponseBodyAsString()

            if (status == HttpConstants.HTTP_OK) {
                Timber.d("Successful response $response")

                // Parse the response
                val moshi: Moshi = Moshi.Builder().build()
                val adapter: JsonAdapter<AppRegistryResponse> = moshi.adapter(AppRegistryResponse::class.java)
                val appRegistryResponse: AppRegistryResponse = response?.let { adapter.fromJson(it) } ?: AppRegistryResponse(value = emptyList())

                result = RemoteOperationResult(OK)
                result.data = appRegistryResponse

                Timber.d("Get AppRegistry completed and parsed to ${result.data}")
            } else {
                result = RemoteOperationResult(getMethod)
                Timber.e("Failed response while getting app registry from the server status code: $status; response message: $response")
            }

        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while getting app registry")
        }

        return result
    }

    private fun removeSubfolder(url: String): String {
        val doubleSlashIndex = url.indexOf("//")
        val nextSlashIndex = url.indexOf('/', doubleSlashIndex + 2)
        return if (nextSlashIndex >= 0) {
            val result = url.substring(0, nextSlashIndex)
            if (result.endsWith("/")) result else "$result/"
        } else {
            if (url.endsWith("/")) url else "$url/"
        }
    }
}
