/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.domain.authentication.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.authentication.AuthenticationRepository
import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationInfo
import com.owncloud.android.domain.server.model.ServerInfo

class LoginOAuthAsyncUseCase(
    private val authenticationRepository: AuthenticationRepository
) : BaseUseCaseWithResult<String, LoginOAuthAsyncUseCase.Params>() {

    override fun run(params: Params): String {
        require(params.serverInfo != null) { "Invalid server info" }
        require(params.authTokenType.isNotEmpty()) { "Invalid authorization token type" }
        require(params.accessToken.isNotEmpty()) { "Invalid access token" }
        require(params.refreshToken.isNotEmpty()) { "Invalid refresh token" }

        val accountName = authenticationRepository.loginOAuth(
            params.serverInfo,
            params.username,
            params.authTokenType,
            params.accessToken,
            params.refreshToken,
            params.scope,
            params.updateAccountWithUsername,
            params.clientRegistrationInfo
        )

        return accountName
    }

    data class Params(
        val serverInfo: ServerInfo?,
        val username: String,
        val authTokenType: String,
        val accessToken: String,
        val refreshToken: String,
        val scope: String?,
        val updateAccountWithUsername: String?,
        val clientRegistrationInfo: ClientRegistrationInfo?
    )
}
