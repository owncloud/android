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
package com.owncloud.android.data.user.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.USER_AVATARS_TABLE_NAME

/**
 * Represents one record of the UserQuota table.
 */
@Entity(tableName = USER_AVATARS_TABLE_NAME)
data class UserAvatarEntity(
    @PrimaryKey
    val accountName: String,
    val cachedKey: String,
    val mimeType: String,
    val eTag: String
)
