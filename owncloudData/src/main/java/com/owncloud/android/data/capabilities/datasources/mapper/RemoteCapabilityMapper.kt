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

package com.owncloud.android.data.capabilities.datasources.mapper

import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.mappers.RemoteMapper
import com.owncloud.android.lib.resources.status.RemoteCapability
import com.owncloud.android.lib.resources.status.RemoteCapability.CapabilityBooleanType as RemoteCapabilityBooleanType

class RemoteCapabilityMapper : RemoteMapper<OCCapability, RemoteCapability> {
    override fun toModel(remote: RemoteCapability?): OCCapability? =
        remote?.let {
            OCCapability(
                accountName = remote.accountName,
                versionMajor = remote.versionMajor,
                versionMinor = remote.versionMinor,
                versionMicro = remote.versionMicro,
                versionString = remote.versionString,
                versionEdition = remote.versionEdition,
                corePollInterval = remote.corePollinterval,
                davChunkingVersion = remote.chunkingVersion,
                filesSharingApiEnabled = CapabilityBooleanType.fromValue(remote.filesSharingApiEnabled.value),
                filesSharingPublicEnabled = CapabilityBooleanType.fromValue(remote.filesSharingPublicEnabled.value),
                filesSharingPublicPasswordEnforced =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicPasswordEnforced.value),
                filesSharingPublicPasswordEnforcedReadOnly =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicPasswordEnforcedReadOnly.value),
                filesSharingPublicPasswordEnforcedReadWrite =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicPasswordEnforcedReadWrite.value),
                filesSharingPublicPasswordEnforcedUploadOnly =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicPasswordEnforcedUploadOnly.value),
                filesSharingPublicExpireDateEnabled =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicExpireDateEnabled.value),
                filesSharingPublicExpireDateDays = remote.filesSharingPublicExpireDateDays,
                filesSharingPublicExpireDateEnforced =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicExpireDateEnforced.value),
                filesSharingPublicUpload = CapabilityBooleanType.fromValue(remote.filesSharingPublicUpload.value),
                filesSharingPublicMultiple = CapabilityBooleanType.fromValue(remote.filesSharingPublicMultiple.value),
                filesSharingPublicSupportsUploadOnly =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicSupportsUploadOnly.value),
                filesSharingResharing = CapabilityBooleanType.fromValue(remote.filesSharingResharing.value),
                filesSharingFederationOutgoing =
                CapabilityBooleanType.fromValue(remote.filesSharingFederationOutgoing.value),
                filesSharingFederationIncoming =
                CapabilityBooleanType.fromValue(remote.filesSharingFederationIncoming.value),
                filesSharingUserProfilePicture = CapabilityBooleanType.fromValue(remote.filesSharingUserProfilePicture.value),
                filesBigFileChunking = CapabilityBooleanType.fromValue(remote.filesBigFileChunking.value),
                filesUndelete = CapabilityBooleanType.fromValue(remote.filesUndelete.value),
                filesVersioning = CapabilityBooleanType.fromValue(remote.filesVersioning.value),
                filesPrivateLinks = CapabilityBooleanType.fromValue(remote.filesPrivateLinks.value),
                filesAppProviders = remote.filesAppProviders?.firstOrNull()?.toAppProviders(),
                spaces = remote.spaces?.toSpaces(),
                passwordPolicy = remote.passwordPolicy?.toPasswordPolicy()
            )
        }

    override fun toRemote(model: OCCapability?): RemoteCapability? =
        model?.let {
            RemoteCapability(
                accountName = model.accountName!!,
                versionMajor = model.versionMajor,
                versionMinor = model.versionMinor,
                versionMicro = model.versionMicro,
                versionString = model.versionString!!,
                versionEdition = model.versionEdition!!,
                chunkingVersion = model.davChunkingVersion,
                corePollinterval = model.corePollInterval,
                filesSharingApiEnabled = RemoteCapabilityBooleanType.fromValue(model.filesSharingApiEnabled.value)!!,
                filesSharingPublicEnabled = RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicEnabled.value)!!,
                filesSharingPublicPasswordEnforced =
                RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicPasswordEnforced.value)!!,
                filesSharingPublicPasswordEnforcedReadOnly =
                RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicPasswordEnforcedReadOnly.value)!!,
                filesSharingPublicPasswordEnforcedReadWrite =
                RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicPasswordEnforcedReadWrite.value)!!,
                filesSharingPublicPasswordEnforcedUploadOnly =
                RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicPasswordEnforcedUploadOnly.value)!!,
                filesSharingPublicExpireDateEnabled =
                RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicExpireDateEnabled.value)!!,
                filesSharingPublicExpireDateDays = model.filesSharingPublicExpireDateDays,
                filesSharingPublicExpireDateEnforced =
                RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicExpireDateEnforced.value)!!,
                filesSharingPublicUpload = RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicUpload.value)!!,
                filesSharingPublicMultiple = RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicMultiple.value)!!,
                filesSharingPublicSupportsUploadOnly =
                RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicSupportsUploadOnly.value)!!,
                filesSharingResharing = RemoteCapabilityBooleanType.fromValue(model.filesSharingResharing.value)!!,
                filesSharingFederationOutgoing =
                RemoteCapabilityBooleanType.fromValue(model.filesSharingFederationOutgoing.value)!!,
                filesSharingFederationIncoming =
                RemoteCapabilityBooleanType.fromValue(model.filesSharingFederationIncoming.value)!!,
                filesSharingUserProfilePicture = RemoteCapabilityBooleanType.fromValue(model.filesSharingUserProfilePicture.value)!!,
                filesBigFileChunking = RemoteCapabilityBooleanType.fromValue(model.filesBigFileChunking.value)!!,
                filesUndelete = RemoteCapabilityBooleanType.fromValue(model.filesUndelete.value)!!,
                filesVersioning = RemoteCapabilityBooleanType.fromValue(model.filesVersioning.value)!!,
                filesPrivateLinks = RemoteCapabilityBooleanType.fromValue(model.filesPrivateLinks.value)!!,
                filesAppProviders = null,
                spaces = null,
                passwordPolicy = null,
            )
        }

    private fun RemoteCapability.RemoteAppProviders.toAppProviders() =
        OCCapability.AppProviders(enabled, version, appsUrl, openUrl, openWebUrl, newUrl)

    private fun RemoteCapability.RemoteSpaces.toSpaces() =
        OCCapability.Spaces(enabled, projects, shareJail)

    private fun RemoteCapability.RemotePasswordPolicy.toPasswordPolicy() =
        OCCapability.PasswordPolicy(maxCharacters, minCharacters, minDigits, minLowercaseCharacters, minSpecialCharacters, minUppercaseCharacters)
}
