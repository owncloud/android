/**
 * ownCloud Android client application
 *
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

class SetLastUsageFileUseCase(
    private val fileRepository: FileRepository,
) : BaseUseCaseWithResult<Unit, SetLastUsageFileUseCase.Params>() {

    override fun run(params: Params) {
        if (params.isAvailableLocally && !params.isFolder) {
            fileRepository.updateFileWithLastUsage(params.fileId, params.lastUsage)
        }
    }

    data class Params(
        val fileId: Long,
        val lastUsage: Long?,
        val isAvailableLocally: Boolean,
        val isFolder: Boolean,
    )
}
