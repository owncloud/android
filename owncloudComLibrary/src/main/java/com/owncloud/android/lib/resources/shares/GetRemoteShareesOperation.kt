/* ownCloud Android Library is available under MIT license
 *
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

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK
import org.json.JSONObject
import timber.log.Timber
import java.net.URL
import java.util.ArrayList

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
    RemoteOperation<ArrayList<JSONObject>>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<ArrayList<JSONObject>> {
        var result: RemoteOperationResult<ArrayList<JSONObject>>

        try {
            val requestUri = client.baseUri
            val uriBuilder = requestUri.buildUpon()
                .appendEncodedPath(OCS_ROUTE)
                .appendQueryParameter(PARAM_FORMAT, VALUE_FORMAT)
                .appendQueryParameter(PARAM_ITEM_TYPE, VALUE_ITEM_TYPE)
                .appendQueryParameter(PARAM_SEARCH, searchString)
                .appendQueryParameter(PARAM_PAGE, page.toString())
                .appendQueryParameter(PARAM_PER_PAGE, perPage.toString())

            val getMethod = GetMethod(URL(uriBuilder.build().toString()))

            getMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)

            val status = client.executeHttpMethod(getMethod)
            val response = getMethod.responseBodyAsString

            if (isSuccess(status)) {
                Timber.d("Successful response: $response")

                // Parse the response
                val respJSON = JSONObject(response)
                val respOCS = respJSON.getJSONObject(NODE_OCS)
                val respData = respOCS.getJSONObject(NODE_DATA)
                val respExact = respData.getJSONObject(NODE_EXACT)
                val respExactUsers = respExact.getJSONArray(NODE_USERS)
                val respExactGroups = respExact.getJSONArray(NODE_GROUPS)
                val respExactRemotes = respExact.getJSONArray(NODE_REMOTES)
                val respPartialUsers = respData.getJSONArray(NODE_USERS)
                val respPartialGroups = respData.getJSONArray(NODE_GROUPS)
                val respPartialRemotes = respData.getJSONArray(NODE_REMOTES)
                val jsonResults = arrayOf(
                    respExactUsers,
                    respExactGroups,
                    respExactRemotes,
                    respPartialUsers,
                    respPartialGroups,
                    respPartialRemotes
                )

                val data = ArrayList<JSONObject>() // For result data
                for (i in 0..5) {
                    for (j in 0 until jsonResults[i].length()) {
                        val jsonResult = jsonResults[i].getJSONObject(j)
                        data.add(jsonResult)
                        Timber.d("*** Added item: ${jsonResult.getString(PROPERTY_LABEL)}")
                    }
                }

                result = RemoteOperationResult(OK)
                result.data = data

                Timber.d("*** Get Users or groups completed ")

            } else {
                result = RemoteOperationResult(getMethod)
                Timber.e("Failed response while getting users/groups from the server ")
                if (response != null) {
                    Timber.e("*** status code: $status; response message: $response")
                } else {
                    Timber.e("*** status code: $status")
                }
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while getting users/groups")
        }

        return result
    }

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_OK

    companion object {

        // OCS Routes
        private const val OCS_ROUTE = "ocs/v2.php/apps/files_sharing/api/v1/sharees"    // from OC 8.2

        // Arguments - names
        private const val PARAM_FORMAT = "format"
        private const val PARAM_ITEM_TYPE = "itemType"
        private const val PARAM_SEARCH = "search"
        private const val PARAM_PAGE = "page"                //  default = 1
        private const val PARAM_PER_PAGE = "perPage"         //  default = 200

        // Arguments - constant values
        private const val VALUE_FORMAT = "json"
        private const val VALUE_ITEM_TYPE = "file"         //  to get the server search for users / groups

        // JSON Node names
        private const val NODE_OCS = "ocs"
        private const val NODE_DATA = "data"
        private const val NODE_EXACT = "exact"
        private const val NODE_USERS = "users"
        private const val NODE_GROUPS = "groups"
        private const val NODE_REMOTES = "remotes"
        const val NODE_VALUE = "value"
        const val PROPERTY_LABEL = "label"
        const val PROPERTY_SHARE_TYPE = "shareType"
        const val PROPERTY_SHARE_WITH = "shareWith"
        const val PROPERTY_SHARE_WITH_ADDITIONAL_INFO = "shareWithAdditionalInfo"
    }
}
