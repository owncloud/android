/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2022 ownCloud GmbH.
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
package com.owncloud.android.domain.appregistry.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.appregistry.AppRegistryRepository
import com.owncloud.android.domain.capabilities.CapabilityRepository

class GetUrlToOpenInWebUseCase(
    private val capabilitiesRepository: CapabilityRepository,
    private val appRegistryRepository: AppRegistryRepository,
) : BaseUseCaseWithResult<String, GetUrlToOpenInWebUseCase.Params>() {

    override fun run(params: Params): String {
        val capabilities = capabilitiesRepository.getStoredCapabilities(params.accountName)
        val openInWebUrl = capabilities?.filesAppProviders?.openWebUrl

        requireNotNull(openInWebUrl)

        return appRegistryRepository.getUrlToOpenInWeb(
            accountName = params.accountName,
            openWebEndpoint = openInWebUrl,
            fileId = params.fileId,
            appName = params.appName,
        )
    }

    data class Params(
        val accountName: String,
        val fileId: String,
        val appName: String,
    )
}
