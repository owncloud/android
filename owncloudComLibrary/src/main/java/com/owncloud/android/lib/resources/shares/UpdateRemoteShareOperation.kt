/* ownCloud Android Library is available under MIT license
 *
 *   Copyright (C) 2019 ownCloud GmbH.
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
import com.owncloud.android.lib.common.http.methods.nonwebdav.PutMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.RemoteShare.Companion.DEFAULT_PERMISSION
import okhttp3.FormBody
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
 */

class UpdateRemoteShareOperation
/**
 * Constructor. No update is initialized by default, need to be applied with setters below.
 */
    (
    /**
     * @param remoteId Identifier of the share to update.
     */
    private val remoteId: Long

) : RemoteOperation<ShareParserResult>() {
    /**
     * Password to update in Share resource.
     *
     * @param password Password to set to the target share.
     * Empty string clears the current password.
     * Null results in no update applied to the password.
     */
    var password: String? = null

    /**
     * Expiration date to update in Share resource.
     *
     * @param expirationDateInMillis Expiration date to set to the target share.
     * A negative value clears the current expiration date.
     * Zero value (start-of-epoch) results in no update done on
     * the expiration date.
     */
    var expirationDateInMillis: Long = INITIAL_EXPIRATION_DATE_IN_MILLIS

    /**
     * Permissions to update in Share resource.
     *
     * @param permissions Permissions to set to the target share.
     * Values <= 0 result in no update applied to the permissions.
     */
    var permissions: Int = DEFAULT_PERMISSION

    /**
     * Enable upload permissions to update in Share resource.
     *
     * @param publicUpload Upload permission to set to the target share.
     * Null results in no update applied to the upload permission.
     */
    var publicUpload: Boolean? = null

    /**
     * Name to update in Share resource. Ignored by servers previous to version 10.0.0
     *
     * @param name Name to set to the target share.
     * Empty string clears the current name.
     * Null results in no update applied to the name.
     */
    var name: String? = null

    override fun run(client: OwnCloudClient): RemoteOperationResult<ShareParserResult> {
        var result: RemoteOperationResult<ShareParserResult>

        try {
            val formBodyBuilder = FormBody.Builder()

            // Parameters to update
            if (name != null) {
                formBodyBuilder.add(PARAM_NAME, name)
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

            val requestUri = client.baseUri
            val uriBuilder = requestUri.buildUpon()
            uriBuilder.appendEncodedPath(ShareUtils.SHARING_API_PATH)
            uriBuilder.appendEncodedPath(remoteId.toString())

            val putMethod = PutMethod(URL(uriBuilder.build().toString()))

            putMethod.setRequestBody(formBodyBuilder.build())

            putMethod.setRequestHeader(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.CONTENT_TYPE_URLENCODED_UTF8)
            putMethod.addRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE)

            val status = client.executeHttpMethod(putMethod)

            if (isSuccess(status)) {
                // Parse xml response
                val parser = ShareToRemoteOperationResultParser(
                    ShareXMLParser()
                )
                parser.ownCloudVersion = client.ownCloudVersion
                parser.serverBaseUri = client.baseUri
                result = parser.parse(putMethod.responseBodyAsString)

            } else {
                result = RemoteOperationResult(putMethod)
            }

        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Log_OC.e(TAG, "Exception while Creating New Share", e)
        }

        return result
    }

    private fun isSuccess(status: Int): Boolean = status == HttpConstants.HTTP_OK

    companion object {
        private val TAG = GetRemoteShareOperation::class.java.simpleName

        private const val PARAM_NAME = "name"
        private const val PARAM_PASSWORD = "password"
        private const val PARAM_EXPIRATION_DATE = "expireDate"
        private const val PARAM_PERMISSIONS = "permissions"
        private const val PARAM_PUBLIC_UPLOAD = "publicUpload"
        private const val FORMAT_EXPIRATION_DATE = "yyyy-MM-dd"
        private const val ENTITY_CONTENT_TYPE = "application/x-www-form-urlencoded"
        private const val ENTITY_CHARSET = "UTF-8"

        private const val INITIAL_EXPIRATION_DATE_IN_MILLIS: Long = 0
    }
}
