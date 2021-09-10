/* ownCloud Android Library is available under MIT license
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
package com.owncloud.android.lib.resources.files

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.webdav.DavUtils.allPropset
import com.owncloud.android.lib.common.http.methods.webdav.PropfindMethod
import com.owncloud.android.lib.common.network.RedirectionPath
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import timber.log.Timber
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Operation to check the existence of a path in a remote server.
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Abel García de Prada
 *
 * @param remotePath      Path to append to the URL owned by the client instance.
 * @param isUserLogged    When `true`, the username won't be added at the end of the PROPFIND url since is not
 *                        needed to check user credentials
 */
class CheckPathExistenceRemoteOperation(
    val remotePath: String? = "",
    val isUserLogged: Boolean
) : RemoteOperation<Boolean>() {
    /**
     * Gets the sequence of redirections followed during the execution of the operation.
     *
     * @return Sequence of redirections followed, if any, or NULL if the operation was not executed.
     */
    var redirectionPath: RedirectionPath? = null
        private set

    override fun run(client: OwnCloudClient): RemoteOperationResult<Boolean> {
        val previousFollowRedirects = client.getFollowRedirects()
        return try {
            val stringUrl =
                if (isUserLogged) client.baseFilesWebDavUri.toString()
                else client.userFilesWebDavUri.toString() + WebdavUtils.encodePath(remotePath)

            val propFindMethod = PropfindMethod(URL(stringUrl), 0, allPropset).apply {
                setReadTimeout(TIMEOUT.toLong(), TimeUnit.SECONDS)
                setConnectionTimeout(TIMEOUT.toLong(), TimeUnit.SECONDS)
            }

            var status = client.executeHttpMethod(propFindMethod)
            if (previousFollowRedirects) {
                redirectionPath = client.followRedirection(propFindMethod)
                status = redirectionPath?.lastStatus!!
            }
            /* PROPFIND method
             * 404 NOT FOUND: path doesn't exist,
             * 207 MULTI_STATUS: path exists.
             */
            Timber.d(
                "Existence check for $stringUrl finished with HTTP status $status${if (!isSuccess(status)) "(FAIL)" else ""}"
            )
            if (isSuccess(status)) RemoteOperationResult<Boolean>(ResultCode.OK).apply { data = true }
            else RemoteOperationResult<Boolean>(propFindMethod).apply { data = false }

        } catch (e: Exception) {
            val result = RemoteOperationResult<Boolean>(e)
            Timber.e(
                e,
                "Existence check for ${client.userFilesWebDavUri}${WebdavUtils.encodePath(remotePath)} : ${result.logMessage}"
            )
            result
        } finally {
            client.setFollowRedirects(previousFollowRedirects)
        }
    }

    /**
     * @return 'True' if the operation was executed and at least one redirection was followed.
     */
    fun wasRedirected() = redirectionPath?.redirectionsCount ?: 0 > 0

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_OK || status == HttpConstants.HTTP_MULTI_STATUS

    companion object {
        /**
         * Maximum time to wait for a response from the server in milliseconds.
         */
        private const val TIMEOUT = 10000
    }
}
