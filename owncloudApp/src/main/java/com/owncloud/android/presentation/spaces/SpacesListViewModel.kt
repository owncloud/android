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
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.usecases.GetProjectSpacesWithSpecialsForAccountAsStreamUseCase
import com.owncloud.android.domain.spaces.usecases.RefreshSpacesFromServerAsyncUseCase
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpacesListViewModel(
    private val refreshSpacesFromServerAsyncUseCase: RefreshSpacesFromServerAsyncUseCase,
    private val getProjectSpacesWithSpecialsForAccountAsStreamUseCase: GetProjectSpacesWithSpecialsForAccountAsStreamUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val account: Account,
) : ViewModel() {

    private val _spacesList: MutableStateFlow<SpacesListUiState> =
        MutableStateFlow(SpacesListUiState(spaces = emptyList(), refreshing = false, error = null))
    val spacesList: StateFlow<SpacesListUiState> = _spacesList

    init {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            refreshSpacesFromServer()
            getProjectSpacesWithSpecialsForAccountAsStreamUseCase.execute(
                GetProjectSpacesWithSpecialsForAccountAsStreamUseCase.Params(accountName = account.name)
            ).collect { spaces ->
                _spacesList.update { it.copy(spaces = spaces) }
            }
        }
    }

    fun refreshSpacesFromServer() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            _spacesList.update { it.copy(refreshing = true) }
            when (val result = refreshSpacesFromServerAsyncUseCase.execute(RefreshSpacesFromServerAsyncUseCase.Params(account.name))) {
                is UseCaseResult.Success -> _spacesList.update { it.copy(refreshing = false, error = null) }
                is UseCaseResult.Error -> _spacesList.update { it.copy(refreshing = false, error = result.throwable) }
            }
        }
    }

    data class SpacesListUiState(
        val spaces: List<OCSpace>,
        val refreshing: Boolean,
        val error: Throwable?,
    )
}
