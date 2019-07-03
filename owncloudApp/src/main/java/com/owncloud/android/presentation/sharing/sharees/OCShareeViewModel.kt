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

package com.owncloud.android.presentation.sharing.sharees

import android.accounts.Account
import android.content.Context
import androidx.lifecycle.ViewModel
import com.owncloud.android.data.common.Resource
import com.owncloud.android.data.sharing.sharees.ShareeRepository
import com.owncloud.android.data.sharing.sharees.datasources.OCRemoteShareesDataSource
import com.owncloud.android.domain.sharing.sharees.OCShareeRepository
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import org.json.JSONObject

class OCShareeViewModel(
    context: Context,
    account: Account,
    val shareeRepository: ShareeRepository = OCShareeRepository(
        remoteSharesDataSource = OCRemoteShareesDataSource(
            OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(
                OwnCloudAccount(account, context),
                context
            )
        )
    )
) : ViewModel() {
    fun getSharees(searchString: String, page: Int, perPage: Int): Resource<ArrayList<JSONObject>> {
        return shareeRepository.getSharees(searchString, page, perPage)
    }
}
