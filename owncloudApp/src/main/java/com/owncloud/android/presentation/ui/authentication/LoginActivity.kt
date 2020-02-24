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
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.presentation.ui.authentication

import android.accounts.Account
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.domain.exceptions.NoNetworkConnectionException
import com.owncloud.android.domain.exceptions.OwncloudVersionNotSupportedException
import com.owncloud.android.domain.exceptions.ServerNotReachableException
import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.extensions.parseError
import com.owncloud.android.lib.common.accounts.AccountTypeUtils
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.viewmodels.authentication.OCAuthenticationViewModel
import com.owncloud.android.ui.dialog.LoadingDialog
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import com.owncloud.android.utils.PreferenceUtils
import kotlinx.android.synthetic.main.account_setup.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class LoginActivity : AppCompatActivity(), SslUntrustedCertDialog.OnSslUntrustedCertListener {

    private val authenticatorViewModel by viewModel<OCAuthenticationViewModel>()

    private var loginAction: Byte = ACTION_CREATE
    private var authTokenType: String? = null
    private var userAccount: Account? = null

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
        savedInstanceState?.let {
            authTokenType = it.getString(KEY_AUTH_TOKEN_TYPE)
        }

        // UI initialization
        setContentView(R.layout.account_setup)

        login_layout.run {
            filterTouchesWhenObscured =
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this@LoginActivity)
            if (resources.getBoolean(R.bool.use_login_background_image)) {
                login_background_image.visibility = VISIBLE
            } else {
                setBackgroundColor(resources.getColor(R.color.login_background_color))
            }
        }

        instructions_message.run {
            if (loginAction == ACTION_UPDATE_EXPIRED_TOKEN) {
                text = if (AccountTypeUtils.getAuthTokenTypeAccessToken(MainApp.authTokenType) == authTokenType) {
                    getString(R.string.auth_expired_oauth_token_toast)
                } else {
                    getString(R.string.auth_expired_basic_auth_toast)
                }
                visibility = VISIBLE
            } else visibility = GONE
        }

        welcome_link.run {
            if (resources.getBoolean(R.bool.show_welcome_link)) {
                visibility = VISIBLE
                text = String.format(getString(R.string.auth_register), getString(R.string.app_name))
            } else visibility = GONE
        }

        embeddedCheckServerButton.run {
            setOnClickListener { checkOcServer() }
        }

        server_status_text.run {
            isVisible = false
        }

        // LiveData observers
        authenticatorViewModel.serverInfo.observe(this, Observer { event ->
            when (event.peekContent()) {
                is UIResult.Success -> getServerInfoIsSuccess(event.peekContent())
                is UIResult.Loading -> getServerInfoIsLoading()
                is UIResult.Error -> getServerInfoIsError(event.peekContent())
            }
        })

        authenticatorViewModel.loginResult.observe(this, Observer { event ->
            when (event.peekContent()) {
                is UIResult.Success -> loginIsSuccess(event.peekContent())
                is UIResult.Loading -> loginIsLoading()
                is UIResult.Error -> loginIsError(event.peekContent())
            }
        })
    }

    private fun checkOcServer() {
        val uri = hostUrlInput.text.toString().trim()
        authenticatorViewModel.getServerInfo(serverUrl = uri)
    }

    private fun updateLoginButtonState() {
        loginButton.isVisible =
            account_username.text.toString().isNotBlank() && account_password.text.toString().isNotBlank()
        loginButton.setOnClickListener {
            authenticatorViewModel.login(
                account_username.text.toString(),
                account_password.text.toString()
            )
        }
    }

    private fun getServerInfoIsSuccess(uiResult: UIResult<ServerInfo>) {
        uiResult.getStoredData()?.run {
            hostUrlInput.doAfterTextChanged {
                if (authenticatorViewModel.serverInfo.value == null || uiResult.getStoredData()?.baseUrl != hostUrlInput.text.toString()) {
                    showBasicAuthFields(shouldBeVisible = false)
                    server_status_text.run{
                        text = ""
                        isVisible = false
                    }
                }
            }

            server_status_text.run {
                if (isSecureConnection) {
                    text = resources.getString(R.string.auth_secure_connection)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, 0, 0)
                } else {
                    text = resources.getString(R.string.auth_connection_established)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_open, 0, 0, 0)
                }
                visibility = VISIBLE
            }

            when (authenticationMethod) {
                AuthenticationMethod.BASIC_HTTP_AUTH -> {
                    showBasicAuthFields(shouldBeVisible = true)
                    account_username.run {
                        doAfterTextChanged { updateLoginButtonState() }
                    }
                    account_password.run {
                        doAfterTextChanged {
                            updateLoginButtonState()
                            account_password_container.isPasswordVisibilityToggleEnabled =
                                !account_password.text.isNullOrEmpty()
                        }
                    }
                }

                AuthenticationMethod.BEARER_TOKEN -> {
                    loginButton.visibility = VISIBLE
                }

                else -> {
                    server_status_text.run {
                        text = resources.getString(R.string.auth_unsupported_auth_method)
                        setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
                        visibility = VISIBLE
                    }
                }
            }
        }
    }

    private fun getServerInfoIsLoading() {
        server_status_text.run {
            text = resources.getString(R.string.auth_testing_connection)
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
                text = uiResult.getThrowableOrNull()?.parseError("", resources)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
            }
        }
        server_status_text.isVisible = true
        showBasicAuthFields(shouldBeVisible = false)
    }

    private fun loginIsSuccess(uiResult: UIResult<Unit>) {
        dismissDialog()
        Toast.makeText(applicationContext, "Login success, time to save account", Toast.LENGTH_LONG).show()
        auth_status_text.isVisible = false
    }

    private fun loginIsLoading() {
        val dialog = LoadingDialog.newInstance(R.string.auth_trying_to_login, true)
        dialog.show(supportFragmentManager, WAIT_DIALOG_TAG)
    }

    private fun loginIsError(uiResult: UIResult<Unit>) {
        dismissDialog()
        when (uiResult.getThrowableOrNull()) {
            is NoNetworkConnectionException, is ServerNotReachableException -> {
                server_status_text.run {
                    text = getString(R.string.error_no_network_connection)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.no_network, 0, 0, 0)
                }
                showBasicAuthFields(shouldBeVisible = false)
            }
            else -> auth_status_text.run {
                text = uiResult.getThrowableOrNull()?.parseError("", resources)
                isVisible = true
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
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
    }

    override fun onFailedSavingCertificate() {
        Timber.d("Server certificate could not be saved")
        Toast.makeText(this, R.string.ssl_validator_not_saved, Toast.LENGTH_LONG).show()
    }

    private fun dismissDialog() {
        val frag = supportFragmentManager.findFragmentByTag(WAIT_DIALOG_TAG)
        if (frag is DialogFragment) {
            frag.dismiss()
        }
    }

    private fun showBasicAuthFields(shouldBeVisible: Boolean) {
        account_username_container.run {
            visibility = if (shouldBeVisible) VISIBLE else GONE
            isFocusable = shouldBeVisible
            isEnabled = shouldBeVisible
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
    }
}
