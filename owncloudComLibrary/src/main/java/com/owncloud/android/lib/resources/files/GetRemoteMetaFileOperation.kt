/* ownCloud Android Library is available under MIT license
*   Copyright (C) 2023 ownCloud GmbH.
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

import at.bitfire.dav4jvm.PropertyRegistry
import at.bitfire.dav4jvm.property.OCId
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.webdav.DavConstants
import com.owncloud.android.lib.common.http.methods.webdav.PropfindMethod
import com.owncloud.android.lib.common.http.methods.webdav.properties.OCFileId
import com.owncloud.android.lib.common.http.methods.webdav.properties.OCMetaPathForUser
import com.owncloud.android.lib.common.http.methods.webdav.properties.OCSpaceId
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import timber.log.Timber
import java.net.URL
import java.util.concurrent.TimeUnit

class GetRemoteMetaFileOperation(val fileId: String) : RemoteOperation<RemoteMetaFile>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<RemoteMetaFile> {
        PropertyRegistry.register(OCMetaPathForUser.Factory())
        PropertyRegistry.register(OCFileId.Factory())
        PropertyRegistry.register(OCSpaceId.Factory())

        val stringUrl = "${client.baseUri}$META_PATH$fileId"
        return try {

            val propFindMethod =
                PropfindMethod(URL(stringUrl), DavConstants.DEPTH_0,
                    arrayOf(OCMetaPathForUser.NAME, OCId.NAME, OCFileId.NAME, OCSpaceId.NAME)
                ).apply {
                    setReadTimeout(TIMEOUT, TimeUnit.SECONDS)
                    setConnectionTimeout(TIMEOUT, TimeUnit.SECONDS)
                }

            val status = client.executeHttpMethod(propFindMethod)
            if (isSuccess(status)) RemoteOperationResult<RemoteMetaFile>(RemoteOperationResult.ResultCode.OK).apply {
                val remoteMetaFile = RemoteMetaFile()
                propFindMethod.root?.properties?.let { properties ->
                   properties.forEach { property ->
                       when (property) {
                           is OCMetaPathForUser -> {
                               remoteMetaFile.metaPathForUser = property.path
                           }
                           is OCId -> {
                               remoteMetaFile.id = property.id
                           }
                           is OCFileId -> {
                               remoteMetaFile.fileId = property.fileId
                           }
                           is OCSpaceId -> {
                               remoteMetaFile.spaceId = property.spaceId
                           }
                       }
                   }
                }
                data = remoteMetaFile
            }
            else RemoteOperationResult<RemoteMetaFile>(propFindMethod)
        } catch (e: Exception) {
            Timber.e(e, "Exception while getting remote meta file")
            RemoteOperationResult<RemoteMetaFile>(e)
        }
    }

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_OK || status == HttpConstants.HTTP_MULTI_STATUS

    companion object {
        private const val META_PATH = "/remote.php/dav/meta/"
        private const val TIMEOUT = 10_000L
    }

}
