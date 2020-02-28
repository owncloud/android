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

package com.owncloud.android.presentation.viewmodels.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.authentication.usecases.GetUserDataUseCase
import com.owncloud.android.domain.authentication.usecases.LoginBasicAsyncUseCase
import com.owncloud.android.domain.authentication.usecases.LoginOAuthAsyncUseCase
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.server.usecases.GetServerInfoAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch
import timber.log.Timber

class OCAuthenticationViewModel(
    private val loginBasicAsyncUseCase: LoginBasicAsyncUseCase,
    private val loginOAuthAsyncUseCase: LoginOAuthAsyncUseCase,
    private val getServerInfoAsyncUseCase: GetServerInfoAsyncUseCase,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _serverInfo = MediatorLiveData<Event<UIResult<ServerInfo>>>()
    val serverInfo: LiveData<Event<UIResult<ServerInfo>>> = _serverInfo

    fun getServerInfo(
        serverUrl: String
    ) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            _serverInfo.postValue(Event(UIResult.Loading()))
            val useCaseResult = getServerInfoAsyncUseCase.execute(
                GetServerInfoAsyncUseCase.Params(serverPath = serverUrl)
            )
            Timber.d(useCaseResult.toString())

            if (useCaseResult.isSuccess) {
                _serverInfo.postValue(Event(UIResult.Success(useCaseResult.getDataOrNull())))
            } else {
                _serverInfo.postValue(Event(UIResult.Error(error = useCaseResult.getThrowableOrNull())))
            }
        }
    }

    private val _userData = MediatorLiveData<Event<UIResult<String>>>()
    val userData: LiveData<Event<UIResult<String>>> = _userData

    fun getUserData(key: String) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val useCaseResult = getUserDataUseCase.execute(
                GetUserDataUseCase.Params(key)
            )
            Timber.d(useCaseResult.toString())

            if (useCaseResult.isSuccess) {
                _userData.postValue(Event(UIResult.Success(useCaseResult.getDataOrNull())))
            } else {
                _userData.postValue(Event(UIResult.Error(error = useCaseResult.getThrowableOrNull())))
            }
        }
    }

    private val _loginResult = MediatorLiveData<Event<UIResult<String>>>()
    val loginResult: LiveData<Event<UIResult<String>>> = _loginResult

    fun loginBasic(
        username: String,
        password: String
    ) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val useCaseResult = loginBasicAsyncUseCase.execute(
                LoginBasicAsyncUseCase.Params(
                    serverInfo = serverInfo.value?.peekContent()?.getStoredData(),
                    username = username,
                    password = password
                )
            )
            Timber.d(useCaseResult.toString())

            if (useCaseResult.isSuccess) {
                _loginResult.postValue(Event(UIResult.Success(useCaseResult.getDataOrNull())))
            } else {
                _loginResult.postValue(Event(UIResult.Error(error = useCaseResult.getThrowableOrNull())))
            }
        }
    }

    fun loginOAuth(
        username: String,
        authTokenType: String,
        accessToken: String,
        refreshToken: String,
        scope: String?
    ) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            val useCaseResult = loginOAuthAsyncUseCase.execute(
                LoginOAuthAsyncUseCase.Params(
                    serverInfo = serverInfo.value?.peekContent()?.getStoredData(),
                    username = username,
                    authTokenType = authTokenType,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    scope = scope
                )
            )
            Timber.d(useCaseResult.toString())

            if (useCaseResult.isSuccess) {
                _loginResult.postValue(Event(UIResult.Success(useCaseResult.getDataOrNull())))
            } else {
                _loginResult.postValue(Event(UIResult.Error(error = useCaseResult.getThrowableOrNull())))
            }
        }
    }
}
