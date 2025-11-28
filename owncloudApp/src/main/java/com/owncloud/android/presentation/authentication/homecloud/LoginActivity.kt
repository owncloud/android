package com.owncloud.android.presentation.authentication.homecloud

import android.accounts.AccountManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.util.Patterns
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.owncloud.android.R
import com.owncloud.android.databinding.AccountDialogCodeBinding
import com.owncloud.android.databinding.AccountSetupHomecloudBinding
import com.owncloud.android.domain.device.model.Device
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

    private var connectFieldTextWatcher: TextWatcher? = null

    private val adapter by lazy {
        DeviceAddressAdapter(
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

        onBackPressedDispatcher.addCallback {
            loginViewModel.onBackPressed()
        }

        binding = AccountSetupHomecloudBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.settingsLink.applyStatusBarInsets(usePaddings = false)
        binding.backButton.applyStatusBarInsets(usePaddings = false)
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
        setupUnableToConnectContent()
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
        binding.backButton.setOnClickListener {
            loginViewModel.onBackPressed()
        }
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
        binding.cantFindDevice.setOnClickListener {
            showMessageInSnackbar(message = "Not implemented yet")
        }
        binding.actionButton.setOnClickListener {
            loginViewModel.onActionClicked()
        }

        binding.hostUrlInput.setAdapter(adapter)
        binding.hostUrlInput.setOnItemClickListener { parent, _, position, _ ->
            val selectedDevice = parent.getItemAtPosition(position) as Device
            loginViewModel.onDeviceSelected(selectedDevice)
        }
        connectFieldTextWatcher = binding.hostUrlInput.doAfterTextChanged { text ->
            loginViewModel.onServerUrlChanged(text.toString())
        }
        binding.serversRefreshButton.setOnClickListener {
            loginViewModel.refreshServers()
        }

        binding.unableToConnectLayout.unableToConnectBackButton.setOnClickListener {
            loginViewModel.onBackPressed()
        }
        binding.unableToConnectLayout.unableToConnectRetryButton.setOnClickListener {
            loginViewModel.onRetryClicked()
        }
    }

    //TODO: The styling of description and text is a subject to change in nearest future. To be defined....
    private fun setupUnableToConnectContent() {
        val linkColor = ContextCompat.getColor(this, R.color.homecloud_color_accent)
        val description = getString(R.string.homecloud_unable_to_connect_description)
        val items = listOf(
            getString(R.string.homecloud_unable_to_connect_item_1),
            getString(R.string.homecloud_unable_to_connect_item_2),
            getString(R.string.homecloud_unable_to_connect_item_3),
            getString(R.string.homecloud_unable_to_connect_item_4),
            getString(R.string.homecloud_unable_to_connect_item_5),
            getString(R.string.homecloud_unable_to_connect_item_6)
        )
        val supportPrefix = getString(R.string.homecloud_unable_to_connect_support)
        val supportLink = getString(R.string.homecloud_unable_to_connect_support_link)
        val supportSuffix = getString(R.string.homecloud_unable_to_connect_support_suffix)

        val builder = SpannableStringBuilder()
        builder.append(description)

        val numberIndent = resources.getDimensionPixelSize(R.dimen.standard_padding)
        val textView = binding.unableToConnectLayout.unableToConnectContent
        val paint = textView.paint

        items.forEachIndexed { index, item ->
            builder.append("\n\n")
            val itemStart = builder.length
            val numberText = "${index + 1}. "
            builder.append(numberText)
            builder.append(item)
            val itemEnd = builder.length

            val numberTextWidth = paint.measureText(numberText).toInt()
            val textIndent = numberIndent + numberTextWidth

            builder.setSpan(
                LeadingMarginSpan.Standard(numberIndent, textIndent),
                itemStart,
                itemEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Add support text with clickable link
        builder.append("\n\n")
        builder.append(supportPrefix)
        builder.append(" ")

        val linkStart = builder.length
        builder.append(supportLink)
        builder.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_LINK))
                startActivity(intent)
            }
        }, linkStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(ForegroundColorSpan(linkColor), linkStart, builder.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)

        builder.append(" ")
        builder.append(supportSuffix)

        binding.unableToConnectLayout.unableToConnectContent.text = builder
        binding.unableToConnectLayout.unableToConnectContent.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun handleEvents(event: LoginViewModel.LoginEvent) {
        when (event) {
            LoginViewModel.LoginEvent.ShowCodeDialog -> showCodeDialog()
            LoginViewModel.LoginEvent.DismissCodeDialog -> hideCodeDialog()
            is LoginViewModel.LoginEvent.LoginResult -> handleLoginResult(event)
            is LoginViewModel.LoginEvent.ShowUntrustedCertDialog -> showUntrustedCertDialog(event.certificateCombinedException)
            LoginViewModel.LoginEvent.Close -> finish()
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

    private fun updateDevices(devices: List<Device>) {
        adapter.setDevices(devices)
        binding.hostUrlInputLayout.startIconDrawable = if (devices.isEmpty()) null else ContextCompat.getDrawable(this, R.drawable.ic_device)
    }

    private fun hideCodeDialog() {
        dialog.dismiss()
    }

    private fun showCodeDialog() {
        dialogBinding.codeEditVerification.clearCode()
        dialogBinding.codeEditVerification.onCodeChangedListener = { code ->
            dialogBinding.allowButton.isEnabled = code.length == dialogBinding.codeEditVerification.getFilledCodeLength()
        }
        dialogBinding.allowButton.setOnClickListener {
            loginViewModel.onCodeEntered(dialogBinding.codeEditVerification.getCode())
        }
        dialogBinding.resendButton.setOnClickListener {
            loginViewModel.onActionClicked()
        }
        dialogBinding.skipButton.setOnClickListener {
            loginViewModel.onSkipClicked()
        }
        dialog.setOnDismissListener {
            loginViewModel.onCodeDialogDismissed()
        }
        dialogBinding.allowButton.isEnabled = dialogBinding.codeEditVerification.getCode().isNotEmpty()
        dialog.show()
    }

    private fun updateLoginState(state: LoginScreenState) {
        binding.errorMessage.text = state.errorMessage
        binding.errorMessage.isVisible = !state.errorMessage.isNullOrBlank()

        when (state) {
            is LoginScreenState.EmailState -> {
                // Show main scroll view, hide unable to connect
                binding.scrollView.visibility = View.VISIBLE
                binding.unableToConnectLayout.unableToConnectContainer.visibility = View.GONE

                // Show email input, hide email text
                binding.accountUsernameContainer.visibility = View.VISIBLE
                binding.accountUsernameText.visibility = View.GONE
                binding.accountUsername.updateTextIfDiffers(state.username)

                binding.backButton.visibility = View.GONE
                binding.accountUsernameContainer.error = state.errorEmailInvalidMessage
                binding.loginStateGroup.visibility = View.GONE
                binding.actionButton.setText(R.string.homecloud_action_button_next)
                // Enable button only if username is not empty and is a valid email
                binding.actionButton.isEnabled = state.username.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(state.username).matches()
                binding.serversRefreshButton.visibility = View.INVISIBLE
                binding.serversRefreshLoading.visibility = View.GONE
                when {
                    state.isAllowLoading -> {
                        dialogBinding.allowButton.visibility = View.INVISIBLE
                        dialogBinding.resendButton.visibility = View.INVISIBLE
                        dialogBinding.allowLoading.visibility = View.VISIBLE
                    }

                    state.errorCodeMessage == null -> {
                        dialogBinding.allowButton.visibility = View.VISIBLE
                        dialogBinding.resendButton.visibility = View.INVISIBLE
                        dialogBinding.allowLoading.visibility = View.INVISIBLE
                        dialogBinding.codeEditVerification.clearError()
                    }

                    else -> {
                        dialogBinding.allowButton.visibility = View.INVISIBLE
                        dialogBinding.resendButton.visibility = View.VISIBLE
                        dialogBinding.allowLoading.visibility = View.INVISIBLE
                        dialogBinding.codeEditVerification.setError(state.errorCodeMessage)
                    }
                }

                binding.accountUsername.isEnabled = true
            }

            is LoginScreenState.LoginState -> {
                if (state.isUnableToConnect) {
                    // Hide main scroll view, show unable to connect
                    binding.scrollView.visibility = View.GONE
                    binding.backButton.visibility = View.GONE
                    binding.unableToConnectLayout.unableToConnectContainer.visibility = View.VISIBLE
                } else {
                    // Show main scroll view, hide unable to connect
                    binding.scrollView.visibility = View.VISIBLE
                    binding.unableToConnectLayout.unableToConnectContainer.visibility = View.GONE

                    // Hide email input, show email text
                    binding.accountUsernameContainer.visibility = View.GONE
                    binding.accountUsernameText.visibility = View.VISIBLE
                    binding.accountUsernameText.text = state.username

                    binding.backButton.visibility = View.VISIBLE
                    updateDevices(state.devices)
                    binding.accountPassword.updateTextIfDiffers(state.password)
                    binding.hostUrlInput.removeTextChangedListener(connectFieldTextWatcher)
                    if (state.selectedDevice == null) {
                        binding.hostUrlInput.updateTextIfDiffers(state.serverUrl)
                    } else {
                        binding.hostUrlInput.updateTextIfDiffers(state.selectedDevice.name)
                    }
                    connectFieldTextWatcher?.let { binding.hostUrlInput.addTextChangedListener(it) }
                    binding.serversRefreshButton.visibility = if (state.isRefreshServersLoading) View.INVISIBLE else View.VISIBLE
                    binding.serversRefreshLoading.visibility = if (state.isRefreshServersLoading) View.VISIBLE else View.GONE
                    if (state.isLoading) {
                        binding.backButton.visibility = View.GONE
                        binding.loadingLayout.visibility = View.VISIBLE
                        binding.actionGroup.visibility = View.GONE
                        binding.loginStateGroup.visibility = View.GONE
                        binding.serversRefreshButton.visibility = View.INVISIBLE
                    } else {
                        binding.backButton.visibility = View.VISIBLE
                        binding.loadingLayout.visibility = View.GONE
                        binding.actionGroup.visibility = View.VISIBLE
                        binding.loginStateGroup.visibility = View.VISIBLE
                        binding.actionButton.setText(R.string.setup_btn_login)
                        binding.actionButton.isEnabled = state.username.isNotEmpty() && state.password.isNotEmpty() &&
                                (state.selectedDevice != null || state.serverUrl.isNotEmpty()) && Patterns.EMAIL_ADDRESS.matcher(state.username).matches()
                    }
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

    companion object {
        private const val SUPPORT_LINK = "https://www.seagate.com/es/es/support/"
    }
}