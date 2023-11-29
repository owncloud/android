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

import at.bitfire.dav4jvm.PropertyRegistry
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.http.HttpConstants.HTTP_MULTI_STATUS
import com.owncloud.android.lib.common.http.HttpConstants.HTTP_OK
import com.owncloud.android.lib.common.http.methods.webdav.DavConstants
import com.owncloud.android.lib.common.http.methods.webdav.DavUtils
import com.owncloud.android.lib.common.http.methods.webdav.PropfindMethod
import com.owncloud.android.lib.common.http.methods.webdav.properties.OCShareTypes
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.isOneOf
import timber.log.Timber
import java.net.URL

/**
 * Remote operation performing the read of remote file or folder in the ownCloud server.
 *
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 */
class ReadRemoteFolderOperation(
    val remotePath: String,
    val spaceWebDavUrl: String? = null,
) : RemoteOperation<ArrayList<RemoteFile>>() {

    /**
     * Performs the read operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    override fun run(client: OwnCloudClient): RemoteOperationResult<ArrayList<RemoteFile>> {
        try {
            PropertyRegistry.register(OCShareTypes.Factory())

            val propfindMethod = PropfindMethod(
                getFinalWebDavUrl(),
                DavConstants.DEPTH_1,
                DavUtils.allPropSet
            )

            val status = client.executeHttpMethod(propfindMethod)

            if (isSuccess(status)) {
                val mFolderAndFiles = ArrayList<RemoteFile>()

                val remoteFolder = RemoteFile.getRemoteFileFromDav(
                    davResource = propfindMethod.root!!,
                    userId = AccountUtils.getUserId(mAccount, mContext),
                    userName = mAccount.name,
                    spaceWebDavUrl = spaceWebDavUrl,
                )
                mFolderAndFiles.add(remoteFolder)

                // loop to update every child
                propfindMethod.members.forEach { resource ->
                    val remoteFile = RemoteFile.getRemoteFileFromDav(
                        davResource = resource,
                        userId = AccountUtils.getUserId(mAccount, mContext),
                        userName = mAccount.name,
                        spaceWebDavUrl = spaceWebDavUrl,
                    )
                    mFolderAndFiles.add(remoteFile)
                }

                // Result of the operation
                return RemoteOperationResult<ArrayList<RemoteFile>>(ResultCode.OK).apply {
                    data = mFolderAndFiles
                    Timber.i("Synchronized $remotePath with ${mFolderAndFiles.size} files. - HTTP status code: $status")
                }
            } else { // synchronization failed
                return RemoteOperationResult<ArrayList<RemoteFile>>(propfindMethod).also {
                    Timber.w("Synchronized $remotePath ${it.logMessage}")
                }
            }
        } catch (e: Exception) {
            return RemoteOperationResult<ArrayList<RemoteFile>>(e).also {
                Timber.e(it.exception, "Synchronized $remotePath")
            }
        }
    }

    private fun getFinalWebDavUrl(): URL {
        val baseWebDavUrl = spaceWebDavUrl ?: client.userFilesWebDavUri.toString()

        return URL(baseWebDavUrl + WebdavUtils.encodePath(remotePath))
    }

    private fun isSuccess(status: Int): Boolean = status.isOneOf(HTTP_OK, HTTP_MULTI_STATUS)
}
