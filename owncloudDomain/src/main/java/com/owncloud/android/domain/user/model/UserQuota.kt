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

package com.owncloud.android.domain.user.model

import androidx.annotation.VisibleForTesting
import kotlin.math.roundToLong

data class UserQuota(
    val accountName: String,
    val available: Long,
    val used: Long
) {
    @VisibleForTesting
    fun isLimited() = available > 0

    fun getRelative() = if (isLimited() && getTotal() > 0) {
        val relativeQuota = (used * 100).toDouble() / getTotal()
        (relativeQuota * 100).roundToLong() / 100.0
    } else 0.0

    fun getTotal() = if (isLimited()) {
        available + used
    } else {
        0
    }
}
