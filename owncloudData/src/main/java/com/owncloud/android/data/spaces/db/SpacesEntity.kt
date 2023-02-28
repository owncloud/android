/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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
import androidx.room.Relation
import com.owncloud.android.data.ProviderMeta
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ACCOUNT_NAME
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ID
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_QUOTA_REMAINING
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_QUOTA_STATE
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_QUOTA_TOTAL
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_QUOTA_USED
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ROOT_DELETED_STATE
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ROOT_ETAG
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ROOT_ID
import com.owncloud.android.data.spaces.db.SpacesEntity.Companion.SPACES_ROOT_WEB_DAV_URL

@Entity(
    tableName = ProviderMeta.ProviderTableMeta.SPACES_TABLE_NAME,
    primaryKeys = [SPACES_ACCOUNT_NAME, SPACES_ID],
)
data class SpacesEntity(
    @ColumnInfo(name = SPACES_ACCOUNT_NAME)
    val accountName: String,
    @ColumnInfo(name = SPACES_DRIVE_ALIAS)
    val driveAlias: String,
    @ColumnInfo(name = SPACES_DRIVE_TYPE)
    val driveType: String,
    @ColumnInfo(name = SPACES_ID)
    val id: String,
    @ColumnInfo(name = SPACES_LAST_MODIFIED_DATE_TIME)
    val lastModifiedDateTime: String?,
    val name: String,
    @ColumnInfo(name = SPACES_OWNER_ID)
    val ownerId: String?,
    @Embedded
    val quota: SpaceQuotaEntity?,
    @Embedded
    val root: SpaceRootEntity,
    @ColumnInfo(name = SPACES_WEB_URL)
    val webUrl: String,
    val description: String?,
) {

    companion object {
        const val SPACES_ACCOUNT_NAME = "account_name"
        const val SPACES_ID = "space_id"
        const val SPACES_DRIVE_ALIAS = "drive_alias"
        const val SPACES_DRIVE_TYPE = "drive_type"
        const val SPACES_LAST_MODIFIED_DATE_TIME = "last_modified_date_time"
        const val SPACES_WEB_URL = "web_url"
        const val SPACES_OWNER_ID = "owner_id"
        const val SPACES_QUOTA_REMAINING = "quota_remaining"
        const val SPACES_QUOTA_STATE = "quota_state"
        const val SPACES_QUOTA_TOTAL = "quota_total"
        const val SPACES_QUOTA_USED = "quota_used"
        const val SPACES_ROOT_ETAG = "root_etag"
        const val SPACES_ROOT_ID = "root_id"
        const val SPACES_ROOT_WEB_DAV_URL = "root_web_dav_url"
        const val SPACES_ROOT_DELETED_STATE = "root_deleted_state"
    }
}

data class SpaceQuotaEntity(
    @ColumnInfo(name = SPACES_QUOTA_REMAINING)
    val remaining: Long?,
    @ColumnInfo(name = SPACES_QUOTA_STATE)
    val state: String?,
    @ColumnInfo(name = SPACES_QUOTA_TOTAL)
    val total: Long,
    @ColumnInfo(name = SPACES_QUOTA_USED)
    val used: Long?,
)

data class SpaceRootEntity(
    @ColumnInfo(name = SPACES_ROOT_ETAG)
    val eTag: String?,
    @ColumnInfo(name = SPACES_ROOT_ID)
    val id: String,
    @ColumnInfo(name = SPACES_ROOT_WEB_DAV_URL)
    val webDavUrl: String,
    @ColumnInfo(name = SPACES_ROOT_DELETED_STATE)
    val deleteState: String?,
)

data class SpacesWithSpecials(
    @Embedded val space: SpacesEntity,
    @Relation(
        parentColumn = SPACES_ID,
        entityColumn = SpaceSpecialEntity.SPACES_SPECIAL_SPACE_ID,
    )
    val specials: List<SpaceSpecialEntity>
)
