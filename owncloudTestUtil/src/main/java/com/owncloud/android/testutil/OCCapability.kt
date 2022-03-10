/**
 * ownCloud Android client application
 *
 * Copyright (C) 2022 ownCloud GmbH.
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


package com.owncloud.android.testutil

import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability

val OC_CAPABILITY =
    OCCapability(
        accountName = OC_ACCOUNT_NAME,
        versionMayor = 2,
        versionMinor = 1,
        versionMicro = 0,
        versionString = "1.0.0",
        versionEdition = "1.0.0",
        corePollInterval = 0,
        davChunkingVersion = "1.0",
        filesSharingApiEnabled = CapabilityBooleanType.TRUE,
        filesSharingPublicEnabled = CapabilityBooleanType.TRUE,
        filesSharingPublicPasswordEnforced = CapabilityBooleanType.FALSE,
        filesSharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.FALSE,
        filesSharingPublicPasswordEnforcedReadWrite = CapabilityBooleanType.FALSE,
        filesSharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.FALSE,
        filesSharingPublicExpireDateEnabled = CapabilityBooleanType.FALSE,
        filesSharingPublicExpireDateDays = 0,
        filesSharingPublicExpireDateEnforced = CapabilityBooleanType.FALSE,
        filesSharingPublicUpload = CapabilityBooleanType.FALSE,
        filesSharingPublicMultiple = CapabilityBooleanType.FALSE,
        filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.FALSE,
        filesSharingResharing = CapabilityBooleanType.FALSE,
        filesSharingFederationOutgoing = CapabilityBooleanType.FALSE,
        filesSharingFederationIncoming = CapabilityBooleanType.FALSE,
        filesSharingUserProfilePicture = CapabilityBooleanType.FALSE,
        filesBigFileChunking = CapabilityBooleanType.FALSE,
        filesUndelete = CapabilityBooleanType.FALSE,
        filesVersioning = CapabilityBooleanType.FALSE
    )
