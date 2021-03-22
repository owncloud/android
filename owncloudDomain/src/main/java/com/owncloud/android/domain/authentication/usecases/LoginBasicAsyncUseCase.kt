/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
import com.owncloud.android.domain.server.model.ServerInfo

class LoginBasicAsyncUseCase(
    private val authenticationRepository: AuthenticationRepository
) : BaseUseCaseWithResult<String, LoginBasicAsyncUseCase.Params>() {

    override fun run(params: Params): String {
        require(params.serverInfo != null) { "Invalid server info" }
        require(params.username.isNotEmpty()) { "Invalid username" }
        require(params.password.isNotEmpty()) { "Invalid password" }

        return authenticationRepository.loginBasic(
            params.serverInfo,
            params.username,
            params.password,
            params.updateAccountWithUsername
        )
    }

    data class Params(
        val serverInfo: ServerInfo?,
        val username: String,
        val password: String,
        val updateAccountWithUsername: String? = null
    )
}
