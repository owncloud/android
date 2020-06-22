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

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.user.datasources.RemoteUserDataSource
import com.owncloud.android.data.user.datasources.mapper.RemoteUserAvatarMapper
import com.owncloud.android.data.user.datasources.mapper.RemoteUserInfoMapper
import com.owncloud.android.data.user.datasources.mapper.RemoteUserQuotaMapper
import com.owncloud.android.domain.user.model.UserAvatar
import com.owncloud.android.domain.user.model.UserInfo
import com.owncloud.android.domain.user.model.UserQuota

class OCRemoteUserDataSource(
    private val remoteUserInfoMapper: RemoteUserInfoMapper,
    private val remoteUserQuotaMapper: RemoteUserQuotaMapper,
    private val remoteUserAvatarMapper: RemoteUserAvatarMapper,
    private val clientManager: ClientManager,
    private val avatarDimension: Int
) : RemoteUserDataSource {

    override fun getUserInfo(accountName: String): UserInfo =
        executeRemoteOperation {
            clientManager.getUserService(accountName).getUserInfo()
        }.let { remoteUserInfoMapper.toModel(it)!! }

    override fun getUserQuota(accountName: String): UserQuota =
        executeRemoteOperation {
            clientManager.getUserService(accountName).getUserQuota()
        }.let { remoteUserQuotaMapper.toModel(it)!! }

    override fun getUserAvatar(accountName: String): UserAvatar =
        executeRemoteOperation {
            clientManager.getUserService(accountName = accountName).getUserAvatar(avatarDimension)
        }.let { remoteUserAvatarMapper.toModel(it)!! }
}
