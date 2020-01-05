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
                versionMayor = remote.versionMayor,
                versionMinor = remote.versionMinor,
                versionMicro = remote.versionMicro,
                versionString = remote.versionString,
                versionEdition = remote.versionEdition,
                corePollInterval = remote.corePollinterval,
                filesSharingApiEnabled = CapabilityBooleanType.fromValue(remote.filesSharingApiEnabled.value),
                filesSharingSearchMinLength = remote.filesSharingSearchMinLength,
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
                filesSharingPublicSendMail = CapabilityBooleanType.fromValue(remote.filesSharingPublicSendMail.value),
                filesSharingPublicUpload = CapabilityBooleanType.fromValue(remote.filesSharingPublicUpload.value),
                filesSharingPublicMultiple = CapabilityBooleanType.fromValue(remote.filesSharingPublicMultiple.value),
                filesSharingPublicSupportsUploadOnly =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicSupportsUploadOnly.value),
                filesSharingUserSendMail = CapabilityBooleanType.fromValue(remote.filesSharingUserSendMail.value),
                filesSharingResharing = CapabilityBooleanType.fromValue(remote.filesSharingResharing.value),
                filesSharingFederationOutgoing =
                CapabilityBooleanType.fromValue(remote.filesSharingFederationOutgoing.value),
                filesSharingFederationIncoming =
                CapabilityBooleanType.fromValue(remote.filesSharingFederationIncoming.value),
                filesBigFileChunking = CapabilityBooleanType.fromValue(remote.filesBigFileChunking.value),
                filesUndelete = CapabilityBooleanType.fromValue(remote.filesUndelete.value),
                filesVersioning = CapabilityBooleanType.fromValue(remote.filesVersioning.value)
            )
        }

    override fun toRemote(model: OCCapability?): RemoteCapability? =
        model?.let {
            RemoteCapability(
                accountName = model.accountName!!,
                versionMayor = model.versionMayor,
                versionMinor = model.versionMinor,
                versionMicro = model.versionMicro,
                versionString = model.versionString!!,
                versionEdition = model.versionEdition!!,
                corePollinterval = model.corePollInterval,
                filesSharingApiEnabled = RemoteCapabilityBooleanType.fromValue(model.filesSharingApiEnabled.value)!!,
                filesSharingSearchMinLength = model.filesSharingSearchMinLength,
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
                filesSharingPublicSendMail = RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicSendMail.value)!!,
                filesSharingPublicUpload = RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicUpload.value)!!,
                filesSharingPublicMultiple = RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicMultiple.value)!!,
                filesSharingPublicSupportsUploadOnly =
                RemoteCapabilityBooleanType.fromValue(model.filesSharingPublicSupportsUploadOnly.value)!!,
                filesSharingUserSendMail = RemoteCapabilityBooleanType.fromValue(model.filesSharingUserSendMail.value)!!,
                filesSharingResharing = RemoteCapabilityBooleanType.fromValue(model.filesSharingResharing.value)!!,
                filesSharingFederationOutgoing =
                RemoteCapabilityBooleanType.fromValue(model.filesSharingFederationOutgoing.value)!!,
                filesSharingFederationIncoming =
                RemoteCapabilityBooleanType.fromValue(model.filesSharingFederationIncoming.value)!!,
                filesBigFileChunking = RemoteCapabilityBooleanType.fromValue(model.filesBigFileChunking.value)!!,
                filesUndelete = RemoteCapabilityBooleanType.fromValue(model.filesUndelete.value)!!,
                filesVersioning = RemoteCapabilityBooleanType.fromValue(model.filesVersioning.value)!!
            )
        }

}
