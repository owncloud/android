package com.owncloud.android.domain.remoteaccess.model

data class RemoteAccessDevice(
    val seagateDeviceId: String,
    val friendlyName: String,
    val hostname: String,
    val certificateCommonName: String,
)
