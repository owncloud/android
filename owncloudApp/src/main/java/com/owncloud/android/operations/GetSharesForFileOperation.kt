/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2019 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.operations.common.SyncOperation

/**
 * Provide a list shares for a specific file.
 */
class GetSharesForFileOperation
/**
 * Constructor
 *
 * @param path      Path to file or folder
 * @param reshares  If set to false (default), only shares from the current user are returned
 * If set to true, all shares from the given file are returned
 * @param subfiles  If set to false (default), lists only the folder being shared
 * If set to true, all shared files within the folder are returned.
 */
    (private val mPath: String, private val mReshares: Boolean, private val mSubfiles: Boolean) :
    SyncOperation<ShareParserResult>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<ShareParserResult> {
        val operation = GetRemoteSharesForFileOperation(
            mPath,
            mReshares, mSubfiles
        )

        val result = operation.execute(client)

        if (result.isSuccess) {

            // Update DB with the response
            Log_OC.d(TAG, "File = " + mPath + " Share list size  " + result.data.shares.size)
            storageManager.saveShares(result.data.shares)

        } else if (result.code == RemoteOperationResult.ResultCode.SHARE_NOT_FOUND) {
            // no share on the file - remove local shares
            storageManager.removeSharesForFile(mPath)

        }

        return result
    }

    companion object {

        private val TAG = GetSharesForFileOperation::class.java.simpleName
    }
}