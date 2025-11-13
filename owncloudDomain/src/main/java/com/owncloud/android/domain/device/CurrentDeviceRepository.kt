package com.owncloud.android.domain.device

import com.owncloud.android.domain.device.model.Device
import com.owncloud.android.domain.device.model.DevicePathType

interface CurrentDeviceRepository {
    fun saveCurrentDevicePaths(device: Device)
    fun getCurrentDevicePaths(): Map<DevicePathType, String>

    fun clearCurrentDevicePaths()
}