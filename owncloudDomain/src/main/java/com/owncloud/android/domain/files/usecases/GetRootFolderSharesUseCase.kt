/**
 * ownCloud Android client application
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

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFile

class GetRootFolderSharesUseCase(
    private val fileRepository: FileRepository
) : BaseUseCase<OCFile?, GetRootFolderSharesUseCase.Params>() {

    override fun run(params: Params): OCFile? =
        fileRepository.getRootFolderShares(owner = params.owner)

    data class Params(
        val owner: String,
    )
}
