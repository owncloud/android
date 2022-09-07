/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.data.user.repository

import com.owncloud.android.data.user.datasources.LocalUserDataSource
import com.owncloud.android.data.user.datasources.RemoteUserDataSource
import com.owncloud.android.domain.user.UserRepository
import com.owncloud.android.domain.user.model.UserAvatar
import com.owncloud.android.domain.user.model.UserInfo
import com.owncloud.android.domain.user.model.UserQuota

class OCUserRepository(
    private val localUserDataSource: LocalUserDataSource,
    private val remoteUserDataSource: RemoteUserDataSource
) : UserRepository {
    override fun getUserInfo(accountName: String): UserInfo = remoteUserDataSource.getUserInfo(accountName)
    override fun getUserQuota(accountName: String): UserQuota =
        remoteUserDataSource.getUserQuota(accountName).also {
            localUserDataSource.saveQuotaForAccount(accountName, it)
        }

    override fun getStoredUserQuota(accountName: String): UserQuota? =
        localUserDataSource.getQuotaForAccount(accountName)

    override fun getUserAvatar(accountName: String): UserAvatar =
        remoteUserDataSource.getUserAvatar(accountName)

    override fun removeUserQuota(accountName: String) {
        localUserDataSource.removeQuotaForAccount(accountName)
    }
}
