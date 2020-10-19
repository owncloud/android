/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.extensions

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.exceptions.NoNetworkConnectionException
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.ContextProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

object ViewModelExt : KoinComponent {

    private val contextProvider: ContextProvider by inject()

    fun <T, Params> ViewModel.runAsyncUseCase(
        coroutineDispatcher: CoroutineDispatcher,
        requiresConnection: Boolean = true,
        useCase: BaseUseCaseWithResult<T, Params>,
        useCaseParams: Params
    ) {
        viewModelScope.launch(coroutineDispatcher) {

            // If usecase requires connection and is not connected, it is not needed to execute use case.
            if (requiresConnection and !contextProvider.isConnected()) {
                Timber.w("${useCase.javaClass.simpleName} will not be executed due to lack of network connection")
                return@launch
            }

            val useCaseResult = useCase.execute(useCaseParams)

            Timber.d("Use case executed: ${useCase.javaClass.simpleName} with result: $useCaseResult")
        }
    }

    fun <T, Params> ViewModel.runUseCaseWithResult(
        coroutineDispatcher: CoroutineDispatcher,
        requiresConnection: Boolean = true,
        showLoading: Boolean = false,
        liveData: MediatorLiveData<Event<UIResult<T>>>,
        useCase: BaseUseCaseWithResult<T, Params>,
        useCaseParams: Params,
        postSuccess: Boolean = true,
        postSuccessWithData: Boolean = true
    ) {
        viewModelScope.launch(coroutineDispatcher) {
            if (showLoading) {
                liveData.postValue(Event(UIResult.Loading()))
            }

            // If usecase requires connection and is not connected, it is not needed to execute use case.
            if (requiresConnection and !contextProvider.isConnected()) {
                liveData.postValue(Event(UIResult.Error(error = NoNetworkConnectionException())))
                Timber.w("${useCase.javaClass.simpleName} will not be executed due to lack of network connection")
                return@launch
            }

            val useCaseResult = useCase.execute(useCaseParams)

            Timber.d("Use case executed: ${useCase.javaClass.simpleName} with result: $useCaseResult")

            if (useCaseResult.isSuccess && postSuccess) {
                if (postSuccessWithData) {
                    liveData.postValue(Event(UIResult.Success(useCaseResult.getDataOrNull())))
                } else {
                    liveData.postValue(Event(UIResult.Success()))
                }
            } else if (useCaseResult.isError) {
                liveData.postValue(Event(UIResult.Error(error = useCaseResult.getThrowableOrNull())))
            }
        }
    }

    fun <T, U, Params> ViewModel.runUseCaseWithResultAndUseCachedData(
        coroutineDispatcher: CoroutineDispatcher,
        requiresConnection: Boolean = true,
        cachedData: T?,
        liveData: MediatorLiveData<Event<UIResult<T>>>,
        useCase: BaseUseCaseWithResult<U, Params>,
        useCaseParams: Params
    ) {
        viewModelScope.launch(coroutineDispatcher) {
            liveData.postValue(Event(UIResult.Loading(cachedData)))

            // If usecase requires connection and is not connected, it is not needed to execute use case.
            if (requiresConnection && !contextProvider.isConnected()) {
                liveData.postValue(Event(UIResult.Error(error = NoNetworkConnectionException(), data = cachedData)))
                Timber.w("${useCase.javaClass.simpleName} will not be executed due to lack of network connection")
                return@launch
            }

            val useCaseResult = useCase.execute(useCaseParams)

            Timber.d("Use case executed: ${useCase.javaClass.simpleName} with result: $useCaseResult")

            if (useCaseResult.isError) {
                liveData.postValue(Event(UIResult.Error(error = useCaseResult.getThrowableOrNull(), data = cachedData)))
            }
        }
    }
}
