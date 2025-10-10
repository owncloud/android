package com.owncloud.android.domain.remoteaccess.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessDevice

class GetRemoteAccessDevicesUseCase(
    private val remoteAccessRepository: RemoteAccessRepository
) : BaseUseCaseWithResult<List<RemoteAccessDevice>, Unit>() {

    override fun run(params: Unit): List<RemoteAccessDevice> =
        remoteAccessRepository.getDevices()
}

