/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
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
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.validator.FileNameValidator

class RenameFileUseCase(
    private val fileRepository: FileRepository,
    private val setLastUsageFileUseCase: SetLastUsageFileUseCase,
) : BaseUseCaseWithResult<Unit, RenameFileUseCase.Params>() {

    private val fileNameValidator = FileNameValidator()

    override fun run(params: Params) {
        fileNameValidator.validateOrThrowException(params.newName)
        val isAvailableLocally = params.ocFileToRename.isAvailableLocally

        fileRepository.renameFile(
            ocFile = params.ocFileToRename,
            newName = params.newName,
        )
        setLastUsageFile(params.ocFileToRename, isAvailableLocally)
    }

    private fun setLastUsageFile(file: OCFile, isAvailableLocally: Boolean) {
        setLastUsageFileUseCase(
            SetLastUsageFileUseCase.Params(
                fileId = file.id!!,
                lastUsage = System.currentTimeMillis(),
                isAvailableLocally = isAvailableLocally,
                isFolder = file.isFolder,
            )
        )
    }

    data class Params(
        val ocFileToRename: OCFile,
        val newName: String
    )
}
