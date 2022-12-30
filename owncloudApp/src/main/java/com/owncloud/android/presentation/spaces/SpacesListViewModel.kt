/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
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

package com.owncloud.android.presentation.spaces

import android.accounts.Account
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.usecases.GetProjectSpacesWithSpecialsForAccountAsStreamUseCase
import com.owncloud.android.domain.spaces.usecases.RefreshSpacesFromServerAsyncUseCase
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpacesListViewModel(
    private val refreshSpacesFromServerAsyncUseCase: RefreshSpacesFromServerAsyncUseCase,
    getProjectSpacesWithSpecialsForAccountAsStreamUseCase: GetProjectSpacesWithSpecialsForAccountAsStreamUseCase,
    coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val account: Account,
) : ViewModel() {
    val spacesList: MutableStateFlow<List<OCSpace>> = MutableStateFlow(emptyList())

    init {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            refreshSpacesFromServerAsyncUseCase.execute(RefreshSpacesFromServerAsyncUseCase.Params(account.name))
            spacesList.update { getProjectSpacesWithSpecialsForAccountAsStreamUseCase.execute(
                GetProjectSpacesWithSpecialsForAccountAsStreamUseCase.Params(
                    accountName = account.name
                )).first() }
        }
    }
}
