package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import kotlinx.coroutines.flow.Flow

class GetFileWithSyncInfoByIdUseCase(
    private val fileRepository: FileRepository
) : BaseUseCase<Flow<OCFileWithSyncInfo?>, GetFileWithSyncInfoByIdUseCase.Params>() {

    override fun run(params: Params): Flow<OCFileWithSyncInfo?> =
        fileRepository.getFileWithSyncInfoByIdAsFlow(params.fileId)

    data class Params(val fileId: Long)

}
