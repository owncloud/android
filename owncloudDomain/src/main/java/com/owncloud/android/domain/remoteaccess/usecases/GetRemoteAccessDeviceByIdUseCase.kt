package com.owncloud.android.domain.remoteaccess.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository
import com.owncloud.android.domain.remoteaccess.model.RemoteAccessPath

class GetRemoteAccessDeviceByIdUseCase(
    private val remoteAccessRepository: RemoteAccessRepository
) : BaseUseCaseWithResult<List<RemoteAccessPath>, GetRemoteAccessDeviceByIdUseCase.Params>() {

    override fun run(params: Params): List<RemoteAccessPath> =
        remoteAccessRepository.getDeviceById(params.deviceId)

    data class Params(
        val deviceId: String
    )
}

