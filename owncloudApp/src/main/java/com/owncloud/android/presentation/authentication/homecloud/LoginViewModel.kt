package com.owncloud.android.presentation.authentication.homecloud

import android.accounts.Account
import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.R
import com.owncloud.android.domain.authentication.usecases.LoginBasicAsyncUseCase
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase
import com.owncloud.android.domain.exceptions.NoNetworkConnectionException
import com.owncloud.android.domain.exceptions.OwncloudVersionNotSupportedException
import com.owncloud.android.domain.exceptions.SSLErrorCode
import com.owncloud.android.domain.exceptions.SSLErrorException
import com.owncloud.android.domain.exceptions.UnknownErrorException
import com.owncloud.android.domain.exceptions.WrongCodeException
import com.owncloud.android.domain.mdnsdiscovery.usecases.DiscoverLocalNetworkDevicesUseCase
import com.owncloud.android.domain.remoteaccess.usecases.GetExistingRemoveAccessUserUseCase
import com.owncloud.android.domain.remoteaccess.usecases.GetRemoteAccessTokenUseCase
import com.owncloud.android.domain.remoteaccess.usecases.InitiateRemoteAccessAuthenticationUseCase
import com.owncloud.android.domain.server.model.Server
import com.owncloud.android.domain.server.usecases.GetAvailableServersUseCase
import com.owncloud.android.domain.server.usecases.GetServerInfoAsyncUseCase
import com.owncloud.android.domain.spaces.usecases.RefreshSpacesFromServerAsyncUseCase
import com.owncloud.android.extensions.parseError
import com.owncloud.android.lib.common.network.CertificateCombinedException
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
    private val getServersUseCase: GetAvailableServersUseCase,
    private val getExistingRemoveAccessUserUseCase: GetExistingRemoveAccessUserUseCase,
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
        restorePreviousUserIfExists()
    }

    private fun initiateToken() {
        viewModelScope.launch {
            runCatchingException(
                block = {
                    val reference = initiateRemoteAccessAuthenticationUseCase.execute(_state.value.username)
                    _state.update { it.copy(reference = reference, errorEmailInvalidMessage = null, errorMessage = null) }
                    _events.emit(LoginEvent.NavigateToCodeDialog)
                    startObserveServers()
                },
                exceptionHandlerBlock = {
                    _state.update { it.copy(errorMessage = contextProvider.getString(R.string.homecloud_code_unknown_error)) }
                }
            )
        }
    }

    fun onUserNameChanged(username: String) {
        val errorEmailValidation = if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            contextProvider.getString(R.string.homecloud_login_invalid_email_message)
        } else {
            null
        }
        _state.update { it.copy(username = username, errorEmailInvalidMessage = errorEmailValidation) }
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
                    _state.update { it.copy(isAllowLoading = true, errorCodeMessage = null) }
                    getRemoteAccessTokenUseCase.execute(_state.value.reference, code, _state.value.username)
                    switchToLoginState()
                },
                exceptionHandlerBlock = {
                    handleCodeError(it)
                },
                completeBlock = {
                    _state.update { it.copy(isAllowLoading = false) }
                    refreshServers()
                }
            )
        }
    }

    private fun handleCodeError(e: Throwable) {
        val errorMessage = when {
            e is WrongCodeException -> {
                contextProvider.getString(R.string.homecloud_incorrect_code)
            }

            else -> {
                contextProvider.getString(R.string.homecloud_code_unknown_error)
            }
        }

        _state.update {
            it.copy(
                errorCodeMessage = errorMessage
            )
        }
    }

    fun onSkipClicked() {
        viewModelScope.launch {
            switchToLoginState()
        }
    }

    private suspend fun switchToLoginState() {
        _state.update { it.copy(loginState = LoginState.LOGIN) }
        _events.emit(LoginEvent.NavigateToLogin)
    }

    private fun restorePreviousUserIfExists() {
        val existingUserName = getExistingRemoveAccessUserUseCase.execute()
        if (existingUserName != null) {
            onPreviousUserRestore(existingUserName)
            startObserveServers()
            refreshServers()
        }
    }

    private fun onPreviousUserRestore(existingUserName: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loginState = LoginState.LOGIN,
                    username = existingUserName,
                )
            }
            _events.emit(LoginEvent.NavigateToLogin)
        }
    }

    private fun startObserveServers() {
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
                _state.update { it.copy(servers = servers) }
            }
        }
    }

    fun refreshServers() {
        viewModelScope.launch {
            runCatchingException(
                block = {
                    _state.update { it.copy(isRefreshServersLoading = true) }
                    withContext(coroutinesDispatcherProvider.io) {
                        getServersUseCase.refreshRemoteAccessDevices()
                    }
                },
                exceptionHandlerBlock = {

                },
                completeBlock = {
                    _state.update { it.copy(isRefreshServersLoading = false) }
                }
            )

        }
    }

    fun onActionClicked() {
        when (_state.value.loginState) {
            LoginState.REMOTE_ACCESS -> initiateToken()
            LoginState.LOGIN -> performLogin()
        }
    }

    fun onServerSelected(selectedServer: Server) {
        _state.update { it.copy(selectedServer = selectedServer, serverUrl = selectedServer.hostUrl) }
    }

    private fun performLogin() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val currentState = _state.value
            runCatchingException(
                block = {
                    val serverInfoResult = withContext(coroutinesDispatcherProvider.io) {
                        getServerInfoAsyncUseCase(
                            GetServerInfoAsyncUseCase.Params(
                                serverPath = currentState.serverUrl,
                                creatingAccount = false,
                                enforceOIDC = contextProvider.getBoolean(R.bool.enforce_oidc),
                                secureConnectionEnforced = contextProvider.getBoolean(R.bool.enforce_secure_connection),
                            )
                        )
                    }

                    if (serverInfoResult.isSuccess) {
                        val accountNameResult = withContext(coroutinesDispatcherProvider.io) {
                            loginBasicAsyncUseCase(
                                LoginBasicAsyncUseCase.Params(
                                    serverInfo = serverInfoResult.getDataOrNull(),
                                    username = currentState.username,
                                    password = currentState.password,
                                    updateAccountWithUsername = if (loginAction != ACTION_CREATE) account?.name else null
                                )
                            )
                        }

                        if (accountNameResult.isSuccess) {
                            val accountName = accountNameResult.getDataOrNull().orEmpty()
                            discoverAccount(accountName, loginAction == ACTION_CREATE)
                            _events.emit(LoginEvent.LoginResult(accountName = accountName))
                        } else {
                            handleLoginError(accountNameResult.getThrowableOrNull())
                        }

                    } else {
                        handleLoginError(serverInfoResult.getThrowableOrNull())
                    }
                },
                exceptionHandlerBlock = {
                    _state.update { it.copy(isLoading = false) }
                },
                completeBlock = {
                }
            )
        }
    }

    private suspend fun handleLoginError(e: Throwable?) {
        val text = when {
            e is CertificateCombinedException -> {
                _events.emit(LoginEvent.ShowUntrustedCertDialog(e))
                null
            }

            e is OwncloudVersionNotSupportedException -> {
                contextProvider.getString(R.string.server_not_supported)
            }

            e is NoNetworkConnectionException -> {
                contextProvider.getString(R.string.error_no_network_connection)
            }

            e is SSLErrorException && e.code == SSLErrorCode.NOT_HTTP_ALLOWED -> {
                contextProvider.getString(R.string.ssl_connection_not_secure)
            }

            e is UnknownErrorException -> {
                contextProvider.getString(R.string.homecloud_login_server_connection_error)
            }

            else -> {
                e?.parseError("", contextProvider.getContext().resources)
            }
        }

        _state.update {
            it.copy(
                isLoading = false,
                errorMessage = text?.toString()
            )
        }
    }

    fun discoverAccount(accountName: String, discoveryNeeded: Boolean = false) {
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
        val isRefreshServersLoading: Boolean = false,
        val selectedServer: Server? = null,
        val servers: List<Server> = emptyList(),
        val serverUrl: String = "",
        val errorMessage: String? = null,
        val errorCodeMessage: String? = null,
        val errorEmailInvalidMessage: String? = null,
    )

    sealed class LoginEvent {
        data object NavigateToCodeDialog : LoginEvent()
        data object NavigateToLogin : LoginEvent()

        data class LoginResult(val accountName: String, val error: String? = null) : LoginEvent()

        data class ShowUntrustedCertDialog(val certificateCombinedException: CertificateCombinedException) : LoginEvent()
    }

    enum class LoginState {
        REMOTE_ACCESS,
        LOGIN
    }
}