package com.owncloud.android.presentation.authentication.homecloud

import android.accounts.Account
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.R
import com.owncloud.android.domain.authentication.usecases.LoginBasicAsyncUseCase
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.domain.mdnsdiscovery.usecases.DiscoverLocalNetworkDevicesUseCase
import com.owncloud.android.domain.remoteaccess.usecases.GetRemoteAccessTokenUseCase
import com.owncloud.android.domain.remoteaccess.usecases.InitiateRemoteAccessAuthenticationUseCase
import com.owncloud.android.domain.server.model.Server
import com.owncloud.android.domain.server.usecases.GetAvailableServersUseCase
import com.owncloud.android.domain.server.usecases.GetServerInfoAsyncUseCase
import com.owncloud.android.domain.spaces.usecases.RefreshSpacesFromServerAsyncUseCase
import com.owncloud.android.presentation.authentication.ACTION_CREATE
import com.owncloud.android.presentation.authentication.EXTRA_ACCOUNT
import com.owncloud.android.presentation.authentication.EXTRA_ACTION
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.providers.WorkManagerProvider
import com.owncloud.android.utils.runCatchingException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class LoginViewModel(
    private val loginBasicAsyncUseCase: LoginBasicAsyncUseCase,
    private val getServerInfoAsyncUseCase: GetServerInfoAsyncUseCase,
    private val refreshCapabilitiesFromServerAsyncUseCase: RefreshCapabilitiesFromServerAsyncUseCase,
    private val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase,
    private val refreshSpacesFromServerAsyncUseCase: RefreshSpacesFromServerAsyncUseCase,
    private val workManagerProvider: WorkManagerProvider,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
    private val contextProvider: ContextProvider,
    private val initiateRemoteAccessAuthenticationUseCase: InitiateRemoteAccessAuthenticationUseCase,
    private val getRemoteAccessTokenUseCase: GetRemoteAccessTokenUseCase,
    private val discoverLocalNetworkDevicesUseCase: DiscoverLocalNetworkDevicesUseCase,
    private val getServersUseCase: GetAvailableServersUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginScreenState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events = _events

    private val loginAction by lazy { savedStateHandle.get<Byte>(EXTRA_ACTION) }
    private val account by lazy { savedStateHandle.get<Account>(EXTRA_ACCOUNT) }

    init {
        if (loginAction != ACTION_CREATE) {
            account?.let {
                onUserNameChanged(it.name)
            }
        }
    }

    private fun initiateToken() {
        viewModelScope.launch {
            runCatchingException(
                block = {
                    val reference = initiateRemoteAccessAuthenticationUseCase.execute(_state.value.username)
                    _state.update { it.copy(reference = reference) }
                    _events.emit(LoginEvent.NavigateToCodeDialog)
                    Timber.d("DEBUG Initiated token $reference")
                },
                exceptionHandlerBlock = {

                }
            )
        }
    }

    fun onUserNameChanged(username: String) {
        _state.update { it.copy(username = username) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password) }
    }

    fun onServerUrlChanged(serverUrl: String) {
        _state.update { it.copy(serverUrl = serverUrl) }
    }

    fun onCodeEntered(code: String) {
        viewModelScope.launch {
            runCatchingException(
                block = {
                    _state.update { it.copy(isAllowLoading = true) }
                    getRemoteAccessTokenUseCase.execute(_state.value.reference, code)
                    Timber.d("DEBUG getRemoteAccessTokenUseCase successful")
                    _state.update { it.copy(loginState = LoginState.LOGIN) }
                    _events.emit(LoginEvent.NavigateToLogin)
                },
                exceptionHandlerBlock = {

                },
                completeBlock = {
                    _state.update { it.copy(isAllowLoading = false) }
                    startObserveServers()
                }
            )
        }
    }

    fun startObserveServers() {
        viewModelScope.launch {
            getServersUseCase.getServersUpdates(
                this@launch,
                DiscoverLocalNetworkDevicesUseCase.Params(
                    serviceType = "_https._tcp",
                    serviceName = "HomeCloud",
                    duration = 30.seconds
                )
            ).collect { servers ->
                Timber.d("DEBUG servers: $servers")
                _events.emit(LoginEvent.UpdateServers(servers))
            }
        }
    }

    fun refreshServers() {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            getServersUseCase.refreshRemoteAccessDevices()
        }
    }

    fun onLoginClick() {
        when (_state.value.loginState) {
            LoginState.REMOTE_ACCESS -> initiateToken()
            LoginState.LOGIN -> performLogin()
        }
    }

    fun onServerSelected(selectedServer: Server) {
        _state.update { it.copy(selectedServer = selectedServer) }
    }

    private fun performLogin() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val currentState = _state.value
            runCatchingException(
                block = {
                    val accountName = withContext(coroutinesDispatcherProvider.io) {
                        val serverInfo = getServerInfoAsyncUseCase(
                            GetServerInfoAsyncUseCase.Params(
                                serverPath = currentState.serverUrl,
                                creatingAccount = false,
                                enforceOIDC = contextProvider.getBoolean(R.bool.enforce_oidc),
                                secureConnectionEnforced = contextProvider.getBoolean(R.bool.enforce_secure_connection),
                            )
                        ).getDataOrNull()

                        loginBasicAsyncUseCase(LoginBasicAsyncUseCase.Params(
                            serverInfo = serverInfo,
                            username = currentState.username,
                            password = currentState.password,
                            updateAccountWithUsername = if (loginAction != ACTION_CREATE) account?.name else null
                        ))
                    }
                    discoverAccount(accountName.getDataOrNull().orEmpty(), loginAction == ACTION_CREATE)
                    _events.emit(LoginEvent.LoginResult(accountName = accountName.getDataOrNull().orEmpty()))
                },
                exceptionHandlerBlock = {
                    _state.update { it.copy(isLoading = false) }
                },
                completeBlock = {
                }
            )
        }
    }

    fun discoverAccount(accountName: String, discoveryNeeded: Boolean = false) {
        Timber.d("Account Discovery for account: $accountName needed: $discoveryNeeded")
        if (!discoveryNeeded) {
            return
        }
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            // 1. Refresh capabilities for account
            refreshCapabilitiesFromServerAsyncUseCase(RefreshCapabilitiesFromServerAsyncUseCase.Params(accountName))
            val capabilities = getStoredCapabilitiesUseCase(GetStoredCapabilitiesUseCase.Params(accountName))

            val spacesAvailableForAccount = capabilities?.isSpacesAllowed() == true

            // 2 If Account does not support spaces we can skip this
            if (spacesAvailableForAccount) {
                refreshSpacesFromServerAsyncUseCase(RefreshSpacesFromServerAsyncUseCase.Params(accountName))
            }
        }
        workManagerProvider.enqueueAccountDiscovery(accountName)
    }

    data class LoginScreenState(
        val actionButtonEnabled: Boolean = false,
        val isLoading: Boolean = false,
        val username: String = "",
        val password: String = "",
        val url: String = "",
        val reference: String = "",
        val loginState: LoginState = LoginState.REMOTE_ACCESS,
        val isAllowLoading: Boolean = false,
        val selectedServer: Server? = null,
        val serverUrl: String = ""
    )

    sealed class LoginEvent {
        data object NavigateToCodeDialog : LoginEvent()
        data object NavigateToLogin : LoginEvent()

        data class UpdateServers(val servers: List<Server>) : LoginEvent()
        data class LoginResult(val accountName: String, val error: String? = null) : LoginEvent()
    }

    enum class LoginState {
        REMOTE_ACCESS,
        LOGIN
    }
}