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

package com.owncloud.android.presentation.authentication

import android.accounts.Account
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ListenableWorker
import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationInfo
import com.owncloud.android.domain.authentication.usecases.GetBaseUrlUseCase
import com.owncloud.android.domain.authentication.usecases.LoginBasicAsyncUseCase
import com.owncloud.android.domain.authentication.usecases.LoginOAuthAsyncUseCase
import com.owncloud.android.domain.authentication.usecases.SupportsOAuth2UseCase
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.server.usecases.GetServerInfoAsyncUseCase
import com.owncloud.android.domain.spaces.usecases.GetPersonalAndProjectSpacesForAccountUseCase
import com.owncloud.android.domain.spaces.usecases.RefreshSpacesFromServerAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.domain.webfinger.usecases.GetJRDFromWebfingerHostUseCase
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase
import kotlinx.coroutines.launch
import org.koin.core.definition._createDefinition
import timber.log.Timber

class AuthenticationViewModel(
    private val loginBasicAsyncUseCase: LoginBasicAsyncUseCase,
    private val loginOAuthAsyncUseCase: LoginOAuthAsyncUseCase,
    private val getServerInfoAsyncUseCase: GetServerInfoAsyncUseCase,
    private val supportsOAuth2UseCase: SupportsOAuth2UseCase,
    private val getBaseUrlUseCase: GetBaseUrlUseCase,
    private val getJRDFromWebfingerHostUseCase: GetJRDFromWebfingerHostUseCase,
    private val refreshCapabilitiesFromServerAsyncUseCase: RefreshCapabilitiesFromServerAsyncUseCase,
    private val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase,
    private val refreshSpacesFromServerAsyncUseCase: RefreshSpacesFromServerAsyncUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
) : ViewModel() {

    private val _serverInfo = MediatorLiveData<Event<UIResult<ServerInfo>>>()
    val serverInfo: LiveData<Event<UIResult<ServerInfo>>> = _serverInfo

    private val _webfingerHost = MediatorLiveData<Event<UIResult<String>>>()
    val webfingerHost: LiveData<Event<UIResult<String>>> = _webfingerHost

    fun getWebfingerHost(
        webfingerLookupServer: String,
        webfingerUsername: String,
    ) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            showLoading = true,
            liveData = _webfingerHost,
            useCase = getJRDFromWebfingerHostUseCase,
            useCaseParams = GetJRDFromWebfingerHostUseCase.Params(server = webfingerLookupServer, resource = webfingerUsername)
        )
    }

    fun getServerInfo(
        serverUrl: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        showLoading = true,
        liveData = _serverInfo,
        useCase = getServerInfoAsyncUseCase,
        useCaseParams = GetServerInfoAsyncUseCase.Params(serverPath = serverUrl)
    )

    private val _loginResult = MediatorLiveData<Event<UIResult<String>>>()
    val loginResult: LiveData<Event<UIResult<String>>> = _loginResult

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
        username: String,
        authTokenType: String,
        accessToken: String,
        refreshToken: String,
        scope: String?,
        updateAccountWithUsername: String? = null,
        clientRegistrationInfo: ClientRegistrationInfo?
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        liveData = _loginResult,
        showLoading = true,
        useCase = loginOAuthAsyncUseCase,
        useCaseParams = LoginOAuthAsyncUseCase.Params(
            serverInfo = serverInfo.value?.peekContent()?.getStoredData(),
            username = username,
            authTokenType = authTokenType,
            accessToken = accessToken,
            refreshToken = refreshToken,
            scope = scope,
            updateAccountWithUsername = updateAccountWithUsername,
            clientRegistrationInfo = clientRegistrationInfo
        )
    )

    private val _supportsOAuth2 = MediatorLiveData<Event<UIResult<Boolean>>>()
    val supportsOAuth2: LiveData<Event<UIResult<Boolean>>> = _supportsOAuth2

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

    private val _baseUrl = MediatorLiveData<Event<UIResult<String>>>()
    val baseUrl: LiveData<Event<UIResult<String>>> = _baseUrl

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

    private val _accountDiscovery = MediatorLiveData<Event<UIResult<Unit>>>()
    val accountDiscovery: LiveData<Event<UIResult<Unit>>> = _accountDiscovery

    fun discoverAccount(account: Account, discoveryNeeded: Boolean = false) {
        Timber.d("Account Discovery for account: $account needed: $discoveryNeeded")
        if (!discoveryNeeded) {
            _accountDiscovery.postValue(Event(UIResult.Success()))
            return
        }
        _accountDiscovery.postValue(Event(UIResult.Loading()))
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            // 1. Refresh capabilities for account
            refreshCapabilitiesFromServerAsyncUseCase.execute(RefreshCapabilitiesFromServerAsyncUseCase.Params(account.name))
            val capabilities = getStoredCapabilitiesUseCase.execute(GetStoredCapabilitiesUseCase.Params(account.name))

            val spacesAvailableForAccount = capabilities?.isSpacesAllowed() == true

            // 2 If Account does not support spaces we can skip this
            if (spacesAvailableForAccount) {
                refreshSpacesFromServerAsyncUseCase.execute(RefreshSpacesFromServerAsyncUseCase.Params(account.name))
            }
            _accountDiscovery.postValue(Event(UIResult.Success()))
        }
    }
}
