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

package com.owncloud.android.lib.resources.files;

import android.net.Uri
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants.HTTP_NO_CONTENT
import com.owncloud.android.lib.common.http.HttpConstants.HTTP_OK
import com.owncloud.android.lib.common.http.methods.nonwebdav.DeleteMethod
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.isOneOf
import timber.log.Timber
import java.net.URL

/**
 * Remote operation performing the removal of a remote file or folder in the ownCloud server.
 *
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 * @author Abel García de Prada
 */
open class RemoveRemoteFileOperation(
    private val mRemotePath: String
) : RemoteOperation<Unit>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<Unit> {
        var result: RemoteOperationResult<Unit>
        try {
            val srcWebDavUri = getSrcWebDavUriForClient(client)
            val deleteMethod = DeleteMethod(
                URL(srcWebDavUri.toString() + WebdavUtils.encodePath(mRemotePath))
            )
            val status = client.executeHttpMethod(deleteMethod)
            result = if (isSuccess(status)) RemoteOperationResult<Unit>(ResultCode.OK) else RemoteOperationResult<Unit>(deleteMethod)
            Timber.i("Remove $mRemotePath: ${result.logMessage}")
        } catch (e: Exception) {
            result = RemoteOperationResult<Unit>(e)
            Timber.e(e, "Remove $mRemotePath: ${result.logMessage}")
        }
        return result
    }

    /**
     * For standard removals, we will use [OwnCloudClient.getUserFilesWebDavUri].
     * In case we need a different source Uri, override this method.
     */
    open fun getSrcWebDavUriForClient(client: OwnCloudClient): Uri = client.userFilesWebDavUri

    private fun isSuccess(status: Int) = status.isOneOf(HTTP_OK, HTTP_NO_CONTENT)
}
