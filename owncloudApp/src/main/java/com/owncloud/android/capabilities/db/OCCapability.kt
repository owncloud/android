/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.capabilities.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.lib.resources.status.RemoteCapability

/**
 * Represents one record of the Capabilities table.
 */
@Entity(tableName = ProviderTableMeta.CAPABILITIES_TABLE_NAME)
data class OCCapability(
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME)
    val accountName: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_VERSION_MAYOR)
    val versionMayor: Int,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_VERSION_MINOR)
    val versionMinor: Int,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_VERSION_MICRO)
    val versionMicro: Int,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_VERSION_STRING)
    val versionString: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_VERSION_EDITION)
    val versionEdition: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL)
    val corePollInterval: Int,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED)
    val filesSharingApiEnabled: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED)
    val filesSharingPublicEnabled: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED)
    val filesSharingPublicPasswordEnforced: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY)
    val filesSharingPublicPasswordEnforcedReadOnly: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE)
    val filesSharingPublicPasswordEnforcedReadWrite: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY)
    val filesSharingPublicPasswordEnforcedUploadOnly: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED)
    val filesSharingPublicExpireDateEnabled: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS)
    val filesSharingPublicExpireDateDays: Int,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED)
    val filesSharingPublicExpireDateEnforced: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL)
    val filesSharingPublicSendMail: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD)
    val filesSharingPublicUpload: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE)
    val filesSharingPublicMultiple: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY)
    val filesSharingPublicSupportsUploadOnly: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL)
    val filesSharingUserSendMail: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_RESHARING)
    val filesSharingResharing: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING)
    val filesSharingFederationOutgoing: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING)
    val filesSharingFederationIncoming: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING)
    val filesBigFileChuncking: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FILES_UNDELETE)
    val filesUndelete: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FILES_VERSIONING)
    val filesVersioning: Int?
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0

    companion object {
        fun fromRemoteCapability(remoteCapability: RemoteCapability): OCCapability {
            return OCCapability(
                remoteCapability.accountName,
                remoteCapability.versionMayor,
                remoteCapability.versionMinor,
                remoteCapability.versionMicro,
                remoteCapability.versionString,
                remoteCapability.versionEdition,
                remoteCapability.corePollinterval,
                remoteCapability.filesSharingApiEnabled?.value,
                remoteCapability.filesSharingPublicEnabled?.value,
                remoteCapability.filesSharingPublicPasswordEnforced?.value,
                remoteCapability.filesSharingPublicPasswordEnforcedReadOnly?.value,
                remoteCapability.filesSharingPublicPasswordEnforcedReadWrite?.value,
                remoteCapability.filesSharingPublicPasswordEnforcedUploadOnly?.value,
                remoteCapability.filesSharingPublicExpireDateEnabled?.value,
                remoteCapability.filesSharingPublicExpireDateDays,
                remoteCapability.filesSharingPublicExpireDateEnforced?.value,
                remoteCapability.filesSharingPublicSendMail?.value,
                remoteCapability.filesSharingPublicUpload?.value,
                remoteCapability.filesSharingPublicMultiple?.value,
                remoteCapability.filesSharingPublicSupportsUploadOnly?.value,
                remoteCapability.filesSharingUserSendMail?.value,
                remoteCapability.filesSharingResharing?.value,
                remoteCapability.filesSharingFederationOutgoing?.value,
                remoteCapability.filesSharingFederationIncoming?.value,
                remoteCapability.filesBigFileChuncking?.value,
                remoteCapability.filesUndelete?.value,
                remoteCapability.filesVersioning?.value
            )
        }
    }
}
