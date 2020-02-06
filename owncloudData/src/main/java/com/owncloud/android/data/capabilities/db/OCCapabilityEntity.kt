/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.data.capabilities.db

import android.content.ContentValues
import android.database.Cursor
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.*
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType.Companion.capabilityBooleanTypeUnknownString
import com.owncloud.android.domain.capabilities.model.OCCapability

/**
 * Represents one record of the Capabilities table.
 */
@Entity(tableName = CAPABILITIES_TABLE_NAME)
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
    @ColumnInfo(name = CAPABILITIES_SHARING_API_ENABLED, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingApiEnabled: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_SEARCH_MIN_LENGTH)
    val filesSharingSearchMinLength: Int?,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_ENABLED, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicEnabled: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicPasswordEnforced: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicPasswordEnforcedReadOnly: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicPasswordEnforcedReadWrite: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicPasswordEnforcedUploadOnly: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicExpireDateEnabled: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS)
    val filesSharingPublicExpireDateDays: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicExpireDateEnforced: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_SEND_MAIL, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicSendMail: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_UPLOAD, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicUpload: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_MULTIPLE, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicMultiple: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicSupportsUploadOnly: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_USER_SEND_MAIL, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingUserSendMail: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_RESHARING, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingResharing: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_FEDERATION_OUTGOING, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingFederationOutgoing: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_FEDERATION_INCOMING, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingFederationIncoming: Int,
    @ColumnInfo(name = CAPABILITIES_FILES_BIGFILECHUNKING, defaultValue = capabilityBooleanTypeUnknownString)
    val filesBigFileChunking: Int,
    @ColumnInfo(name = CAPABILITIES_FILES_UNDELETE, defaultValue = capabilityBooleanTypeUnknownString)
    val filesUndelete: Int,
    @ColumnInfo(name = CAPABILITIES_FILES_VERSIONING, defaultValue = capabilityBooleanTypeUnknownString)
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
            cursor.getInt(cursor.getColumnIndex(CAPABILITIES_SHARING_SEARCH_MIN_LENGTH)),
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
            values.getAsInteger(CAPABILITIES_SHARING_SEARCH_MIN_LENGTH),
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

        fun toContentValues(capability: OCCapability) = ContentValues().apply {
            capability.accountName?.let { put(CAPABILITIES_ACCOUNT_NAME, it) }
            put(CAPABILITIES_VERSION_MAYOR, capability.versionMayor)
            put(CAPABILITIES_VERSION_MINOR, capability.versionMinor)
            put(CAPABILITIES_VERSION_MICRO, capability.versionMicro)
            capability.versionString?.let { put(CAPABILITIES_VERSION_STRING, it) }
            capability.versionEdition?.let { put(CAPABILITIES_VERSION_EDITION, it) }
            put(CAPABILITIES_CORE_POLLINTERVAL, capability.corePollInterval)
            put(CAPABILITIES_SHARING_API_ENABLED, capability.filesSharingApiEnabled.value)
            capability.filesSharingSearchMinLength?.let { put(CAPABILITIES_SHARING_SEARCH_MIN_LENGTH, it) }
            put(CAPABILITIES_SHARING_PUBLIC_ENABLED, capability.filesSharingPublicEnabled.value)
            put(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED, capability.filesSharingPublicPasswordEnforced.value)
            put(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY, capability.filesSharingPublicPasswordEnforcedReadOnly.value)
            put(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE, capability.filesSharingPublicPasswordEnforcedReadWrite.value)
            put(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY, capability.filesSharingPublicPasswordEnforcedUploadOnly.value)
            put(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED, capability.filesSharingPublicExpireDateEnabled.value)
            put(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS, capability.filesSharingPublicExpireDateDays)
            put(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED, capability.filesSharingPublicExpireDateEnforced.value)
            put(CAPABILITIES_SHARING_PUBLIC_SEND_MAIL, capability.filesSharingPublicSendMail.value)
            put(CAPABILITIES_SHARING_PUBLIC_UPLOAD, capability.filesSharingPublicUpload.value)
            put(CAPABILITIES_SHARING_PUBLIC_MULTIPLE, capability.filesSharingPublicMultiple.value)
            put(CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY, capability.filesSharingPublicSupportsUploadOnly.value)
            put(CAPABILITIES_SHARING_RESHARING, capability.filesSharingResharing.value)
            put(CAPABILITIES_SHARING_FEDERATION_OUTGOING, capability.filesSharingFederationOutgoing.value)
            put(CAPABILITIES_SHARING_FEDERATION_INCOMING, capability.filesSharingFederationIncoming.value)
            put(CAPABILITIES_FILES_BIGFILECHUNKING, capability.filesBigFileChunking.value)
            put(CAPABILITIES_FILES_UNDELETE, capability.filesUndelete.value)
            put(CAPABILITIES_FILES_VERSIONING, capability.filesVersioning.value)
        }
    }
}
