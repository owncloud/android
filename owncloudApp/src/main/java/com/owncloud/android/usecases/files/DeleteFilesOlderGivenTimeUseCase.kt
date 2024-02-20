/**
 * ownCloud Android client application
 *
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.usecases.files

import com.owncloud.android.data.providers.LocalStorageProvider
import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.usecases.RemoveFileUseCase

class DeleteFilesOlderGivenTimeUseCase(
    private val fileRepository: FileRepository,
    private val removeFileUseCase: RemoveFileUseCase,
    private val localStorageProvider: LocalStorageProvider,
) : BaseUseCaseWithResult<Unit, DeleteFilesOlderGivenTimeUseCase.Params>() {
    override fun run(params: Params) {
        val listOfFilesToDelete = fileRepository.getFilesLastUsageIsOlderThanGivenTime(params.milliseconds)
        localStorageProvider.clearTemporalFilesAutomatic(params.milliseconds)
        if (listOfFilesToDelete.isNotEmpty()) {
            val listOfFilesToDeleteUpdated = listOfFilesToDelete.filter { file ->
                file.remoteId != params.idFilePreviewing
            }
            removeFileUseCase(RemoveFileUseCase.Params(listOfFilesToDeleteUpdated, true))
        }
    }

    data class Params(
        val milliseconds: Long,
        val idFilePreviewing: String?,
    )
}
