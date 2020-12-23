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
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import com.owncloud.android.MainApp
import com.owncloud.android.MainApp.Companion.accountType
import com.owncloud.android.R
import com.owncloud.android.authentication.oauth.AuthStateManager
import com.owncloud.android.authentication.oauth.OAuthConnectionBuilder
import com.owncloud.android.authentication.oauth.OAuthUtils
import com.owncloud.android.data.authentication.KEY_USER_ID
import com.owncloud.android.data.authentication.OAUTH2_OIDC_SCOPE
import com.owncloud.android.domain.authentication.oauth.model.TokenRequest
import com.owncloud.android.domain.exceptions.NoNetworkConnectionException
import com.owncloud.android.domain.exceptions.OwncloudVersionNotSupportedException
import com.owncloud.android.domain.exceptions.ServerNotReachableException
import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.extensions.parseError
import com.owncloud.android.extensions.showErrorInToast
import com.owncloud.android.lib.common.accounts.AccountTypeUtils
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.viewmodels.authentication.OCAuthenticationViewModel
import com.owncloud.android.presentation.viewmodels.oauth.OAuthViewModel
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import com.owncloud.android.utils.DocumentProviderUtils.Companion.notifyDocumentProviderRoots
import com.owncloud.android.utils.PreferenceUtils
import kotlinx.android.synthetic.main.account_setup.*
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthorizationServiceConfiguration.RetrieveConfigurationCallback
import net.openid.appauth.ResponseTypeValues
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

    private var authService: AuthorizationService? = null
    private lateinit var authStateManager: AuthStateManager
    private var oidcSupported = false
    private var authorizationServiceConfiguration: AuthorizationServiceConfiguration? = null

    // For handling AbstractAccountAuthenticator responses
    private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var resultBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Protection against screen recording
        if (!MainApp.isDeveloper) {
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
        setContentView(R.layout.account_setup)

        if (loginAction != ACTION_CREATE) {
            account_username.isEnabled = false
            account_username.isFocusable = false
        }

        if (savedInstanceState == null) {
            if (userAccount != null) {
                authenticationViewModel.getBaseUrl((userAccount as Account).name)
            } else {
                serverBaseUrl = getString(R.string.server_url).trim()
            }

            userAccount?.let {
                AccountUtils.getUsernameForAccount(it)?.let { username ->
                    account_username.setText(username)
                }
            }
        }

        login_layout.filterTouchesWhenObscured =
            PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this@LoginActivity)

        initBrandableOptionsUI()

        thumbnail.setOnClickListener { checkOcServer() }

        embeddedCheckServerButton.setOnClickListener { checkOcServer() }

        loginButton.setOnClickListener {
            if (AccountTypeUtils.getAuthTokenTypeAccessToken(accountType) == authTokenType) { // OAuth
                startOIDCOauthorization()
            } else { // Basic
                authenticationViewModel.loginBasic(
                    account_username.text.toString().trim(),
                    account_password.text.toString(),
                    if (loginAction != ACTION_CREATE) userAccount?.name else null
                )
            }
        }

        accountAuthenticatorResponse = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        accountAuthenticatorResponse?.onRequestContinued()

        // LiveData observers
        authenticationViewModel.serverInfo.observe(this, Observer { event ->
            when (event.peekContent()) {
                is UIResult.Success -> getServerInfoIsSuccess(event.peekContent())
                is UIResult.Loading -> getServerInfoIsLoading()
                is UIResult.Error -> getServerInfoIsError(event.peekContent())
            }
        })

        authenticationViewModel.loginResult.observe(this, Observer { event ->
            when (event.peekContent()) {
                is UIResult.Success -> loginIsSuccess(event.peekContent())
                is UIResult.Loading -> loginIsLoading()
                is UIResult.Error -> loginIsError(event.peekContent())
            }
        })

        authenticationViewModel.supportsOAuth2.observe(this, Observer { event ->
            when (event.peekContent()) {
                is UIResult.Success -> updateAuthTokenTypeAndInstructions(event.peekContent())
                is UIResult.Error -> showErrorInToast(
                    R.string.supports_oauth2_error,
                    event.peekContent().getThrowableOrNull()
                )
            }
        })

        authenticationViewModel.baseUrl.observe(this, Observer { event ->
            when (event.peekContent()) {
                is UIResult.Success -> updateBaseUrlAndHostInput(event.peekContent())
                is UIResult.Error -> showErrorInToast(
                    R.string.get_base_url_error,
                    event.peekContent().getThrowableOrNull()
                )
            }
        })

        authStateManager = AuthStateManager.getInstance(this)
    }

    private fun checkOcServer() {
        val uri = hostUrlInput.text.toString().trim()
        if (uri.isNotEmpty()) {
            authenticationViewModel.getServerInfo(serverUrl = uri)
        } else {
            server_status_text.run {
                text = getString(R.string.auth_can_not_auth_against_server).also { Timber.d(it) }
                isVisible = true
            }
        }
    }

    private fun getServerInfoIsSuccess(uiResult: UIResult<ServerInfo>) {
        uiResult.getStoredData()?.run {
            serverBaseUrl = baseUrl
            hostUrlInput.run {
                setText(baseUrl)
                doAfterTextChanged {
                    //If user modifies url, reset fields and force him to check url again
                    if (authenticationViewModel.serverInfo.value == null || baseUrl != hostUrlInput.text.toString()) {
                        showOrHideBasicAuthFields(shouldBeVisible = false)
                        loginButton.isVisible = false
                        server_status_text.run {
                            text = ""
                            visibility = INVISIBLE
                        }
                    }
                }
            }

            server_status_text.run {
                if (isSecureConnection) {
                    text = getString(R.string.auth_secure_connection)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, 0, 0)
                } else {
                    text = getString(R.string.auth_connection_established)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_open, 0, 0, 0)
                }
                visibility = VISIBLE
            }

            when (authenticationMethod) {
                AuthenticationMethod.BASIC_HTTP_AUTH -> {
                    authTokenType = BASIC_TOKEN_TYPE
                    showOrHideBasicAuthFields(shouldBeVisible = true)
                    account_username.doAfterTextChanged { updateLoginButtonVisibility() }
                    account_password.doAfterTextChanged { updateLoginButtonVisibility() }
                }

                AuthenticationMethod.BEARER_TOKEN -> {
                    showOrHideBasicAuthFields(shouldBeVisible = false)
                    authTokenType = OAUTH_TOKEN_TYPE
                    loginButton.visibility = VISIBLE
                }

                else -> {
                    server_status_text.run {
                        text = getString(R.string.auth_unsupported_auth_method)
                        setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
                        visibility = VISIBLE
                    }
                }
            }
        }
    }

    private fun getServerInfoIsLoading() {
        server_status_text.run {
            text = getString(R.string.auth_testing_connection)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.progress_small, 0, 0, 0)
            visibility = VISIBLE
        }
    }

    private fun getServerInfoIsError(uiResult: UIResult<ServerInfo>) {
        when (uiResult.getThrowableOrNull()) {
            is CertificateCombinedException ->
                showUntrustedCertDialog(uiResult.getThrowableOrNull() as CertificateCombinedException)
            is OwncloudVersionNotSupportedException -> server_status_text.run {
                text = getString(R.string.server_not_supported)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
            }
            is NoNetworkConnectionException -> server_status_text.run {
                text = getString(R.string.error_no_network_connection)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.no_network, 0, 0, 0)
            }
            else -> server_status_text.run {
                text = uiResult.getThrowableOrNull()?.parseError("", resources, true)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
            }
        }
        server_status_text.isVisible = true
        showOrHideBasicAuthFields(shouldBeVisible = false)
    }

    private fun loginIsSuccess(uiResult: UIResult<String>) {
        auth_status_text.run {
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

        authorizationServiceConfiguration?.let { authServiceConfig ->
            accountName?.let {
                val authState = AuthState(authServiceConfig)
                authStateManager.replace(it, authState)
            }
        }

        finish()
    }

    private fun loginIsLoading() {
        auth_status_text.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.progress_small, 0, 0, 0)
            isVisible = true
            text = getString(R.string.auth_trying_to_login)
        }
    }

    private fun loginIsError(uiResult: UIResult<String>) {
        when (uiResult.getThrowableOrNull()) {
            is NoNetworkConnectionException, is ServerNotReachableException -> {
                server_status_text.run {
                    text = getString(R.string.error_no_network_connection)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.no_network, 0, 0, 0)
                }
                showOrHideBasicAuthFields(shouldBeVisible = false)
            }
            else -> {
                server_status_text.isVisible = false
                auth_status_text.run {
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
        server_status_text.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.progress_small, 0, 0, 0)
            text = resources.getString(R.string.oauth_login_connection)
        }

        oauthViewModel.getOIDCServerConfiguration(serverBaseUrl)
        oauthViewModel.oidcDiscovery.observe(this, {
            when (it.peekContent()) {
                is UIResult.Loading -> TODO()
                is UIResult.Success -> {
                    Timber.d("Service discovery: ${it.peekContent().getStoredData()}")
                    oidcSupported = true
                    val oidcServerConfiguration = it.peekContent().getStoredData() ?: return@observe
                    val newServerConfiguration = AuthorizationServiceConfiguration(
                        oidcServerConfiguration.authorization_endpoint.toUri(),
                        oidcServerConfiguration.token_endpoint.toUri(),
                        oidcServerConfiguration.registration_endpoint.toUri()
                    )
                    performGetAuthorizationCodeRequest(newServerConfiguration)
                    authorizationServiceConfiguration = newServerConfiguration
                }
                is UIResult.Error -> {
                    Timber.e(it.peekContent().getThrowableOrNull(), "OIDC failed. Try with normal OAuth")
                    startNormalOauthorization()
                }
            }
        })
    }

    /**
     * OAuth step 1: Get authorization code
     * If OIDC is not available, falling back to normal OAuth
     */
    private fun startNormalOauthorization() {
        val retrieveConfigurationCallback =
            RetrieveConfigurationCallback { serviceConfiguration, exception ->
                if (exception != null) {
                    Timber.e(exception, "OAuth failed.")
                    Timber.e(
                        exception,
                        "OAuth failed. Code: ${exception.code} Error: ${exception.error} Error Description: ${exception.errorDescription} Error Uri: ${exception.errorUri} Type: ${exception.type}"
                    )

                    updateOAuthStatusIconAndText(exception)
                } else if (serviceConfiguration != null) {
                    performGetAuthorizationCodeRequest(serviceConfiguration)
                    authorizationServiceConfiguration = serviceConfiguration
                }
            }

        OAuthUtils.buildOAuthorizationServiceConfig(
            this,
            serverBaseUrl,
            retrieveConfigurationCallback
        )
    }

    private fun performGetAuthorizationCodeRequest(authorizationServiceConfiguration: AuthorizationServiceConfiguration) {
        Timber.d("A browser should be opened now to authenticate this user.")
        val clientId = getString(R.string.oauth2_client_id)
        val redirectUri = Uri.Builder()
            .scheme(getString(R.string.oauth2_redirect_uri_scheme))
            .authority(getString(R.string.oauth2_redirect_uri_path))
            .build()
        val scope = if (oidcSupported) OAUTH2_OIDC_SCOPE else ""
        val builder = AuthorizationRequest.Builder(
            authorizationServiceConfiguration,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        ).setScope(scope)

        val request = builder.build()
        Timber.d("Request information: ${request.jsonSerializeString()}")
        val completedIntent = Intent(this, LoginActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, completedIntent, 0)

        val appAuthConfigurationBuilder = AppAuthConfiguration.Builder()
        appAuthConfigurationBuilder.setConnectionBuilder(OAuthConnectionBuilder(this))
        authService = AuthorizationService(this, appAuthConfigurationBuilder.build())
        Timber.d("Sends an authorization request to the authorization service using a custom tab or browser instance.")
        authService?.performAuthorizationRequest(request, pendingIntent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            handleGetAuthorizationCodeResponse(it)
        }
    }

    private fun handleGetAuthorizationCodeResponse(intent: Intent) {
        val authorizationResponse = AuthorizationResponse.fromIntent(intent)
        val authorizationException = AuthorizationException.fromIntent(intent)
        if (authorizationResponse?.authorizationCode != null) {
            exchangeAuthorizationCodeForTokens(authorizationResponse)
        } else if (authorizationException != null) {
            updateOAuthStatusIconAndText(authorizationException)
            Timber.e(authorizationException, "OAuth request to get authorization code failed")
        }
    }

    /**
     * OAuth step 2: Exchange the received authorization code for access and refresh tokens
     */
    private fun exchangeAuthorizationCodeForTokens(authorizationResponse: AuthorizationResponse) {
        server_status_text.text = getString(R.string.auth_getting_authorization)
        val clientAuth = OAuthUtils.createClientSecretBasic(getString(R.string.oauth2_client_secret))

        // Use oidc discovery one, or build an oauth endpoint using serverBaseUrl + Setup string.
        val tokenEndPoint = oauthViewModel.oidcDiscovery.value?.peekContent()?.getStoredData()?.token_endpoint
            ?: "$serverBaseUrl${File.separator}${contextProvider.getString(R.string.oauth2_url_endpoint_access)}"

        val requestToken = TokenRequest(
            baseUrl = serverBaseUrl,
            tokenEndpoint = tokenEndPoint,
            authorizationCode = authorizationResponse.authorizationCode ?: "",
            grantType = TokenRequest.GrantType.AUTHORIZATION_CODE.string,
            redirectUri = authorizationResponse.request.redirectUri.toString(),
            codeVerifier = authorizationResponse.request.codeVerifier ?: "",
            // TODO: DO IT MORE ELEGANT!
            clientSecretBasic = clientAuth.getRequestHeaders(getString(R.string.oauth2_client_id))["Authorization"]!!
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
                        if (loginAction != ACTION_CREATE) userAccount?.name else null
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

        instructions_message.run {
            if (loginAction == ACTION_UPDATE_EXPIRED_TOKEN) {
                text =
                    if (AccountTypeUtils.getAuthTokenTypeAccessToken(accountType) == authTokenType) {
                        getString(R.string.auth_expired_oauth_token_toast)
                    } else {
                        getString(R.string.auth_expired_basic_auth_toast)
                    }
                visibility = VISIBLE
            } else visibility = GONE
        }
    }

    private fun updateBaseUrlAndHostInput(uiResult: UIResult<String>) {
        uiResult.getStoredData()?.let { serverUrl ->
            serverBaseUrl = serverUrl

            hostUrlInput.run {
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
        server_status_text.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0)
            text = getString(R.string.ssl_certificate_not_trusted)
        }
    }

    override fun onFailedSavingCertificate() {
        Timber.d("Server certificate could not be saved")
        server_status_text.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0)
            text = getString(R.string.ssl_validator_not_saved)
        }
    }

    /* Show or hide Basic Auth fields and reset its values */
    private fun showOrHideBasicAuthFields(shouldBeVisible: Boolean) {
        account_username_container.run {
            visibility = if (shouldBeVisible) VISIBLE else GONE
            isFocusable = shouldBeVisible
            isEnabled = shouldBeVisible
            if (shouldBeVisible) requestFocus()
        }
        account_password_container.run {
            visibility = if (shouldBeVisible) VISIBLE else GONE
            isFocusable = shouldBeVisible
            isEnabled = shouldBeVisible
        }

        if (!shouldBeVisible) {
            account_username.setText("")
            account_password.setText("")
        }

        auth_status_text.run {
            isVisible = false
            text = ""
        }
        loginButton.isVisible = false
    }

    private fun initBrandableOptionsUI() {
        if (!contextProvider.getBoolean(R.bool.show_server_url_input)) {
            hostUrlFrame.visibility = GONE
            centeredRefreshButton.run {
                isVisible = true
                setOnClickListener { checkOcServer() }
            }
        }

        if (contextProvider.getString(R.string.server_url).isNotEmpty()) {
            hostUrlInput.setText(contextProvider.getString(R.string.server_url))
            checkOcServer()
        }

        login_layout.run {
            if (contextProvider.getBoolean(R.bool.use_login_background_image)) {
                login_background_image.visibility = VISIBLE
            } else {
                setBackgroundColor(resources.getColor(R.color.login_background_color))
            }
        }

        welcome_link.run {
            if (contextProvider.getBoolean(R.bool.show_welcome_link)) {
                visibility = VISIBLE
                text = String.format(getString(R.string.auth_register), getString(R.string.app_name))
                setOnClickListener {
                    val openWelcomeLinkIntent =
                        Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.welcome_link_url)))
                    setResult(Activity.RESULT_CANCELED)
                    startActivity(openWelcomeLinkIntent)
                }
            } else visibility = GONE
        }
    }

    private fun updateOAuthStatusIconAndText(authorizationException: Throwable?) {
        server_status_text.run {
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
        loginButton.run {
            isVisible = account_username.text.toString().isNotBlank() && account_password.text.toString().isNotBlank()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_AUTH_TOKEN_TYPE, authTokenType)
    }

    override fun onDestroy() {
        super.onDestroy()
        authService?.dispose()
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
