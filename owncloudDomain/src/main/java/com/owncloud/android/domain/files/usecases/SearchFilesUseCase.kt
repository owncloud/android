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
            fileRepository.searchFiles(
                searchPattern = params.searchPattern,
                ignoreCase = params.ignoreCase,
                minSize = params.minSize,
                maxSize = params.maxSize,
                mimePrefix = params.mimePrefix,
                minDate = params.minDate,
                maxDate = params.maxDate,
            )
        }
    }

    data class Params(
        val searchPattern: String,
        val ignoreCase: Boolean = true,
        val minSize: Long = 0L,
        val maxSize: Long = Long.MAX_VALUE,
        val mimePrefix: String = "",
        val minDate: Long = 0L,
        val maxDate: Long = Long.MAX_VALUE,
    )

}
