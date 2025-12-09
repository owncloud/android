package com.owncloud.android.domain.remoteaccess.usecases

import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository

class GetExistingRemoteAccessUserUseCase(
    private val remoteAccessRepository: RemoteAccessRepository
) {

    fun execute(): String? = remoteAccessRepository.getUserName()
}