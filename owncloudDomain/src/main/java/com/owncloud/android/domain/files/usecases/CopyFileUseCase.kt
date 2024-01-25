/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
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
import com.owncloud.android.domain.exceptions.CopyIntoDescendantException
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFile

/**
 * Copy a list of files with the SAME hierarchy to a target folder.
 *
 * Copying files to a descendant or copying files to the same directory will throw an exception.
 */
class CopyFileUseCase(
    private val fileRepository: FileRepository,
) : BaseUseCaseWithResult<List<OCFile>, CopyFileUseCase.Params>() {

    override fun run(params: Params): List<OCFile> {
        validateOrThrowException(params.listOfFilesToCopy, params.targetFolder)
        return fileRepository.copyFile(
            listOfFilesToCopy = params.listOfFilesToCopy,
            targetFolder = params.targetFolder,
            replace = params.replace,
            isUserLogged = params.isUserLogged,
        )
    }

    @Throws(IllegalArgumentException::class, CopyIntoDescendantException::class)
    fun validateOrThrowException(listOfFilesToCopy: List<OCFile>, targetFolder: OCFile) {
        require(listOfFilesToCopy.isNotEmpty())

        if (listOfFilesToCopy.any { targetFolder.remotePath.startsWith(it.remotePath) && targetFolder.spaceId == it.spaceId }) {
            throw CopyIntoDescendantException()
        }
    }

    data class Params(
        val listOfFilesToCopy: List<OCFile>,
        val targetFolder: OCFile,
        val replace: List<Boolean?> = emptyList(),
        val isUserLogged: Boolean,
    )
}
