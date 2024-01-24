/**
 * ownCloud Android client application
 *
 * @author Parneet Singh
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
package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFile

class GetIfFileFolderLocalUseCase(private val fileRepository: FileRepository) : BaseUseCaseWithResult<Boolean, GetIfFileFolderLocalUseCase.Params>() {

    override fun run(params: Params): Boolean = getIfFileFolderLocal(params.listOfFiles)

    private fun getIfFileFolderLocal(listOfFiles: List<OCFile>): Boolean {
        listOfFiles.forEach { file ->
            if (file.isFolder) {
                if (getIfFileFolderLocal(fileRepository.getFolderContent(file.id!!))) return true
            } else {
                if (file.isAvailableLocally) return true
            }
        }
        return false
    }

    data class Params(val listOfFiles: List<OCFile>)

}
