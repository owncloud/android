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
    val versionMayor: Int,
    val versionMinor: Int,
    val versionMicro: Int,
    val versionString: String?,
    val versionEdition: String?,
    val corePollInterval: Int,
    val filesSharingApiEnabled: CapabilityBooleanType,
    val filesSharingSearchMinLength: Int? = null,
    val filesSharingPublicEnabled: CapabilityBooleanType,
    val filesSharingPublicPasswordEnforced: CapabilityBooleanType,
    val filesSharingPublicPasswordEnforcedReadOnly: CapabilityBooleanType,
    val filesSharingPublicPasswordEnforcedReadWrite: CapabilityBooleanType,
    val filesSharingPublicPasswordEnforcedUploadOnly: CapabilityBooleanType,
    val filesSharingPublicExpireDateEnabled: CapabilityBooleanType,
    val filesSharingPublicExpireDateDays: Int,
    val filesSharingPublicExpireDateEnforced: CapabilityBooleanType,
    val filesSharingPublicSendMail: CapabilityBooleanType,
    val filesSharingPublicUpload: CapabilityBooleanType,
    val filesSharingPublicMultiple: CapabilityBooleanType,
    val filesSharingPublicSupportsUploadOnly: CapabilityBooleanType,
    val filesSharingUserSendMail: CapabilityBooleanType,
    val filesSharingResharing: CapabilityBooleanType,
    val filesSharingFederationOutgoing: CapabilityBooleanType,
    val filesSharingFederationIncoming: CapabilityBooleanType,
    val filesBigFileChunking: CapabilityBooleanType,
    val filesUndelete: CapabilityBooleanType,
    val filesVersioning: CapabilityBooleanType
)

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
        const val capabilityBooleanTypeUnknownInt = -1

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
