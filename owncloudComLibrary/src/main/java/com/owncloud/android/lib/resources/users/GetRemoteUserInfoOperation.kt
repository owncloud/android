/*   ownCloud Android Library is available under MIT license
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
package com.owncloud.android.lib.resources.users

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.CommonOcsResponse
import com.owncloud.android.lib.resources.users.responses.UserInfoResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import java.lang.reflect.Type
import java.net.URL

/**
 * Gets information (id, display name, and e-mail address) about the user logged in.
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Abel García de Prada
 */
class GetRemoteUserInfoOperation : RemoteOperation<RemoteUserInfo>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<RemoteUserInfo> {
        var result: RemoteOperationResult<RemoteUserInfo>
        //Get the user
        try {
            val getMethod = GetMethod(client, URL(client.baseUri.toString() + OCS_ROUTE))
            val status = client.executeHttpMethod(getMethod)
            val response = getMethod.getResponseBodyAsString() ?: ""
            if (status == HttpConstants.HTTP_OK) {
                Timber.d("Successful response $response")

                val moshi: Moshi = Moshi.Builder().build()
                val type: Type = Types.newParameterizedType(CommonOcsResponse::class.java, UserInfoResponse::class.java)
                val adapter: JsonAdapter<CommonOcsResponse<UserInfoResponse>> = moshi.adapter(type)
                val commonResponse: CommonOcsResponse<UserInfoResponse>? = adapter.fromJson(response)

                result = RemoteOperationResult(ResultCode.OK)
                result.data = commonResponse?.ocs?.data?.toRemoteUserInfo()

                Timber.d("Get User Info completed and parsed to ${result.data}")

            } else {
                result = RemoteOperationResult(getMethod)
                Timber.e("Failed response while getting user information status code: $status, response: $response")
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while getting OC user information")
        }
        return result
    }

    companion object {
        // OCS Route
        private const val OCS_ROUTE = "/ocs/v2.php/cloud/user?format=json"
    }
}
