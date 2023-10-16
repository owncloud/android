package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.exceptions.DeepLinkException
import com.owncloud.android.domain.files.model.OCFile
import java.net.URI

class ManageDeepLinkUseCase : BaseUseCaseWithResult<OCFile?, ManageDeepLinkUseCase.Params>() {

    override fun run(params: Params): OCFile? {
        val pathParts = params.uri.path.split(PATH_SEPARATOR)
        if (pathParts[pathParts.size - 2] != DEEP_LINK_PREVIOUS_PATH_SEGMENT) {
            throw DeepLinkException()
        }
        return null
    }

    data class Params(val uri: URI)

    companion object {
        const val PATH_SEPARATOR = "/"
        const val DEEP_LINK_PREVIOUS_PATH_SEGMENT = "f"
    }

}