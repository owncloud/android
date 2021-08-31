/*
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Fernando Sanz Velasco
 * Copyright (C) 2021 ownCloud GmbH.
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
 *
 *
 *
 *
 */

package com.owncloud.android.lib.resources.shares

import android.net.Uri
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.DeleteMethod
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
 * Remove a share
 */

/**
 * Constructor
 *
 * @param remoteShareId Share ID
 */
class RemoveRemoteShareOperation(private val remoteShareId: String) : RemoteOperation<ShareResponse>() {

    private fun buildRequestUri(baseUri: Uri) =
        baseUri.buildUpon()
            .appendEncodedPath(OCS_ROUTE)
            .appendEncodedPath(remoteShareId)
            .appendQueryParameter(PARAM_FORMAT, VALUE_FORMAT)
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
        method: DeleteMethod,
        response: String?,
        status: Int
    ): RemoteOperationResult<ShareResponse> {
        Timber.e("Failed response while unshare link ")
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
        Timber.d("*** Unshare link completed ")
        return result
    }

    override fun run(client: OwnCloudClient): RemoteOperationResult<ShareResponse> {

        val requestUri = buildRequestUri(client.baseUri)

        val deleteMethod = DeleteMethod(URL(requestUri.toString())).apply {
            addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)
        }

        return try {
            val status = client.executeHttpMethod(deleteMethod)
            val response = deleteMethod.getResponseBodyAsString()

            if (!isSuccess(status)) {
                onResultUnsuccessful(deleteMethod, response, status)
            } else {
                onRequestSuccessful(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while unshare link")
            RemoteOperationResult(e)
        }
    }

    private fun isSuccess(status: Int): Boolean = status == HttpConstants.HTTP_OK

    companion object {

        //OCS Route
        private const val OCS_ROUTE = "ocs/v2.php/apps/files_sharing/api/v1/shares"

        //Arguments - names
        private const val PARAM_FORMAT = "format"

        //Arguments - constant values
        private const val VALUE_FORMAT = "json"

    }
}
