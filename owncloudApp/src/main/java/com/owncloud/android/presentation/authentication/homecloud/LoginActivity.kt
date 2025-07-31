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

package com.owncloud.android.presentation.authentication.homecloud

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.owncloud.android.BuildConfig
import com.owncloud.android.MainApp.Companion.accountType
import com.owncloud.android.R
import com.owncloud.android.databinding.AccountSetupHomecloudBinding
import com.owncloud.android.domain.exceptions.NoNetworkConnectionException
import com.owncloud.android.domain.exceptions.OwncloudVersionNotSupportedException
import com.owncloud.android.domain.exceptions.SSLErrorCode
import com.owncloud.android.domain.exceptions.SSLErrorException
import com.owncloud.android.domain.exceptions.ServerNotReachableException
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.extensions.checkPasscodeEnforced
import com.owncloud.android.extensions.manageOptionLockSelected
import com.owncloud.android.extensions.parseError
import com.owncloud.android.extensions.showErrorInToast
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.extensions.updateTextIfDiffers
import com.owncloud.android.lib.common.accounts.AccountTypeUtils
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.presentation.authentication.ACTION_CREATE
import com.owncloud.android.presentation.authentication.AccountUtils.getAccounts
import com.owncloud.android.presentation.authentication.AccountUtils.getUsernameOfAccount
import com.owncloud.android.presentation.authentication.BASIC_TOKEN_TYPE
import com.owncloud.android.presentation.authentication.EXTRA_ACCOUNT
import com.owncloud.android.presentation.authentication.EXTRA_ACTION
import com.owncloud.android.presentation.authentication.KEY_AUTH_TOKEN_TYPE
import com.owncloud.android.presentation.authentication.UNTRUSTED_CERT_DIALOG_TAG
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.documentsprovider.DocumentsProviderUtils.notifyDocumentsProviderRoots
import com.owncloud.android.presentation.security.LockType
import com.owncloud.android.presentation.security.SecurityEnforced
import com.owncloud.android.presentation.settings.SettingsActivity
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.MdmProvider
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import com.owncloud.android.utils.CONFIGURATION_SERVER_URL
import com.owncloud.android.utils.CONFIGURATION_SERVER_URL_INPUT_VISIBILITY
import com.owncloud.android.utils.PreferenceUtils
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class LoginActivity : AppCompatActivity(), SslUntrustedCertDialog.OnSslUntrustedCertListener, SecurityEnforced {

    private val authenticationViewModel by viewModel<AuthenticationViewModel>()
    private val contextProvider by inject<ContextProvider>()
    private val mdmProvider by inject<MdmProvider>()

    private var authTokenType: String? = null
    private var loginAction: Byte = ACTION_CREATE

    private var userAccount: Account? = null
    private lateinit var serverBaseUrl: String


    private lateinit var binding: AccountSetupHomecloudBinding

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
        if (savedInstanceState != null) {
            authTokenType = savedInstanceState.getString(KEY_AUTH_TOKEN_TYPE)
        }

        // UI initialization
        binding = AccountSetupHomecloudBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (loginAction != ACTION_CREATE) {
            binding.accountUsername.isEnabled = false
            binding.accountUsername.isFocusable = false
            userAccount?.name?.let {
                authenticationViewModel.handleLoginChanged(getUsernameOfAccount(it))
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
                    authenticationViewModel.handleLoginChanged(username)
                }
            }
        }

        binding.root.filterTouchesWhenObscured =
            PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this@LoginActivity)

        initBrandableOptionsUI()

        binding.hostUrlInputLayout.setEndIconOnClickListener {
            binding.hostUrlInput.setText("")
        }

        binding.ctaButton.setOnClickListener {
            authenticationViewModel.handleCtaButtonClicked()
        }

        binding.settingsLink.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }

        accountAuthenticatorResponse = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        accountAuthenticatorResponse?.onRequestContinued()

        initTextFieldsWatchers()
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
        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(newIntent)
        finish()
    }

    private fun initTextFieldsWatchers() {
        binding.hostUrlInput.doAfterTextChanged { authenticationViewModel.handleUrlChanged(it.toString()) }
        binding.accountUsername.doAfterTextChanged { authenticationViewModel.handleLoginChanged(it.toString()) }
        binding.accountPassword.doAfterTextChanged { authenticationViewModel.handlePasswordChanged(it.toString()) }
    }

    private fun initLiveDataObservers() {
        // LiveData observers
        authenticationViewModel.serverInfo.observe(this) { event ->
            when (val uiResult = event.peekContent()) {
                is UIResult.Loading -> { /* do nothing */
                }

                is UIResult.Success -> getServerInfoIsSuccess(uiResult)
                is UIResult.Error -> getServerInfoIsError(uiResult)
            }
        }

        authenticationViewModel.loginResult.observe(this) { event ->
            when (val uiResult = event.peekContent()) {
                is UIResult.Loading -> { /* do nothing */
                }

                is UIResult.Success -> loginIsSuccess(uiResult)
                is UIResult.Error -> loginIsError(uiResult)
            }
        }

        authenticationViewModel.accountDiscovery.observe(this) {
            if (it.peekContent() is UIResult.Success) {
                notifyDocumentsProviderRoots(applicationContext)
                launchFileDisplayActivity()
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

        authenticationViewModel.screenState.observe(this) {
            updateLoginButtonState(it.ctaButtonEnabled, it.ctaButtonLabel)
            binding.accountUsernameContainer.isVisible = it.credentialsAreVisible
            binding.accountPasswordContainer.isVisible = it.credentialsAreVisible
            binding.hostUrlInput.updateTextIfDiffers(it.url)
            binding.accountPassword.updateTextIfDiffers(it.password)
            binding.accountUsername.updateTextIfDiffers(it.username)
            if (it.url.isEmpty()) {
                binding.hostUrlInputLayout.endIconDrawable = null
            } else {
                binding.hostUrlInputLayout.setEndIconDrawable(R.drawable.ic_clear_input)
            }
        }
    }

    private fun getServerInfoIsSuccess(uiResult: UIResult.Success<ServerInfo>) {
        uiResult.data?.run {
            val serverInfo = this
            serverBaseUrl = baseUrl
            binding.hostUrlInput.run {
                setText(baseUrl)
            }

            if (isSecureConnection) {
                proceedLogin(serverInfo)
            } else {
                val builder = MaterialAlertDialogBuilder(this@LoginActivity)
                builder.apply {
                    setTitle(context.getString(R.string.insecure_http_url_title_dialog))
                    setMessage(context.getString(R.string.insecure_http_url_message_dialog))
                    setPositiveButton(R.string.insecure_http_url_continue_button) { dialog, which ->
                        proceedLogin(serverInfo)
                    }
                    setNegativeButton(android.R.string.cancel) { dialog, which ->
                        // do nothing
                    }
                    setCancelable(false)
                    show()
                }
            }
        }
    }

    private fun proceedLogin(serverInfo: ServerInfo) {
        when (serverInfo) {
            is ServerInfo.BasicServer -> {
                authTokenType = BASIC_TOKEN_TYPE
                if (AccountTypeUtils.getAuthTokenTypeAccessToken(accountType) != authTokenType) { // Basic
                    authenticationViewModel.loginBasic(
                        binding.accountUsername.text.toString().trim(),
                        binding.accountPassword.text.toString(),
                        if (loginAction != ACTION_CREATE) userAccount?.name else null
                    )
                }
            }

            else -> {
                // TODO it's supposed to be impossible case, but maybe we need to show an error?
            }
        }
    }

    private fun getServerInfoIsError(uiResult: UIResult.Error<ServerInfo>) {
        var text: CharSequence? = null
        when {
            uiResult.error is CertificateCombinedException ->
                showUntrustedCertDialog(uiResult.error)

            uiResult.error is OwncloudVersionNotSupportedException -> {
                text = getString(R.string.server_not_supported)
            }

            uiResult.error is NoNetworkConnectionException -> {
                text = getString(R.string.error_no_network_connection)
            }

            uiResult.error is SSLErrorException && uiResult.error.code == SSLErrorCode.NOT_HTTP_ALLOWED -> {
                text = getString(R.string.ssl_connection_not_secure)
            }

            else -> {
                text = uiResult.error?.parseError("", resources, true)
            }
        }
        text?.let {
            showError(it)
        }
    }

    private fun loginIsSuccess(uiResult: UIResult.Success<String>) {
        // Return result to account authenticator, multiaccount does not work without this
        val accountName = uiResult.data!!
        val intent = Intent()
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName)
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, contextProvider.getString(R.string.account_type))
        resultBundle = intent.extras
        setResult(RESULT_OK, intent)

        authenticationViewModel.discoverAccount(accountName = accountName, discoveryNeeded = loginAction == ACTION_CREATE)
    }

    private fun loginIsError(uiResult: UIResult.Error<String>) {
        val text = when (uiResult.error) {
            is NoNetworkConnectionException, is ServerNotReachableException -> {
                getString(R.string.error_no_network_connection)
            }

            else -> {
                uiResult.error?.parseError("", resources, true)
            }
        }
        text?.let {
            showError(it)
        }
    }

    private fun showError(text: CharSequence) {
        MaterialAlertDialogBuilder(this)
            .setMessage(text)
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .show()
    }

    private fun updateBaseUrlAndHostInput(uiResult: UIResult<String>) {
        uiResult.getStoredData()?.let { serverUrl ->
            serverBaseUrl = serverUrl

            binding.hostUrlInput.run {
                setText(serverBaseUrl)
                isEnabled = false
                isFocusable = false
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
        // do nothing
    }

    override fun onCancelCertificate() {
        Timber.d("Server certificate is not trusted")
        showError(getString(R.string.ssl_certificate_not_trusted))
    }

    override fun onFailedSavingCertificate() {
        Timber.d("Server certificate could not be saved")
        showError(getString(R.string.ssl_validator_not_saved))
    }

    private fun initBrandableOptionsUI() {
        val showInput = mdmProvider.getBrandingBoolean(mdmKey = CONFIGURATION_SERVER_URL_INPUT_VISIBILITY, booleanKey = R.bool.show_server_url_input)
        binding.hostUrlFrame.isVisible = showInput

        val url = mdmProvider.getBrandingString(mdmKey = CONFIGURATION_SERVER_URL, stringKey = R.string.server_url)
        if (url.isNotEmpty()) {
            binding.hostUrlInput.setText(url)
        }
    }

    private fun updateLoginButtonState(isEnabled: Boolean, label: String) {
        binding.ctaButton.run {
            this.isEnabled = isEnabled
            this.text = label
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
