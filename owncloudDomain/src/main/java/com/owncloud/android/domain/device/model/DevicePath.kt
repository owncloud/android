package com.owncloud.android.domain.device.model

data class DevicePath(
    val hostName: String,
    val hostUrl: String,
    val devicePathType: DevicePathType,
)