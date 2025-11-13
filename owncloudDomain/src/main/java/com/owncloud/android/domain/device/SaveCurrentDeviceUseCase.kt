package com.owncloud.android.domain.device

import com.owncloud.android.domain.device.model.Device

class SaveCurrentDeviceUseCase(
    private val currentDeviceRepository: CurrentDeviceRepository
) {

    operator fun invoke(device: Device) {
        currentDeviceRepository.saveCurrentDevicePaths(device)
    }
}