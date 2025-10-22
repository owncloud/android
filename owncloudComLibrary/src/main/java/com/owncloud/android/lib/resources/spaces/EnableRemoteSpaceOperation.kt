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
import com.owncloud.android.lib.common.http.methods.nonwebdav.PatchMethod
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

class EnableRemoteSpaceOperation(
    private val spaceId: String,
): RemoteOperation<SpaceResponse>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<SpaceResponse> {
        var result: RemoteOperationResult<SpaceResponse>
        try {
            val moshi = Moshi.Builder().build()

            val uriBuilder = client.baseUri.buildUpon().apply {
                appendEncodedPath(GRAPH_API_SPACES_PATH)
                appendEncodedPath(spaceId)
            }

            val requestBody = JSONObject().toString().toRequestBody(CONTENT_TYPE_JSON.toMediaType())

            val patchMethod = PatchMethod(URL(uriBuilder.build().toString()), requestBody).apply {
                addRequestHeader(RESTORE_HEADER, RESTORE_HEADER_VALUE)
            }

            val status = client.executeHttpMethod(patchMethod)

            val response = patchMethod.getResponseBodyAsString()

            if (status == HttpConstants.HTTP_OK) {
                Timber.d("Successful response: $response")

                val responseAdapter: JsonAdapter<SpaceResponse> = moshi.adapter(SpaceResponse::class.java)

                result = RemoteOperationResult(ResultCode.OK)
                result.data = responseAdapter.fromJson(response)

                Timber.d("Space enabled successfully and parsed to ${result.data}")
            } else {
                result = RemoteOperationResult(patchMethod)
                Timber.e("Failed response while enabling the space; status code: $status, response: $response")
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while enabling the space $spaceId")
        }
        return result
    }

    companion object {
        private const val GRAPH_API_SPACES_PATH = "graph/v1.0/drives/"
        private const val RESTORE_HEADER = "Restore"
        private const val RESTORE_HEADER_VALUE = "T"
    }

}
