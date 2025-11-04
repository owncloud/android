package com.owncloud.android.domain.remoteaccess.usecases

import android.os.Build
import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository
import java.util.UUID

class InitiateRemoteAccessAuthenticationUseCase(
    private val remoteAccessRepository: RemoteAccessRepository
) {

    suspend fun execute(
        email: String,
    ): String =
        remoteAccessRepository.initiateAuthentication(
            email = email,
            clientId = UUID.randomUUID().toString(),
            clientFriendlyName = Build.MODEL
        )
}

