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

package com.owncloud.android.data.authentication.datasources

import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationInfo
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.user.model.UserInfo

interface LocalAuthenticationDataSource {
    fun addBasicAccount(
        userName: String,
        lastPermanentLocation: String?,
        password: String,
        serverInfo: ServerInfo,
        userInfo: UserInfo,
        updateAccountWithUsername: String?
    ): String

    fun addOAuthAccount(
        userName: String,
        lastPermanentLocation: String?,
        authTokenType: String,
        accessToken: String,
        serverInfo: ServerInfo,
        userInfo: UserInfo,
        refreshToken: String,
        scope: String?,
        updateAccountWithUsername: String?,
        clientRegistrationInfo: ClientRegistrationInfo?
    ): String

    fun supportsOAuth2(accountName: String): Boolean

    fun getBaseUrl(accountName: String): String
}
