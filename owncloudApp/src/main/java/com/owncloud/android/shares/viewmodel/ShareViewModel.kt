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
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.content.Context
import com.owncloud.android.Resource
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.datasources.OCRemoteSharesDataSource
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.repository.OCShareRepository
import com.owncloud.android.shares.repository.ShareRepository

/**
 * View Model to keep a reference to the share repository and an up-to-date list of a shares
 */
class ShareViewModel(
    application: Application,
    context: Context,
    val client: OwnCloudClient,
    val filePath: String,
    shareTypes: List<ShareType>
) : AndroidViewModel(application) {

    private val shareRepository: ShareRepository
    val sharesForFile: LiveData<Resource<List<OCShare>>>

    init {
        val shareDao = OwncloudDatabase.getDatabase(context).shareDao()

        shareRepository = OCShareRepository(
            shareDao,
            OCRemoteSharesDataSource(client)
        )

        sharesForFile = shareRepository.getSharesForFileAsLiveData()

        shareRepository.loadSharesForFile(filePath, client.account.name, shareTypes, false, false)
    }
}
