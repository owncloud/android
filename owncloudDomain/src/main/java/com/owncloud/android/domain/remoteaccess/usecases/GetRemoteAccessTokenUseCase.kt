package com.owncloud.android.domain.remoteaccess.usecases

import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository

class GetRemoteAccessTokenUseCase(
    private val remoteAccessRepository: RemoteAccessRepository
) {

    suspend fun execute(reference: String, code: String) =
        remoteAccessRepository.getToken(reference, code)
}

