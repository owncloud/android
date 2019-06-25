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

package com.owncloud.android.sharees.domain

import androidx.lifecycle.MutableLiveData
import com.owncloud.android.AppExecutors
import com.owncloud.android.sharees.data.datasources.RemoteShareesDataSource
import com.owncloud.android.sharees.data.ShareeRepository
import com.owncloud.android.vo.Resource
import org.json.JSONObject

class OCShareeRepository(
    private val appExecutors: AppExecutors = AppExecutors(),
    private val remoteSharesDataSource: RemoteShareesDataSource
) : ShareeRepository {

    override fun getSharees(
        searchString: String,
        page: Int,
        perPage: Int
    ): MutableLiveData<Resource<ArrayList<JSONObject>>> {
        val result = MutableLiveData<Resource<ArrayList<JSONObject>>>()
        result.postValue(Resource.loading())

        appExecutors.networkIO().execute {
            // Perform network operation
            val remoteOperationResult = remoteSharesDataSource.getSharees(
                searchString, page, perPage
            )

            if (remoteOperationResult.isSuccess) {
                val newShareesFromServer = remoteOperationResult.data
                result.postValue(Resource.success(newShareesFromServer))
            } else {
                result.postValue(
                    Resource.error(
                        remoteOperationResult.code,
                        msg = remoteOperationResult.httpPhrase,
                        exception = remoteOperationResult.exception
                    )
                )
            }
        }

        return result
    }
}
