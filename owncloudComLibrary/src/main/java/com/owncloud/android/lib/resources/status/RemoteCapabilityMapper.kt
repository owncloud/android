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

package com.owncloud.android.lib.resources.status

import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.mappers.RemoteMapper

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
                filesSharingApiEnabled = remote.filesSharingApiEnabled,
                filesSharingPublicEnabled = remote.filesSharingPublicEnabled,
                filesSharingPublicPasswordEnforced = remote.filesSharingPublicPasswordEnforced,
                filesSharingPublicPasswordEnforcedReadOnly = remote.filesSharingPublicPasswordEnforcedReadOnly,
                filesSharingPublicPasswordEnforcedReadWrite = remote.filesSharingPublicPasswordEnforcedReadWrite,
                filesSharingPublicPasswordEnforcedUploadOnly = remote.filesSharingPublicPasswordEnforcedUploadOnly,
                filesSharingPublicExpireDateEnabled = remote.filesSharingPublicExpireDateEnabled,
                filesSharingPublicExpireDateDays = remote.filesSharingPublicExpireDateDays,
                filesSharingPublicExpireDateEnforced = remote.filesSharingPublicExpireDateEnforced,
                filesSharingPublicSendMail = remote.filesSharingPublicSendMail,
                filesSharingPublicUpload = remote.filesSharingPublicUpload,
                filesSharingPublicMultiple = remote.filesSharingPublicMultiple,
                filesSharingPublicSupportsUploadOnly = remote.filesSharingPublicSupportsUploadOnly,
                filesSharingUserSendMail = remote.filesSharingUserSendMail,
                filesSharingResharing = remote.filesSharingResharing,
                filesSharingFederationOutgoing = remote.filesSharingFederationOutgoing,
                filesSharingFederationIncoming = remote.filesSharingFederationIncoming,
                filesBigFileChunking = remote.filesBigFileChunking,
                filesUndelete = remote.filesUndelete,
                filesVersioning = remote.filesVersioning
            )
        }

    override fun toRemote(model: OCCapability?): RemoteCapability? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
