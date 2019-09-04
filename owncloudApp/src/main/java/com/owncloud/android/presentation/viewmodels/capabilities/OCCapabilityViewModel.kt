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
import com.owncloud.android.domain.capabilities.usecases.CapabilitiesLiveDataUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesUseCase
import com.owncloud.android.operations.common.OperationType
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View Model to keep a reference to the capability repository and an up-to-date capability
 */
class OCCapabilityViewModel(
    val context: Context,
    val account: Account,
    capabilitiesLiveDataUseCase: CapabilitiesLiveDataUseCase = CapabilitiesLiveDataUseCase(context, account),
    private val refreshCapabilitiesUseCase: RefreshCapabilitiesUseCase = RefreshCapabilitiesUseCase(context, account)
) : ViewModel() {

    private val _capabilities = MutableLiveData<UIResult<OCCapabilityEntity>>()
    val capabilities: LiveData<UIResult<OCCapabilityEntity>> = _capabilities

    private var capabilitiesLiveData: LiveData<OCCapabilityEntity>? = capabilitiesLiveDataUseCase.execute(
        CapabilitiesLiveDataUseCase.Params(
            accountName = account.name
        )
    ).data

    // to detect changes in capabilities
    private val capabilitiesObserver: Observer<OCCapabilityEntity> = Observer { capabilities ->
        if (capabilities != null) {
            _capabilities.postValue(UIResult.success(capabilities))
        }
    }

    init {
        capabilitiesLiveData?.observeForever(capabilitiesObserver)
        refreshCapabilitiesFromNetwork()
    }

    private fun refreshCapabilitiesFromNetwork() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _capabilities.postValue(
                    UIResult.loading(capabilitiesLiveData?.value)
                )

                refreshCapabilitiesUseCase.execute(
                    RefreshCapabilitiesUseCase.Params(
                        accountName = account.name
                    )
                ).also { useCaseResult ->
                    if (!useCaseResult.isSuccess()) {
                        _capabilities.postValue(
                            UIResult.error(
                                capabilitiesLiveData?.value,
                                errorMessage = useCaseResult.msg ?: ErrorMessageAdapter.getResultMessage(
                                    useCaseResult.code,
                                    useCaseResult.exception,
                                    OperationType.GET_CAPABILITIES,
                                    context.resources
                                )
                            )
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
