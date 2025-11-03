package com.owncloud.android.domain.remoteaccess.usecases

import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository
import com.owncloud.android.domain.server.model.Server

class GetRemoteAvailableServersUseCase(
    private val remoteAccessRepository: RemoteAccessRepository
) {

    suspend fun execute(): List<Server> =
        remoteAccessRepository.getAvailableServers()
}

