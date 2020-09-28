package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.files.model.OCFile

class SaveFileOrFolderUseCase(
    private val file: OCFile
) : BaseUseCase<OCFile, SaveFileOrFolderUseCase.Params>() {
    override fun run(params: Params): OCFile {
        TODO("Not yet implemented")
    }

    data class Params(val remotePath: String)
}
