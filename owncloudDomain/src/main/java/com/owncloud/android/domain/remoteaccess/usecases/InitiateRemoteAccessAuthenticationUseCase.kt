package com.owncloud.android.domain.remoteaccess.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository

class InitiateRemoteAccessAuthenticationUseCase(
    private val remoteAccessRepository: RemoteAccessRepository
) : BaseUseCaseWithResult<String, InitiateRemoteAccessAuthenticationUseCase.Params>() {

    override fun run(params: Params): String =
        remoteAccessRepository.initiateAuthentication(
            email = params.email,
            clientId = params.clientId,
            clientFriendlyName = params.clientFriendlyName
        )

    data class Params(
        val email: String,
        val clientId: String,
        val clientFriendlyName: String
    )
}

