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

import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.operations.common.SyncOperation
import com.owncloud.android.shares.ShareRepository
import com.owncloud.android.shares.ShareRepositoryImpl
import com.owncloud.android.shares.datasources.OCRemoteSharesDataSource

/**
 * Provide a list shares for a specific file.
 */
class GetSharesForFileOperation(
    private val path: String,          // Path to file or folder
    private val reshares: Boolean,     // If set to false (default), only shares from the current user are returned
    // If set to true, all shares from the given file are returned
    private val subfiles: Boolean      // If set to false (default), lists only the folder being shared
    // If set to true, all shared files within the folder are returned.
) : SyncOperation<ShareParserResult>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<ShareParserResult>? {

        val shareDao = OwncloudDatabase.getDatabase(mContext).shareDao()
        val shareRepository: ShareRepository = ShareRepositoryImpl(
            shareDao,
            OCRemoteSharesDataSource(client)
        )

        // TODO Review this, call it directly from ShareViewModel
        try {
            shareRepository.fetchSharesForFileFromServer(path, reshares, subfiles)
        } catch (throwable: Throwable) {
            println(throwable.cause.toString())
        }

        //        GetRemoteSharesForFileOperation operation = new GetRemoteSharesForFileOperation(mPath,
        //                mReshares, mSubfiles);
        //
        //        RemoteOperationResult<ShareParserResult> result = operation.execute(client);
        //
        //        if (result.isSuccess()) {
        //
        //            // Update DB with the response
        //            Log_OC.d(TAG, "File = " + mPath + " OCShare list size  " + result.getData().getShares().size());
        //            getStorageManager().saveShares(result.getData().getShares());
        //
        //        } else if (result.getCode() == RemoteOperationResult.ResultCode.SHARE_NOT_FOUND) {
        //            // no share on the file - remove local shares
        //            getStorageManager().removeSharesForFile(mPath);
        //
        //        }

        return null
    }

    companion object {
        private val TAG = GetSharesForFileOperation::class.java.simpleName
    }
}