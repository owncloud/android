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
import com.owncloud.android.domain.user.model.UserAvatar
import com.owncloud.android.domain.user.model.UserInfo
import com.owncloud.android.domain.user.model.UserQuota
import com.owncloud.android.lib.resources.users.GetRemoteUserQuotaOperation
import com.owncloud.android.lib.resources.users.RemoteAvatarData
import com.owncloud.android.lib.resources.users.RemoteUserInfo

class OCRemoteUserDataSource(
    private val clientManager: ClientManager,
    private val avatarDimension: Int
) : RemoteUserDataSource {

    override fun getUserInfo(accountName: String): UserInfo =
        executeRemoteOperation {
            clientManager.getUserService(accountName).getUserInfo()
        }.toDomain()

    override fun getUserQuota(accountName: String): UserQuota =
        executeRemoteOperation {
            clientManager.getUserService(accountName).getUserQuota()
        }.toDomain(accountName)

    override fun getUserAvatar(accountName: String): UserAvatar =
        executeRemoteOperation {
            clientManager.getUserService(accountName = accountName).getUserAvatar(avatarDimension)
        }.toDomain()

}

/**************************************************************************************************************
 ************************************************* Mappers ****************************************************
 **************************************************************************************************************/
fun RemoteUserInfo.toDomain(): UserInfo =
    UserInfo(
        id = this.id,
        displayName = this.displayName,
        email = this.email
    )

private fun RemoteAvatarData.toDomain(): UserAvatar =
    UserAvatar(
        avatarData = this.avatarData,
        eTag = this.eTag,
        mimeType = this.mimeType
    )

private fun GetRemoteUserQuotaOperation.RemoteQuota.toDomain(accountName: String): UserQuota =
    UserQuota(
        accountName = accountName,
        available = this.free,
        used = this.used
    )
