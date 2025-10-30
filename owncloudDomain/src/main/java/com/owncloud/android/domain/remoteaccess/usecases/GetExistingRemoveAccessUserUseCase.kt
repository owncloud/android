package com.owncloud.android.domain.remoteaccess.usecases

import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository

class GetExistingRemoveAccessUserUseCase(
    private val remoteAccessRepository: RemoteAccessRepository
) {

    fun execute(): String? = remoteAccessRepository.getUserName()
}