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
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.webdav.DavConstants
import com.owncloud.android.lib.common.http.methods.webdav.DavUtils
import com.owncloud.android.lib.common.http.methods.webdav.PropfindMethod
import com.owncloud.android.lib.common.http.methods.webdav.properties.OCShareTypes
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
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
    val remotePath: String
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
                URL(client.userFilesWebDavUri.toString() + WebdavUtils.encodePath(remotePath)),
                DavConstants.DEPTH_1,
                DavUtils.allPropset
            )

            val status = client.executeHttpMethod(propfindMethod)

            if (isSuccess(status)) {
                val mFolderAndFiles = ArrayList<RemoteFile>()

                // parse data from remote folder
                mFolderAndFiles.add(RemoteFile(propfindMethod.root, AccountUtils.getUserId(mAccount, mContext)))

                // loop to update every child
                propfindMethod.members.forEach { resource ->
                    val file = RemoteFile(resource, AccountUtils.getUserId(mAccount, mContext))
                    mFolderAndFiles.add(file)
                }

                // Result of the operation
                return RemoteOperationResult<ArrayList<RemoteFile>>(ResultCode.OK).apply {
                    data = mFolderAndFiles
                    Timber.i("Synchronized $remotePath with ${mFolderAndFiles.size} files. ${this.logMessage}")
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

    private fun isSuccess(status: Int): Boolean =
        status == HttpConstants.HTTP_MULTI_STATUS || status == HttpConstants.HTTP_OK
}
