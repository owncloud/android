/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.data.sharing.shares.db

import android.content.ContentValues
import android.database.Cursor
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta

/**
 * Represents one record of the Shares table.
 */
@Entity(tableName = ProviderTableMeta.OCSHARES_TABLE_NAME)
data class OCShareEntity(
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_FILE_SOURCE)
    val fileSource: String,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_ITEM_SOURCE)
    val itemSource: String,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARE_TYPE)
    val shareType: Int,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARE_WITH)
    val shareWith: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_PATH)
    val path: String,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_PERMISSIONS)
    val permissions: Int,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARED_DATE)
    val sharedDate: Long,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_EXPIRATION_DATE)
    val expirationDate: Long,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_TOKEN)
    val token: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME)
    val sharedWithDisplayName: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARE_WITH_ADDITIONAL_INFO)
    val sharedWithAdditionalInfo: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_IS_DIRECTORY)
    val isFolder: Boolean,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_USER_ID)
    val userId: Long,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED)
    val remoteId: Long,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER)
    var accountOwner: String = "",
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_NAME)
    val name: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_URL)
    val shareLink: String?
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0

    companion object {

        fun fromCursor(cursor: Cursor): OCShareEntity {
            return OCShareEntity(
                cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_FILE_SOURCE)),
                cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_ITEM_SOURCE)),
                cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_TYPE)),
                cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_WITH)),
                cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_PATH)),
                cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_PERMISSIONS)),
                cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_SHARED_DATE)),
                cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_EXPIRATION_DATE)),
                cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_TOKEN)),
                cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME)),
                cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_WITH_ADDITIONAL_INFO)),
                cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_IS_DIRECTORY)) == 1,
                cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_USER_ID)),
                cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED)),
                cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER)),
                cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_NAME)),
                cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_URL))
            )
        }

        fun fromContentValues(values: ContentValues): OCShareEntity {
            return OCShareEntity(
                values.getAsString(ProviderTableMeta.OCSHARES_FILE_SOURCE),
                values.getAsString(ProviderTableMeta.OCSHARES_ITEM_SOURCE),
                values.getAsInteger(ProviderTableMeta.OCSHARES_SHARE_TYPE),
                values.getAsString(ProviderTableMeta.OCSHARES_SHARE_WITH),
                values.getAsString(ProviderTableMeta.OCSHARES_PATH),
                values.getAsInteger(ProviderTableMeta.OCSHARES_PERMISSIONS),
                values.getAsLong(ProviderTableMeta.OCSHARES_SHARED_DATE),
                values.getAsLong(ProviderTableMeta.OCSHARES_EXPIRATION_DATE),
                values.getAsString(ProviderTableMeta.OCSHARES_TOKEN),
                values.getAsString(ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME),
                values.getAsString(ProviderTableMeta.OCSHARES_SHARE_WITH_ADDITIONAL_INFO),
                values.getAsBoolean(ProviderTableMeta.OCSHARES_IS_DIRECTORY),
                values.getAsLong(ProviderTableMeta.OCSHARES_USER_ID),
                values.getAsLong(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED),
                values.getAsString(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER),
                values.getAsString(ProviderTableMeta.OCSHARES_NAME),
                values.getAsString(ProviderTableMeta.OCSHARES_URL)
            )
        }
    }
}
