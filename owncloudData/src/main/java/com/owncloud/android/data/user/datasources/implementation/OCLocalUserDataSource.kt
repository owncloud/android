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

package com.owncloud.android.data.user.datasources.implementation

import androidx.annotation.VisibleForTesting
import com.owncloud.android.data.user.datasources.LocalUserDataSource
import com.owncloud.android.data.user.db.UserDao
import com.owncloud.android.data.user.db.UserQuotaEntity
import com.owncloud.android.domain.user.model.UserQuota

class OCLocalUserDataSource(
    private val userDao: UserDao
) : LocalUserDataSource {

    override fun saveQuotaForAccount(accountName: String, userQuota: UserQuota) =
        userDao.insertOrReplace(userQuota.toEntity())

    override fun getQuotaForAccount(accountName: String): UserQuota? =
        userDao.getQuotaForAccount(accountName = accountName)?.toModel()

    override fun getAllUserQuotas(): List<UserQuota> {
        return userDao.getAllUserQuotas().map { userQuotaEntity ->
            userQuotaEntity.toModel()
        }
    }

    override fun deleteQuotaForAccount(accountName: String) {
        userDao.deleteQuotaForAccount(accountName = accountName)
    }

    companion object {
        @VisibleForTesting
        fun UserQuotaEntity.toModel(): UserQuota =
            UserQuota(
                accountName = accountName,
                available = available,
                used = used
            )

        @VisibleForTesting
        fun UserQuota.toEntity(): UserQuotaEntity =
            UserQuotaEntity(
                accountName = accountName,
                available = available,
                used = used
            )
    }
}
