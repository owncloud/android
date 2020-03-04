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
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

fun <T, Params> ViewModel.runUseCaseWithResult(
    coroutineDispatcher: CoroutineDispatcher,
    showLoading: Boolean = false,
    liveData: MediatorLiveData<Event<UIResult<T>>>,
    useCase: BaseUseCaseWithResult<T, Params>,
    useCaseParams: Params,
    postSuccess: Boolean = true
) {
    viewModelScope.launch(coroutineDispatcher) {
        if (showLoading) {
            liveData.postValue(Event(UIResult.Loading()))
        }

        val useCaseResult = useCase.execute(useCaseParams)

        Timber.d(useCaseResult.toString())

        if (useCaseResult.isSuccess && postSuccess) {
            liveData.postValue(Event(UIResult.Success(useCaseResult.getDataOrNull())))
        } else if (useCaseResult.isError) {
            liveData.postValue(Event(UIResult.Error(error = useCaseResult.getThrowableOrNull())))
        }
    }
}

fun <T, U, Params> ViewModel.runUseCaseWithResultAndUseCachedData(
    coroutineDispatcher: CoroutineDispatcher,
    cachedData: T?,
    liveData: MediatorLiveData<Event<UIResult<T>>>,
    useCase: BaseUseCaseWithResult<U, Params>,
    useCaseParams: Params
) {
    viewModelScope.launch(coroutineDispatcher) {
        liveData.postValue(Event(UIResult.Loading(cachedData)))

        val useCaseResult = useCase.execute(useCaseParams)

        Timber.d(useCaseResult.toString())

        if (useCaseResult.isError) {
            liveData.postValue(Event(UIResult.Error(error = useCaseResult.getThrowableOrNull(), data = cachedData)))
        }
    }
}
