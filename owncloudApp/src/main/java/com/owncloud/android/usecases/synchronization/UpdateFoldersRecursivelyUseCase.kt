package com.owncloud.android.usecases.synchronization

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.files.FileRepository

class UpdateFoldersRecursivelyUseCase(
    private val fileRepository: FileRepository,
) : BaseUseCaseWithResult<Unit, UpdateFoldersRecursivelyUseCase.Params>() {

    override fun run(params: Params) {
        val accountName = params.accountName

        fileRepository.refreshFoldersRecursively(
            accountName = accountName,
        )
    }

    data class Params(
        val accountName: String,
    )
}
