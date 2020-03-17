/* ownCloud Android Library is available under MIT license
 *   @author David A. Velasco
 *   @author David González Verdugo
 *   @author Christian Schabesberger
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
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.ArrayList

class ShareToRemoteOperationResultParser(private var shareXmlParser: ShareXMLParser?) {
    var oneOrMoreSharesRequired = false
    var ownCloudVersion: OwnCloudVersion? = null
    var serverBaseUri: Uri? = null

    fun parse(serverResponse: String?): RemoteOperationResult<ShareParserResult> {
        if (serverResponse.isNullOrEmpty()) {
            return RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE)
        }

        var result: RemoteOperationResult<ShareParserResult>
        val resultData: List<RemoteShare>?

        try {
            // Parse xml response and obtain the list of shares
            val byteArrayServerResponse = ByteArrayInputStream(serverResponse.toByteArray())
            if (shareXmlParser == null) {
                Timber.w("No ShareXmlParser provided, creating new instance")
                shareXmlParser = ShareXMLParser()
            }
            val shares = shareXmlParser?.parseXMLResponse(byteArrayServerResponse)

            when {
                shareXmlParser?.isSuccess!! -> {
                    if (!shares.isNullOrEmpty() || !oneOrMoreSharesRequired) {
                        result = RemoteOperationResult(RemoteOperationResult.ResultCode.OK)

                        resultData = shares?.map { share ->
                            if (share.shareType != ShareType.PUBLIC_LINK ||
                                share.shareLink.isNotEmpty() ||
                                share.token.isEmpty()
                            ) {
                                return@map share
                            }

                            if (serverBaseUri != null) {
                                val sharingLinkPath = ShareUtils.SHARING_LINK_PATH
                                share.shareLink = serverBaseUri.toString() + sharingLinkPath + share.token
                            } else {
                                Timber.e("Couldn't build link for public share :(")
                            }

                            share
                        }

                        if (resultData != null) {
                            result.setData(ShareParserResult(ArrayList(resultData.toMutableList())))
                        }

                    } else {
                        result = RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE)
                        Timber.e("Successful status with no share in the response")
                    }
                }
                shareXmlParser?.isWrongParameter!! -> {
                    result = RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_WRONG_PARAMETER)
                    result.httpPhrase = shareXmlParser?.message
                }
                shareXmlParser?.isNotFound!! -> {
                    result = RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND)
                    result.httpPhrase = shareXmlParser?.message
                }
                shareXmlParser?.isForbidden!! -> {
                    result = RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_FORBIDDEN)
                    result.httpPhrase = shareXmlParser?.message
                }
                else -> {
                    result = RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE)
                }
            }
        } catch (e: XmlPullParserException) {
            Timber.e(e, "Error parsing response from server")
            result = RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE)

        } catch (e: IOException) {
            Timber.e(e, "Error reading response from server")
            result = RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE)
        }

        return result
    }
}
