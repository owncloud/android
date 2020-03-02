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
package com.owncloud.android.data.authentication.repository

import com.owncloud.android.data.authentication.datasources.LocalAuthenticationDataSource
import com.owncloud.android.data.authentication.datasources.RemoteAuthenticationDataSource
import com.owncloud.android.domain.authentication.AuthenticationRepository
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.user.model.UserInfo

class OCAuthenticationRepository(
    private val localAuthenticationDataSource: LocalAuthenticationDataSource,
    private val remoteAuthenticationDataSource: RemoteAuthenticationDataSource
) : AuthenticationRepository {
    override fun loginBasic(serverInfo: ServerInfo, username: String, password: String): String {
        val userInfoAndRedirectionPath: Pair<UserInfo, String?> =
            remoteAuthenticationDataSource.loginBasic(
                serverPath = serverInfo.baseUrl,
                username = username,
                password = password
            )

        val accountName = localAuthenticationDataSource.addAccountIfDoesNotExist(
            userInfoAndRedirectionPath.second,
            username,
            password,
            serverInfo,
            userInfoAndRedirectionPath.first
        );

        return accountName;
    }

    override fun loginOAuth(
        serverInfo: ServerInfo,
        username: String,
        authTokenType: String,
        accessToken: String,
        refreshToken: String,
        scope: String?
    ): String {
        val userInfoAndRedirectionPath: Pair<UserInfo, String?> =
            remoteAuthenticationDataSource.loginOAuth(
                serverPath = serverInfo.baseUrl,
                username = username,
                accessToken = accessToken
            )

        val accountName = localAuthenticationDataSource.addOAuthAccountIfDoesNotExist(
            userInfoAndRedirectionPath.second,
            username,
            authTokenType,
            accessToken,
            serverInfo,
            userInfoAndRedirectionPath.first,
            refreshToken,
            scope
        )

        return accountName;
    }

    override fun supportsOAuth2UseCase(): Boolean =
        localAuthenticationDataSource.supportsOAuth2()
}
