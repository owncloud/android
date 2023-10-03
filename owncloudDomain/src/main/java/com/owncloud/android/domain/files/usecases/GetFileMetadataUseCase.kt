package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFile

class GetFileMetadataUseCase(private val fileRepository: FileRepository) : BaseUseCaseWithResult<List<OCFile>, GetFileMetadataUseCase.Params>() {

    override fun run(params: Params): List<OCFile> {
        return fileRepository.getFileMetadata(params.id, params.accountName)
    }

    data class Params(val id: String, val accountName: String)

}