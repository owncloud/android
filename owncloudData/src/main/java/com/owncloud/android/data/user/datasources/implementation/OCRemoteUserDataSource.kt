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

package com.owncloud.android.data.user.datasources.implementation

import com.owncloud.android.data.ClientHandler
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.user.datasources.RemoteUserDataSource
import com.owncloud.android.data.user.datasources.mapper.RemoteUserAvatarMapper
import com.owncloud.android.data.user.datasources.mapper.RemoteUserInfoMapper
import com.owncloud.android.data.user.datasources.mapper.RemoteUserQuotaMapper
import com.owncloud.android.domain.user.model.UserAvatar
import com.owncloud.android.domain.user.model.UserInfo
import com.owncloud.android.domain.user.model.UserQuota
import com.owncloud.android.lib.resources.users.services.UserService
import com.owncloud.android.lib.resources.users.services.implementation.OCUserService

class OCRemoteUserDataSource(
    private val clientHandler: ClientHandler,
    private val remoteUserInfoMapper: RemoteUserInfoMapper,
    private val remoteUserQuotaMapper: RemoteUserQuotaMapper,
    private val remoteUserAvatarMapper: RemoteUserAvatarMapper,
    private val avatarDimension: Int
) : RemoteUserDataSource {

    override fun getUserInfo(accountName: String): UserInfo =
        executeRemoteOperation {
            getUserService(accountName).getUserInfo()
        }.let { remoteUserInfoMapper.toModel(it)!! }

    override fun getUserQuota(accountName: String): UserQuota =
        executeRemoteOperation {
            getUserService(accountName).getUserQuota()
        }.let { remoteUserQuotaMapper.toModel(it)!! }

    override fun getUserAvatar(accountName: String): UserAvatar =
        executeRemoteOperation {
            getUserService(accountName = accountName).getUserAvatar()
        }.let { remoteUserAvatarMapper.toModel(it)!! }

    private fun getUserService(accountName: String? = ""): UserService {
        val owncloudClient = clientHandler.getClientForAccount(accountName = accountName)
        return OCUserService(client = owncloudClient, avatarDimension = avatarDimension)
    }
}
