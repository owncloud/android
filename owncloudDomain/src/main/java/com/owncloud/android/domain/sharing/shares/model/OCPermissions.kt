/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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

package com.owncloud.android.domain.sharing.shares.model

import android.os.Parcelable
import com.owncloud.android.domain.links.model.OCLink
import com.owncloud.android.domain.roles.model.OCRole
import kotlinx.parcelize.Parcelize

@Parcelize
data class OCPermissions(
    val roles: List<OCRole>,
    val members: List<MemberPermission>,
    val links: List<OCLink>
) : Parcelable

@Parcelize
data class MemberPermission(
    val id: String,
    val memberId: String,
    val expirationDateTime: String?,
    val displayName: String,
    val roles: List<String>,
    val isGroup: Boolean = false
) : Parcelable
