/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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

package com.owncloud.android.domain.sharing.shares.usecases

import com.owncloud.android.domain.sharing.shares.ShareRepository
import com.owncloud.android.domain.BaseUseCaseWithResult

class RefreshOcsSharesFromServerAsyncUseCase(
    private val shareRepository: ShareRepository
) : BaseUseCaseWithResult<Unit, RefreshOcsSharesFromServerAsyncUseCase.Params>() {

    override fun run(params: Params) =
        shareRepository.refreshSharesFromNetwork(
            params.filePath,
            params.accountName
        )

    data class Params(
        val filePath: String,
        val accountName: String
    )
}
