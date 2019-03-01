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
 *
 * @param remoteId Identifier of the share to update.
 */
    (
    /**
     * Identifier of the share to update
     */
    private val mRemoteId: Long
) : RemoteOperation<ShareParserResult>() {

    /**
     * Password to set for the public link
     */
    private var mPassword: String? = null

    /**
     * Expiration date to set for the public link
     */
    private var mExpirationDateInMillis: Long = 0

    /**
     * Access permissions for the file bound to the share
     */
    private var mPermissions: Int = 0

    /**
     * Upload permissions for the public link (only folders)
     */
    private var mPublicUpload: Boolean? = null
    private var mName: String? = null

    init {
        mPassword = null               // no update
        mExpirationDateInMillis = 0    // no update
        mPublicUpload = null
        mPermissions = RemoteShare.DEFAULT_PERMISSION
    }

    /**
     * Set name to update in Share resource. Ignored by servers previous to version 10.0.0
     *
     * @param name Name to set to the target share.
     * Empty string clears the current name.
     * Null results in no update applied to the name.
     */
    fun setName(name: String) {
        this.mName = name
    }

    /**
     * Set password to update in Share resource.
     *
     * @param password Password to set to the target share.
     * Empty string clears the current password.
     * Null results in no update applied to the password.
     */
    fun setPassword(password: String) {
        mPassword = password
    }

    /**
     * Set expiration date to update in Share resource.
     *
     * @param expirationDateInMillis Expiration date to set to the target share.
     * A negative value clears the current expiration date.
     * Zero value (start-of-epoch) results in no update done on
     * the expiration date.
     */
    fun setExpirationDate(expirationDateInMillis: Long) {
        mExpirationDateInMillis = expirationDateInMillis
    }

    /**
     * Set permissions to update in Share resource.
     *
     * @param permissions Permissions to set to the target share.
     * Values <= 0 result in no update applied to the permissions.
     */
    fun setPermissions(permissions: Int) {
        mPermissions = permissions
    }

    /**
     * Enable upload permissions to update in Share resource.
     *
     * @param publicUpload Upload permission to set to the target share.
     * Null results in no update applied to the upload permission.
     */
    fun setPublicUpload(publicUpload: Boolean?) {
        mPublicUpload = publicUpload
    }

    override fun run(client: OwnCloudClient): RemoteOperationResult<ShareParserResult> {
        var result: RemoteOperationResult<ShareParserResult>

        try {
            val formBodyBuilder = FormBody.Builder()

            // Parameters to update
            if (mName != null) {
                formBodyBuilder.add(PARAM_NAME, mName!!)
            }

            if (mExpirationDateInMillis < 0) {
                // clear expiration date
                formBodyBuilder.add(PARAM_EXPIRATION_DATE, "")

            } else if (mExpirationDateInMillis > 0) {
                // set expiration date
                val dateFormat = SimpleDateFormat(FORMAT_EXPIRATION_DATE, Locale.GERMAN)
                val expirationDate = Calendar.getInstance()
                expirationDate.timeInMillis = mExpirationDateInMillis
                val formattedExpirationDate = dateFormat.format(expirationDate.time)
                formBodyBuilder.add(PARAM_EXPIRATION_DATE, formattedExpirationDate)
            } // else, ignore - no update

            if (mPublicUpload != null) {
                formBodyBuilder.add(PARAM_PUBLIC_UPLOAD, java.lang.Boolean.toString(mPublicUpload!!))
            }

            // IMPORTANT: permissions parameter needs to be updated after mPublicUpload parameter,
            // otherwise they would be set always as 1 (READ) in the server when mPublicUpload was updated
            if (mPermissions > 0) {
                // set permissions
                formBodyBuilder.add(PARAM_PERMISSIONS, Integer.toString(mPermissions))
            }

            val requestUri = client.baseUri
            val uriBuilder = requestUri.buildUpon()
            uriBuilder.appendEncodedPath(ShareUtils.SHARING_API_PATH)
            uriBuilder.appendEncodedPath(java.lang.Long.toString(mRemoteId))

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
                parser.setOwnCloudVersion(client.ownCloudVersion)
                parser.setServerBaseUri(client.baseUri)
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

    private fun isSuccess(status: Int): Boolean {
        return status == HttpConstants.HTTP_OK
    }

    companion object {

        private val TAG = GetRemoteShareOperation::class.java.simpleName

        private val PARAM_NAME = "name"
        private val PARAM_PASSWORD = "password"
        private val PARAM_EXPIRATION_DATE = "expireDate"
        private val PARAM_PERMISSIONS = "permissions"
        private val PARAM_PUBLIC_UPLOAD = "publicUpload"
        private val FORMAT_EXPIRATION_DATE = "yyyy-MM-dd"
        private val ENTITY_CONTENT_TYPE = "application/x-www-form-urlencoded"
        private val ENTITY_CHARSET = "UTF-8"
    }
}