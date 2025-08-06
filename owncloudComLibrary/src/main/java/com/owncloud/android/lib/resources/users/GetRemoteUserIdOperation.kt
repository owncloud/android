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
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import timber.log.Timber
import java.net.URL

class GetRemoteUserIdOperation: RemoteOperation<String>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<String> {
        var result: RemoteOperationResult<String>
        try {
            val getMethod = GetMethod(URL(client.baseUri.toString() + GRAPH_ME_ENDPOINT))

            val status = client.executeHttpMethod(getMethod)

            val response = getMethod.getResponseBodyAsString()

            if (status == HttpConstants.HTTP_OK) {
                Timber.d("Successful response: $response")

                val moshi: Moshi = Moshi.Builder().build()
                val adapter: JsonAdapter<GraphMeResponse> = moshi.adapter(GraphMeResponse::class.java)

                result = RemoteOperationResult(ResultCode.OK)
                result.data = getMethod.getResponseBodyAsString().let { adapter.fromJson(it)!!.id }

                Timber.d("Get user id completed and parsed to ${result.data}")
            } else {
                result = RemoteOperationResult(getMethod)
                Timber.e("Failed response while getting user id; status code: $status, response: $response")
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while getting oCIS user id")
        }
        return result
    }

    @JsonClass(generateAdapter = true)
    data class GraphMeResponse(val id: String)

    companion object {
        private const val GRAPH_ME_ENDPOINT = "/graph/v1.0/me"
    }

}
