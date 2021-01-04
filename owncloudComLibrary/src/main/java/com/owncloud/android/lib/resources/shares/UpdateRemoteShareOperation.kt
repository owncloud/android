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
import com.owncloud.android.lib.common.http.methods.nonwebdav.PutMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.CommonOcsResponse
import com.owncloud.android.lib.resources.shares.RemoteShare.Companion.DEFAULT_PERMISSION
import com.owncloud.android.lib.resources.shares.responses.ShareItem
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.FormBody
import timber.log.Timber
import java.lang.reflect.Type
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Updates parameters of an existing Share resource, known its remote ID.
 *
 *
 * Allow updating several parameters, triggering a request to the server per parameter.
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Fernando Sanz Velasco
 */
class UpdateRemoteShareOperation
/**
 * Constructor. No update is initialized by default, need to be applied with setters below.
 */
    (
    /**
     * @param remoteId Identifier of the share to update.
     */
    private val remoteId: String

) : RemoteOperation<ShareResponse>() {
    /**
     * Name to update in Share resource. Ignored by servers previous to version 10.0.0
     *
     * Empty string clears the current name.
     * Null results in no update applied to the name.
     */
    var name: String? = null

    /**
     * Password to update in Share resource.
     *
     * Empty string clears the current password.
     * Null results in no update applied to the password.
     */
    var password: String? = null

    /**
     * Expiration date to update in Share resource.
     *
     * A negative value clears the current expiration date.
     * Zero value (start-of-epoch) results in no update done on
     * the expiration date.
     */
    var expirationDateInMillis: Long = INITIAL_EXPIRATION_DATE_IN_MILLIS

    /**
     * Permissions to update in Share resource.
     *
     * Values <= 0 result in no update applied to the permissions.
     */
    var permissions: Int = DEFAULT_PERMISSION

    /**
     * Enable upload permissions to update in Share resource.
     *
     * Null results in no update applied to the upload permission.
     */
    var publicUpload: Boolean? = null

    var retrieveShareDetails = false // To retrieve more info about the just updated share

    private fun buildRequestUri(baseUri: Uri) =
        baseUri.buildUpon()
            .appendEncodedPath(OCS_ROUTE)
            .appendEncodedPath(remoteId)
            .appendQueryParameter(PARAM_FORMAT, VALUE_FORMAT)
            .build()

    private fun parseResponse(response: String): ShareResponse? {
        val moshi = Moshi.Builder().build()
        val commonOcsType: Type = Types.newParameterizedType(CommonOcsResponse::class.java, ShareItem::class.java)
        val adapter: JsonAdapter<CommonOcsResponse<ShareItem>> = moshi.adapter(commonOcsType)
        val remoteShare = adapter.fromJson(response)?.ocs?.data?.toRemoteShare()
        return ShareResponse(remoteShare?.let { listOf(it) } ?: listOf())
    }

    private fun onResultUnsuccessful(
        method: PutMethod,
        response: String?,
        status: Int
    ): RemoteOperationResult<ShareResponse> {
        Timber.e("Failed response while while updating remote shares ")
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
        Timber.d("*** Retrieve the index of the new share completed ")
        val emptyShare = result.data.shares.first()

        return if (retrieveShareDetails) {
            // retrieve more info - PUT only returns the index of the new share
            GetRemoteShareOperation(emptyShare.id).execute(client)
        } else {
            result
        }
    }

    private fun createFormBodyBuilder(): FormBody.Builder {
        val formBodyBuilder = FormBody.Builder()

        // Parameters to update
        if (name != null) {
            formBodyBuilder.add(PARAM_NAME, name.orEmpty())
        }

        if (password != null) {
            formBodyBuilder.add(PARAM_PASSWORD, password.orEmpty())
        }

        if (expirationDateInMillis < INITIAL_EXPIRATION_DATE_IN_MILLIS) {
            // clear expiration date
            formBodyBuilder.add(PARAM_EXPIRATION_DATE, "")

        } else if (expirationDateInMillis > INITIAL_EXPIRATION_DATE_IN_MILLIS) {
            // set expiration date
            val dateFormat = SimpleDateFormat(FORMAT_EXPIRATION_DATE, Locale.getDefault())
            val expirationDate = Calendar.getInstance()
            expirationDate.timeInMillis = expirationDateInMillis
            val formattedExpirationDate = dateFormat.format(expirationDate.time)
            formBodyBuilder.add(PARAM_EXPIRATION_DATE, formattedExpirationDate)
        } // else, ignore - no update

        if (publicUpload != null) {
            formBodyBuilder.add(PARAM_PUBLIC_UPLOAD, publicUpload.toString())
        }

        // IMPORTANT: permissions parameter needs to be updated after mPublicUpload parameter,
        // otherwise they would be set always as 1 (READ) in the server when mPublicUpload was updated
        if (permissions > DEFAULT_PERMISSION) {
            // set permissions
            formBodyBuilder.add(PARAM_PERMISSIONS, permissions.toString())
        }

        return formBodyBuilder
    }

    override fun run(client: OwnCloudClient): RemoteOperationResult<ShareResponse> {
        val requestUri = buildRequestUri(client.baseUri)

        val formBodyBuilder = createFormBodyBuilder()

        val putMethod = PutMethod(client, URL(requestUri.toString()), formBodyBuilder.build()).apply {
            setRequestHeader(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.CONTENT_TYPE_URLENCODED_UTF8)
            addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)
        }


        return try {
            val status = client.executeHttpMethod(putMethod)
            val response = putMethod.getResponseBodyAsString()

            if (isSuccess(status)) {
                onRequestSuccessful(response)
            } else {
                onResultUnsuccessful(putMethod, response, status)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while updating remote share")
            RemoteOperationResult(e)
        }
    }

    private fun isSuccess(status: Int): Boolean = status == HttpConstants.HTTP_OK

    companion object {

        //OCS Route
        private const val OCS_ROUTE = "ocs/v2.php/apps/files_sharing/api/v1/shares"

        //Arguments - names
        private const val PARAM_NAME = "name"
        private const val PARAM_PASSWORD = "password"
        private const val PARAM_EXPIRATION_DATE = "expireDate"
        private const val PARAM_PERMISSIONS = "permissions"
        private const val PARAM_PUBLIC_UPLOAD = "publicUpload"

        //Arguments - constant values
        private const val FORMAT_EXPIRATION_DATE = "yyyy-MM-dd"
        private const val INITIAL_EXPIRATION_DATE_IN_MILLIS: Long = 0
    }
}
