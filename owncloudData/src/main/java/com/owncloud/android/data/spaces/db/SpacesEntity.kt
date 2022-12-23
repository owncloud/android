/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
package com.owncloud.android.data.spaces.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.owncloud.android.data.ProviderMeta
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ID

data class SpacesWithSpecials(
    @Embedded val space: SpacesEntity,
    @Relation(
        parentColumn = SPACES_ID,
        entityColumn = SpaceSpecialEntity.SPACES_SPECIAL_SPACE_ID,
    )
    val specials: List<SpaceSpecialEntity>
)

@Entity(tableName = ProviderMeta.ProviderTableMeta.SPACES_TABLE_NAME)
data class SpacesEntity(
    @ColumnInfo(name = SPACES_DRIVE_ALIAS)
    val driveAlias: String,
    @ColumnInfo(name = SPACES_DRIVE_TYPE)
    val driveType: String,
    @PrimaryKey @ColumnInfo(name = SPACES_ID)
    val id: String,
    @ColumnInfo(name = SPACES_LAST_MODIFIED_DATE_TIME)
    val lastModifiedDateTime: String,
    val name: String,
    @ColumnInfo(name = SPACES_OWNER_ID)
    val ownerId: String,
    @Embedded
    val quota: SpaceQuotaEntity?,
    val rootETag: String,
    val rootId: String,
    val rootWebDavUrl: String,
    @ColumnInfo(name = SPACES_WEB_URL)
    val webUrl: String,
    val description: String?,
) {

    companion object {
        const val DRIVE_TYPE_PERSONAL = "personal"
        const val DRIVE_TYPE_PROJECT = "project"
        const val SPACES_ID = "space_id"
        const val SPACES_DRIVE_ALIAS = "drive_alias"
        const val SPACES_DRIVE_TYPE = "drive_type"
        const val SPACES_LAST_MODIFIED_DATE_TIME = "last_modified_date_time"
        const val SPACES_WEB_URL = "web_url"
        const val SPACES_OWNER_ID = "owner_id"
    }
}

data class SpaceQuotaEntity(
    val remaining: Long,
    val state: String,
    val total: Int,
    val used: Int
)
