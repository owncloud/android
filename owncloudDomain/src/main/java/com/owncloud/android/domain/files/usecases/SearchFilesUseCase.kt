package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFile

class SearchFilesUseCase(
    private val fileRepository: FileRepository
) : BaseUseCase<List<OCFile>, SearchFilesUseCase.Params>() {

    override fun run(params: Params): List<OCFile> {
        return if (params.searchPattern.isBlank()) {
            emptyList()
        } else {
            fileRepository.searchFiles(params.searchPattern, params.ignoreCase)
        }
    }

    data class Params(val searchPattern: String, val ignoreCase: Boolean = true)

}
