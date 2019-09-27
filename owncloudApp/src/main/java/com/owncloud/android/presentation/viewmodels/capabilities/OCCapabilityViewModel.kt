/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

import android.accounts.Account
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.domain.capabilities.usecases.GetCapabilitiesAsLiveDataUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.presentation.UIResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View Model to keep a reference to the capability repository and an up-to-date capability
 */
class OCCapabilityViewModel(
    val context: Context,
    val account: Account,
    getCapabilitiesAsLiveDataUseCase: GetCapabilitiesAsLiveDataUseCase = GetCapabilitiesAsLiveDataUseCase(
        context,
        account
    ),
    private val refreshCapabilitiesFromServerUseCase: RefreshCapabilitiesFromServerAsyncUseCase = RefreshCapabilitiesFromServerAsyncUseCase(
        context,
        account
    )
) : ViewModel() {

    private val _capabilities = MutableLiveData<UIResult<OCCapabilityEntity>>()
    val capabilities: LiveData<UIResult<OCCapabilityEntity>> = _capabilities

    private var capabilitiesLiveData: LiveData<OCCapabilityEntity>? = getCapabilitiesAsLiveDataUseCase.execute(
        GetCapabilitiesAsLiveDataUseCase.Params(
            accountName = account.name
        )
    ).getDataOrNull()

    // to detect changes in capabilities
    private val capabilitiesObserver: Observer<OCCapabilityEntity> = Observer { capabilities ->
        if (capabilities != null) {
            _capabilities.postValue(UIResult.Success(capabilities))
        }
    }

    init {
        capabilitiesLiveData?.observeForever(capabilitiesObserver)
    }

    fun refreshCapabilitiesFromNetwork() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _capabilities.postValue(
                    UIResult.Loading(capabilitiesLiveData?.value)
                )

                refreshCapabilitiesFromServerUseCase.execute(
                    RefreshCapabilitiesFromServerAsyncUseCase.Params(
                        accountName = account.name
                    )
                ).also { useCaseResult ->
                    if (!useCaseResult.isSuccess) {
                        _capabilities.postValue(
                            UIResult.Error(useCaseResult.getThrowableOrNull(), capabilitiesLiveData?.value)
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        capabilitiesLiveData?.removeObserver(capabilitiesObserver)
    }
}
