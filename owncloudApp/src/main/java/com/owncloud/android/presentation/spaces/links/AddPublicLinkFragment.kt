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

import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.owncloud.android.R
import com.owncloud.android.databinding.AddPublicLinkFragmentBinding
import com.owncloud.android.domain.links.model.OCLink
import com.owncloud.android.domain.links.model.OCLinkType
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.hideSoftKeyboard
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.utils.DisplayUtils
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AddPublicLinkFragment: Fragment(), SetPasswordDialogFragment.SetPasswordListener {
    private var _binding: AddPublicLinkFragmentBinding? = null
    private val binding get() = _binding!!

    private val accountName get() = requireArguments().getString(ARG_ACCOUNT_NAME) ?: ""

    private val spaceLinksViewModel: SpaceLinksViewModel by activityViewModel {
        parametersOf(
            accountName,
            requireArguments().getParcelable(ARG_CURRENT_SPACE)
        )
    }

    private var isPasswordEnforced = true
    private var hasPassword = false
    private var editMode = false
    private var selectedPublicLink: OCLink? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = AddPublicLinkFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editMode = requireArguments().getBoolean(ARG_EDIT_MODE)
        selectedPublicLink = requireArguments().getParcelable(ARG_SELECTED_PUBLIC_LINK)
        requireActivity().setTitle(if (editMode) R.string.public_link_edit_title else R.string.public_link_create_title)

        binding.publicLinkPermissions.apply {
            canViewPublicLinkRadioButton.tag = OCLinkType.CAN_VIEW
            canEditPublicLinkRadioButton.tag = OCLinkType.CAN_EDIT
            secretFileDropPublicLinkRadioButton.tag = OCLinkType.CREATE_ONLY
        }

        collectLatestLifecycleFlow(spaceLinksViewModel.addPublicLinkUIState) { uiState ->
            uiState?.let {
                it.selectedExpirationDate?.let { expirationDate ->
                    binding.expirationDateLayout.expirationDateValue.apply {
                        visibility = View.VISIBLE
                        text = DisplayUtils.displayDateToHumanReadable(expirationDate)
                    }
                }

                hasPassword = it.hasPassword
                it.selectedPermission?.let {
                    binding.optionsLayout.isVisible = true
                    binding.passwordLayout.apply {
                        passwordValue.isVisible = hasPassword
                        setPasswordButton.isVisible = !hasPassword && isPasswordEnforced
                        removePasswordButton.isVisible = hasPassword && isPasswordEnforced
                        setPasswordSwitch.isVisible = !isPasswordEnforced
                        setPasswordSwitch.isChecked = hasPassword
                    }
                    binding.createPublicLinkButton.isEnabled = (isPasswordEnforced && hasPassword) || !isPasswordEnforced
                }

                bindDatePickerDialog(uiState.selectedExpirationDate)

                binding.expirationDateLayout.apply {
                    expirationDateLayout.setOnClickListener {
                        if (uiState.selectedExpirationDate != null) {
                            openDatePickerDialog(uiState.selectedExpirationDate)
                        } else {
                            expirationDateSwitch.isChecked = true
                            openDatePickerDialog(null)
                        }
                    }
                }

                binding.passwordLayout.apply {
                    passwordLayout.setOnClickListener {
                        if (!isPasswordEnforced){
                            setPasswordSwitch.isChecked = true
                        }
                        showPasswordDialog(uiState.selectedPassword)
                    }
                }
            }
        }

        collectLatestLifecycleFlow(spaceLinksViewModel.addLinkResultFlow) { event ->
            event?.peekContent()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> { }
                    is UIResult.Success -> parentFragmentManager.popBackStack()
                    is UIResult.Error -> showErrorInSnackbar(R.string.public_link_add_failed, uiResult.error)
                }
            }
        }

        collectLatestLifecycleFlow(spaceLinksViewModel.editLinkResultFlow) { event ->
            event?.peekContent()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> { }
                    is UIResult.Success -> parentFragmentManager.popBackStack()
                    is UIResult.Error -> showErrorInSnackbar(R.string.public_link_edit_failed, uiResult.error)
                }
            }
        }

        if (editMode) { bindEditMode() }

        binding.publicLinkPermissions.apply {
            canViewPublicLinkRadioButton.setOnClickListener { selectRadioButton(canViewPublicLinkRadioButton) }
            canViewPublicLinkLayout.setOnClickListener { selectRadioButton(canViewPublicLinkRadioButton) }
            canEditPublicLinkRadioButton.setOnClickListener { selectRadioButton(canEditPublicLinkRadioButton) }
            canEditPublicLinkLayout.setOnClickListener { selectRadioButton(canEditPublicLinkRadioButton) }
            secretFileDropPublicLinkRadioButton.setOnClickListener { selectRadioButton(secretFileDropPublicLinkRadioButton) }
            secretFileDropPublicLinkLayout.setOnClickListener { selectRadioButton(secretFileDropPublicLinkRadioButton) }
        }

        binding.passwordLayout.apply {
            setPasswordButton.setOnClickListener {
                showPasswordDialog()
            }
            removePasswordButton.setOnClickListener {
                removePassword()
            }
            setPasswordSwitch.setOnClickListener {
                if (setPasswordSwitch.isChecked) showPasswordDialog() else removePassword()
            }
        }

        binding.createPublicLinkButton.setOnClickListener {
            val displayName = binding.publicLinkNameEditText.text.toString().ifEmpty { getString(R.string.public_link_default_display_name) }
            if (editMode) {
                selectedPublicLink?.let { spaceLinksViewModel.editPublicLink(it.id, displayName) }
            } else {
                spaceLinksViewModel.createPublicLink(displayName)
            }
        }
    }

    override fun onCancelPassword() {
        if (!isPasswordEnforced && !hasPassword) {
            binding.passwordLayout.setPasswordSwitch.isChecked = false
        }
    }

    override fun onSetPassword(password: String) {
        val normalizedPassword = password.ifBlank { null }
        val hasPassword = normalizedPassword != null
        if (!isPasswordEnforced && !hasPassword) {
            binding.passwordLayout.setPasswordSwitch.isChecked = false
        }
        spaceLinksViewModel.onPasswordSelected(normalizedPassword, hasPassword)
    }

    private fun selectRadioButton(selectedRadioButton: RadioButton) {
        hideKeyboardAndClearFocus()
        binding.publicLinkPermissions.apply {
            canViewPublicLinkRadioButton.isChecked = false
            canEditPublicLinkRadioButton.isChecked = false
            secretFileDropPublicLinkRadioButton.isChecked = false
            selectedRadioButton.isChecked = true
        }
        val selectedPermission = selectedRadioButton.tag as OCLinkType
        isPasswordEnforced = spaceLinksViewModel.checkPasswordEnforced(selectedPermission)
        spaceLinksViewModel.onPermissionSelected(selectedPermission)
    }

    private fun bindDatePickerDialog(expirationDate: String?) {
        binding.expirationDateLayout.expirationDateSwitch.setOnClickListener {
            hideKeyboardAndClearFocus()
            if (binding.expirationDateLayout.expirationDateSwitch.isChecked) {
                openDatePickerDialog(expirationDate)
            } else {
                binding.expirationDateLayout.expirationDateValue.visibility = View.GONE
                spaceLinksViewModel.onExpirationDateSelected(null)
            }
        }
    }

    private fun openDatePickerDialog(expirationDate: String?) {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat(DisplayUtils.DATE_FORMAT_ISO, Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        expirationDate?.let {
            calendar.time = formatter.parse(it)
        }

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val isoExpirationDate = formatter.format(calendar.time)
                spaceLinksViewModel.onExpirationDateSelected(isoExpirationDate)
                binding.expirationDateLayout.expirationDateValue.apply {
                    visibility = View.VISIBLE
                    text = DisplayUtils.displayDateToHumanReadable(isoExpirationDate)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = Calendar.getInstance().timeInMillis
            show()
            setOnCancelListener {
                if (expirationDate == null) {
                    binding.expirationDateLayout.expirationDateSwitch.isChecked = false
                }
            }
        }
    }

    private fun showPasswordDialog(password: String? = null) {
        binding.publicLinkNameEditText.clearFocus()
        val dialog = SetPasswordDialogFragment.newInstance(accountName, password, this)
        dialog.show(parentFragmentManager, DIALOG_SET_PASSWORD)
    }

    private fun removePassword() {
        hideKeyboardAndClearFocus()
        spaceLinksViewModel.onPasswordSelected(null, false)
    }

    private fun hideKeyboardAndClearFocus() {
        hideSoftKeyboard()
        binding.publicLinkNameEditText.clearFocus()
    }

    private fun bindEditMode() {
        selectedPublicLink?.let {
            binding.publicLinkNameEditText.setText(it.displayName)
            binding.createPublicLinkButton.apply {
                setText(R.string.share_confirm_public_link_button)
                contentDescription = getString(R.string.share_confirm_public_link_button)
            }

            // Do not recreate the edit view after the first iteration
            if (spaceLinksViewModel.addPublicLinkUIState.value?.selectedPermission != null) return

            when (it.type) {
                OCLinkType.CAN_VIEW -> selectRadioButton(binding.publicLinkPermissions.canViewPublicLinkRadioButton)
                OCLinkType.CAN_EDIT -> selectRadioButton(binding.publicLinkPermissions.canEditPublicLinkRadioButton)
                OCLinkType.CREATE_ONLY -> selectRadioButton(binding.publicLinkPermissions.secretFileDropPublicLinkRadioButton)
                else -> {}
            }

            if (it.hasPassword) {
                spaceLinksViewModel.onPasswordSelected(password = null, hasPassword = true, wasPasswordChanged = false)
            }

            it.expirationDateTime?.let { expirationDate ->
                spaceLinksViewModel.onExpirationDateSelected(expirationDate)
                binding.expirationDateLayout.expirationDateSwitch.isChecked = true
            }
        }
    }

    companion object {
        private const val DIALOG_SET_PASSWORD = "DIALOG_SET_PASSWORD"
        private const val ARG_ACCOUNT_NAME = "ARG_ACCOUNT_NAME"
        private const val ARG_CURRENT_SPACE = "ARG_CURRENT_SPACE"
        private const val ARG_EDIT_MODE = "ARG_EDIT_MODE"
        private const val ARG_SELECTED_PUBLIC_LINK = "ARG_SELECTED_PUBLIC_LINK"

        fun newInstance(
            accountName: String,
            currentSpace: OCSpace,
            editMode: Boolean,
            selectedPublicLink: OCLink?
        ): AddPublicLinkFragment {
            val args = Bundle().apply {
                putString(ARG_ACCOUNT_NAME, accountName)
                putParcelable(ARG_CURRENT_SPACE, currentSpace)
                putBoolean(ARG_EDIT_MODE, editMode)
                putParcelable(ARG_SELECTED_PUBLIC_LINK, selectedPublicLink)
            }
            return AddPublicLinkFragment().apply {
                arguments = args
            }
        }
    }
}
