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

package com.owncloud.android.domain.capabilities.usecases

import android.accounts.Account
import android.content.Context
import com.owncloud.android.data.capabilities.CapabilityRepository
import com.owncloud.android.data.capabilities.datasources.OCLocalCapabilitiesDataSource
import com.owncloud.android.data.capabilities.datasources.OCRemoteCapabilitiesDataSource
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.capabilities.OCCapabilityRepository
import com.owncloud.android.domain.sharing.shares.usecases.BaseUseCase
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory

class GetStoredCapabilitiesUseCase(
    context: Context,
    account: Account,
    private val capabilityRepository: CapabilityRepository = OCCapabilityRepository(
        localCapabilitiesDataSource = OCLocalCapabilitiesDataSource(context),
        remoteCapabilitiesDataSource = OCRemoteCapabilitiesDataSource(
            OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(
                OwnCloudAccount(account, context),
                context
            )
        )
    )
) : BaseUseCase<OCCapabilityEntity, GetStoredCapabilitiesUseCase.Params>() {
    override fun run(params: Params): UseCaseResult<OCCapabilityEntity> {
        capabilityRepository.getStoredCapabilities(
            params.accountName
        ).also { storedCapabilities ->
            return UseCaseResult.success(storedCapabilities) // Always successful here, data comes from database
        }
    }

    data class Params(
        val accountName: String
    )
}
