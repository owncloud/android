/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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

package com.owncloud.android.lib.resources.shares

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.spaces.responses.PermissionsResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import timber.log.Timber
import java.net.URL

class GetRemoteGraphSharesForFileOperation(
    private val spaceId: String,
    private val itemId: String,
) : RemoteOperation<PermissionsResponse>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<PermissionsResponse> {
        var result: RemoteOperationResult<PermissionsResponse>
        try {
            val requestUri = client.baseUri.buildUpon().apply {
                appendEncodedPath(GRAPH_API_DRIVES_PATH)
                appendEncodedPath(spaceId)
                appendEncodedPath(ITEMS_PATH)
                appendEncodedPath(itemId)
                appendEncodedPath(PERMISSIONS_ENDPOINT)
                build()
            }
            val getMethod = GetMethod(URL(requestUri.toString()))

            val status = client.executeHttpMethod(getMethod)

            val response = getMethod.getResponseBodyAsString()

            if (status == HttpConstants.HTTP_OK) {
                Timber.d("Successful response: $response")

                val moshi: Moshi = Moshi.Builder().build()
                val adapter: JsonAdapter<PermissionsResponse> = moshi.adapter(PermissionsResponse::class.java)

                result = RemoteOperationResult(ResultCode.OK)
                result.data = getMethod.getResponseBodyAsString().let { adapter.fromJson(it) }

                Timber.d("Get graph shares completed and parsed to ${result.data}")
            } else {
                result = RemoteOperationResult(getMethod)
                Timber.e("Failed response while getting graph shares; status code: $status, response: $response")
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while getting graph shares")
        }
        return result
    }

    companion object {
        private const val GRAPH_API_DRIVES_PATH = "graph/v1beta1/drives/"
        private const val ITEMS_PATH = "items"
        private const val PERMISSIONS_ENDPOINT = "permissions"
    }
}
