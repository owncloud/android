package com.owncloud.android.data.device

import com.owncloud.android.domain.device.CurrentDeviceRepository
import com.owncloud.android.domain.device.model.Device
import com.owncloud.android.domain.device.model.DevicePathType

class HCCurrentDeviceRepository(
    private val currentDeviceStorage: CurrentDeviceStorage
) : CurrentDeviceRepository {

    override fun saveCurrentDevicePaths(device: Device) {
        device.availablePaths.forEach {
            currentDeviceStorage.saveDeviceBaseUrl(it.value.hostUrl, it.key.name)
        }
    }

    override fun getCurrentDevicePaths(): Map<DevicePathType, String> {
        val paths = mutableMapOf<DevicePathType, String>()
        DevicePathType.entries.forEach {
            currentDeviceStorage.getDeviceBaseUrl(it.name)?.let { url ->
                paths[it] = url
            }
        }
        return paths
    }

    override fun clearCurrentDevicePaths() {
        currentDeviceStorage.clearDevicePaths()
    }
}