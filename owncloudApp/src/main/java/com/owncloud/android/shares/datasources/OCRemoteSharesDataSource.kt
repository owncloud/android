/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.shares.datasources

import android.app.Application
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.shares.db.OCShare
import java.util.*

class OCRemoteSharesDataSource(private val client: OwnCloudClient) : RemoteSharesDataSource {
    override suspend fun getShares(application: Application): List<OCShare> {
        return listOf() //TODO
    }

    override fun getSharesForAFile(
        path: String,
        reshares: Boolean,
        subfiles: Boolean
    ): ArrayList<RemoteShare> {

        val operation = GetRemoteSharesForFileOperation(path, reshares, subfiles)

        val result: RemoteOperationResult<ShareParserResult> = operation.execute(client)

        val exception: Exception = Exception(result.exception)

        return if (result.isSuccess()) result.data.shares else throw exception
    }
}