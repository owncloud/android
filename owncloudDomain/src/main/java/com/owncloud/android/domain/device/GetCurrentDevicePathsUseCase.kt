package com.owncloud.android.domain.device

import com.owncloud.android.domain.device.model.DevicePathType

class GetCurrentDevicePathsUseCase(
    private val currentDeviceRepository: CurrentDeviceRepository
) {

    operator fun invoke(): Map<DevicePathType, String> = currentDeviceRepository.getCurrentDevicePaths()
}

