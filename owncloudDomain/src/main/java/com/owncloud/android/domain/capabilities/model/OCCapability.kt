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

package com.owncloud.android.domain.capabilities.model

data class OCCapability(
    val id: Int? = null,
    var accountName: String?,
    val versionMajor: Int,
    val versionMinor: Int,
    val versionMicro: Int,
    val versionString: String?,
    val versionEdition: String?,
    val corePollInterval: Int,
    val davChunkingVersion: String,
    val filesSharingApiEnabled: CapabilityBooleanType,
    val filesSharingPublicEnabled: CapabilityBooleanType,
    val filesSharingPublicPasswordEnforced: CapabilityBooleanType,
    val filesSharingPublicPasswordEnforcedReadOnly: CapabilityBooleanType,
    val filesSharingPublicPasswordEnforcedReadWrite: CapabilityBooleanType,
    val filesSharingPublicPasswordEnforcedUploadOnly: CapabilityBooleanType,
    val filesSharingPublicExpireDateEnabled: CapabilityBooleanType,
    val filesSharingPublicExpireDateDays: Int,
    val filesSharingPublicExpireDateEnforced: CapabilityBooleanType,
    val filesSharingPublicUpload: CapabilityBooleanType,
    val filesSharingPublicMultiple: CapabilityBooleanType,
    val filesSharingPublicSupportsUploadOnly: CapabilityBooleanType,
    val filesSharingResharing: CapabilityBooleanType,
    val filesSharingFederationOutgoing: CapabilityBooleanType,
    val filesSharingFederationIncoming: CapabilityBooleanType,
    val filesSharingUserProfilePicture: CapabilityBooleanType,
    val filesBigFileChunking: CapabilityBooleanType,
    val filesUndelete: CapabilityBooleanType,
    val filesVersioning: CapabilityBooleanType,
    val filesPrivateLinks: CapabilityBooleanType,
    val filesAppProviders: AppProviders?,
    val spaces: Spaces?,
    val passwordPolicy: PasswordPolicy?,
) {
    fun isChunkingAllowed(): Boolean {
        val doubleChunkingVersion = davChunkingVersion.toDoubleOrNull()
        return (filesBigFileChunking.isTrue && doubleChunkingVersion != null && doubleChunkingVersion >= 1.0)
    }

    fun isFetchingAvatarAllowed(): Boolean {
        return filesSharingUserProfilePicture.isTrue || filesSharingUserProfilePicture.isUnknown
    }

    fun isOpenInWebAllowed(): Boolean = filesAppProviders?.openWebUrl?.isNotBlank() ?: false

    fun isSpacesAllowed(): Boolean = spaces?.enabled == true

    fun isSpacesProjectsAllowed(): Boolean = spaces?.projects == true

    data class AppProviders(
        val enabled: Boolean,
        val version: String,
        val appsUrl: String?,
        val openUrl: String?,
        val openWebUrl: String?,
        val newUrl: String?,
    )

    data class Spaces(
        val enabled: Boolean,
        val projects: Boolean,
        val shareJail: Boolean,
    )

    data class PasswordPolicy(
        val maxCharacters: Int?,
        val minCharacters: Int?,
        val minDigits: Int?,
        val minLowercaseCharacters: Int?,
        val minSpecialCharacters: Int?,
        val minUppercaseCharacters: Int?,
    )
}

/**
 * Enum for Boolean Type in capabilities, with values:
 * -1 - Unknown
 * 0 - False
 * 1 - True
 */
enum class CapabilityBooleanType constructor(val value: Int) {
    UNKNOWN(-1),
    FALSE(0),
    TRUE(1);

    val isUnknown: Boolean
        get() = value == -1

    val isFalse: Boolean
        get() = value == 0

    val isTrue: Boolean
        get() = value == 1

    companion object {
        const val capabilityBooleanTypeUnknownString = "-1"

        fun fromValue(value: Int): CapabilityBooleanType =
            when (value) {
                0 -> FALSE
                1 -> TRUE
                else -> UNKNOWN
            }

        fun fromBooleanValue(boolValue: Boolean): CapabilityBooleanType =
            if (boolValue) {
                TRUE
            } else {
                FALSE
            }
    }
}
