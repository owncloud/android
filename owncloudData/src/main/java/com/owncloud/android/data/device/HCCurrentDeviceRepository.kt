package com.owncloud.android.data.device

import com.owncloud.android.domain.device.CurrentDeviceRepository
import com.owncloud.android.domain.device.model.Device
import com.owncloud.android.domain.device.model.DevicePathType

class HCCurrentDeviceRepository(
    private val currentDeviceStorage: CurrentDeviceStorage
) : CurrentDeviceRepository {

    override fun saveCurrentDevice(device: Device) {
        device.availablePaths.forEach {
            currentDeviceStorage.saveDeviceBaseUrl(pathType = it.key.name, baseUrl = it.value)
        }
        currentDeviceStorage.saveCertificateCommonName(device.certificateCommonName)
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