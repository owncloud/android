/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.presentation.viewmodels.capabilities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.capabilities.usecases.GetCapabilitiesAsLiveDataUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch

/**
 * View Model to keep a reference to the capability repository and an up-to-date capability
 */
class OCCapabilityViewModel(
    private val accountName: String,
    getCapabilitiesAsLiveDataUseCase: GetCapabilitiesAsLiveDataUseCase,
    private val refreshCapabilitiesFromServerUseCase: RefreshCapabilitiesFromServerAsyncUseCase,
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _capabilities = MediatorLiveData<Event<UIResult<OCCapability>>>()
    val capabilities: LiveData<Event<UIResult<OCCapability>>> = _capabilities

    private var capabilitiesLiveData: LiveData<OCCapability?> = getCapabilitiesAsLiveDataUseCase.execute(
        GetCapabilitiesAsLiveDataUseCase.Params(
            accountName = accountName
        )
    )

    init {
        _capabilities.addSource(capabilitiesLiveData) { capabilities ->
            _capabilities.postValue(Event(UIResult.Success(capabilities)))
        }

        refreshCapabilitiesFromNetwork()
    }

    fun refreshCapabilitiesFromNetwork() {
        viewModelScope.launch(coroutineDispatcherProvider.io) {
            viewModelScope.launch(coroutineDispatcherProvider.main) {
                _capabilities.postValue(
                    Event(UIResult.Loading(capabilitiesLiveData.value))
                )
            }

            val useCaseResult = refreshCapabilitiesFromServerUseCase.execute(
                RefreshCapabilitiesFromServerAsyncUseCase.Params(
                    accountName = accountName
                )
            )

            viewModelScope.launch(coroutineDispatcherProvider.main) {
                if (useCaseResult.isError) {
                    _capabilities.postValue(
                        Event(UIResult.Error(useCaseResult.getThrowableOrNull(), capabilitiesLiveData.value))
                    )
                }
            }
        }
    }
}
