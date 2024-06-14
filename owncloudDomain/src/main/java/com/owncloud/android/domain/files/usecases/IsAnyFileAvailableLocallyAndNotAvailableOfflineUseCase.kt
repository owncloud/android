/**
 * ownCloud Android client application
 *
 * @author Parneet Singh
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

package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFile

class IsAnyFileAvailableLocallyAndNotAvailableOfflineUseCase(private val fileRepository: FileRepository) :
    BaseUseCaseWithResult<Boolean, IsAnyFileAvailableLocallyAndNotAvailableOfflineUseCase.Params>() {

    override fun run(params: Params): Boolean = isAnyFileAvailableLocallyAndNotAvailableOffline(params.listOfFiles)
    private fun isAnyFileAvailableLocallyAndNotAvailableOffline(filesToRemove: List<OCFile>): Boolean {

        if (filesToRemove.any { it.isAvailableLocally && !it.isAvailableOffline }) {
            return true
        } else {
            filesToRemove.filter { it.isFolder }.forEach { folder ->
                if (isAnyFileAvailableLocallyAndNotAvailableOffline(fileRepository.getFolderContent(folder.id!!))) {
                    return true
                }
            }
        }
        return false
    }

    data class Params(val listOfFiles: List<OCFile>)

}
