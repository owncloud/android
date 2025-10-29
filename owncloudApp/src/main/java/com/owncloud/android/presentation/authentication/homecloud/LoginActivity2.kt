package com.owncloud.android.presentation.authentication.homecloud

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.owncloud.android.R
import com.owncloud.android.databinding.AccountDialogCodeBinding
import com.owncloud.android.databinding.AccountSetupHomecloud2Binding
import com.owncloud.android.domain.server.model.Server
import com.owncloud.android.extensions.applyStatusBarInsets
import com.owncloud.android.extensions.checkPasscodeEnforced
import com.owncloud.android.extensions.manageOptionLockSelected
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.presentation.authentication.AccountUtils.getAccounts
import com.owncloud.android.presentation.authentication.homecloud.LoginViewModel.LoginScreenState
import com.owncloud.android.presentation.security.LockType
import com.owncloud.android.presentation.security.SecurityEnforced
import com.owncloud.android.presentation.settings.SettingsActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import com.owncloud.android.utils.PreferenceUtils
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginActivity2 : AppCompatActivity(), SslUntrustedCertDialog.OnSslUntrustedCertListener, SecurityEnforced {

    private val loginViewModel by viewModel<LoginViewModel>()

    private lateinit var binding: AccountSetupHomecloud2Binding
    private val dialogBinding by lazy { AccountDialogCodeBinding.inflate(layoutInflater) }

    private val adapter by lazy {
        ServerAddressAdapter(
            this, mutableListOf()
        )
    }

    private val dialog by lazy {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setView(dialogBinding.root)
            .setCancelable(false)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPasscodeEnforced(this)
        enableEdgeToEdge()

        handleDeepLink()

        binding = AccountSetupHomecloud2Binding.inflate(layoutInflater)
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
        binding.actionButton.setOnClickListener {
            loginViewModel.onLoginClick()
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
            is LoginViewModel.LoginEvent.UpdateServers -> updateServers(event.servers)
            is LoginViewModel.LoginEvent.LoginResult -> handleLoginResult(event)
        }
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
    }

    private fun showLoginScreen() {
        binding.loginStateGroup.visibility = View.VISIBLE
        dialog.dismiss()
    }

    private fun showCodeDialog() {
        dialogBinding.codeEditText.doAfterTextChanged {
            dialogBinding.allowButton.isEnabled = it.toString().isNotEmpty()
        }
        dialogBinding.allowButton.setOnClickListener {
            loginViewModel.onCodeEntered(dialogBinding.codeEditText.text.toString())
        }
        dialog.show()
    }

    private fun updateLoginState(state: LoginScreenState) {
        when (state.loginState) {
            LoginViewModel.LoginState.REMOTE_ACCESS -> {
                binding.loginStateGroup.visibility = View.GONE
                binding.actionButton.setText(R.string.homecloud_action_button_next)
                dialogBinding.allowButton.visibility = if (state.isAllowLoading) View.INVISIBLE else View.VISIBLE
                dialogBinding.allowLoading.visibility = if (state.isAllowLoading) View.VISIBLE else View.GONE
                binding.actionButton.isEnabled = state.username.isNotEmpty()
            }

            LoginViewModel.LoginState.LOGIN -> {
                if (state.isLoading) {
                    binding.loadingLayout.visibility = View.VISIBLE
                    binding.actionGroup.visibility = View.GONE
                    binding.loginStateGroup.visibility = View.GONE
                } else {
                    binding.loadingLayout.visibility = View.GONE
                    binding.actionGroup.visibility = View.VISIBLE
                    binding.loginStateGroup.visibility = View.VISIBLE
                    binding.accountUsername.isEnabled = false
                    binding.actionButton.setText(R.string.setup_btn_login)
                    binding.actionButton.isEnabled = state.username.isNotEmpty() && state.password.isNotEmpty() &&
                            (state.selectedServer != null || state.serverUrl.isNotEmpty())
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
        TODO("Not yet implemented")
    }

    override fun onFailedSavingCertificate() {
        TODO("Not yet implemented")
    }

    override fun onCancelCertificate() {
        TODO("Not yet implemented")
    }

    override fun optionLockSelected(type: LockType) {
        manageOptionLockSelected(type)
    }
}