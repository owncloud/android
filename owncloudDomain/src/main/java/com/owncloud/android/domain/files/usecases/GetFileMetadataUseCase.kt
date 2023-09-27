package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.files.FileRepository

class GetFileMetadataUseCase(private val fileRepository: FileRepository) : BaseUseCaseWithResult<String?, GetFileMetadataUseCase.Params>() {

    override fun run(params: Params): String? {
        return fileRepository.getFileMetadata(params.id, params.accountName)
    }

    data class Params(val id: String, val accountName: String)

}