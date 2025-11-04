package com.owncloud.android.presentation.authentication.homecloud

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.owncloud.android.R
import com.owncloud.android.databinding.AccountDialogCodeBinding
import com.owncloud.android.databinding.AccountSetupHomecloudBinding
import com.owncloud.android.domain.server.model.Server
import com.owncloud.android.extensions.applyStatusBarInsets
import com.owncloud.android.extensions.checkPasscodeEnforced
import com.owncloud.android.extensions.manageOptionLockSelected
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.extensions.updateTextIfDiffers
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.presentation.authentication.AccountUtils.getAccounts
import com.owncloud.android.presentation.authentication.UNTRUSTED_CERT_DIALOG_TAG
import com.owncloud.android.presentation.authentication.homecloud.LoginViewModel.LoginScreenState
import com.owncloud.android.presentation.security.LockType
import com.owncloud.android.presentation.security.SecurityEnforced
import com.owncloud.android.presentation.settings.SettingsActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import com.owncloud.android.utils.PreferenceUtils
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginActivity : AppCompatActivity(), SslUntrustedCertDialog.OnSslUntrustedCertListener, SecurityEnforced {

    private val loginViewModel by viewModel<LoginViewModel>()

    private lateinit var binding: AccountSetupHomecloudBinding
    private val dialogBinding by lazy { AccountDialogCodeBinding.inflate(layoutInflater) }

    private val adapter by lazy {
        ServerAddressAdapter(
            this, mutableListOf()
        )
    }

    private val dialog by lazy {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setView(dialogBinding.root)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPasscodeEnforced(this)
        enableEdgeToEdge()

        handleDeepLink()

        binding = AccountSetupHomecloudBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.settingsLink.applyStatusBarInsets(usePaddings = false)
        binding.root.filterTouchesWhenObscured =
            PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED, block = {
                launch {
                    loginViewModel.state.collect {
                        updateLoginState(it)
                    }
                }
                launch {
                    loginViewModel.events.collect {
                        handleEvents(it)
                    }
                }
            })
        }
        setupListeners()
    }

    private fun handleDeepLink() {
        if (intent.data != null) {
            if (getAccounts(baseContext).isNotEmpty()) {
                launchFileDisplayActivity()
            } else {
                showMessageInSnackbar(message = baseContext.getString(R.string.uploader_wrn_no_account_title))
            }
        }
    }

    private fun setupListeners() {
        binding.settingsLink.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }
        binding.accountUsername.doAfterTextChanged { text ->
            loginViewModel.onUserNameChanged(text.toString())
        }
        binding.accountPassword.doAfterTextChanged { text ->
            loginViewModel.onPasswordChanged(text.toString())
        }
        binding.resetPasswordLink.setOnClickListener {
            showMessageInSnackbar(message = "Not implemented yet")
        }
        binding.actionButton.setOnClickListener {
            loginViewModel.onActionClicked()
        }

        binding.hostUrlInput.setAdapter(adapter)
        binding.hostUrlInput.setOnItemClickListener { parent, view, position, id ->
            val selectedServer = parent.getItemAtPosition(position) as Server
            loginViewModel.onServerSelected(selectedServer)
        }
        binding.hostUrlInput.doAfterTextChanged { text ->
            loginViewModel.onServerUrlChanged(text.toString())
        }
        binding.serversRefreshButton.setOnClickListener {
            loginViewModel.refreshServers()
        }
    }

    private fun handleEvents(event: LoginViewModel.LoginEvent) {
        when (event) {
            LoginViewModel.LoginEvent.NavigateToCodeDialog -> showCodeDialog()
            LoginViewModel.LoginEvent.NavigateToLogin -> showLoginScreen()
            is LoginViewModel.LoginEvent.LoginResult -> handleLoginResult(event)
            is LoginViewModel.LoginEvent.ShowUntrustedCertDialog -> showUntrustedCertDialog(event.certificateCombinedException)
        }
    }

    private fun showUntrustedCertDialog(certificateCombinedException: CertificateCombinedException) {
        val dialog = SslUntrustedCertDialog.newInstanceForFullSslError(certificateCombinedException)
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.addToBackStack(null)
        dialog.show(ft, UNTRUSTED_CERT_DIALOG_TAG)
    }

    private fun handleLoginResult(result: LoginViewModel.LoginEvent.LoginResult) {
        val intent = Intent()
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, result.accountName)
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type))
        setResult(RESULT_OK, intent)
        launchFileDisplayActivity()
    }

    private fun updateServers(servers: List<Server>) {
        adapter.setServers(servers)
        dialogBinding.skipButton.isEnabled = servers.isNotEmpty()
    }

    private fun showLoginScreen() {
        binding.loginStateGroup.visibility = View.VISIBLE
        dialog.dismiss()
    }

    private fun showCodeDialog() {
        dialogBinding.codeEditText.doAfterTextChanged {
            val isNotEmpty = it.toString().isNotEmpty()
            dialogBinding.allowButton.isEnabled = isNotEmpty
            if (isNotEmpty) {
                dialogBinding.codeInputLayout.error = null
            }
        }
        dialogBinding.allowButton.setOnClickListener {
            loginViewModel.onCodeEntered(dialogBinding.codeEditText.text.toString())
        }
        dialogBinding.skipButton.setOnClickListener {
            loginViewModel.onSkipClicked()
        }
        dialog.show()
    }

    private fun updateLoginState(state: LoginScreenState) {
        updateServers(state.servers)
        binding.accountUsername.updateTextIfDiffers(state.username)
        binding.accountPassword.updateTextIfDiffers(state.password)
        binding.serversRefreshButton.visibility = if (state.isRefreshServersLoading) View.INVISIBLE else View.VISIBLE
        binding.serversRefreshLoading.visibility = if (state.isRefreshServersLoading) View.VISIBLE else View.GONE
        binding.errorMessage.text = state.errorMessage
        binding.errorMessage.isVisible = !state.errorMessage.isNullOrBlank()

        when (state.loginState) {
            LoginViewModel.LoginState.REMOTE_ACCESS -> {
                binding.accountUsernameContainer.error = state.errorEmailInvalidMessage
                binding.loginStateGroup.visibility = View.GONE
                binding.actionButton.setText(R.string.homecloud_action_button_next)
                dialogBinding.allowButton.visibility = if (state.isAllowLoading) View.INVISIBLE else View.VISIBLE
                dialogBinding.allowLoading.visibility = if (state.isAllowLoading) View.VISIBLE else View.GONE
                binding.actionButton.isEnabled = state.errorEmailInvalidMessage == null && state.username.isNotEmpty()
                binding.serversRefreshButton.visibility = View.INVISIBLE
                dialogBinding.codeInputLayout.error = state.errorCodeMessage
            }

            LoginViewModel.LoginState.LOGIN -> {
                if (state.isLoading) {
                    binding.loadingLayout.visibility = View.VISIBLE
                    binding.actionGroup.visibility = View.GONE
                    binding.loginStateGroup.visibility = View.GONE
                    binding.serversRefreshButton.visibility = View.INVISIBLE
                } else {
                    binding.loadingLayout.visibility = View.GONE
                    binding.actionGroup.visibility = View.VISIBLE
                    binding.loginStateGroup.visibility = View.VISIBLE
                    binding.accountUsername.isEnabled = false
                    binding.actionButton.setText(R.string.setup_btn_login)
                    binding.actionButton.isEnabled = state.username.isNotEmpty() && state.password.isNotEmpty() &&
                            (state.selectedServer != null || state.serverUrl.isNotEmpty()) && Patterns.EMAIL_ADDRESS.matcher(state.username).matches()
                }
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

    override fun onSavedCertificate() {
        loginViewModel.onActionClicked()
    }

    override fun onFailedSavingCertificate() {

    }

    override fun onCancelCertificate() {

    }

    override fun optionLockSelected(type: LockType) {
        manageOptionLockSelected(type)
    }
}