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
package com.owncloud.android.data.authentication.datasources.implementation

import android.content.Context
import android.net.Uri
import com.owncloud.android.data.authentication.datasources.RemoteAuthenticationDataSource
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.user.datasources.mapper.RemoteUserInfoMapper
import com.owncloud.android.domain.user.model.UserInfo
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory
import com.owncloud.android.lib.resources.files.CheckPathExistenceRemoteOperation
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation

class OCRemoteAuthenticationDataSource(
    private val context: Context,
    private val remoteUserInfoMapper: RemoteUserInfoMapper
) : RemoteAuthenticationDataSource {
    override fun loginBasic(serverPath: String, username: String, password: String): Pair<UserInfo, String?> =
        login(OwnCloudCredentialsFactory.newBasicCredentials(username, password), serverPath)

    override fun loginOAuth(serverPath: String, username: String, accessToken: String): Pair<UserInfo, String?> =
        login(OwnCloudCredentialsFactory.newBearerCredentials(username, accessToken), serverPath)

    private fun login(ownCloudCredentials: OwnCloudCredentials, serverPath: String): Pair<UserInfo, String?> {
        val url: Uri = Uri.parse(serverPath)

        val client: OwnCloudClient =
            OwnCloudClientFactory.createOwnCloudClient(url, context, true).apply {
                credentials = ownCloudCredentials
            }

        val checkPathExistenceRemoteOperation = CheckPathExistenceRemoteOperation("/", true)
        executeRemoteOperation { checkPathExistenceRemoteOperation.execute(client) }

        // Get user info. It is needed to save the account into the account manager
        lateinit var userInfo: UserInfo

        executeRemoteOperation {
            GetRemoteUserInfoOperation().execute(client)
        }.let { userInfo = remoteUserInfoMapper.toModel(it)!! }

        return Pair(userInfo, client.baseUri.toString())
    }
}
