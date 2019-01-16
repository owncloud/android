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

package com.owncloud.android.shares.viewmodel

import android.app.Application
import android.arch.lifecycle.*
import com.owncloud.android.Resource
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.datasources.OCLocalSharesDataSource
import com.owncloud.android.shares.datasources.OCRemoteSharesDataSource
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.repository.OCShareRepository
import com.owncloud.android.shares.repository.ShareRepository
import com.owncloud.android.utils.distinctUntilChanged
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * View Model to keep a reference to the share repository and an up-to-date list of a shares
 */
class ShareViewModel(
    application: Application,
    private val client: OwnCloudClient,
    private val filePath: String,
    private val shareTypes: List<ShareType>
) : AndroidViewModel(application), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val shareRepository: ShareRepository

    val sharesForFile: LiveData<Resource<List<OCShare>>>

    init {
        val shareDao = OwncloudDatabase.getDatabase(application).shareDao()

        shareRepository = OCShareRepository(
            OCLocalSharesDataSource(shareDao),
            OCRemoteSharesDataSource(client)
        )

        /**
         * Observe two different livedata objects and react on change events from them
         * - Shares livedata from Room to detect changes in database
         * - Errors livedata from remote operations
         */
        sharesForFile = MediatorLiveData<Resource<List<OCShare>>>().apply {
            val database = shareRepository.getSharesForFileAsLiveData(
                filePath, client.account.name, shareTypes
            )

            addSource(database.distinctUntilChanged()) { shares ->
                this.value = Resource.success(shares)
            }

            addSource(shareRepository.errors) { error ->
                this.value = error
            }
        }
    }

    override fun onCleared() {
        job.cancel()
        super.onCleared()
    }

    fun fetchShares() {
        launch {
            withContext(Dispatchers.IO) {
                shareRepository.loadSharesForFile(
                    filePath, client.account.name, shareTypes, true, false
                )
            }
        }
    }
}
