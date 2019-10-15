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

package com.owncloud.android.data.capabilities.db

import android.content.ContentValues
import android.database.Cursor
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_UNDELETE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_VERSIONING
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_RESHARING
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_EDITION
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MAYOR
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MICRO
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MINOR
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_STRING

/**
 * Represents one record of the Capabilities table.
 */
@Entity(
    tableName = CAPABILITIES_TABLE_NAME
)
data class OCCapabilityEntity(
    @ColumnInfo(name = CAPABILITIES_ACCOUNT_NAME)
    val accountName: String?,
    @ColumnInfo(name = CAPABILITIES_VERSION_MAYOR)
    val versionMayor: Int,
    @ColumnInfo(name = CAPABILITIES_VERSION_MINOR)
    val versionMinor: Int,
    @ColumnInfo(name = CAPABILITIES_VERSION_MICRO)
    val versionMicro: Int,
    @ColumnInfo(name = CAPABILITIES_VERSION_STRING)
    val versionString: String?,
    @ColumnInfo(name = CAPABILITIES_VERSION_EDITION)
    val versionEdition: String?,
    @ColumnInfo(name = CAPABILITIES_CORE_POLLINTERVAL)
    val corePollInterval: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_API_ENABLED)
    val filesSharingApiEnabled: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_ENABLED)
    val filesSharingPublicEnabled: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED)
    val filesSharingPublicPasswordEnforced: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY)
    val filesSharingPublicPasswordEnforcedReadOnly: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE)
    val filesSharingPublicPasswordEnforcedReadWrite: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY)
    val filesSharingPublicPasswordEnforcedUploadOnly: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED)
    val filesSharingPublicExpireDateEnabled: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS)
    val filesSharingPublicExpireDateDays: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED)
    val filesSharingPublicExpireDateEnforced: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_SEND_MAIL)
    val filesSharingPublicSendMail: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_UPLOAD)
    val filesSharingPublicUpload: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_MULTIPLE)
    val filesSharingPublicMultiple: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY)
    val filesSharingPublicSupportsUploadOnly: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_USER_SEND_MAIL)
    val filesSharingUserSendMail: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_RESHARING)
    val filesSharingResharing: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_FEDERATION_OUTGOING)
    val filesSharingFederationOutgoing: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_FEDERATION_INCOMING)
    val filesSharingFederationIncoming: Int,
    @ColumnInfo(name = CAPABILITIES_FILES_BIGFILECHUNKING)
    val filesBigFileChunking: Int,
    @ColumnInfo(name = CAPABILITIES_FILES_UNDELETE)
    val filesUndelete: Int,
    @ColumnInfo(name = CAPABILITIES_FILES_VERSIONING)
    val filesVersioning: Int
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0

    companion object {
        fun fromCursor(cursor: Cursor) = OCCapabilityEntity(
            cursor.getString(cursor.getColumnIndex(CAPABILITIES_ACCOUNT_NAME)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_VERSION_MAYOR)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_VERSION_MINOR)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_VERSION_MICRO)),
            cursor.getString(cursor.getColumnIndex(CAPABILITIES_VERSION_STRING)),
            cursor.getString(cursor.getColumnIndex(CAPABILITIES_VERSION_EDITION)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_CORE_POLLINTERVAL)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_API_ENABLED)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_PUBLIC_ENABLED)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_PUBLIC_SEND_MAIL)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_PUBLIC_UPLOAD)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_PUBLIC_MULTIPLE)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_USER_SEND_MAIL)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_RESHARING)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_FEDERATION_OUTGOING)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_FEDERATION_INCOMING)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_FILES_BIGFILECHUNKING)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_FILES_UNDELETE)),
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_FILES_VERSIONING))
        )

        fun fromContentValues(values: ContentValues) = OCCapabilityEntity(
            values.getAsString(CAPABILITIES_ACCOUNT_NAME),
            values.getAsInteger(CAPABILITIES_VERSION_MAYOR),
            values.getAsInteger(CAPABILITIES_VERSION_MINOR),
            values.getAsInteger(CAPABILITIES_VERSION_MICRO),
            values.getAsString(CAPABILITIES_VERSION_STRING),
            values.getAsString(CAPABILITIES_VERSION_EDITION),
            values.getAsInteger(CAPABILITIES_CORE_POLLINTERVAL),
            values.getAsInteger(CAPABILITIES_SHARING_API_ENABLED),
            values.getAsInteger(CAPABILITIES_SHARING_PUBLIC_ENABLED),
            values.getAsInteger(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED),
            values.getAsInteger(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY),
            values.getAsInteger(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE),
            values.getAsInteger(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY),
            values.getAsInteger(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED),
            values.getAsInteger(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS),
            values.getAsInteger(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED),
            values.getAsInteger(CAPABILITIES_SHARING_PUBLIC_SEND_MAIL),
            values.getAsInteger(CAPABILITIES_SHARING_PUBLIC_UPLOAD),
            values.getAsInteger(CAPABILITIES_SHARING_PUBLIC_MULTIPLE),
            values.getAsInteger(CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY),
            values.getAsInteger(CAPABILITIES_SHARING_USER_SEND_MAIL),
            values.getAsInteger(CAPABILITIES_SHARING_RESHARING),
            values.getAsInteger(CAPABILITIES_SHARING_FEDERATION_OUTGOING),
            values.getAsInteger(CAPABILITIES_SHARING_FEDERATION_INCOMING),
            values.getAsInteger(CAPABILITIES_FILES_BIGFILECHUNKING),
            values.getAsInteger(CAPABILITIES_FILES_UNDELETE),
            values.getAsInteger(CAPABILITIES_FILES_VERSIONING)
        )
    }
}
