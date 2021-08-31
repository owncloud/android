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
import com.owncloud.android.lib.common.http.methods.nonwebdav.PostMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.CommonOcsResponse
import com.owncloud.android.lib.resources.shares.RemoteShare.Companion.INIT_EXPIRATION_DATE_IN_MILLIS
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
 * Constructor
 *
 * @param remoteFilePath Full path of the file/folder being shared. Mandatory argument
 * @param shareType      0 = user, 1 = group, 3 = Public link. Mandatory argument
 * @param shareWith      User/group ID with who the file should be shared.  This is mandatory for shareType
 * of 0 or 1
 * @param permissions    1 - Read only Default for public shares
 * 2 - Update
 * 4 - Create
 * 8 - Delete
 * 16- Re-share
 * 31- All above Default for private shares
 * For user or group shares.
 * To obtain combinations, add the desired values together.
 * For instance, for Re-Share, delete, read, update, add 16+8+2+1 = 27.
 */
class CreateRemoteShareOperation(
    private val remoteFilePath: String,
    private val shareType: ShareType,
    private val shareWith: String,
    private val permissions: Int
) : RemoteOperation<ShareResponse>() {

    var name = "" // Name to set for the public link

    var password: String = "" // Password to set for the public link

    var expirationDateInMillis: Long = INIT_EXPIRATION_DATE_IN_MILLIS // Expiration date to set for the public link

    var publicUpload: Boolean = false // Upload permissions for the public link (only folders)

    var retrieveShareDetails = false // To retrieve more info about the just created share

    private fun buildRequestUri(baseUri: Uri) =
        baseUri.buildUpon()
            .appendEncodedPath(OCS_ROUTE)
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
        method: PostMethod,
        response: String?,
        status: Int
    ): RemoteOperationResult<ShareResponse> {
        Timber.e("Failed response while while creating new remote share operation ")
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
        Timber.d("*** Creating new remote share operation completed ")
        return result
    }

    private fun createFormBodyBuilder(): FormBody.Builder {

        val formBodyBuilder = FormBody.Builder()
            .add(PARAM_PATH, remoteFilePath)
            .add(PARAM_SHARE_TYPE, shareType.value.toString())
            .add(PARAM_SHARE_WITH, shareWith)

        if (name.isNotEmpty()) {
            formBodyBuilder.add(PARAM_NAME, name)
        }

        if (expirationDateInMillis > INIT_EXPIRATION_DATE_IN_MILLIS) {
            val dateFormat = SimpleDateFormat(FORMAT_EXPIRATION_DATE, Locale.getDefault())
            val expirationDate = Calendar.getInstance()
            expirationDate.timeInMillis = expirationDateInMillis
            val formattedExpirationDate = dateFormat.format(expirationDate.time)
            formBodyBuilder.add(PARAM_EXPIRATION_DATE, formattedExpirationDate)
        }

        if (publicUpload) {
            formBodyBuilder.add(PARAM_PUBLIC_UPLOAD, publicUpload.toString())
        }
        if (password.isNotEmpty()) {
            formBodyBuilder.add(PARAM_PASSWORD, password)
        }
        if (RemoteShare.DEFAULT_PERMISSION != permissions) {
            formBodyBuilder.add(PARAM_PERMISSIONS, permissions.toString())
        }

        return formBodyBuilder
    }

    override fun run(client: OwnCloudClient): RemoteOperationResult<ShareResponse> {
        val requestUri = buildRequestUri(client.baseUri)

        val formBodyBuilder = createFormBodyBuilder()

        val postMethod = PostMethod(URL(requestUri.toString()), formBodyBuilder.build()).apply {
            setRequestHeader(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.CONTENT_TYPE_URLENCODED_UTF8)
            addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)
        }

        return try {
            val status = client.executeHttpMethod(postMethod)
            val response = postMethod.getResponseBodyAsString()

            if (!isSuccess(status)) {
                onResultUnsuccessful(postMethod, response, status)
            } else {
                onRequestSuccessful(response)
            }

        } catch (e: Exception) {
            Timber.e(e, "Exception while creating new remote share operation ")
            RemoteOperationResult(e)
        }
    }

    private fun isSuccess(status: Int): Boolean = status == HttpConstants.HTTP_OK

    companion object {

        //OCS Route
        private const val OCS_ROUTE = "ocs/v2.php/apps/files_sharing/api/v1/shares"

        //Arguments - names
        private const val PARAM_FORMAT = "format"
        private const val PARAM_NAME = "name"
        private const val PARAM_EXPIRATION_DATE = "expireDate"
        private const val PARAM_PATH = "path"
        private const val PARAM_SHARE_TYPE = "shareType"
        private const val PARAM_SHARE_WITH = "shareWith"
        private const val PARAM_PASSWORD = "password"
        private const val PARAM_PUBLIC_UPLOAD = "publicUpload"
        private const val PARAM_PERMISSIONS = "permissions"

        //Arguments - constant values
        private const val VALUE_FORMAT = "json"
        private const val FORMAT_EXPIRATION_DATE = "yyyy-MM-dd"
    }
}
