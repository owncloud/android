/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * Copyright (C) 2019 ownCloud GmbH.
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
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_ACCOUNT_OWNER
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_EXPIRATION_DATE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_FILE_SOURCE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_IS_DIRECTORY
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_ITEM_SOURCE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_PATH
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_PERMISSIONS
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_SHARED_DATE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_SHARE_TYPE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_SHARE_WITH
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_SHARE_WITH_ADDITIONAL_INFO
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_TOKEN
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_URL
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.OCSHARES_USER_ID

/**
 * Represents one record of the Shares table.
 */
@Entity(tableName = OCSHARES_TABLE_NAME)
data class OCShareEntity(
    @ColumnInfo(name = OCSHARES_FILE_SOURCE)
    val fileSource: Long,
    @ColumnInfo(name = OCSHARES_ITEM_SOURCE)
    val itemSource: Long,
    @ColumnInfo(name = OCSHARES_SHARE_TYPE)
    val shareType: Int,
    @ColumnInfo(name = OCSHARES_SHARE_WITH)
    val shareWith: String?,
    @ColumnInfo(name = OCSHARES_PATH)
    val path: String,
    @ColumnInfo(name = OCSHARES_PERMISSIONS)
    val permissions: Int,
    @ColumnInfo(name = OCSHARES_SHARED_DATE)
    val sharedDate: Long,
    @ColumnInfo(name = OCSHARES_EXPIRATION_DATE)
    val expirationDate: Long,
    @ColumnInfo(name = OCSHARES_TOKEN)
    val token: String?,
    @ColumnInfo(name = OCSHARES_SHARE_WITH_DISPLAY_NAME)
    val sharedWithDisplayName: String?,
    @ColumnInfo(name = OCSHARES_SHARE_WITH_ADDITIONAL_INFO)
    val sharedWithAdditionalInfo: String?,
    @ColumnInfo(name = OCSHARES_IS_DIRECTORY)
    val isFolder: Boolean,
    @ColumnInfo(name = OCSHARES_USER_ID)
    val userId: Long,
    @ColumnInfo(name = OCSHARES_ID_REMOTE_SHARED)
    val remoteId: Long,
    @ColumnInfo(name = OCSHARES_ACCOUNT_OWNER)
    var accountOwner: String = "",
    @ColumnInfo(name = OCSHARES_NAME)
    val name: String?,
    @ColumnInfo(name = OCSHARES_URL)
    val shareLink: String?
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0

    companion object {
        private val TAG = OCShareEntity::class.java.simpleName

        const val DEFAULT_PERMISSION = -1
        const val READ_PERMISSION_FLAG = 1
        const val UPDATE_PERMISSION_FLAG = 2
        const val CREATE_PERMISSION_FLAG = 4
        const val DELETE_PERMISSION_FLAG = 8
        const val SHARE_PERMISSION_FLAG = 16
        const val MAXIMUM_PERMISSIONS_FOR_FILE = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                SHARE_PERMISSION_FLAG
        const val MAXIMUM_PERMISSIONS_FOR_FOLDER = MAXIMUM_PERMISSIONS_FOR_FILE +
                CREATE_PERMISSION_FLAG +
                DELETE_PERMISSION_FLAG
        const val FEDERATED_PERMISSIONS_FOR_FILE_UP_TO_OC9 = READ_PERMISSION_FLAG + UPDATE_PERMISSION_FLAG
        const val FEDERATED_PERMISSIONS_FOR_FILE_AFTER_OC9 = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                SHARE_PERMISSION_FLAG
        const val FEDERATED_PERMISSIONS_FOR_FOLDER_UP_TO_OC9 = READ_PERMISSION_FLAG +
                UPDATE_PERMISSION_FLAG +
                CREATE_PERMISSION_FLAG +
                DELETE_PERMISSION_FLAG
        const val FEDERATED_PERMISSIONS_FOR_FOLDER_AFTER_OC9 =
            FEDERATED_PERMISSIONS_FOR_FOLDER_UP_TO_OC9 + SHARE_PERMISSION_FLAG

        fun fromCursor(cursor: Cursor) = OCShareEntity(
            cursor.getLong(cursor.getColumnIndex(OCSHARES_FILE_SOURCE)),
            cursor.getLong(cursor.getColumnIndex(OCSHARES_ITEM_SOURCE)),
            cursor.getInt(cursor.getColumnIndex(OCSHARES_SHARE_TYPE)),
            cursor.getString(cursor.getColumnIndex(OCSHARES_SHARE_WITH)),
            cursor.getString(cursor.getColumnIndex(OCSHARES_PATH)),
            cursor.getInt(cursor.getColumnIndex(OCSHARES_PERMISSIONS)),
            cursor.getLong(cursor.getColumnIndex(OCSHARES_SHARED_DATE)),
            cursor.getLong(cursor.getColumnIndex(OCSHARES_EXPIRATION_DATE)),
            cursor.getString(cursor.getColumnIndex(OCSHARES_TOKEN)),
            cursor.getString(cursor.getColumnIndex(OCSHARES_SHARE_WITH_DISPLAY_NAME)),
            cursor.getString(cursor.getColumnIndex(OCSHARES_SHARE_WITH_ADDITIONAL_INFO)),
            cursor.getInt(cursor.getColumnIndex(OCSHARES_IS_DIRECTORY)) == 1,
            cursor.getLong(cursor.getColumnIndex(OCSHARES_USER_ID)),
            cursor.getLong(cursor.getColumnIndex(OCSHARES_ID_REMOTE_SHARED)),
            cursor.getString(cursor.getColumnIndex(OCSHARES_ACCOUNT_OWNER)),
            cursor.getString(cursor.getColumnIndex(OCSHARES_NAME)),
            cursor.getString(cursor.getColumnIndex(OCSHARES_URL))
        )

        fun fromContentValues(values: ContentValues) = OCShareEntity(
            values.getAsLong(OCSHARES_FILE_SOURCE),
            values.getAsLong(OCSHARES_ITEM_SOURCE),
            values.getAsInteger(OCSHARES_SHARE_TYPE),
            values.getAsString(OCSHARES_SHARE_WITH),
            values.getAsString(OCSHARES_PATH),
            values.getAsInteger(OCSHARES_PERMISSIONS),
            values.getAsLong(OCSHARES_SHARED_DATE),
            values.getAsLong(OCSHARES_EXPIRATION_DATE),
            values.getAsString(OCSHARES_TOKEN),
            values.getAsString(OCSHARES_SHARE_WITH_DISPLAY_NAME),
            values.getAsString(OCSHARES_SHARE_WITH_ADDITIONAL_INFO),
            values.getAsBoolean(OCSHARES_IS_DIRECTORY),
            values.getAsLong(OCSHARES_USER_ID),
            values.getAsLong(OCSHARES_ID_REMOTE_SHARED),
            values.getAsString(OCSHARES_ACCOUNT_OWNER),
            values.getAsString(OCSHARES_NAME),
            values.getAsString(OCSHARES_URL)
        )
    }
}
