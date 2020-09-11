package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFile

class GetFileByIdUseCase(
    private val fileRepository: FileRepository
) : BaseUseCaseWithResult<OCFile?, GetFileByIdUseCase.Params>() {

    override fun run(params: Params): OCFile? {
        return fileRepository.getFileById(params.fileId)
    }

    data class Params(val fileId: Long)

}
