package com.owncloud.android.domain.remoteaccess.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.remoteaccess.RemoteAccessRepository

class GetRemoteAccessTokenUseCase(
    private val remoteAccessRepository: RemoteAccessRepository
) : BaseUseCaseWithResult<Unit, GetRemoteAccessTokenUseCase.Params>() {

    override fun run(params: Params) {
        remoteAccessRepository.getToken(
            reference = params.reference,
            code = params.code
        )
    }

    data class Params(
        val reference: String,
        val code: String
    )
}

