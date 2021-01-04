/* ownCloud Android Library is available under MIT license
 *
 *   @author Christian Schabesberger
 *   @author masensio
 *   @author David A. Velasco
 *   @author David González Verdugo
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

package com.owncloud.android.lib.resources.shares

import android.net.Uri
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.HttpConstants.PARAM_FORMAT
import com.owncloud.android.lib.common.http.HttpConstants.VALUE_FORMAT
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK
import com.owncloud.android.lib.resources.CommonOcsResponse
import com.owncloud.android.lib.resources.shares.responses.ShareeOcsResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import java.lang.reflect.Type
import java.net.URL

/**
 * Created by masensio on 08/10/2015.
 *
 *
 * Retrieves a list of sharees (possible targets of a share) from the ownCloud server.
 *
 *
 * Currently only handles users and groups. Users in other OC servers (federation) should be added later.
 *
 *
 * Depends on SHAREE API. {@See https://github.com/owncloud/documentation/issues/1626}
 *
 *
 * Syntax:
 * Entry point: ocs/v2.php/apps/files_sharing/api/v1/sharees
 * HTTP method: GET
 * url argument: itemType - string, required
 * url argument: format - string, optional
 * url argument: search - string, optional
 * url arguments: perPage - int, optional
 * url arguments: page - int, optional
 *
 *
 * Status codes:
 * 100 - successful
 *
 * @author Christian Schabesberger
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 */
class GetRemoteShareesOperation
/**
 * Constructor
 *
 * @param searchString string for searching users, optional
 * @param page         page index in the list of results; beginning in 1
 * @param perPage      maximum number of results in a single page
 */
    (private val searchString: String, private val page: Int, private val perPage: Int) :
    RemoteOperation<ShareeOcsResponse>() {

    private fun buildRequestUri(baseUri: Uri) =
        baseUri.buildUpon()
            .appendEncodedPath(OCS_ROUTE)
            .appendQueryParameter(PARAM_FORMAT, VALUE_FORMAT)
            .appendQueryParameter(PARAM_ITEM_TYPE, VALUE_ITEM_TYPE)
            .appendQueryParameter(PARAM_SEARCH, searchString)
            .appendQueryParameter(PARAM_PAGE, page.toString())
            .appendQueryParameter(PARAM_PER_PAGE, perPage.toString())
            .build()

    private fun parseResponse(response: String): ShareeOcsResponse? {
        val moshi = Moshi.Builder().build()
        val type: Type = Types.newParameterizedType(CommonOcsResponse::class.java, ShareeOcsResponse::class.java)
        val adapter: JsonAdapter<CommonOcsResponse<ShareeOcsResponse>> = moshi.adapter(type)
        return adapter.fromJson(response)!!.ocs.data
    }

    private fun onResultUnsuccessful(
        method: GetMethod,
        response: String?,
        status: Int
    ): RemoteOperationResult<ShareeOcsResponse> {
        Timber.e("Failed response while getting users/groups from the server ")
        if (response != null) {
            Timber.e("*** status code: $status; response message: $response")
        } else {
            Timber.e("*** status code: $status")
        }
        return RemoteOperationResult(method)
    }

    private fun onRequestSuccessful(response: String?): RemoteOperationResult<ShareeOcsResponse> {
        val result = RemoteOperationResult<ShareeOcsResponse>(OK)
        Timber.d("Successful response: $response")
        result.data = parseResponse(response!!)
        Timber.d("*** Get Users or groups completed ")
        return result
    }

    override fun run(client: OwnCloudClient): RemoteOperationResult<ShareeOcsResponse> {
        val requestUri = buildRequestUri(client.baseUri)

        val getMethod = GetMethod(client, URL(requestUri.toString()))
        getMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)

        return try {
            val status = client.executeHttpMethod(getMethod)
            val response = getMethod.getResponseBodyAsString()

            if (isSuccess(status)) {
                onRequestSuccessful(response)
            } else {
                onResultUnsuccessful(getMethod, response, status)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while getting users/groups")
            RemoteOperationResult(e)
        }
    }

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_OK

    companion object {

        // OCS Routes
        private const val OCS_ROUTE = "ocs/v2.php/apps/files_sharing/api/v1/sharees"    // from OC 8.2

        // Arguments - names
        private const val PARAM_ITEM_TYPE = "itemType"
        private const val PARAM_SEARCH = "search"
        private const val PARAM_PAGE = "page"                //  default = 1
        private const val PARAM_PER_PAGE = "perPage"         //  default = 200

        // Arguments - constant values
        private const val VALUE_ITEM_TYPE = "file"         //  to get the server search for users / groups
    }
}
