/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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
import com.owncloud.android.domain.exceptions.MoveIntoDescendantException
import com.owncloud.android.domain.exceptions.MoveIntoSameFolderException
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFile

/**
 * Move a list of files with the SAME hierarchy to a target folder.
 *
 * Moving files to a descendant or moving files to the same directory will throw an exception.
 */
class MoveFileUseCase(
    private val fileRepository: FileRepository
) : BaseUseCaseWithResult<Unit, MoveFileUseCase.Params>() {

    override fun run(params: Params) {
        validateOrThrowException(params.listOfFilesToMove, params.targetFolder)

        return fileRepository.moveFile(
            listOfFilesToMove = params.listOfFilesToMove,
            targetFile = params.targetFolder
        )
    }

    @Throws(IllegalArgumentException::class, MoveIntoSameFolderException::class, MoveIntoDescendantException::class)
    fun validateOrThrowException(listOfFilesToMove: List<OCFile>, targetFolder: OCFile) {
        require(listOfFilesToMove.isNotEmpty())

        if (listOfFilesToMove.any { targetFolder.remotePath.startsWith(it.remotePath) }) {
            throw MoveIntoDescendantException()
        } else if (listOfFilesToMove.any { it.parentId == targetFolder.id }) {
            throw MoveIntoSameFolderException()
        }
    }

    data class Params(
        val listOfFilesToMove: List<OCFile>,
        val targetFolder: OCFile
    )
}
