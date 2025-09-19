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

package com.owncloud.android.lib.resources.spaces

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.HttpConstants.CONTENT_TYPE_JSON
import com.owncloud.android.lib.common.http.methods.nonwebdav.PostMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.spaces.responses.SpaceResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.net.URL

class CreateRemoteSpaceOperation(
    private val spaceName: String,
    private val spaceSubtitle: String,
    private val spaceQuota: Long,
): RemoteOperation<SpaceResponse>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<SpaceResponse> {
        var result: RemoteOperationResult<SpaceResponse>
        try {
            val moshi = Moshi.Builder().build()

            val uriBuilder = client.baseUri.buildUpon().apply {
                appendEncodedPath(SPACE_CREATION_ENDPOINT)
                appendQueryParameter(QUERY_PARAMETER_TEMPLATE, QUERY_PARAMETER_TEMPLATE_VALUE)
            }

            val requestBody = JSONObject().apply {
                put(SPACE_NAME_BODY_PARAM, spaceName)
                put(SPACE_QUOTA_BODY_PARAM, JSONObject().apply {
                    put(SPACE_QUOTA_TOTAL_BODY_PARAM, spaceQuota)
                })
                put(SPACE_DESCRIPTION_BODY_PARAM, spaceSubtitle)
            }.toString().toRequestBody(CONTENT_TYPE_JSON.toMediaType())

            val postMethod = PostMethod(URL(uriBuilder.build().toString()), requestBody)

            val status = client.executeHttpMethod(postMethod)

            val response = postMethod.getResponseBodyAsString()

            if (status == HttpConstants.HTTP_CREATED) {
                Timber.d("Successful response: $response")

                val responseAdapter: JsonAdapter<SpaceResponse> = moshi.adapter(SpaceResponse::class.java)

                result = RemoteOperationResult(ResultCode.OK)
                result.data = responseAdapter.fromJson(response)

                Timber.d("Creation of space completed and parsed to ${result.data}")
            } else {
                result = RemoteOperationResult(postMethod)
                Timber.e("Failed response while creating a space; status code: $status, response: $response")
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while creating a space")
        }
        return result
    }

    companion object {
        private const val SPACE_CREATION_ENDPOINT = "graph/v1.0/drives"
        private const val SPACE_NAME_BODY_PARAM = "name"
        private const val SPACE_QUOTA_BODY_PARAM = "quota"
        private const val SPACE_QUOTA_TOTAL_BODY_PARAM = "total"
        private const val SPACE_DESCRIPTION_BODY_PARAM = "description"
        private const val QUERY_PARAMETER_TEMPLATE = "template"
        private const val QUERY_PARAMETER_TEMPLATE_VALUE = "default"
    }

}
