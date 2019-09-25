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

package com.owncloud.android.domain.sharing.shares.usecases

import android.accounts.Account
import android.content.Context
import androidx.lifecycle.LiveData
import com.owncloud.android.data.sharing.shares.ShareRepository
import com.owncloud.android.data.sharing.shares.datasources.OCLocalShareDataSource
import com.owncloud.android.data.sharing.shares.datasources.OCRemoteShareDataSource
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.sharing.shares.OCShareRepository
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory

class GetSharesAsLiveDataUseCase(
    context: Context,
    account: Account,
    private val shareRepository: ShareRepository = OCShareRepository(
        localShareDataSource = OCLocalShareDataSource(context),
        remoteShareDataSource = OCRemoteShareDataSource(
            OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(
                OwnCloudAccount(account, context),
                context
            )
        )
    )
) : BaseUseCase<LiveData<List<OCShareEntity>>, GetSharesAsLiveDataUseCase.Params>() {

    override fun run(params: Params): LiveData<List<OCShareEntity>> {
        return shareRepository.getSharesAsLiveData(
            params.filePath,
            params.accountName
        )
    }

    data class Params(
        val filePath: String,
        val accountName: String
    )
}
