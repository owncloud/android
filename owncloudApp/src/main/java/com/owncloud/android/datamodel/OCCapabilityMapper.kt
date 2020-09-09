package com.owncloud.android.datamodel

import android.database.Cursor
import com.owncloud.android.db.ProviderMeta
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability

object OCCapabilityMapper {
    fun createCapabilityInstance(cursor: Cursor?): OCCapability? {
        if (cursor == null)
            return null

        fun getStringByIndex(index: String): String {
            return cursor.getString(cursor.getColumnIndex(index))
        }

        fun getIntByIndex(index: String): Int {
            return cursor.getInt(cursor.getColumnIndex(index))
        }

        fun getCapabilityBooleanByIndex(index: String): CapabilityBooleanType {
            return CapabilityBooleanType.fromValue(
                cursor.getInt(cursor.getColumnIndex(index))
            )
        }

        return OCCapability(
            accountName = getStringByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME),
            versionMayor = getIntByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MAYOR),
            versionMinor = getIntByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MINOR),
            versionMicro = getIntByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MICRO),
            versionString = getStringByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_STRING),
            versionEdition = getStringByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_EDITION),
            corePollInterval = getIntByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL),
            davChunkingVersion = getStringByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_DAV_CHUNKING_VERSION),
            filesSharingApiEnabled = getCapabilityBooleanByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED),
            filesSharingPublicEnabled = getCapabilityBooleanByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED),
            filesSharingPublicPasswordEnforced = getCapabilityBooleanByIndex(
                ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED
            ),
            filesSharingPublicPasswordEnforcedReadOnly = getCapabilityBooleanByIndex(
                ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY
            ),
            filesSharingPublicPasswordEnforcedReadWrite = getCapabilityBooleanByIndex(
                ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE
            ),
            filesSharingPublicPasswordEnforcedUploadOnly = getCapabilityBooleanByIndex(
                ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY
            ),
            filesSharingPublicExpireDateEnabled = getCapabilityBooleanByIndex(
                ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED
            ),
            filesSharingPublicExpireDateDays = getIntByIndex(
                ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS
            ),
            filesSharingPublicExpireDateEnforced = getCapabilityBooleanByIndex(
                ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED
            ),
            filesSharingPublicUpload = getCapabilityBooleanByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD),
            filesSharingPublicMultiple = getCapabilityBooleanByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE),
            filesSharingPublicSupportsUploadOnly = getCapabilityBooleanByIndex(
                ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY
            ),
            filesSharingResharing = getCapabilityBooleanByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_RESHARING),
            filesSharingFederationOutgoing = getCapabilityBooleanByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING),
            filesSharingFederationIncoming = getCapabilityBooleanByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING),
            filesBigFileChunking = getCapabilityBooleanByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING),
            filesUndelete = getCapabilityBooleanByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_UNDELETE),
            filesVersioning = getCapabilityBooleanByIndex(ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_VERSIONING)
        )
    }
}
