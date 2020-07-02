/* ownCloud Android Library is available under MIT license
 *   @author masensio
 *   @author David A. Velasco
 *   @author David González Verdugo
 *   Copyright (C) 2020 ownCloud GmbH
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
import com.owncloud.android.lib.common.http.methods.nonwebdav.PostMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.RemoteShare.Companion.INIT_EXPIRATION_DATE_IN_MILLIS
import okhttp3.FormBody
import timber.log.Timber
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Creates a new share.  This allows sharing with a user or group or as a link.
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 */

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
) : RemoteOperation<ShareParserResult>() {
    var name = "" // Name to set for the public link

    var password: String = "" // Password to set for the public link

    var expirationDateInMillis: Long = INIT_EXPIRATION_DATE_IN_MILLIS // Expiration date to set for the public link

    var publicUpload: Boolean = false // Upload permissions for the public link (only folders)

    var retrieveShareDetails = false // To retrieve more info about the just created share

    override fun run(client: OwnCloudClient): RemoteOperationResult<ShareParserResult> {
        var result: RemoteOperationResult<ShareParserResult>

        try {
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

            val requestUri = client.baseUri
            val uriBuilder = requestUri.buildUpon()
            uriBuilder.appendEncodedPath(ShareUtils.SHARING_API_PATH)

            val postMethod = PostMethod(URL(uriBuilder.build().toString()), formBodyBuilder.build())

            postMethod.setRequestHeader(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.CONTENT_TYPE_URLENCODED_UTF8)
            postMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)

            val status = client.executeHttpMethod(postMethod)

            val parser = ShareToRemoteOperationResultParser(
                ShareXMLParser()
            )

            if (isSuccess(status)) {
                parser.oneOrMoreSharesRequired = true
                parser.ownCloudVersion = client.ownCloudVersion
                parser.serverBaseUri = client.baseUri
                result = parser.parse(postMethod.getResponseBodyAsString())

                if (result.isSuccess && retrieveShareDetails) {
                    // retrieve more info - POST only returns the index of the new share
                    val emptyShare = result.data.shares[0]
                    val getInfo = GetRemoteShareOperation(
                        emptyShare.id
                    )
                    result = getInfo.execute(client)
                }

            } else {
                result = parser.parse(postMethod.getResponseBodyAsString())
            }

        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while Creating New Share")
        }

        return result
    }

    private fun isSuccess(status: Int): Boolean = status == HttpConstants.HTTP_OK

    companion object {
        private const val PARAM_NAME = "name"
        private const val PARAM_PASSWORD = "password"
        private const val PARAM_EXPIRATION_DATE = "expireDate"
        private const val PARAM_PUBLIC_UPLOAD = "publicUpload"
        private const val PARAM_PATH = "path"
        private const val PARAM_SHARE_TYPE = "shareType"
        private const val PARAM_SHARE_WITH = "shareWith"
        private const val PARAM_PERMISSIONS = "permissions"
        private const val FORMAT_EXPIRATION_DATE = "yyyy-MM-dd"
    }
}
