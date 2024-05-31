/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2024 ownCloud GmbH.
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
 */

package com.owncloud.android.lib.resources.files

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.http.HttpConstants.HTTP_MULTI_STATUS
import com.owncloud.android.lib.common.http.HttpConstants.HTTP_OK
import com.owncloud.android.lib.common.http.methods.webdav.DavConstants.DEPTH_0
import com.owncloud.android.lib.common.http.methods.webdav.DavUtils
import com.owncloud.android.lib.common.http.methods.webdav.PropfindMethod
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.isOneOf
import timber.log.Timber
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Remote operation performing the read a file from the ownCloud server.
 *
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 * @author Juan Carlos Garrote Gascón
 */

class ReadRemoteFileOperation(
    val remotePath: String,
    val spaceWebDavUrl: String? = null,
) : RemoteOperation<RemoteFile>() {

    /**
     * Performs the read operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    override fun run(client: OwnCloudClient): RemoteOperationResult<RemoteFile> {
        try {
            if (client.account == null) {
                throw AccountUtils.AccountNotFoundException()
            }
            val propFind = PropfindMethod(
                url = getFinalWebDavUrl(),
                depth = DEPTH_0,
                propertiesToRequest = DavUtils.allPropSet
            ).apply {
                setReadTimeout(SYNC_READ_TIMEOUT, TimeUnit.SECONDS)
                setConnectionTimeout(SYNC_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            }

            val status = client.executeHttpMethod(propFind)
            Timber.i("Read remote file $remotePath with status ${propFind.statusCode}")

            return if (isSuccess(status)) {
                val remoteFile = RemoteFile.getRemoteFileFromDav(
                    davResource = propFind.root!!,
                    userId = AccountUtils.getUserId(mAccount, mContext),
                    userName = mAccount.name,
                    spaceWebDavUrl = spaceWebDavUrl,
                )

                RemoteOperationResult<RemoteFile>(RemoteOperationResult.ResultCode.OK).apply {
                    data = remoteFile
                }
            } else {
                RemoteOperationResult<RemoteFile>(propFind).also {
                    client.exhaustResponse(propFind.getResponseBodyAsStream())
                }
            }
        } catch (exception: Exception) {
            return RemoteOperationResult(exception)
        }
    }

    private fun getFinalWebDavUrl(): URL {
        val baseWebDavUrl = spaceWebDavUrl ?: client.userFilesWebDavUri.toString()

        return URL(baseWebDavUrl + WebdavUtils.encodePath(remotePath))
    }

    private fun isSuccess(status: Int) = status.isOneOf(HTTP_MULTI_STATUS, HTTP_OK)

    companion object {
        private const val SYNC_READ_TIMEOUT = 40_000L
        private const val SYNC_CONNECTION_TIMEOUT = 5_000L
    }
}
