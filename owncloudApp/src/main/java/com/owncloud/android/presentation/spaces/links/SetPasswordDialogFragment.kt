/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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

package com.owncloud.android.presentation.spaces.links

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import com.owncloud.android.R
import com.owncloud.android.databinding.SetPasswordDialogBinding
import com.owncloud.android.domain.capabilities.model.PasswordPolicy
import com.owncloud.android.presentation.capabilities.CapabilityViewModel
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.sharing.generatePassword
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class SetPasswordDialogFragment: DialogFragment() {
    private var _binding: SetPasswordDialogBinding? = null
    private val binding get() = _binding!!

    private val capabilityViewModel: CapabilityViewModel by viewModel {
        parametersOf(
            requireArguments().getString(ARG_ACCOUNT_NAME)
        )
    }

    private lateinit var setPasswordListener: SetPasswordListener

    private var passwordPolicy: PasswordPolicy? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = SetPasswordDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireArguments().getString(ARG_PASSWORD)?.let {
            binding.passwordValue.setText(it)
        }

        capabilityViewModel.capabilities.observe(viewLifecycleOwner) { event ->
            when (val uiResult = event.peekContent()) {
                is UIResult.Success -> {
                    passwordPolicy = uiResult.data?.passwordPolicy
                    passwordPolicy?.let {
                        updatePasswordPolicyRequirements(binding.passwordValue.text.toString(), it)
                    }
                }
                is UIResult.Loading -> { }
                is UIResult.Error -> {
                    Timber.e(uiResult.error, "Failed to retrieve server capabilities")
                }
            }
        }

        binding.passwordValue.doOnTextChanged { text, _, _, _ ->
            passwordPolicy?.let {
                updatePasswordPolicyRequirements(text.toString(), it)
            }
        }

        binding.generatePasswordButton.setOnClickListener {
            passwordPolicy?.let { passwordPolicy ->
                binding.passwordValue.setText(
                    generatePassword(
                        minChars = passwordPolicy.minCharacters,
                        maxChars = passwordPolicy.maxCharacters,
                        minDigitsChars = passwordPolicy.minDigits,
                        minLowercaseChars = passwordPolicy.minLowercaseCharacters,
                        minUppercaseChars = passwordPolicy.minUppercaseCharacters,
                        minSpecialChars = passwordPolicy.minSpecialCharacters,
                    )
                )
            }
        }

        binding.copyPasswordButton.setOnClickListener {
            val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(getString(R.string.auth_password), binding.passwordValue.text.toString())
            clipboard.setPrimaryClip(clip)
        }

        binding.cancelPasswordButton.setOnClickListener {
            setPasswordListener.onCancelPassword()
            dismiss()
        }

        binding.setPasswordButton.setOnClickListener {
            setPasswordListener.onSetPassword(binding.passwordValue.text.toString())
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        setPasswordListener.onCancelPassword()
    }

    private fun updatePasswordPolicyRequirements(password: String, passwordPolicy: PasswordPolicy) {
        var hasMinCharacters = true
        var hasMaxCharacters = true
        var hasUpperCase = true
        var hasLowerCase = true
        var hasSpecialCharacter = true
        var hasDigit = true

        passwordPolicy.minCharacters?.let { minCharacters ->
            if (minCharacters > 0) {
                hasMinCharacters = password.length >= minCharacters
                updateRequirement(
                    hasRequirement = hasMinCharacters,
                    layout = binding.passwordPolicyMinCharacters.passwordPolicyLayout,
                    textView = binding.passwordPolicyMinCharacters.passwordPolicyText,
                    textViewIcon = binding.passwordPolicyMinCharacters.passwordPolicyIcon,
                    text = getString(R.string.password_policy_min_characters, passwordPolicy.minCharacters)
                )
            }
        }

        passwordPolicy.maxCharacters?.let { maxCharacters ->
            if (maxCharacters > 0) {
                hasMaxCharacters = password.length <= maxCharacters
                updateRequirement(
                    hasRequirement = hasMaxCharacters,
                    layout = binding.passwordPolicyMaxCharacters.passwordPolicyLayout,
                    textView = binding.passwordPolicyMaxCharacters.passwordPolicyText,
                    textViewIcon = binding.passwordPolicyMaxCharacters.passwordPolicyIcon,
                    text = getString(R.string.password_policy_max_characters, passwordPolicy.maxCharacters)
                )
            }
        }

        passwordPolicy.minUppercaseCharacters?.let { minUppercaseCharacters ->
            if (minUppercaseCharacters > 0) {
                hasUpperCase = password.count { it.isUpperCase() } >= minUppercaseCharacters
                updateRequirement(
                    hasRequirement = hasUpperCase,
                    layout = binding.passwordPolicyUpperCharacters.passwordPolicyLayout,
                    textView = binding.passwordPolicyUpperCharacters.passwordPolicyText,
                    textViewIcon = binding.passwordPolicyUpperCharacters.passwordPolicyIcon,
                    text = getString(R.string.password_policy_uppercase_characters, passwordPolicy.minUppercaseCharacters)
                )
            }
        }

        passwordPolicy.minLowercaseCharacters?.let { minLowercaseCharacters ->
            if (minLowercaseCharacters > 0) {
                hasLowerCase = password.count { it.isLowerCase() } >= minLowercaseCharacters
                updateRequirement(
                    hasRequirement = hasLowerCase,
                    layout = binding.passwordPolicyLowerCaseCharacters.passwordPolicyLayout,
                    textView = binding.passwordPolicyLowerCaseCharacters.passwordPolicyText,
                    textViewIcon = binding.passwordPolicyLowerCaseCharacters.passwordPolicyIcon,
                    text = getString(R.string.password_policy_lowercase_characters, passwordPolicy.minLowercaseCharacters)
                )
            }
        }

        passwordPolicy.minSpecialCharacters?.let { minSpecialCharacters ->
            if (minSpecialCharacters > 0) {
                hasSpecialCharacter = password.count { SPECIALS_CHARACTERS.contains(it) } >= minSpecialCharacters
                updateRequirement(
                    hasRequirement = hasSpecialCharacter,
                    layout = binding.passwordPolicyMinSpecialCharacters.passwordPolicyLayout,
                    textView = binding.passwordPolicyMinSpecialCharacters.passwordPolicyText,
                    textViewIcon = binding.passwordPolicyMinSpecialCharacters.passwordPolicyIcon,
                    text = getString(R.string.password_policy_min_special_character, passwordPolicy.minSpecialCharacters, SPECIALS_CHARACTERS)
                )
            }
        }

        passwordPolicy.minDigits?.let { minDigits ->
            if (minDigits > 0) {
                hasDigit = password.count { it.isDigit() } >= minDigits
                updateRequirement(
                    hasRequirement = hasDigit,
                    layout = binding.passwordPolicyMinDigits.passwordPolicyLayout,
                    textView = binding.passwordPolicyMinDigits.passwordPolicyText,
                    textViewIcon = binding.passwordPolicyMinDigits.passwordPolicyIcon,
                    text = getString(R.string.password_policy_min_digits, passwordPolicy.minDigits)
                )
            }
        }

        val allConditionsCheck = hasMinCharacters && hasUpperCase && hasLowerCase && hasDigit && hasSpecialCharacter && hasMaxCharacters
        enableButton(binding.setPasswordButton, allConditionsCheck)
        enableButton(binding.copyPasswordButton, allConditionsCheck)
    }

    private fun updateRequirement(hasRequirement: Boolean, layout: View, textView: TextView, textViewIcon: ImageView, text: String) {
        val textColor = if (hasRequirement) R.color.success else R.color.warning
        val drawable = if (hasRequirement) R.drawable.ic_check_password_policy else R.drawable.ic_cross_warning_password_policy

        layout.isVisible = true
        textView.apply {
            setText(text)
            setTextColor(ContextCompat.getColor(context, textColor))
        }
        textViewIcon.setImageResource(drawable)
    }

    private fun enableButton(button: AppCompatButton, enable: Boolean) {
        val textColor = if (enable) R.color.primary_button_background_color else R.color.grey
        button.apply {
            isEnabled = enable
            setTextColor(ContextCompat.getColor(context, textColor))
        }
    }

    interface SetPasswordListener {
        fun onCancelPassword()
        fun onSetPassword(password: String)
    }

    companion object {
        private const val ARG_ACCOUNT_NAME = "ARG_ACCOUNT_NAME"
        private const val ARG_PASSWORD = "ARG_PASSWORD"
        private const val SPECIALS_CHARACTERS = "!#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"

        fun newInstance(
            accountName: String,
            password: String?,
            listener: SetPasswordListener
        ): SetPasswordDialogFragment {
            val args = Bundle().apply {
                putString(ARG_ACCOUNT_NAME, accountName)
                putString(ARG_PASSWORD, password)
            }
            return SetPasswordDialogFragment().apply {
                setPasswordListener = listener
                arguments = args
            }
        }
    }
}
