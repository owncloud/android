/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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

import com.owncloud.android.data.authentication.datasources.LocalAuthenticationDataSource
import com.owncloud.android.data.user.datasources.LocalUserDataSource
import com.owncloud.android.data.user.datasources.RemoteUserDataSource
import com.owncloud.android.domain.user.UserRepository
import com.owncloud.android.domain.user.model.UserAvatar
import com.owncloud.android.domain.user.model.UserInfo
import com.owncloud.android.domain.user.model.UserQuota
import kotlinx.coroutines.flow.Flow

class OCUserRepository(
    private val localUserDataSource: LocalUserDataSource,
    private val remoteUserDataSource: RemoteUserDataSource,
    private val localAuthenticationDataSource: LocalAuthenticationDataSource
) : UserRepository {
    override fun getUserInfo(accountName: String): UserInfo = remoteUserDataSource.getUserInfo(accountName)
    override fun getUserQuota(accountName: String): UserQuota =
        remoteUserDataSource.getUserQuota(accountName).also {
            localUserDataSource.saveQuotaForAccount(accountName, it)
        }

    override fun getStoredUserQuota(accountName: String): UserQuota? =
        localUserDataSource.getQuotaForAccount(accountName)

    override fun getStoredUserQuotaAsFlow(accountName: String): Flow<UserQuota?> =
        localUserDataSource.getQuotaForAccountAsFlow(accountName)

    override fun getAllUserQuotas(): List<UserQuota> =
        localUserDataSource.getAllUserQuotas()

    override fun getAllUserQuotasAsFlow(): Flow<List<UserQuota>> =
        localUserDataSource.getAllUserQuotasAsFlow()

    override fun getUserAvatar(accountName: String): UserAvatar =
        remoteUserDataSource.getUserAvatar(accountName)

    override fun saveUserId(accountName: String){
        remoteUserDataSource.getUserId(accountName).also {
            localAuthenticationDataSource.saveIdForAccount(accountName, it)
        }
    }

}
