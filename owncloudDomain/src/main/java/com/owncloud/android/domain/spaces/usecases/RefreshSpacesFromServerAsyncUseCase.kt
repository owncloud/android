/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2022 ownCloud GmbH.
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
package com.owncloud.android.domain.spaces.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.spaces.SpacesRepository
import com.owncloud.android.domain.user.usecases.GetUserIdAsyncUseCase

class RefreshSpacesFromServerAsyncUseCase(
    private val spacesRepository: SpacesRepository,
    private val getUserIdAsyncUseCase: GetUserIdAsyncUseCase
) : BaseUseCaseWithResult<Unit, RefreshSpacesFromServerAsyncUseCase.Params>() {

    override fun run(params: Params) {
        val userId = when (val userIdResult = getUserIdAsyncUseCase(GetUserIdAsyncUseCase.Params(params.accountName))) {
            is UseCaseResult.Error -> ""
            is UseCaseResult.Success -> userIdResult.data
        }
        spacesRepository.refreshSpacesForAccount(accountName = params.accountName, userId = userId)

    }

    data class Params(
        val accountName: String,
    )
}
