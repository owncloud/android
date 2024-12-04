 /**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
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

package com.owncloud.android.domain.user.model

import kotlin.math.roundToLong

data class UserQuota(
    val accountName: String,
    val available: Long, // -4 : Light Users | -3: Unlimited quota | OTHER: Limited quota
    val used: Long,
    val total: Long?,
    val state: UserQuotaState?
) {

    fun getRelative(): Double {
        if (getTotal() == 0L) {
            return 0.0
        } else {
            val relativeQuota = (used * 100).toDouble() / getTotal()
            return (relativeQuota * 100).roundToLong() / 100.0
        }
    }

    fun getTotal(): Long = total ?: (available + used)
}
