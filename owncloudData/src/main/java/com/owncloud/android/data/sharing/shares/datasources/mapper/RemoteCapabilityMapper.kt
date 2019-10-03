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

package com.owncloud.android.data.sharing.shares.datasources.mapper

import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.mappers.RemoteMapper
import com.owncloud.android.lib.resources.status.RemoteCapability

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
                filesSharingApiEnabled = CapabilityBooleanType.fromValue(remote.filesSharingApiEnabled.value)!!,
                filesSharingSearchMinLength = remote.filesSharingSearchMinLength.value,
                filesSharingPublicEnabled = CapabilityBooleanType.fromValue(remote.filesSharingPublicEnabled.value)!!,
                filesSharingPublicPasswordEnforced =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicPasswordEnforced.value)!!,
                filesSharingPublicPasswordEnforcedReadOnly =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicPasswordEnforcedReadOnly.value)!!,
                filesSharingPublicPasswordEnforcedReadWrite =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicPasswordEnforcedReadWrite.value)!!,
                filesSharingPublicPasswordEnforcedUploadOnly =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicPasswordEnforcedUploadOnly.value)!!,
                filesSharingPublicExpireDateEnabled =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicExpireDateEnabled.value)!!,
                filesSharingPublicExpireDateDays = remote.filesSharingPublicExpireDateDays,
                filesSharingPublicExpireDateEnforced =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicExpireDateEnforced.value)!!,
                filesSharingPublicSendMail = CapabilityBooleanType.fromValue(remote.filesSharingPublicSendMail.value)!!,
                filesSharingPublicUpload = CapabilityBooleanType.fromValue(remote.filesSharingPublicUpload.value)!!,
                filesSharingPublicMultiple = CapabilityBooleanType.fromValue(remote.filesSharingPublicMultiple.value)!!,
                filesSharingPublicSupportsUploadOnly =
                CapabilityBooleanType.fromValue(remote.filesSharingPublicSupportsUploadOnly.value)!!,
                filesSharingUserSendMail = CapabilityBooleanType.fromValue(remote.filesSharingUserSendMail.value)!!,
                filesSharingResharing = CapabilityBooleanType.fromValue(remote.filesSharingResharing.value)!!,
                filesSharingFederationOutgoing =
                CapabilityBooleanType.fromValue(remote.filesSharingFederationOutgoing.value)!!,
                filesSharingFederationIncoming =
                CapabilityBooleanType.fromValue(remote.filesSharingFederationIncoming.value)!!,
                filesBigFileChunking = CapabilityBooleanType.fromValue(remote.filesBigFileChunking.value)!!,
                filesUndelete = CapabilityBooleanType.fromValue(remote.filesUndelete.value)!!,
                filesVersioning = CapabilityBooleanType.fromValue(remote.filesVersioning.value)!!
            )
        }

    override fun toRemote(model: OCCapability?): RemoteCapability? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
