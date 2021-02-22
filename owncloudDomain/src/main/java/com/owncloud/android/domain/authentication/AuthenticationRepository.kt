/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.owncloud.android.domain.authentication

import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationInfo
import com.owncloud.android.domain.server.model.ServerInfo

interface AuthenticationRepository {
    fun loginBasic(
        serverInfo: ServerInfo,
        username: String,
        password: String,
        updateAccountWithUsername: String?
    ): String

    fun loginOAuth(
        serverInfo: ServerInfo,
        username: String,
        authTokenType: String,
        accessToken: String,
        refreshToken: String,
        scope: String?,
        updateAccountWithUsername: String?,
        clientRegistrationInfo: ClientRegistrationInfo?
    ): String

    fun supportsOAuth2UseCase(accountName: String): Boolean

    fun getBaseUrl(accountName: String): String
}
