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

package com.owncloud.android.lib.resources.members

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.members.responses.MemberResponse
import com.owncloud.android.lib.resources.members.responses.MembersResponseWrapper
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import timber.log.Timber
import java.net.URL

class SearchRemoteMembersOperation(
    private val query: String,
    private val searchGroups: Boolean
): RemoteOperation<List<MemberResponse>>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<List<MemberResponse>> {
        var result: RemoteOperationResult<List<MemberResponse>>
        try {
            val moshi = Moshi.Builder().build()

            val uriBuilder = client.baseUri.buildUpon().apply {
                appendEncodedPath(if (searchGroups) GRAPH_API_GROUPS_PATH else GRAPH_API_USERS_PATH)
                appendQueryParameter(SEARCH_QUERY, query)
                appendQueryParameter(ORDER_BY_QUERY, ORDER_BY_QUERY_VALUE)
            }

            val getMethod = GetMethod(URL(uriBuilder.build().toString()))

            val status = client.executeHttpMethod(getMethod)

            val response = getMethod.getResponseBodyAsString()

            if (status == HttpConstants.HTTP_OK) {
                Timber.d("Successful response: $response")

                val responseAdapter: JsonAdapter<MembersResponseWrapper> = moshi.adapter(MembersResponseWrapper::class.java)

                result = RemoteOperationResult(ResultCode.OK)
                result.data = responseAdapter.fromJson(response)?.value

                Timber.d("Get all available users and groups completed and parsed to ${result.data}")
            } else {
                result = RemoteOperationResult(getMethod)
                Timber.e("Failed response while getting the users and groups; status code: $status, response: $response")
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while getting the users and groups")
        }
        return result
    }

    companion object {
        private const val GRAPH_API_USERS_PATH = "graph/v1.0/users"
        private const val GRAPH_API_GROUPS_PATH = "graph/v1.0/groups"
        private const val SEARCH_QUERY = "\$search"
        private const val ORDER_BY_QUERY = "\$orderby"
        private const val ORDER_BY_QUERY_VALUE = "displayName"
    }

}
