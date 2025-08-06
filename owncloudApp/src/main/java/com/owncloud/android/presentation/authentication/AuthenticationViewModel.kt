/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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

package com.owncloud.android.presentation.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.domain.authentication.oauth.RegisterClientUseCase
import com.owncloud.android.domain.authentication.oauth.RequestTokenUseCase
import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationInfo
import com.owncloud.android.domain.authentication.oauth.model.TokenRequest
import com.owncloud.android.domain.authentication.oauth.model.TokenResponse
import com.owncloud.android.domain.authentication.usecases.GetBaseUrlUseCase
import com.owncloud.android.domain.authentication.usecases.LoginBasicAsyncUseCase
import com.owncloud.android.domain.authentication.usecases.LoginOAuthAsyncUseCase
import com.owncloud.android.domain.authentication.usecases.SupportsOAuth2UseCase
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.server.usecases.GetServerInfoAsyncUseCase
import com.owncloud.android.domain.spaces.usecases.RefreshSpacesFromServerAsyncUseCase
import com.owncloud.android.domain.user.usecases.SaveIdForAccountUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.domain.webfinger.usecases.GetOwnCloudInstanceFromWebFingerUseCase
import com.owncloud.android.domain.webfinger.usecases.GetOwnCloudInstancesFromAuthenticatedWebFingerUseCase
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.authentication.oauth.OAuthUtils
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.providers.WorkManagerProvider
import kotlinx.coroutines.launch
import timber.log.Timber

class AuthenticationViewModel(
    private val loginBasicAsyncUseCase: LoginBasicAsyncUseCase,
    private val loginOAuthAsyncUseCase: LoginOAuthAsyncUseCase,
    private val getServerInfoAsyncUseCase: GetServerInfoAsyncUseCase,
    private val supportsOAuth2UseCase: SupportsOAuth2UseCase,
    private val getBaseUrlUseCase: GetBaseUrlUseCase,
    private val getOwnCloudInstancesFromAuthenticatedWebFingerUseCase: GetOwnCloudInstancesFromAuthenticatedWebFingerUseCase,
    private val getOwnCloudInstanceFromWebFingerUseCase: GetOwnCloudInstanceFromWebFingerUseCase,
    private val refreshCapabilitiesFromServerAsyncUseCase: RefreshCapabilitiesFromServerAsyncUseCase,
    private val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase,
    private val refreshSpacesFromServerAsyncUseCase: RefreshSpacesFromServerAsyncUseCase,
    private val saveIdForAccountUseCase: SaveIdForAccountUseCase,
    private val workManagerProvider: WorkManagerProvider,
    private val requestTokenUseCase: RequestTokenUseCase,
    private val registerClientUseCase: RegisterClientUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val contextProvider: ContextProvider,
) : ViewModel() {

    val codeVerifier: String = OAuthUtils().generateRandomCodeVerifier()
    val codeChallenge: String = OAuthUtils().generateCodeChallenge(codeVerifier)
    val oidcState: String = OAuthUtils().generateRandomState()

    private val _legacyWebfingerHost = MediatorLiveData<Event<UIResult<String>>>()
    val legacyWebfingerHost: LiveData<Event<UIResult<String>>> = _legacyWebfingerHost

    private val _serverInfo = MediatorLiveData<Event<UIResult<ServerInfo>>>()
    val serverInfo: LiveData<Event<UIResult<ServerInfo>>> = _serverInfo

    private val _loginResult = MediatorLiveData<Event<UIResult<String>>>()
    val loginResult: LiveData<Event<UIResult<String>>> = _loginResult

    private val _supportsOAuth2 = MediatorLiveData<Event<UIResult<Boolean>>>()
    val supportsOAuth2: LiveData<Event<UIResult<Boolean>>> = _supportsOAuth2

    private val _baseUrl = MediatorLiveData<Event<UIResult<String>>>()
    val baseUrl: LiveData<Event<UIResult<String>>> = _baseUrl

    private val _registerClient = MediatorLiveData<Event<UIResult<ClientRegistrationInfo>>>()
    val registerClient: LiveData<Event<UIResult<ClientRegistrationInfo>>> = _registerClient

    private val _requestToken = MediatorLiveData<Event<UIResult<TokenResponse>>>()
    val requestToken: LiveData<Event<UIResult<TokenResponse>>> = _requestToken

    private val _accountDiscovery = MediatorLiveData<Event<UIResult<Unit>>>()
    val accountDiscovery: LiveData<Event<UIResult<Unit>>> = _accountDiscovery

    var launchedFromDeepLink = false

    fun getLegacyWebfingerHost(
        webfingerLookupServer: String,
        webfingerUsername: String,
    ) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            showLoading = true,
            liveData = _legacyWebfingerHost,
            useCase = getOwnCloudInstanceFromWebFingerUseCase,
            useCaseParams = GetOwnCloudInstanceFromWebFingerUseCase.Params(server = webfingerLookupServer, resource = webfingerUsername)
        )
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

    fun loginOAuth(
        serverBaseUrl: String,
        username: String,
        authTokenType: String,
        accessToken: String,
        refreshToken: String,
        scope: String?,
        updateAccountWithUsername: String? = null,
        clientRegistrationInfo: ClientRegistrationInfo?
    ) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            _loginResult.postValue(Event(UIResult.Loading()))

            val serverInfo = serverInfo.value?.peekContent()?.getStoredData() ?: throw java.lang.IllegalArgumentException("Server info value cannot" +
                    " be null")

            // Authenticated WebFinger needed only for account creations. Logged accounts already know their instances.
            if (updateAccountWithUsername == null) {
                val ownCloudInstancesAvailable = getOwnCloudInstancesFromAuthenticatedWebFingerUseCase(
                    GetOwnCloudInstancesFromAuthenticatedWebFingerUseCase.Params(
                        server = serverBaseUrl,
                        username = username,
                        accessToken = accessToken,
                    )
                )
                Timber.d("Instances retrieved from authenticated webfinger: $ownCloudInstancesAvailable")

                // Multiple instances are not supported yet. Let's use the first instance we receive for the moment.
                ownCloudInstancesAvailable.getDataOrNull()?.let {
                    if (it.isNotEmpty()) {
                        serverInfo.baseUrl = it.first()
                    }
                }
            }

            val useCaseResult = loginOAuthAsyncUseCase(
                LoginOAuthAsyncUseCase.Params(
                    serverInfo = serverInfo,
                    username = username,
                    authTokenType = authTokenType,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    scope = scope,
                    updateAccountWithUsername = updateAccountWithUsername,
                    clientRegistrationInfo = clientRegistrationInfo,
                )
            )

            if (useCaseResult.isSuccess) {
                _loginResult.postValue(Event(UIResult.Success(useCaseResult.getDataOrNull())))
            } else if (useCaseResult.isError) {
                _loginResult.postValue(Event(UIResult.Error(error = useCaseResult.getThrowableOrNull())))
            }
        }
    }

    fun supportsOAuth2(
        accountName: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        requiresConnection = false,
        liveData = _supportsOAuth2,
        useCase = supportsOAuth2UseCase,
        useCaseParams = SupportsOAuth2UseCase.Params(
            accountName = accountName
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

    fun registerClient(
        registrationEndpoint: String
    ) {
        val registrationRequest = OAuthUtils.buildClientRegistrationRequest(
            registrationEndpoint = registrationEndpoint,
            MainApp.appContext
        )

        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            showLoading = false,
            liveData = _registerClient,
            useCase = registerClientUseCase,
            useCaseParams = RegisterClientUseCase.Params(registrationRequest)
        )
    }

    fun requestToken(
        tokenRequest: TokenRequest
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        showLoading = false,
        liveData = _requestToken,
        useCase = requestTokenUseCase,
        useCaseParams = RequestTokenUseCase.Params(tokenRequest = tokenRequest)
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
                saveIdForAccountUseCase(SaveIdForAccountUseCase.Params(accountName))
            }
            _accountDiscovery.postValue(Event(UIResult.Success()))
        }
        workManagerProvider.enqueueAccountDiscovery(accountName)
    }
}
