/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * Copyright (C) 2019 ownCloud GmbH.
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
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.domain.capabilities.usecases.GetCapabilitiesAsLiveDataUseCase
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.presentation.UIResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * View Model to keep a reference to the capability repository and an up-to-date capability
 */
class OCCapabilityViewModel(
    private val accountName: String,
    getCapabilitiesAsLiveDataUseCase: GetCapabilitiesAsLiveDataUseCase,
    private val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase,
    private val refreshCapabilitiesFromServerUseCase: RefreshCapabilitiesFromServerAsyncUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _capabilities = MediatorLiveData<UIResult<OCCapabilityEntity>>()
    val capabilities: LiveData<UIResult<OCCapabilityEntity>> = _capabilities

    private var capabilitiesLiveData: LiveData<OCCapabilityEntity>? = getCapabilitiesAsLiveDataUseCase.execute(
        GetCapabilitiesAsLiveDataUseCase.Params(
            accountName = accountName
        )
    ).getDataOrNull()

    init {
        _capabilities.addSource(capabilitiesLiveData!!) { capabilities ->
            _capabilities.postValue(UIResult.Success(capabilities))
        }

        refreshCapabilitiesFromNetwork()
    }

    fun getStoredCapabilities(): OCCapabilityEntity? = getStoredCapabilitiesUseCase.execute(
        GetStoredCapabilitiesUseCase.Params(
            accountName = accountName
        )
    ).getDataOrNull()

    fun refreshCapabilitiesFromNetwork() {
        viewModelScope.launch(ioDispatcher) {
            _capabilities.postValue(
                UIResult.Loading(capabilitiesLiveData?.value)
            )

            val useCaseResult = refreshCapabilitiesFromServerUseCase.execute(
                RefreshCapabilitiesFromServerAsyncUseCase.Params(
                    accountName = accountName
                )
            )

            if (!useCaseResult.isSuccess) {
                _capabilities.postValue(
                    UIResult.Error(useCaseResult.getThrowableOrNull(), capabilitiesLiveData?.value)
                )
            }
        }
    }
}
