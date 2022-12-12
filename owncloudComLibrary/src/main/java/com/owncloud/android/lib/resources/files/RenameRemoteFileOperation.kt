/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2021 ownCloud GmbH.
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
import com.owncloud.android.lib.common.http.methods.webdav.MoveMethod
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.isOneOf
import timber.log.Timber
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Remote operation performing the rename of a remote file or folder in the ownCloud server.
 *
 * @author David A. Velasco
 * @author masensio
 */
class RenameRemoteFileOperation(
    private val oldName: String,
    private val oldRemotePath: String,
    private val newName: String,
    isFolder: Boolean,
) : RemoteOperation<Unit>() {

    private var newRemotePath: String

    init {
        var parent = (File(oldRemotePath)).parent ?: throw IllegalArgumentException()
        if (!parent.endsWith(File.separator)) {
            parent = parent.plus(File.separator)
        }
        newRemotePath = parent.plus(newName)
        if (isFolder) {
            newRemotePath.plus(File.separator)
        }
    }

    override fun run(client: OwnCloudClient): RemoteOperationResult<Unit> {
        var result: RemoteOperationResult<Unit>
        try {
            if (newName == oldName) {
                return RemoteOperationResult<Unit>(ResultCode.OK)
            }

            if (targetPathIsUsed(client)) {
                return RemoteOperationResult<Unit>(ResultCode.INVALID_OVERWRITE)
            }

            val moveMethod: MoveMethod = MoveMethod(
                url = URL(client.userFilesWebDavUri.toString() + WebdavUtils.encodePath(oldRemotePath)),
                destinationUrl = client.userFilesWebDavUri.toString() + WebdavUtils.encodePath(newRemotePath),
            ).apply {
                setReadTimeout(RENAME_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                setConnectionTimeout(RENAME_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            }
            val status = client.executeHttpMethod(moveMethod)

            result = if (isSuccess(status)) {
                RemoteOperationResult<Unit>(ResultCode.OK)
            } else {
                RemoteOperationResult<Unit>(moveMethod)
            }

            Timber.i("Rename $oldRemotePath to $newRemotePath: ${result.logMessage}")
            client.exhaustResponse(moveMethod.getResponseBodyAsStream())
            return result
        } catch (exception: Exception) {
            result = RemoteOperationResult<Unit>(exception)
            Timber.e(exception, "Rename $oldRemotePath to $newName: ${result.logMessage}")
            return result
        }
    }

    /**
     * Checks if a file with the new name already exists.
     *
     * @return 'True' if the target path is already used by an existing file.
     */
    private fun targetPathIsUsed(client: OwnCloudClient): Boolean {
        val checkPathExistenceRemoteOperation = CheckPathExistenceRemoteOperation(newRemotePath, false)
        val exists = checkPathExistenceRemoteOperation.execute(client)
        return exists.isSuccess
    }

    private fun isSuccess(status: Int) = status.isOneOf(HttpConstants.HTTP_CREATED, HttpConstants.HTTP_NO_CONTENT)

    companion object {
        private const val RENAME_READ_TIMEOUT = 10_000L
        private const val RENAME_CONNECTION_TIMEOUT = 5_000L
    }
}
