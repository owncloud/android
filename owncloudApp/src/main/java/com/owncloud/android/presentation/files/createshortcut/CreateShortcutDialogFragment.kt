/**
 * ownCloud Android client application
 *
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.presentation.files.createshortcut

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import com.owncloud.android.R
import com.owncloud.android.databinding.CreateShortcutDialogBinding
import com.owncloud.android.domain.files.model.OCFile

class CreateShortcutDialogFragment : DialogFragment() {
    private lateinit var parentFolder: OCFile
    private lateinit var createShortcutListener: CreateShortcutListener
    private var _binding: CreateShortcutDialogBinding? = null
    private var isCreateShortcutButtonEnable = false
    private var isValidUrl = false
    private var isValidFileName = false
    private var hasForbiddenCharacters = false
    private var hasMaxCharacters = false
    private var hasSpace = false
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CreateShortcutDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            handleInputsUrlAndFileName()
            yesButton.setOnClickListener {
                if (isCreateShortcutButtonEnable) {
                    createShortcutListener.createShortcutFileFromApp(
                        fileName = createShortcutDialogNameFileValue.text.toString(),
                        url = formatUrl(createShortcutDialogUrlValue.text.toString()),
                    )
                    dialog?.dismiss()
                }
            }
            cancelButton.setOnClickListener {
                dialog?.dismiss()
            }
        }
    }

    private fun formatUrl(url: String): String {
        var formattedUrl = url
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            formattedUrl = "http://$url"
        }
        return formattedUrl
    }

    private fun handleInputsUrlAndFileName() {
        binding.createShortcutDialogNameFileValue.doOnTextChanged { fileNameValue, _, _, _ ->
            fileNameValue?.let {
                hasForbiddenCharacters = FORBIDDEN_CHARACTERS.any { fileNameValue.contains(it) }
                hasMaxCharacters = fileNameValue.length >= MAX_FILE_NAME
                isValidFileName = fileNameValue.isNotBlank() && !hasForbiddenCharacters && !hasMaxCharacters
                handleNameRequirements(hasForbiddenCharacters, hasMaxCharacters)
                updateCreateShortcutButtonState()
            }
        }
        binding.createShortcutDialogUrlValue.doOnTextChanged { urlValue, _, _, _ ->
            urlValue?.let {
                hasSpace = urlValue.contains(" ")
                isValidUrl = urlValue.isNotBlank() && !hasSpace
                handleUrlRequirements(hasSpace)
                updateCreateShortcutButtonState()
            }
        }
    }

    private fun updateCreateShortcutButtonState() {
        isCreateShortcutButtonEnable = isValidFileName && isValidUrl
        enableYesButton(isCreateShortcutButtonEnable)
    }

    private fun handleNameRequirements(hasForbiddenCharacters: Boolean, hasMaxCharacters: Boolean) {
        binding.createShortcutDialogNameFileLayout.apply {
            error = when {
                hasMaxCharacters -> getString(R.string.uploader_upload_text_dialog_filename_error_length_max, MAX_FILE_NAME)
                hasForbiddenCharacters -> getString(R.string.filename_forbidden_characters)
                else -> null
            }
        }
    }

    private fun handleUrlRequirements(hasSpace: Boolean) {
        binding.createShortcutDialogUrlLayout.apply {
            if (hasSpace) {
                error = getString(R.string.create_shortcut_dialog_url_error_no_spaces)
            } else {
                error = null
            }
        }
    }

    private fun enableYesButton(enable: Boolean) {
        binding.yesButton.apply {
            isEnabled = enable
            setTextColor(
                if (enable) resources.getColor(R.color.primary_button_background_color, null)
                else resources.getColor(R.color.grey, null)
            )
        }
    }

    interface CreateShortcutListener {
        fun createShortcutFileFromApp(fileName: String, url: String)
    }

    companion object {

        private const val FORBIDDEN_CHARACTERS = "/\\"
        private const val MAX_FILE_NAME = 256

        /**
         * Public factory method to create new CreateShortcutDialogFragment instances.
         *
         * @param parentFile File to create
         * @return Dialog ready to show.
         */
        @JvmStatic
        fun newInstance(parent: OCFile, listener: CreateShortcutListener): CreateShortcutDialogFragment {
            return CreateShortcutDialogFragment().apply {
                createShortcutListener = listener
                parentFolder = parent
            }
        }
    }
}
