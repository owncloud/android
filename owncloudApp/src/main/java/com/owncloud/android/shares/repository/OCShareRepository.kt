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

package com.owncloud.android.shares.repository

import android.arch.lifecycle.LiveData
import com.owncloud.android.NetworkBoundResource
import com.owncloud.android.Resource
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.datasources.LocalSharesDataSource
import com.owncloud.android.shares.datasources.RemoteSharesDataSource
import com.owncloud.android.shares.db.OCShare
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class OCShareRepository(
    private val localSharesDataSource: LocalSharesDataSource,
    private val remoteShareSharesDataSource: RemoteSharesDataSource
) : ShareRepository, CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun loadSharesForFile(
        filePath: String,
        accountName: String,
        shareTypes: List<ShareType>,
        reshares: Boolean,
        subfiles: Boolean
    ): LiveData<Resource<List<OCShare>>> {

        return object : NetworkBoundResource<List<OCShare>, ShareParserResult>() {

            override fun saveCallResult(item: ShareParserResult) {
                val localShares = item.shares.map { remoteShare ->
                    OCShare(remoteShare).also { it.accountOwner = accountName }
                }

                launch {
                    withContext(Dispatchers.IO) {
                        localSharesDataSource.insert(localShares)
                    }
                }
            }

            override fun loadFromDb(): LiveData<List<OCShare>> {
                return localSharesDataSource.getSharesForFileAsLiveData(filePath, accountName, shareTypes)
            }

            override fun createCall() = remoteShareSharesDataSource.getSharesForFile(filePath, reshares, subfiles)

        }.asLiveData()
    }
}
