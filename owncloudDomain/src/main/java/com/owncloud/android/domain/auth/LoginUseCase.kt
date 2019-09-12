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

package com.owncloud.android.domain.auth

import android.content.Context
import android.net.Uri
import com.owncloud.android.data.auth.datasources.OCRemoteAuthDataSource
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials

class LoginUseCase(
    context: Context,
    private val baseUrl: String,
    private val ocAuthRepository: OCAuthRepository = OCAuthRepository(
        remoteAuthDataSource = OCRemoteAuthDataSource(
            OwnCloudClientFactory.createOwnCloudClient(
                Uri.parse(baseUrl),
                context,
                true
            )
        )
    )
) : BaseUseCase<Unit, LoginUseCase.Params>() {

    override fun run(params: Params): UseCaseResult<Unit> {
        ocAuthRepository.login(baseUrl, params.ownCloudCredentials)
        return UseCaseResult.success()
    }

    data class Params(
        val ownCloudCredentials: OwnCloudCredentials
    )
}
