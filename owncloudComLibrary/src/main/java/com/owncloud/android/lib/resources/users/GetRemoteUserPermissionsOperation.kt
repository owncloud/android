/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.lib.resources.users

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.HttpConstants.CONTENT_TYPE_JSON
import com.owncloud.android.lib.common.http.methods.nonwebdav.PostMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.net.URL

class GetRemoteUserPermissionsOperation(
    private val accountId: String
): RemoteOperation<List<String>>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<List<String>> {
        var result: RemoteOperationResult<List<String>>
        try {
            val requestBody = JSONObject().apply {
                put(ACCOUNT_UUID_BODY_PARAM, accountId)
            }.toString().toRequestBody(CONTENT_TYPE_JSON.toMediaType())

            val postMethod = PostMethod(URL(client.baseUri.toString() + PERMISSIONS_LIST_ENDPOINT), requestBody)

            val status = client.executeHttpMethod(postMethod)

            val response = postMethod.getResponseBodyAsString()

            if (status == HttpConstants.HTTP_CREATED) {
                Timber.d("Successful response: $response")

                val moshi: Moshi = Moshi.Builder().build()
                val adapter: JsonAdapter<PermissionsListResponse> = moshi.adapter(PermissionsListResponse::class.java)

                result = RemoteOperationResult(ResultCode.OK)
                result.data = postMethod.getResponseBodyAsString().let { adapter.fromJson(it)?.permissions ?: emptyList() }

                Timber.d("Get user permissions completed and parsed to ${result.data}")
            } else {
                result = RemoteOperationResult(postMethod)
                Timber.e("Failed response while getting user permissions; status code: $status, response: $response")
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while getting oCIS user permissions")
        }
        return result
    }

    @JsonClass(generateAdapter = true)
    data class PermissionsListResponse(val permissions: List<String>)

    companion object {
        private const val PERMISSIONS_LIST_ENDPOINT = "/api/v0/settings/permissions-list"
        private const val ACCOUNT_UUID_BODY_PARAM = "account_uuid"
    }

}
