package com.owncloud.android.domain.remoteaccess.usecases

import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessDevice

class GetRemoteAccessDevicesUseCase(
    private val remoteAccessRepository: RemoteAccessRepository
) {

    suspend fun execute(): List<RemoteAccessDevice> =
        remoteAccessRepository.getDevices()
}

