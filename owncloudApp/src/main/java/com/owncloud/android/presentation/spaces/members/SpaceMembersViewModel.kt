/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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

package com.owncloud.android.presentation.spaces.members

import androidx.lifecycle.ViewModel
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.SpaceMembers
import com.owncloud.android.domain.spaces.usecases.GetSpaceMembersUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SpaceMembersViewModel(
    private val getSpaceMembersUseCase: GetSpaceMembersUseCase,
    private val accountName: String,
    private val space: OCSpace,
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider
): ViewModel() {

    private val _spaceMembers = MutableStateFlow<Event<UIResult<SpaceMembers>>?>(null)
    val spaceMembers: StateFlow<Event<UIResult<SpaceMembers>>?> = _spaceMembers

    init {
        runUseCaseWithResult(
            coroutineDispatcher = coroutineDispatcherProvider.io,
            flow = _spaceMembers,
            useCase = getSpaceMembersUseCase,
            useCaseParams = GetSpaceMembersUseCase.Params(accountName = accountName, spaceId = space.id),
            showLoading = false,
            requiresConnection = true
        )
    }

}
