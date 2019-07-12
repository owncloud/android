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

package com.owncloud.android.domain.sharing.sharees

import com.owncloud.android.data.DataResult
import com.owncloud.android.data.sharing.sharees.ShareeRepository
import com.owncloud.android.data.sharing.sharees.datasources.RemoteShareesDataSource
import org.json.JSONObject

class OCShareeRepository(
    private val remoteSharesDataSource: RemoteShareesDataSource
) : ShareeRepository {

    override fun getSharees(
        searchString: String,
        page: Int,
        perPage: Int
    ): DataResult<ArrayList<JSONObject>> {
        val remoteOperationResult = remoteSharesDataSource.getSharees(
            searchString, page, perPage
        )
        return if (remoteOperationResult.isSuccess)
            DataResult.success(remoteOperationResult.data) else
            DataResult.error(
                remoteOperationResult.code,
                msg = remoteOperationResult.httpPhrase,
                exception = remoteOperationResult.exception
            )
    }
}
