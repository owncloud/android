/* ownCloud Android Library is available under MIT license
 *   @author masensio
 *   @author David A. Velasco
 *   @author David González Verdugo
 *   @author Fernando Sanz Velasco
 *   Copyright (C) 2021 ownCloud GmbH
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
import com.owncloud.android.lib.resources.CommonOcsResponse
import com.owncloud.android.lib.resources.shares.responses.ShareItem
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import java.lang.reflect.Type
import java.net.URL

/**
 * Provide a list shares for a specific file.
 * The input is the full path of the desired file.
 * The output is a list of everyone who has the file shared with them.
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Fernando Sanz Velasco
 */

/**
 * Constructor
 *
 * @param remoteFilePath Path to file or folder
 * @param reshares       If set to false (default), only shares owned by the current user are
 * returned.
 * If set to true, shares owned by any user from the given file are returned.
 * @param subfiles       If set to false (default), lists only the folder being shared
 * If set to true, all shared files within the folder are returned.
 */
class GetRemoteSharesForFileOperation(
    private val remoteFilePath: String,
    private val reshares: Boolean,
    private val subfiles: Boolean
) : RemoteOperation<ShareResponse>() {

    private fun buildRequestUri(baseUri: Uri) =
        baseUri.buildUpon()
            .appendEncodedPath(OCS_ROUTE)
            .appendQueryParameter(PARAM_FORMAT, VALUE_FORMAT)
            .appendQueryParameter(PARAM_PATH, remoteFilePath)
            .appendQueryParameter(PARAM_RESHARES, reshares.toString())
            .appendQueryParameter(PARAM_SUBFILES, subfiles.toString())
            .build()

    private fun parseResponse(response: String): ShareResponse? {
        val moshi = Moshi.Builder().build()
        val listOfShareItemType: Type = Types.newParameterizedType(List::class.java, ShareItem::class.java)
        val commonOcsType: Type = Types.newParameterizedType(CommonOcsResponse::class.java, listOfShareItemType)
        val adapter: JsonAdapter<CommonOcsResponse<List<ShareItem>>> = moshi.adapter(commonOcsType)
        return adapter.fromJson(response)?.ocs?.data?.let { listOfShareItems ->
            ShareResponse(listOfShareItems.map { shareItem ->
                shareItem.toRemoteShare()
            })
        }
    }

    private fun onResultUnsuccessful(
        method: GetMethod,
        response: String?,
        status: Int
    ): RemoteOperationResult<ShareResponse> {
        Timber.e("Failed response while while getting remote shares for file operation ")
        if (response != null) {
            Timber.e("*** status code: $status; response message: $response")
        } else {
            Timber.e("*** status code: $status")
        }
        return RemoteOperationResult(method)
    }

    private fun onRequestSuccessful(response: String?): RemoteOperationResult<ShareResponse> {
        val result = RemoteOperationResult<ShareResponse>(RemoteOperationResult.ResultCode.OK)
        Timber.d("Successful response: $response")
        result.data = parseResponse(response!!)
        Timber.d("*** Getting remote shares for file completed ")
        Timber.d("Got ${result.data.shares.size} shares")
        return result
    }

    override fun run(client: OwnCloudClient): RemoteOperationResult<ShareResponse> {
        val requestUri = buildRequestUri(client.baseUri)

        val getMethod = GetMethod(client, URL(requestUri.toString())).apply {
            addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)
        }

        return try {
            val status = client.executeHttpMethod(getMethod)
            val response = getMethod.getResponseBodyAsString()

            if (isSuccess(status)) {
                onRequestSuccessful(response)
            } else {
                onResultUnsuccessful(getMethod, response, status)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while getting remote shares for file operation")
            RemoteOperationResult(e)
        }
    }

    private fun isSuccess(status: Int): Boolean = status == HttpConstants.HTTP_OK

    companion object {

        //OCS Route
        private const val OCS_ROUTE = "ocs/v2.php/apps/files_sharing/api/v1/shares"

        //Arguments - names
        private const val PARAM_PATH = "path"
        private const val PARAM_RESHARES = "reshares"
        private const val PARAM_SUBFILES = "subfiles"
    }
}
