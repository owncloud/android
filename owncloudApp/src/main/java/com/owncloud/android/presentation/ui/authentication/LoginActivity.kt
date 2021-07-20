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
 * Copyright (C) 2012  Bartek Przybylski
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

package com.owncloud.android.presentation.ui.authentication

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Activity
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
import com.owncloud.android.MainApp.Companion.accountType
import com.owncloud.android.R
import com.owncloud.android.authentication.oauth.OAuthUtils
import com.owncloud.android.data.authentication.KEY_USER_ID
import com.owncloud.android.data.authentication.OAUTH2_OIDC_SCOPE
import com.owncloud.android.databinding.AccountSetupBinding
import com.owncloud.android.domain.authentication.oauth.model.ResponseType
import com.owncloud.android.domain.authentication.oauth.model.TokenRequest
import com.owncloud.android.domain.exceptions.NoNetworkConnectionException
import com.owncloud.android.domain.exceptions.OwncloudVersionNotSupportedException
import com.owncloud.android.domain.exceptions.ServerNotReachableException
import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.extensions.goToUrl
import com.owncloud.android.extensions.parseError
import com.owncloud.android.extensions.showErrorInToast
import com.owncloud.android.lib.common.accounts.AccountTypeUtils
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.ui.settings.SettingsActivity
import com.owncloud.android.presentation.viewmodels.authentication.OCAuthenticationViewModel
import com.owncloud.android.presentation.viewmodels.oauth.OAuthViewModel
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import com.owncloud.android.utils.DocumentProviderUtils.Companion.notifyDocumentProviderRoots
import com.owncloud.android.utils.PreferenceUtils
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File

class LoginActivity : AppCompatActivity(), SslUntrustedCertDialog.OnSslUntrustedCertListener {

    private val authenticationViewModel by viewModel<OCAuthenticationViewModel>()
    private val oauthViewModel by viewModel<OAuthViewModel>()
    private val contextProvider by inject<ContextProvider>()

    private var loginAction: Byte = ACTION_CREATE
    private var authTokenType: String? = null
    private var userAccount: Account? = null
    private lateinit var serverBaseUrl: String

    private var oidcSupported = false

    private lateinit var binding: AccountSetupBinding

    // For handling AbstractAccountAuthenticator responses
    private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var resultBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Protection against screen recording
        if (!BuildConfig.DEBUG) {
            window.addFlags(FLAG_SECURE)
        } // else, let it go, or taking screenshots & testing will not be possible

        // Get values from intent
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
            if (AccountTypeUtils.getAuthTokenTypeAccessToken(accountType) == authTokenType) { // OAuth
                startOIDCOauthorization()
            } else { // Basic
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

    private fun initLiveDataObservers() {
        // LiveData observers
        authenticationViewModel.serverInfo.observe(this, { event ->
            when (event.peekContent()) {
                is UIResult.Success -> getServerInfoIsSuccess(event.peekContent())
                is UIResult.Loading -> getServerInfoIsLoading()
                is UIResult.Error -> getServerInfoIsError(event.peekContent())
            }
        })

        authenticationViewModel.loginResult.observe(this, { event ->
            when (event.peekContent()) {
                is UIResult.Success -> loginIsSuccess(event.peekContent())
                is UIResult.Loading -> loginIsLoading()
                is UIResult.Error -> loginIsError(event.peekContent())
            }
        })

        authenticationViewModel.supportsOAuth2.observe(this, { event ->
            when (event.peekContent()) {
                is UIResult.Success -> updateAuthTokenTypeAndInstructions(event.peekContent())
                is UIResult.Error -> showErrorInToast(
                    R.string.supports_oauth2_error,
                    event.peekContent().getThrowableOrNull()
                )
            }
        })

        authenticationViewModel.baseUrl.observe(this, { event ->
            when (event.peekContent()) {
                is UIResult.Success -> updateBaseUrlAndHostInput(event.peekContent())
                is UIResult.Error -> showErrorInToast(
                    R.string.get_base_url_error,
                    event.peekContent().getThrowableOrNull()
                )
            }
        })
    }

    private fun checkOcServer() {
        val uri = binding.hostUrlInput.text.toString().trim()
        if (uri.isNotEmpty()) {
            authenticationViewModel.getServerInfo(serverUrl = uri)
        } else {
            binding.serverStatusText.run {
                text = getString(R.string.auth_can_not_auth_against_server).also { Timber.d(it) }
                isVisible = true
            }
        }
    }

    private fun getServerInfoIsSuccess(uiResult: UIResult<ServerInfo>) {
        uiResult.getStoredData()?.run {
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
                } else {
                    text = getString(R.string.auth_connection_established)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_open, 0, 0, 0)
                }
                isVisible = true
            }

            when (authenticationMethod) {
                AuthenticationMethod.BASIC_HTTP_AUTH -> {
                    authTokenType = BASIC_TOKEN_TYPE
                    showOrHideBasicAuthFields(shouldBeVisible = true)
                    binding.accountUsername.doAfterTextChanged { updateLoginButtonVisibility() }
                    binding.accountPassword.doAfterTextChanged { updateLoginButtonVisibility() }
                }

                AuthenticationMethod.BEARER_TOKEN -> {
                    showOrHideBasicAuthFields(shouldBeVisible = false)
                    authTokenType = OAUTH_TOKEN_TYPE
                    binding.loginButton.isVisible = true
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
    }

    private fun getServerInfoIsLoading() {
        binding.serverStatusText.run {
            text = getString(R.string.auth_testing_connection)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.progress_small, 0, 0, 0)
            isVisible = true
        }
    }

    private fun getServerInfoIsError(uiResult: UIResult<ServerInfo>) {
        when (uiResult.getThrowableOrNull()) {
            is CertificateCombinedException ->
                showUntrustedCertDialog(uiResult.getThrowableOrNull() as CertificateCombinedException)
            is OwncloudVersionNotSupportedException -> binding.serverStatusText.run {
                text = getString(R.string.server_not_supported)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
            }
            is NoNetworkConnectionException -> binding.serverStatusText.run {
                text = getString(R.string.error_no_network_connection)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.no_network, 0, 0, 0)
            }
            else -> binding.serverStatusText.run {
                text = uiResult.getThrowableOrNull()?.parseError("", resources, true)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
            }
        }
        binding.serverStatusText.isVisible = true
        showOrHideBasicAuthFields(shouldBeVisible = false)
    }

    private fun loginIsSuccess(uiResult: UIResult<String>) {
        binding.authStatusText.run {
            isVisible = false
            text = ""
        }

        // Return result to account authenticator, multiaccount does not work without this
        val accountName = uiResult.getStoredData()
        val intent = Intent()
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName)
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, contextProvider.getString(R.string.account_type))
        resultBundle = intent.extras
        setResult(Activity.RESULT_OK, intent)

        notifyDocumentProviderRoots(applicationContext)

        finish()
    }

    private fun loginIsLoading() {
        binding.authStatusText.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.progress_small, 0, 0, 0)
            isVisible = true
            text = getString(R.string.auth_trying_to_login)
        }
    }

    private fun loginIsError(uiResult: UIResult<String>) {
        when (uiResult.getThrowableOrNull()) {
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
                    text = uiResult.getThrowableOrNull()?.parseError("", resources, true)
                    isVisible = true
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
                }
            }
        }
    }

    /**
     * OAuth step 1: Get authorization code
     * Firstly, try the OAuth authorization with Open Id Connect, checking whether there's an available .well-known url
     * to use or not
     */
    private fun startOIDCOauthorization() {
        binding.serverStatusText.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.progress_small, 0, 0, 0)
            text = resources.getString(R.string.oauth_login_connection)
        }

        oauthViewModel.getOIDCServerConfiguration(serverBaseUrl)
        oauthViewModel.oidcDiscovery.observe(this, {
            when (it.peekContent()) {
                is UIResult.Loading -> {
                }
                is UIResult.Success -> {
                    Timber.d("Service discovery: ${it.peekContent().getStoredData()}")
                    oidcSupported = true
                    it.peekContent().getStoredData()?.let { oidcServerConfiguration ->
                        val registrationEndpoint = oidcServerConfiguration.registrationEndpoint
                        if (registrationEndpoint != null) {
                            registerClient(
                                oidcServerConfiguration.authorizationEndpoint.toUri(),
                                registrationEndpoint
                            )
                        } else {
                            performGetAuthorizationCodeRequest(oidcServerConfiguration.authorizationEndpoint.toUri())
                        }
                    }
                }
                is UIResult.Error -> {
                    Timber.e(it.peekContent().getThrowableOrNull(), "OIDC failed. Try with normal OAuth")
                    startNormalOauthorization()
                }
            }
        })
    }

    /**
     * Register client if possible.
     */
    private fun registerClient(
        authorizationEndpoint: Uri,
        registrationEndpoint: String
    ) {
        oauthViewModel.registerClient(registrationEndpoint)
        oauthViewModel.registerClient.observe(this, {
            when (it.peekContent()) {
                is UIResult.Loading -> {
                }
                is UIResult.Success -> {
                    Timber.d("Client registered: ${it.peekContent().getStoredData()}")
                    it.peekContent().getStoredData()?.let { clientRegistrationInfo ->
                        performGetAuthorizationCodeRequest(
                            authorizationEndpoint,
                            clientRegistrationInfo.clientId
                        )
                    }
                }
                is UIResult.Error -> {
                    Timber.e(it.peekContent().getThrowableOrNull(), "Client registration failed.")
                    performGetAuthorizationCodeRequest(authorizationEndpoint)
                }
            }
        })
    }

    /**
     * OAuth step 1: Get authorization code
     * If OIDC is not available, falling back to normal OAuth
     */
    private fun startNormalOauthorization() {
        val oauth2authorizationEndpoint =
            Uri.parse("$serverBaseUrl${File.separator}${getString(R.string.oauth2_url_endpoint_auth)}")
        performGetAuthorizationCodeRequest(oauth2authorizationEndpoint)
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
            scope = if (oidcSupported) OAUTH2_OIDC_SCOPE else "",
            codeChallenge = oauthViewModel.codeChallenge
        )

        customTabsIntent.launchUrl(
            this,
            authorizationEndpointUri
        )
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            handleGetAuthorizationCodeResponse(it)
        }
    }

    private fun handleGetAuthorizationCodeResponse(intent: Intent) {
        val authorizationCode = intent.data?.getQueryParameter("code")

        if (authorizationCode != null) {
            Timber.d("Authorization code received [$authorizationCode]. Let's exchange it for access token")
            exchangeAuthorizationCodeForTokens(authorizationCode)
        } else {
            val authorizationError = intent.data?.getQueryParameter("error")
            val authorizationErrorDescription = intent.data?.getQueryParameter("error_description")

            Timber.e("OAuth request to get authorization code failed. Error: [$authorizationError]. Error description: [$authorizationErrorDescription]")
            val authorizationException =
                if (authorizationError == "access_denied") UnauthorizedException() else Throwable()
            updateOAuthStatusIconAndText(authorizationException)
        }
    }

    /**
     * OAuth step 2: Exchange the received authorization code for access and refresh tokens
     */
    private fun exchangeAuthorizationCodeForTokens(authorizationCode: String) {
        binding.serverStatusText.text = getString(R.string.auth_getting_authorization)

        val clientRegistrationInfo = oauthViewModel.registerClient.value?.peekContent()?.getStoredData()

        val clientAuth = if (clientRegistrationInfo?.clientId != null && clientRegistrationInfo.clientSecret != null) {
            OAuthUtils.getClientAuth(clientRegistrationInfo.clientSecret as String, clientRegistrationInfo.clientId)

        } else {
            OAuthUtils.getClientAuth(getString(R.string.oauth2_client_secret), getString(R.string.oauth2_client_id))
        }

        // Use oidc discovery one, or build an oauth endpoint using serverBaseUrl + Setup string.
        val tokenEndPoint = oauthViewModel.oidcDiscovery.value?.peekContent()?.getStoredData()?.tokenEndpoint
            ?: "$serverBaseUrl${File.separator}${contextProvider.getString(R.string.oauth2_url_endpoint_access)}"

        val requestToken = TokenRequest.AccessToken(
            baseUrl = serverBaseUrl,
            tokenEndpoint = tokenEndPoint,
            authorizationCode = authorizationCode,
            redirectUri = OAuthUtils.buildRedirectUri(applicationContext).toString(),
            clientAuth = clientAuth,
            codeVerifier = oauthViewModel.codeVerifier
        )

        oauthViewModel.requestToken(requestToken)

        oauthViewModel.requestToken.observe(this, {
            when (it.peekContent()) {
                is UIResult.Loading -> TODO()
                is UIResult.Success -> {
                    Timber.d(
                        "Tokens received ${
                            it.peekContent().getStoredData()
                        }, trying to login, creating account and adding it to account manager"
                    )
                    val tokenResponse = it.peekContent().getStoredData() ?: return@observe

                    authenticationViewModel.loginOAuth(
                        tokenResponse.additionalParameters?.get(KEY_USER_ID) ?: "",
                        OAUTH_TOKEN_TYPE,
                        tokenResponse.accessToken,
                        tokenResponse.refreshToken.orEmpty(),
                        if (oidcSupported) OAUTH2_OIDC_SCOPE else tokenResponse.scope,
                        if (loginAction != ACTION_CREATE) userAccount?.name else null,
                        clientRegistrationInfo
                    )
                }
                is UIResult.Error -> {
                    updateOAuthStatusIconAndText(it.peekContent().getThrowableOrNull())
                    Timber.e(
                        it.peekContent().getThrowableOrNull(),
                        "OAuth request to exchange authorization code for tokens failed"
                    )
                }
            }
        })
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

    private fun initBrandableOptionsUI() {
        if (!contextProvider.getBoolean(R.bool.show_server_url_input)) {
            binding.hostUrlFrame.isVisible = false
            binding.centeredRefreshButton.run {
                isVisible = true
                setOnClickListener { checkOcServer() }
            }
        }

        if (contextProvider.getString(R.string.server_url).isNotEmpty()) {
            binding.hostUrlInput.setText(contextProvider.getString(R.string.server_url))
            checkOcServer()
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
            } else isVisible = false
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
}
