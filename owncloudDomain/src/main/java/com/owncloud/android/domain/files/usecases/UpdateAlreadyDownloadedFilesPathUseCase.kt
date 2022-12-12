/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.files.FileRepository

class UpdateAlreadyDownloadedFilesPathUseCase(
    private val fileRepository: FileRepository,
) : BaseUseCase<Unit, UpdateAlreadyDownloadedFilesPathUseCase.Params>() {

    override fun run(params: Params) {
        fileRepository.updateDownloadedFilesStorageDirectoryInStoragePath(params.oldDirectory, params.newDirectory)
    }

    data class Params(
        val oldDirectory: String,
        val newDirectory: String
    )
}
