package com.owncloud.android.domain.device.model

/**
 * Represents a device with multiple access paths
 *
 * @property availablePaths Map of all available paths to access this device
 * @property preferredPath The preferred path to use for accessing this device
 */
data class Device(
    val id: String,
    val availablePaths: Map<DevicePathType, DevicePath>,
    // TODO: remove it once dynamic switching implemented
    val preferredPath: DevicePath,
    val certificateCommonName: String = "",
) {

    val name: String
        get() = preferredPath.hostName
}

