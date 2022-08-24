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
package com.owncloud.android.ui.fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.capabilities.usecases.GetCapabilitiesAsLiveDataUseCase
import com.owncloud.android.domain.files.GetUrlToOpenInWebUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider

class FileDetailViewModel(
    private val openInWebUseCase: GetUrlToOpenInWebUseCase,
    getCapabilitiesAsLiveDataUseCase: GetCapabilitiesAsLiveDataUseCase,
    val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    val contextProvider: ContextProvider,
) : ViewModel() {
    var capabilities: LiveData<OCCapability?> =
        getCapabilitiesAsLiveDataUseCase.execute(GetCapabilitiesAsLiveDataUseCase.Params(AccountUtils.getCurrentOwnCloudAccount(contextProvider.getContext()).name))

    private val _openInWebUriLiveData: MediatorLiveData<Event<UIResult<String>>> = MediatorLiveData()
    val openInWebUriLiveData: LiveData<Event<UIResult<String>>> = _openInWebUriLiveData

    fun isOpenInWebAvailable(): Boolean = capabilities.value?.isOpenInWebAllowed() ?: false

    fun openInWeb(fileId: String) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            liveData = _openInWebUriLiveData,
            useCase = openInWebUseCase,
            useCaseParams = GetUrlToOpenInWebUseCase.Params(openWebEndpoint = capabilities.value?.filesOcisProviders?.openWebUrl!!, fileId = fileId),
            showLoading = false,
            requiresConnection = true,
        )
    }
}
