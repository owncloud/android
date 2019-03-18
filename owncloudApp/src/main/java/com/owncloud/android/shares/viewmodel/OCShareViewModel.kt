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

import android.accounts.Account
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.datasource.OCLocalSharesDataSource
import com.owncloud.android.shares.datasource.OCRemoteSharesDataSource
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.repository.OCShareRepository
import com.owncloud.android.shares.repository.ShareRepository
import com.owncloud.android.testing.OpenForTesting
import com.owncloud.android.vo.Resource

/**
 * View Model to keep a reference to the share repository and an up-to-date list of a shares
 */
@OpenForTesting
class OCShareViewModel(
        val account: Account,
        val filePath: String,
        shareTypes: List<ShareType>,
        val shareRepository: ShareRepository = OCShareRepository.create(
                localSharesDataSource = OCLocalSharesDataSource(),
                remoteSharesDataSource = OCRemoteSharesDataSource(
                        OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(
                                OwnCloudAccount(account, MainApp.getAppContext()),
                                MainApp.getAppContext()
                        )
                )
        )
) : ViewModel() {

    val sharesForFile: LiveData<Resource<List<OCShare>>> = shareRepository.loadSharesForFile(
            filePath, account.name, shareTypes, true, false
    )

    fun insertPublicShareForFile(
            filePath: String,
            name: String,
            password: String,
            expirationTimeInMillis: Long,
            uploadToFolderPermission: Boolean,
            permissions: Int
    ): LiveData<Resource<List<OCShare>>> {
        return shareRepository.insertPublicShareForFile(
                filePath, account.name, permissions, name, password, expirationTimeInMillis, uploadToFolderPermission
        )
    }
}
