/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.presentation.sharing

import androidx.lifecycle.ViewModel
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.domain.roles.usecases.GetRolesAsyncUseCase
import com.owncloud.android.domain.sharing.shares.usecases.GetOcisSharesAsyncUseCase
import com.owncloud.android.domain.sharing.shares.model.OCPermissions
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OcisShareViewModel(
    private val getRolesAsyncUseCase: GetRolesAsyncUseCase,
    private val getOcisSharesAsyncUseCase: GetOcisSharesAsyncUseCase,
    private val accountName: String,
    private val file: OCFile,
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider,
) : ViewModel() {

    private val _roles = MutableStateFlow<Event<UIResult<List<OCRole>>>?>(null)
    val roles: StateFlow<Event<UIResult<List<OCRole>>>?> = _roles

    private val _shares = MutableStateFlow<Event<UIResult<OCPermissions>>?>(null)
    val shares: StateFlow<Event<UIResult<OCPermissions>>?> = _shares

    init {
        runUseCaseWithResult(
            coroutineDispatcher = coroutineDispatcherProvider.io,
            flow = _roles,
            useCase = getRolesAsyncUseCase,
            useCaseParams = GetRolesAsyncUseCase.Params(accountName = accountName),
        )
    }

    fun getOcisShares() = runUseCaseWithResult(
        coroutineDispatcher = coroutineDispatcherProvider.io,
        showLoading = true,
        flow = _shares,
        useCase = getOcisSharesAsyncUseCase,
        useCaseParams = GetOcisSharesAsyncUseCase.Params(
            accountName = accountName,
            spaceId = file.spaceId.orEmpty(),
            itemId = file.remoteId.orEmpty()
        )
    )
}
