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
        filesBigFileChunking = CapabilityBooleanType.FALSE,
        filesUndelete = CapabilityBooleanType.FALSE,
        filesVersioning = CapabilityBooleanType.FALSE
    )
