package com.owncloud.android.domain.remoteaccess.usecases

import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessPath

class GetRemoteAccessDeviceByIdUseCase(
    private val remoteAccessRepository: RemoteAccessRepository
) {

    suspend fun execute(deviceId: String): List<RemoteAccessPath> =
        remoteAccessRepository.getDeviceById(deviceId)
}

