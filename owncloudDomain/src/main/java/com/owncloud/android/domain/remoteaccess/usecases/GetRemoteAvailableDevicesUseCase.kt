package com.owncloud.android.domain.remoteaccess.usecases

import com.owncloud.android.domain.device.model.Device
import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository

class GetRemoteAvailableDevicesUseCase(
    private val remoteAccessRepository: RemoteAccessRepository
) {

    suspend fun execute(): List<Device> =
        remoteAccessRepository.getAvailableDevices()

    suspend fun currentDevice(): Device? =
        remoteAccessRepository.getCurrentDevice()
}

