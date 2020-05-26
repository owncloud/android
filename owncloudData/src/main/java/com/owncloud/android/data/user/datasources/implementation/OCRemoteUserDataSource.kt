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

import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.user.datasources.RemoteUserDataSource
import com.owncloud.android.data.user.datasources.mapper.RemoteUserInfoMapper
import com.owncloud.android.data.user.datasources.mapper.RemoteUserQuotaMapper
import com.owncloud.android.domain.user.model.UserInfo
import com.owncloud.android.domain.user.model.UserQuota
import com.owncloud.android.lib.resources.users.services.UserService

class OCRemoteUserDataSource(
    private val userService: UserService,
    private val remoteUserInfoMapper: RemoteUserInfoMapper,
    private val remoteUserQuotaMapper: RemoteUserQuotaMapper
) : RemoteUserDataSource {

    override fun getUserInfo(): UserInfo =
        executeRemoteOperation {
            userService.getUserInfo()
        }.let { remoteUserInfoMapper.toModel(it)!! }

    override fun getUserQuota(): UserQuota =
        executeRemoteOperation {
            userService.getUserQuota()
        }.let { remoteUserQuotaMapper.toModel(it)!! }

}
