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

import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.mappers.Mapper

class OCCapabilityMapper : Mapper<OCCapability, OCCapabilityEntity> {
    override fun toModel(entity: OCCapabilityEntity?): OCCapability? =
        entity?.let {
            OCCapability(
                id = entity.id,
                accountName = entity.accountName,
                versionMayor = entity.versionMayor,
                versionMinor = entity.versionMinor,
                versionMicro = entity.versionMicro,
                versionString = entity.versionString,
                versionEdition = entity.versionEdition,
                corePollInterval = entity.corePollInterval,
                filesSharingApiEnabled = CapabilityBooleanType.fromValue(entity.filesSharingApiEnabled),
                filesSharingSearchMinLength = entity.filesSharingSearchMinLength,
                filesSharingPublicEnabled = CapabilityBooleanType.fromValue(entity.filesSharingPublicEnabled),
                filesSharingPublicPasswordEnforced = CapabilityBooleanType.fromValue(entity.filesSharingPublicPasswordEnforced),
                filesSharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.fromValue(entity.filesSharingPublicPasswordEnforcedReadOnly),
                filesSharingPublicPasswordEnforcedReadWrite = CapabilityBooleanType.fromValue(entity.filesSharingPublicPasswordEnforcedReadWrite),
                filesSharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.fromValue(entity.filesSharingPublicPasswordEnforcedUploadOnly),
                filesSharingPublicExpireDateEnabled = CapabilityBooleanType.fromValue(entity.filesSharingPublicExpireDateEnabled),
                filesSharingPublicExpireDateDays = entity.filesSharingPublicExpireDateDays,
                filesSharingPublicExpireDateEnforced = CapabilityBooleanType.fromValue(entity.filesSharingPublicExpireDateEnforced),
                filesSharingPublicSendMail = CapabilityBooleanType.fromValue(entity.filesSharingPublicSendMail),
                filesSharingPublicUpload = CapabilityBooleanType.fromValue(entity.filesSharingPublicUpload),
                filesSharingPublicMultiple = CapabilityBooleanType.fromValue(entity.filesSharingPublicMultiple),
                filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.fromValue(entity.filesSharingPublicSupportsUploadOnly),
                filesSharingUserSendMail = CapabilityBooleanType.fromValue(entity.filesSharingUserSendMail),
                filesSharingResharing = CapabilityBooleanType.fromValue(entity.filesSharingResharing),
                filesSharingFederationOutgoing = CapabilityBooleanType.fromValue(entity.filesSharingFederationOutgoing),
                filesSharingFederationIncoming = CapabilityBooleanType.fromValue(entity.filesSharingFederationIncoming),
                filesBigFileChunking = CapabilityBooleanType.fromValue(entity.filesBigFileChunking),
                filesUndelete = CapabilityBooleanType.fromValue(entity.filesUndelete),
                filesVersioning = CapabilityBooleanType.fromValue(entity.filesVersioning)
            )
        }

    override fun toEntity(model: OCCapability?): OCCapabilityEntity? =
        model?.let {
            OCCapabilityEntity(
                accountName = model.accountName,
                versionMayor = model.versionMayor,
                versionMinor = model.versionMinor,
                versionMicro = model.versionMicro,
                versionString = model.versionString,
                versionEdition = model.versionEdition,
                corePollInterval = model.corePollInterval,
                filesSharingApiEnabled = model.filesSharingApiEnabled.value,
                filesSharingSearchMinLength = model.filesSharingSearchMinLength,
                filesSharingPublicEnabled = model.filesSharingPublicEnabled.value,
                filesSharingPublicPasswordEnforced = model.filesSharingPublicPasswordEnforced.value,
                filesSharingPublicPasswordEnforcedReadOnly = model.filesSharingPublicPasswordEnforcedReadOnly.value,
                filesSharingPublicPasswordEnforcedReadWrite = model.filesSharingPublicPasswordEnforcedReadWrite.value,
                filesSharingPublicPasswordEnforcedUploadOnly = model.filesSharingPublicPasswordEnforcedUploadOnly.value,
                filesSharingPublicExpireDateEnabled = model.filesSharingPublicExpireDateEnabled.value,
                filesSharingPublicExpireDateDays = model.filesSharingPublicExpireDateDays,
                filesSharingPublicExpireDateEnforced = model.filesSharingPublicExpireDateEnforced.value,
                filesSharingPublicSendMail = model.filesSharingPublicSendMail.value,
                filesSharingPublicUpload = model.filesSharingPublicUpload.value,
                filesSharingPublicMultiple = model.filesSharingPublicMultiple.value,
                filesSharingPublicSupportsUploadOnly = model.filesSharingPublicSupportsUploadOnly.value,
                filesSharingUserSendMail = model.filesSharingUserSendMail.value,
                filesSharingResharing = model.filesSharingResharing.value,
                filesSharingFederationOutgoing = model.filesSharingFederationOutgoing.value,
                filesSharingFederationIncoming = model.filesSharingFederationIncoming.value,
                filesBigFileChunking = model.filesBigFileChunking.value,
                filesUndelete = model.filesUndelete.value,
                filesVersioning = model.filesVersioning.value
            )
        }
}
