/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2012  Bartek Przybylski
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

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.owncloud.android.BuildConfig
import com.owncloud.android.MainApp
import com.owncloud.android.MainApp.Companion.accountType
import com.owncloud.android.R
import com.owncloud.android.data.authentication.KEY_USER_ID
import com.owncloud.android.databinding.AccountSetupBinding
import com.owncloud.android.domain.authentication.oauth.model.ResponseType
import com.owncloud.android.domain.authentication.oauth.model.TokenRequest
import com.owncloud.android.domain.exceptions.NoNetworkConnectionException
import com.owncloud.android.domain.exceptions.OwncloudVersionNotSupportedException
import com.owncloud.android.domain.exceptions.SSLErrorCode
import com.owncloud.android.domain.exceptions.SSLErrorException
import com.owncloud.android.domain.exceptions.ServerNotReachableException
import com.owncloud.android.domain.exceptions.StateMismatchException
import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.extensions.checkPasscodeEnforced
import com.owncloud.android.extensions.goToUrl
import com.owncloud.android.extensions.manageOptionLockSelected
import com.owncloud.android.extensions.parseError
import com.owncloud.android.extensions.showErrorInToast
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.lib.common.accounts.AccountTypeUtils
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.presentation.authentication.AccountUtils.getAccounts
import com.owncloud.android.presentation.authentication.AccountUtils.getUsernameOfAccount
import com.owncloud.android.presentation.authentication.oauth.OAuthUtils
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.documentsprovider.DocumentsProviderUtils.notifyDocumentsProviderRoots
import com.owncloud.android.presentation.security.LockType
import com.owncloud.android.presentation.security.SecurityEnforced
import com.owncloud.android.presentation.settings.SettingsActivity
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.MdmProvider
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import com.owncloud.android.utils.CONFIGURATION_OAUTH2_OPEN_ID_PROMPT
import com.owncloud.android.utils.CONFIGURATION_OAUTH2_OPEN_ID_SCOPE
import com.owncloud.android.utils.CONFIGURATION_SEND_LOGIN_HINT_AND_USER
import com.owncloud.android.utils.CONFIGURATION_SERVER_URL
import com.owncloud.android.utils.CONFIGURATION_SERVER_URL_INPUT_VISIBILITY
import com.owncloud.android.utils.NO_MDM_RESTRICTION_YET
import com.owncloud.android.utils.PreferenceUtils
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File

class LoginActivity : AppCompatActivity(), SslUntrustedCertDialog.OnSslUntrustedCertListener, SecurityEnforced {

    private val authenticationViewModel by viewModel<AuthenticationViewModel>()
    private val contextProvider by inject<ContextProvider>()
    private val mdmProvider by inject<MdmProvider>()

    private var loginAction: Byte = ACTION_CREATE
    private var authTokenType: String? = null
    private var userAccount: Account? = null
    private var username: String? = null
    private lateinit var serverBaseUrl: String

    private var oidcSupported = false

    private lateinit var binding: AccountSetupBinding

    // For handling AbstractAccountAuthenticator responses
    private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var resultBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPasscodeEnforced(this)

        // Protection against screen recording
        if (!BuildConfig.DEBUG) {
            window.addFlags(FLAG_SECURE)
        } // else, let it go, or taking screenshots & testing will not be possible

        // Get values from intent
        handleDeepLink()
        loginAction = intent.getByteExtra(EXTRA_ACTION, ACTION_CREATE)
        authTokenType = intent.getStringExtra(KEY_AUTH_TOKEN_TYPE)
        userAccount = intent.getParcelableExtra(EXTRA_ACCOUNT)

        // Get values from savedInstanceState
        if (savedInstanceState == null) {
            if (authTokenType == null && userAccount != null) {
                authenticationViewModel.supportsOAuth2((userAccount as Account).name)
            }
        } else {
            authTokenType = savedInstanceState.getString(KEY_AUTH_TOKEN_TYPE)
        }

        // UI initialization
        binding = AccountSetupBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (loginAction != ACTION_CREATE) {
            binding.accountUsername.isEnabled = false
            binding.accountUsername.isFocusable = false
            userAccount?.name?.let {
                username = getUsernameOfAccount(it)
            }

        }

        if (savedInstanceState == null) {
            if (userAccount != null) {
                authenticationViewModel.getBaseUrl((userAccount as Account).name)
            } else {
                serverBaseUrl = getString(R.string.server_url).trim()
            }

            userAccount?.let {
                AccountUtils.getUsernameForAccount(it)?.let { username ->
                    binding.accountUsername.setText(username)
                }
            }
        }

        binding.root.filterTouchesWhenObscured =
            PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this@LoginActivity)

        initBrandableOptionsUI()

        binding.thumbnail.setOnClickListener { checkOcServer() }

        binding.embeddedCheckServerButton.setOnClickListener { checkOcServer() }

        binding.loginButton.setOnClickListener {
            if (AccountTypeUtils.getAuthTokenTypeAccessToken(accountType) != authTokenType) { // Basic
                authenticationViewModel.loginBasic(
                    binding.accountUsername.text.toString().trim(),
                    binding.accountPassword.text.toString(),
                    if (loginAction != ACTION_CREATE) userAccount?.name else null
                )
            }
        }

        binding.settingsLink.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }

        accountAuthenticatorResponse = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        accountAuthenticatorResponse?.onRequestContinued()

        initLiveDataObservers()
    }

    private fun handleDeepLink() {
        if (intent.data != null) {
            authenticationViewModel.launchedFromDeepLink = true
            if (getAccounts(baseContext).isNotEmpty()) {
                launchFileDisplayActivity()
            } else {
                showMessageInSnackbar(message = baseContext.getString(R.string.uploader_wrn_no_account_title))
            }
        }
    }

    private fun launchFileDisplayActivity() {
        val newIntent = Intent(this, FileDisplayActivity::class.java)
        newIntent.data = intent.data
        startActivity(newIntent)
        finish()
    }

    private fun initLiveDataObservers() {
        // LiveData observers
        authenticationViewModel.legacyWebfingerHost.observe(this) { event ->
            when (val uiResult = event.peekContent()) {
                is UIResult.Loading -> getLegacyWebfingerIsLoading()
                is UIResult.Success -> getLegacyWebfingerIsSuccess(uiResult)
                is UIResult.Error -> getLegacyWebfingerIsError(uiResult)
            }
        }

        authenticationViewModel.serverInfo.observe(this) { event ->
            when (val uiResult = event.peekContent()) {
                is UIResult.Loading -> getServerInfoIsLoading()
                is UIResult.Success -> getServerInfoIsSuccess(uiResult)
                is UIResult.Error -> getServerInfoIsError(uiResult)
            }
        }

        authenticationViewModel.loginResult.observe(this) { event ->
            when (val uiResult = event.peekContent()) {
                is UIResult.Loading -> loginIsLoading()
                is UIResult.Success -> loginIsSuccess(uiResult)
                is UIResult.Error -> loginIsError(uiResult)
            }
        }

        authenticationViewModel.accountDiscovery.observe(this) {
            if (it.peekContent() is UIResult.Success) {
                notifyDocumentsProviderRoots(applicationContext)
                if (authenticationViewModel.launchedFromDeepLink) {
                    launchFileDisplayActivity()
                } else {
                    finish()
                }
            } else {
                binding.authStatusText.run {
                    text = context.getString(R.string.login_account_preparing)
                    isVisible = true
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.progress_small, 0, 0, 0)
                }
            }
        }

        authenticationViewModel.supportsOAuth2.observe(this) { event ->
            when (val uiResult = event.peekContent()) {
                is UIResult.Loading -> {}
                is UIResult.Success -> updateAuthTokenTypeAndInstructions(uiResult)
                is UIResult.Error -> showErrorInToast(
                    genericErrorMessageId = R.string.supports_oauth2_error,
                    throwable = uiResult.error
                )
            }
        }

        authenticationViewModel.baseUrl.observe(this) { event ->
            when (val uiResult = event.peekContent()) {
                is UIResult.Loading -> {}
                is UIResult.Success -> updateBaseUrlAndHostInput(uiResult)
                is UIResult.Error -> showErrorInToast(
                    genericErrorMessageId = R.string.get_base_url_error,
                    throwable = uiResult.error
                )
            }
        }
    }

    private fun getLegacyWebfingerIsLoading() {
        binding.webfingerStatusText.run {
            text = getString(R.string.auth_testing_connection)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.progress_small, 0, 0, 0)
            isVisible = true
        }
    }

    private fun getLegacyWebfingerIsSuccess(uiResult: UIResult.Success<String>) {
        val serverUrl = uiResult.data ?: return
        username = binding.webfingerUsername.text.toString()
        binding.webfingerLayout.isVisible = false
        binding.mainLoginLayout.isVisible = true
        binding.hostUrlInput.setText(serverUrl)
        checkOcServer()
    }

    private fun getLegacyWebfingerIsError(uiResult: UIResult.Error<String>) {
        if (uiResult.error is NoNetworkConnectionException) {
            binding.webfingerStatusText.run {
                text = getString(R.string.error_no_network_connection)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.no_network, 0, 0, 0)
            }
        } else {
            binding.webfingerStatusText.run {
                text = uiResult.getThrowableOrNull()?.parseError("", resources, true)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
            }
        }
        binding.webfingerStatusText.isVisible = true
    }

    private fun checkOcServer() {
        val uri = binding.hostUrlInput.text.toString().trim()
        if (uri.isNotEmpty()) {
            authenticationViewModel.getServerInfo(serverUrl = uri, loginAction == ACTION_CREATE)
        } else {
            binding.serverStatusText.run {
                text = getString(R.string.auth_can_not_auth_against_server).also { Timber.d(it) }
                isVisible = true
            }
        }
    }

    private fun getServerInfoIsSuccess(uiResult: UIResult.Success<ServerInfo>) {
        updateCenteredRefreshButtonVisibility(shouldBeVisible = false)
        uiResult.data?.run {
            val serverInfo = this
            serverBaseUrl = baseUrl
            binding.hostUrlInput.run {
                setText(baseUrl)
                doAfterTextChanged {
                    //If user modifies url, reset fields and force him to check url again
                    if (authenticationViewModel.serverInfo.value == null || baseUrl != binding.hostUrlInput.text.toString()) {
                        showOrHideBasicAuthFields(shouldBeVisible = false)
                        binding.loginButton.isVisible = false
                        binding.serverStatusText.run {
                            text = ""
                            visibility = INVISIBLE
                        }
                    }
                }
            }

            binding.serverStatusText.run {
                if (isSecureConnection) {
                    text = getString(R.string.auth_secure_connection)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, 0, 0)
                    checkServerType(serverInfo)
                } else {
                    text = getString(R.string.auth_connection_established)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_open, 0, 0, 0)
                    val builder = AlertDialog.Builder(context)
                    builder.apply {
                        setTitle(context.getString(R.string.insecure_http_url_title_dialog))
                        setMessage(context.getString(R.string.insecure_http_url_message_dialog))
                        setPositiveButton(R.string.insecure_http_url_continue_button) { dialog, which ->
                            checkServerType(serverInfo)
                        }
                        setNegativeButton(android.R.string.cancel) { dialog, which ->
                            showOrHideBasicAuthFields(shouldBeVisible = false)
                        }
                        setCancelable(false)
                        show()
                    }
                }
                isVisible = true
            }
        }
    }

    private fun checkServerType(serverInfo: ServerInfo) {
        if (BuildConfig.FLAVOR == MainApp.QA_FLAVOR) {
            handleBasicAuth()
            return
        }

        when (serverInfo) {
            is ServerInfo.BasicServer -> {
                handleBasicAuth()
            }

            is ServerInfo.OAuth2Server -> {
                showOrHideBasicAuthFields(shouldBeVisible = false)
                authTokenType = OAUTH_TOKEN_TYPE
                oidcSupported = false

                val oauth2authorizationEndpoint =
                    Uri.parse("$serverBaseUrl${File.separator}${getString(R.string.oauth2_url_endpoint_auth)}")
                performGetAuthorizationCodeRequest(oauth2authorizationEndpoint)
            }

            is ServerInfo.OIDCServer -> {
                showOrHideBasicAuthFields(shouldBeVisible = false)
                authTokenType = OAUTH_TOKEN_TYPE
                oidcSupported = true
                val registrationEndpoint = serverInfo.oidcServerConfiguration.registrationEndpoint
                if (registrationEndpoint != null) {
                    registerClient(
                        authorizationEndpoint = serverInfo.oidcServerConfiguration.authorizationEndpoint.toUri(),
                        registrationEndpoint = registrationEndpoint
                    )
                } else {
                    performGetAuthorizationCodeRequest(serverInfo.oidcServerConfiguration.authorizationEndpoint.toUri())
                }
            }

            else -> {
                binding.serverStatusText.run {
                    text = getString(R.string.auth_unsupported_auth_method)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
                    isVisible = true
                }
            }
        }
    }

    private fun handleBasicAuth() {
        authTokenType = BASIC_TOKEN_TYPE
        oidcSupported = false
        showOrHideBasicAuthFields(shouldBeVisible = true)
        binding.accountUsername.doAfterTextChanged { updateLoginButtonVisibility() }
        binding.accountPassword.doAfterTextChanged { updateLoginButtonVisibility() }
    }

    private fun getServerInfoIsLoading() {
        binding.serverStatusText.run {
            text = getString(R.string.auth_testing_connection)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.progress_small, 0, 0, 0)
            isVisible = true
        }
    }

    private fun getServerInfoIsError(uiResult: UIResult.Error<ServerInfo>) {
        updateCenteredRefreshButtonVisibility(shouldBeVisible = true)
        when {
            uiResult.error is CertificateCombinedException ->
                showUntrustedCertDialog(uiResult.error)

            uiResult.error is OwncloudVersionNotSupportedException -> binding.serverStatusText.run {
                text = getString(R.string.server_not_supported)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
            }

            uiResult.error is NoNetworkConnectionException -> binding.serverStatusText.run {
                text = getString(R.string.error_no_network_connection)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.no_network, 0, 0, 0)
            }

            uiResult.error is SSLErrorException && uiResult.error.code == SSLErrorCode.NOT_HTTP_ALLOWED -> binding.serverStatusText.run {
                text = getString(R.string.ssl_connection_not_secure)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
            }

            else -> binding.serverStatusText.run {
                text = uiResult.error?.parseError("", resources, true)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
            }
        }
        binding.serverStatusText.isVisible = true
        showOrHideBasicAuthFields(shouldBeVisible = false)
    }

    private fun loginIsSuccess(uiResult: UIResult.Success<String>) {
        binding.authStatusText.run {
            isVisible = false
            text = ""
        }

        // Return result to account authenticator, multiaccount does not work without this
        val accountName = uiResult.data!!
        val intent = Intent()
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName)
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, contextProvider.getString(R.string.account_type))
        resultBundle = intent.extras
        setResult(Activity.RESULT_OK, intent)

        authenticationViewModel.discoverAccount(accountName = accountName, discoveryNeeded = loginAction == ACTION_CREATE)
    }

    private fun loginIsLoading() {
        binding.authStatusText.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.progress_small, 0, 0, 0)
            isVisible = true
            text = getString(R.string.auth_trying_to_login)
        }
    }

    private fun loginIsError(uiResult: UIResult.Error<String>) {
        when (uiResult.error) {
            is NoNetworkConnectionException, is ServerNotReachableException -> {
                binding.serverStatusText.run {
                    text = getString(R.string.error_no_network_connection)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.no_network, 0, 0, 0)
                }
                showOrHideBasicAuthFields(shouldBeVisible = false)
            }

            else -> {
                binding.serverStatusText.isVisible = false
                binding.authStatusText.run {
                    text = uiResult.error?.parseError("", resources, true)
                    isVisible = true
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
                }
            }
        }
    }

    /**
     * Register client if possible.
     */
    private fun registerClient(
        authorizationEndpoint: Uri,
        registrationEndpoint: String
    ) {
        authenticationViewModel.registerClient(registrationEndpoint)
        authenticationViewModel.registerClient.observe(this) {
            when (val uiResult = it.peekContent()) {
                is UIResult.Loading -> {}
                is UIResult.Success -> {
                    Timber.d("Client registered: ${it.peekContent().getStoredData()}")
                    uiResult.data?.let { clientRegistrationInfo ->
                        performGetAuthorizationCodeRequest(
                            authorizationEndpoint = authorizationEndpoint,
                            clientId = clientRegistrationInfo.clientId
                        )
                    }
                }

                is UIResult.Error -> {
                    Timber.e(uiResult.error, "Client registration failed.")
                    performGetAuthorizationCodeRequest(authorizationEndpoint)
                }
            }
        }
    }

    private fun performGetAuthorizationCodeRequest(
        authorizationEndpoint: Uri,
        clientId: String = getString(R.string.oauth2_client_id)
    ) {
        Timber.d("A browser should be opened now to authenticate this user.")

        val customTabsBuilder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
        val customTabsIntent: CustomTabsIntent = customTabsBuilder.build()

        val authorizationEndpointUri = OAuthUtils.buildAuthorizationRequest(
            authorizationEndpoint = authorizationEndpoint,
            redirectUri = OAuthUtils.buildRedirectUri(applicationContext).toString(),
            clientId = clientId,
            responseType = ResponseType.CODE.string,
            scope = if (oidcSupported) mdmProvider.getBrandingString(CONFIGURATION_OAUTH2_OPEN_ID_SCOPE, R.string.oauth2_openid_scope) else "",
            prompt = if (oidcSupported) mdmProvider.getBrandingString(CONFIGURATION_OAUTH2_OPEN_ID_PROMPT, R.string.oauth2_openid_prompt) else "",
            codeChallenge = authenticationViewModel.codeChallenge,
            state = authenticationViewModel.oidcState,
            username = username,
            sendLoginHintAndUser = mdmProvider.getBrandingBoolean(mdmKey = CONFIGURATION_SEND_LOGIN_HINT_AND_USER,
                booleanKey = R.bool.send_login_hint_and_user),
        )

        try {
            customTabsIntent.launchUrl(
                this,
                authorizationEndpointUri
            )
        } catch (e: ActivityNotFoundException) {
            binding.serverStatusText.visibility = INVISIBLE
            showMessageInSnackbar(message = this.getString(R.string.file_list_no_app_for_perform_action))
            Timber.e("No Activity found to handle Intent")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            handleGetAuthorizationCodeResponse(it)
        }
    }

    private fun handleGetAuthorizationCodeResponse(intent: Intent) {
        val authorizationCode = intent.data?.getQueryParameter("code")
        val state = intent.data?.getQueryParameter("state")

        if (state != authenticationViewModel.oidcState) {
            Timber.e("OAuth request to get authorization code failed. State mismatching, maybe somebody is trying a CSRF attack.")
            updateOAuthStatusIconAndText(StateMismatchException())
        } else {
            if (authorizationCode != null) {
                Timber.d("Authorization code received [$authorizationCode]. Let's exchange it for access token")
                exchangeAuthorizationCodeForTokens(authorizationCode)
            } else {
                val authorizationError = intent.data?.getQueryParameter("error")
                val authorizationErrorDescription = intent.data?.getQueryParameter("error_description")

                Timber.e("OAuth request to get authorization code failed. Error: [$authorizationError]." +
                        " Error description: [$authorizationErrorDescription]")
                val authorizationException =
                    if (authorizationError == "access_denied") UnauthorizedException() else Throwable("An unknown authorization error has " +
                            "occurred")
                updateOAuthStatusIconAndText(authorizationException)
            }
        }
    }

    /**
     * OAuth step 2: Exchange the received authorization code for access and refresh tokens
     */
    private fun exchangeAuthorizationCodeForTokens(authorizationCode: String) {
        binding.serverStatusText.text = getString(R.string.auth_getting_authorization)

        val clientRegistrationInfo = authenticationViewModel.registerClient.value?.peekContent()?.getStoredData()

        val clientAuth = if (clientRegistrationInfo?.clientId != null && clientRegistrationInfo.clientSecret != null) {
            OAuthUtils.getClientAuth(clientRegistrationInfo.clientSecret as String, clientRegistrationInfo.clientId)

        } else {
            OAuthUtils.getClientAuth(getString(R.string.oauth2_client_secret), getString(R.string.oauth2_client_id))
        }

        // Use oidc discovery one, or build an oauth endpoint using serverBaseUrl + Setup string.
        val tokenEndPoint: String

        var clientId: String? = null
        var clientSecret: String? = null

        val serverInfo = authenticationViewModel.serverInfo.value?.peekContent()?.getStoredData()
        if (serverInfo is ServerInfo.OIDCServer) {
            tokenEndPoint = serverInfo.oidcServerConfiguration.tokenEndpoint
            if (serverInfo.oidcServerConfiguration.isTokenEndpointAuthMethodSupportedClientSecretPost()) {
                clientId = clientRegistrationInfo?.clientId ?: contextProvider.getString(R.string.oauth2_client_id)
                clientSecret = clientRegistrationInfo?.clientSecret ?: contextProvider.getString(R.string.oauth2_client_secret)
            }
        } else {
            tokenEndPoint = "$serverBaseUrl${File.separator}${contextProvider.getString(R.string.oauth2_url_endpoint_access)}"
        }

        val scope = resources.getString(R.string.oauth2_openid_scope)

        val requestToken = TokenRequest.AccessToken(
            baseUrl = serverBaseUrl,
            tokenEndpoint = tokenEndPoint,
            clientAuth = clientAuth,
            scope = scope,
            clientId = clientId,
            clientSecret = clientSecret,
            authorizationCode = authorizationCode,
            redirectUri = OAuthUtils.buildRedirectUri(applicationContext).toString(),
            codeVerifier = authenticationViewModel.codeVerifier
        )

        authenticationViewModel.requestToken(requestToken)

        authenticationViewModel.requestToken.observe(this) {
            when (val uiResult = it.peekContent()) {
                is UIResult.Loading -> {}
                is UIResult.Success -> {
                    Timber.d("Tokens received ${uiResult.data}, trying to login, creating account and adding it to account manager")
                    val tokenResponse = uiResult.data ?: return@observe

                    authenticationViewModel.loginOAuth(
                        serverBaseUrl = serverBaseUrl,
                        username = tokenResponse.additionalParameters?.get(KEY_USER_ID).orEmpty(),
                        authTokenType = OAUTH_TOKEN_TYPE,
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken.orEmpty(),
                        scope = if (oidcSupported) mdmProvider.getBrandingString(
                            CONFIGURATION_OAUTH2_OPEN_ID_SCOPE,
                            R.string.oauth2_openid_scope,
                        ) else tokenResponse.scope,
                        updateAccountWithUsername = if (loginAction != ACTION_CREATE) userAccount?.name else null,
                        clientRegistrationInfo = clientRegistrationInfo
                    )
                }

                is UIResult.Error -> {
                    Timber.e(uiResult.error, "OAuth request to exchange authorization code for tokens failed")
                    updateOAuthStatusIconAndText(uiResult.error)
                }
            }
        }
    }

    private fun updateAuthTokenTypeAndInstructions(uiResult: UIResult<Boolean?>) {
        val supportsOAuth2 = uiResult.getStoredData()
        authTokenType = if (supportsOAuth2 != null && supportsOAuth2) OAUTH_TOKEN_TYPE else BASIC_TOKEN_TYPE

        binding.instructionsMessage.run {
            if (loginAction == ACTION_UPDATE_EXPIRED_TOKEN) {
                text =
                    if (AccountTypeUtils.getAuthTokenTypeAccessToken(accountType) == authTokenType) {
                        getString(R.string.auth_expired_oauth_token_toast)
                    } else {
                        getString(R.string.auth_expired_basic_auth_toast)
                    }
                isVisible = true
            } else {
                isVisible = false
            }
        }
    }

    private fun updateBaseUrlAndHostInput(uiResult: UIResult<String>) {
        uiResult.getStoredData()?.let { serverUrl ->
            serverBaseUrl = serverUrl

            binding.hostUrlInput.run {
                setText(serverBaseUrl)
                isEnabled = false
                isFocusable = false
            }

            if (loginAction != ACTION_CREATE && serverBaseUrl.isNotEmpty()) {
                checkOcServer()
            }
        }
    }

    /**
     * Show untrusted cert dialog
     */
    private fun showUntrustedCertDialog(certificateCombinedException: CertificateCombinedException) { // Show a dialog with the certificate info
        val dialog = SslUntrustedCertDialog.newInstanceForFullSslError(certificateCombinedException)
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.addToBackStack(null)
        dialog.show(ft, UNTRUSTED_CERT_DIALOG_TAG)
    }

    override fun onSavedCertificate() {
        Timber.d("Server certificate is trusted")
        checkOcServer()
    }

    override fun onCancelCertificate() {
        Timber.d("Server certificate is not trusted")
        binding.serverStatusText.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0)
            text = getString(R.string.ssl_certificate_not_trusted)
        }
    }

    override fun onFailedSavingCertificate() {
        Timber.d("Server certificate could not be saved")
        binding.serverStatusText.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0)
            text = getString(R.string.ssl_validator_not_saved)
        }
    }

    /* Show or hide Basic Auth fields and reset its values */
    private fun showOrHideBasicAuthFields(shouldBeVisible: Boolean) {
        binding.accountUsernameContainer.run {
            isVisible = shouldBeVisible
            isFocusable = shouldBeVisible
            isEnabled = shouldBeVisible
            if (shouldBeVisible) requestFocus()
        }
        binding.accountPasswordContainer.run {
            isVisible = shouldBeVisible
            isFocusable = shouldBeVisible
            isEnabled = shouldBeVisible
        }

        if (!shouldBeVisible) {
            binding.accountUsername.setText("")
            binding.accountPassword.setText("")
        }

        binding.authStatusText.run {
            isVisible = false
            text = ""
        }
        binding.loginButton.isVisible = false
    }

    private fun updateCenteredRefreshButtonVisibility(shouldBeVisible: Boolean) {
        if (!contextProvider.getBoolean(R.bool.show_server_url_input)) {
            binding.centeredRefreshButton.isVisible = shouldBeVisible
        }
    }

    private fun initBrandableOptionsUI() {
        val showInput = mdmProvider.getBrandingBoolean(mdmKey = CONFIGURATION_SERVER_URL_INPUT_VISIBILITY, booleanKey = R.bool.show_server_url_input)
        binding.hostUrlFrame.isVisible = showInput
        binding.centeredRefreshButton.isVisible = !showInput
        if (!showInput) {
            binding.centeredRefreshButton.setOnClickListener { checkOcServer() }
        }

        val url = mdmProvider.getBrandingString(mdmKey = CONFIGURATION_SERVER_URL, stringKey = R.string.server_url)
        if (url.isNotEmpty()) {
            binding.hostUrlInput.setText(url)
        }

        binding.loginLayout.run {
            if (contextProvider.getBoolean(R.bool.use_login_background_image)) {
                binding.loginBackgroundImage.isVisible = true
            } else {
                setBackgroundColor(resources.getColor(R.color.login_background_color))
            }
        }

        binding.welcomeLink.run {
            if (contextProvider.getBoolean(R.bool.show_welcome_link)) {
                isVisible = true
                text = contextProvider.getString(R.string.login_welcome_text).takeUnless { it.isBlank() }
                    ?: String.format(contextProvider.getString(R.string.auth_register), contextProvider.getString(R.string.app_name))
                setOnClickListener {
                    setResult(Activity.RESULT_CANCELED)
                    goToUrl(url = getString(R.string.welcome_link_url))
                }
            } else {
                isVisible = false
            }
        }

        val legacyWebfingerLookupServer = mdmProvider.getBrandingString(NO_MDM_RESTRICTION_YET, R.string.webfinger_lookup_server)
        val shouldShowLegacyWebfingerFlow = loginAction == ACTION_CREATE && legacyWebfingerLookupServer.isNotBlank()
        binding.webfingerLayout.isVisible = shouldShowLegacyWebfingerFlow
        binding.mainLoginLayout.isVisible = !shouldShowLegacyWebfingerFlow

        if (shouldShowLegacyWebfingerFlow) {
            binding.webfingerButton.setOnClickListener {
                val webfingerUsername = binding.webfingerUsername.text.toString()
                if (webfingerUsername.isNotEmpty()) {
                    authenticationViewModel.getLegacyWebfingerHost(
                        legacyWebfingerLookupServer,
                        webfingerUsername
                    )
                } else {
                    binding.webfingerStatusText.run {
                        text = getString(R.string.error_webfinger_username_empty).also { Timber.d(it) }
                        setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
                        isVisible = true
                    }
                }
            }
        }
    }

    private fun updateOAuthStatusIconAndText(authorizationException: Throwable?) {
        binding.serverStatusText.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
            text =
                if (authorizationException is UnauthorizedException) {
                    getString(R.string.auth_oauth_error_access_denied)
                } else {
                    getString(R.string.auth_oauth_error)
                }
        }
    }

    private fun updateLoginButtonVisibility() {
        binding.loginButton.run {
            isVisible = binding.accountUsername.text.toString().isNotBlank() && binding.accountPassword.text.toString().isNotBlank()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_AUTH_TOKEN_TYPE, authTokenType)
    }

    override fun finish() {
        if (accountAuthenticatorResponse != null) { // send the result bundle back if set, otherwise send an error.
            if (resultBundle != null) {
                accountAuthenticatorResponse?.onResult(resultBundle)
            } else {
                accountAuthenticatorResponse?.onError(
                    AccountManager.ERROR_CODE_CANCELED,
                    "canceled"
                )
            }
            accountAuthenticatorResponse = null
        }
        super.finish()
    }

    override fun optionLockSelected(type: LockType) {
        manageOptionLockSelected(type)
    }
}
