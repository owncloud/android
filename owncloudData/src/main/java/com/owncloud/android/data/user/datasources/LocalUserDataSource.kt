/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.data.user.datasources

import com.owncloud.android.domain.user.model.UserQuota
import kotlinx.coroutines.flow.Flow

interface LocalUserDataSource {
    fun saveQuotaForAccount(
        accountName: String,
        userQuota: UserQuota
    )

    fun getQuotaForAccount(
        accountName: String
    ): UserQuota?

    fun getQuotaForAccountAsFlow(
        accountName: String
    ): Flow<UserQuota?>

    fun getAllUserQuotas(): List<UserQuota>

    fun getAllUserQuotasAsFlow(): Flow<List<UserQuota>>

    fun deleteQuotaForAccount(
        accountName: String
    )
}
