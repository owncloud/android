/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.presentation.authentication.homecloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.R
import com.owncloud.android.domain.authentication.usecases.GetBaseUrlUseCase
import com.owncloud.android.domain.authentication.usecases.LoginBasicAsyncUseCase
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.domain.mdnsdiscovery.usecases.DiscoverLocalNetworkDevicesUseCase
import com.owncloud.android.domain.remoteaccess.usecases.GetRemoteAccessDeviceByIdUseCase
import com.owncloud.android.domain.remoteaccess.usecases.GetRemoteAccessDevicesUseCase
import com.owncloud.android.domain.remoteaccess.usecases.GetRemoteAccessTokenUseCase
import com.owncloud.android.domain.remoteaccess.usecases.InitiateRemoteAccessAuthenticationUseCase
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.server.usecases.GetServerInfoAsyncUseCase
import com.owncloud.android.domain.spaces.usecases.RefreshSpacesFromServerAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.extensions.update
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.providers.WorkManagerProvider
import kotlinx.coroutines.launch
import timber.log.Timber

class AuthenticationViewModel(
    private val loginBasicAsyncUseCase: LoginBasicAsyncUseCase,
    private val getServerInfoAsyncUseCase: GetServerInfoAsyncUseCase,
    private val getBaseUrlUseCase: GetBaseUrlUseCase,
    private val refreshCapabilitiesFromServerAsyncUseCase: RefreshCapabilitiesFromServerAsyncUseCase,
    private val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase,
    private val refreshSpacesFromServerAsyncUseCase: RefreshSpacesFromServerAsyncUseCase,
    private val workManagerProvider: WorkManagerProvider,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val contextProvider: ContextProvider,
    private val initiateRemoteAccessAuthenticationUseCase: InitiateRemoteAccessAuthenticationUseCase,
    private val getRemoteAccessDeviceByIdUseCase: GetRemoteAccessDeviceByIdUseCase,
    private val getRemoteAccessTokenUseCase: GetRemoteAccessTokenUseCase,
    private val getRemoteAccessDevicesUseCase: GetRemoteAccessDevicesUseCase,
    private val discoverLocalNetworkDevicesUseCase: DiscoverLocalNetworkDevicesUseCase,
) : ViewModel() {

    private val _serverInfo = MediatorLiveData<Event<UIResult<ServerInfo>>>()
    val serverInfo: LiveData<Event<UIResult<ServerInfo>>> = _serverInfo

    private val _loginResult = MediatorLiveData<Event<UIResult<String>>>()
    val loginResult: LiveData<Event<UIResult<String>>> = _loginResult

    private val _baseUrl = MediatorLiveData<Event<UIResult<String>>>()
    val baseUrl: LiveData<Event<UIResult<String>>> = _baseUrl

    private val _accountDiscovery = MediatorLiveData<Event<UIResult<Unit>>>()
    val accountDiscovery: LiveData<Event<UIResult<Unit>>> = _accountDiscovery

    private val _screenState = MediatorLiveData(LoginScreenState())
    val screenState: LiveData<LoginScreenState> = _screenState

    var launchedFromDeepLink = false

    init {
        _screenState.addSource(_serverInfo) { event ->
            if (event.peekContent().isError) {
                resetLoadingState()
            }
        }
        _screenState.addSource(_loginResult) { event ->
            if (event.peekContent().isError) {
                resetLoadingState()
            }
        }
        _screenState.addSource(_accountDiscovery) { event ->
            if (event.peekContent().isError) {
                resetLoadingState()
            }
        }
    }

    fun handleUrlChanged(url: String) {
        _screenState.update {
            it.copy(
                url = url
            )
        }
        resetLoginError()
        updateCtaButtonState()
    }

    fun handleLoginChanged(username: String) {
        _screenState.update {
            it.copy(
                username = username
            )
        }
        resetLoginError()
        updateCtaButtonState()
    }

    fun handlePasswordChanged(password: String) {
        _screenState.update {
            it.copy(
                password = password
            )
        }
        resetLoginError()
        updateCtaButtonState()
    }

    fun handleCtaButtonClicked() {
        val currentValue = _screenState.value ?: return
        if (currentValue.url.isNotEmpty()) {
            _screenState.update {
                it.copy(
                    isLoading = true
                )
            }
            getServerInfo(serverUrl = currentValue.url, creatingAccount = false)
        }
    }

    private fun updateCtaButtonState() {
        val currentValue = _screenState.value ?: return
        val isCtaButtonEnabled = with(currentValue) {
            url.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty() && error.isEmpty()
        }
        _screenState.update {
            it.copy(
                ctaButtonEnabled = isCtaButtonEnabled,
            )
        }
    }

    private fun resetLoadingState() {
        _screenState.update {
            it.copy(
                isLoading = false
            )
        }
    }

    fun handleInsecureConnectionCancelled() {
        resetLoadingState()
    }

    fun getServerInfo(
        serverUrl: String,
        creatingAccount: Boolean = false,
    ) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            showLoading = true,
            liveData = _serverInfo,
            useCase = getServerInfoAsyncUseCase,
            useCaseParams = GetServerInfoAsyncUseCase.Params(
                serverPath = serverUrl,
                creatingAccount = creatingAccount,
                enforceOIDC = contextProvider.getBoolean(R.bool.enforce_oidc),
                secureConnectionEnforced = contextProvider.getBoolean(R.bool.enforce_secure_connection),
            )
        )
    }

    fun loginBasic(
        username: String,
        password: String,
        updateAccountWithUsername: String?
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        liveData = _loginResult,
        showLoading = true,
        useCase = loginBasicAsyncUseCase,
        useCaseParams = LoginBasicAsyncUseCase.Params(
            serverInfo = serverInfo.value?.peekContent()?.getStoredData(),
            username = username,
            password = password,
            updateAccountWithUsername = updateAccountWithUsername
        )
    )

    fun getBaseUrl(
        accountName: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        requiresConnection = false,
        liveData = _baseUrl,
        useCase = getBaseUrlUseCase,
        useCaseParams = GetBaseUrlUseCase.Params(
            accountName = accountName
        )
    )

    fun discoverAccount(accountName: String, discoveryNeeded: Boolean = false) {
        Timber.d("Account Discovery for account: $accountName needed: $discoveryNeeded")
        if (!discoveryNeeded) {
            _accountDiscovery.postValue(Event(UIResult.Success()))
            return
        }
        _accountDiscovery.postValue(Event(UIResult.Loading()))
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            // 1. Refresh capabilities for account
            refreshCapabilitiesFromServerAsyncUseCase(RefreshCapabilitiesFromServerAsyncUseCase.Params(accountName))
            val capabilities = getStoredCapabilitiesUseCase(GetStoredCapabilitiesUseCase.Params(accountName))

            val spacesAvailableForAccount = capabilities?.isSpacesAllowed() == true

            // 2 If Account does not support spaces we can skip this
            if (spacesAvailableForAccount) {
                refreshSpacesFromServerAsyncUseCase(RefreshSpacesFromServerAsyncUseCase.Params(accountName))
            }
            _accountDiscovery.postValue(Event(UIResult.Success()))
        }
        workManagerProvider.enqueueAccountDiscovery(accountName)
    }

    fun showLoginError(message: String, highlightFields: Boolean) {
        _screenState.update {
            it.copy(
                error = it.error.copy(
                    fields = it.error.fields.toMutableMap().apply {
                        if (highlightFields) {
                            this[Field.EMAIL] = " "
                            this[Field.PASSWORD] = " "
                        }
                    },
                    message = message,
                ),
                ctaButtonEnabled = false,
            )
        }
    }

    fun showServerError(message: CharSequence) {
        _screenState.update {
            it.copy(
                error = it.error.copy(
                    fields = it.error.fields.toMutableMap().apply {
                        this[Field.SERVER] = message
                    }
                ),
                ctaButtonEnabled = false,
            )
        }
    }

    private fun resetLoginError() {
        _screenState.update {
            it.copy(
                error = LoginError.none(),
            )
        }
    }

    data class LoginScreenState(
        val ctaButtonEnabled: Boolean = false,
        val isLoading: Boolean = false,
        val username: String = "",
        val password: String = "",
        val url: String = "",
        val error: LoginError = LoginError.none(),
    )

    data class LoginError(
        val message: String,
        val fields: Map<Field, CharSequence?>,
    ) {

        fun isEmpty() = message.isEmpty() && fields.isEmpty()

        companion object {
            fun none() = LoginError(message = "", fields = emptyMap())
        }
    }

    enum class Field {
        EMAIL, PASSWORD, SERVER,
    }
}


