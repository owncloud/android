/**
 * ownCloud Android client application
 *
 * @author Manuel Plazas Palacio
 *
 * Copyright (C) 2023 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.exceptions.DeepLinkException
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.PATH_SEPARATOR
import java.net.URI

class ManageDeepLinkUseCase(
    private val fileRepository: FileRepository,
) :
    BaseUseCaseWithResult<OCFile?, ManageDeepLinkUseCase.Params>() {

    override fun run(params: Params): OCFile? {
        val path = params.uri.fragment ?: params.uri.path
        val pathParts = path.split(PATH_SEPARATOR)
        if (pathParts[pathParts.size - 2] != DEEP_LINK_PREVIOUS_PATH_SEGMENT) {
            throw DeepLinkException()
        }

        return fileRepository.getFileFromRemoteId(pathParts[pathParts.size - 1], params.accountName)
    }

    data class Params(val uri: URI, val accountName: String)

    companion object {
        const val DEEP_LINK_PREVIOUS_PATH_SEGMENT = "f"
    }

}
