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
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import com.owncloud.android.R
import com.owncloud.android.databinding.CreateShortcutDialogBinding
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.presentation.files.filelist.MainFileListFragment.Companion.MAX_FILENAME_LENGTH
import com.owncloud.android.presentation.files.filelist.MainFileListFragment.Companion.forbiddenChars
import com.owncloud.android.ui.activity.FileDisplayActivity

class CreateShortcutDialogFragment : DialogFragment() {
    private lateinit var parentFolder: OCFile
    private lateinit var createShortcutListener: CreateShortcutListener
    private var _binding: CreateShortcutDialogBinding? = null
    private val binding get() = _binding!!
    private var isCreateShortcutButtonEnabled = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CreateShortcutDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            handleInputsUrlAndFileName()
            cancelButton.setOnClickListener {
                dialog?.dismiss()
            }
        }
    }

    private fun handleInputsUrlAndFileName() {
        var isValidUrl = false
        var isValidFileName = false
        var hasForbiddenCharacters: Boolean
        var hasMaxCharacters: Boolean
        var hasEmptyValue: Boolean
        binding.createShortcutDialogNameFileValue.doOnTextChanged { fileNameValue, _, _, _ ->
            fileNameValue?.let {
                hasForbiddenCharacters = forbiddenChars.any { fileNameValue.contains(it) }
                hasMaxCharacters = fileNameValue.length > MAX_FILENAME_LENGTH
                isValidFileName = fileNameValue.isNotBlank() && !hasForbiddenCharacters && !hasMaxCharacters
                handleNameRequirements(hasForbiddenCharacters, hasMaxCharacters)
                updateCreateShortcutButtonState(isValidFileName, isValidUrl)
            }
        }
        binding.createShortcutDialogUrlValue.doOnTextChanged { urlValue, _, _, _ ->
            urlValue?.let {
                hasEmptyValue = urlValue.contains(" ")
                isValidUrl = urlValue.isNotBlank() && !hasEmptyValue
                handleUrlRequirements(hasEmptyValue)
                updateCreateShortcutButtonState(isValidFileName, isValidUrl)
            }
        }
    }

    private fun updateCreateShortcutButtonState(isValidFileName: Boolean, isValidUrl: Boolean) {
        isCreateShortcutButtonEnabled = isValidFileName && isValidUrl
        enableCreateButton(isCreateShortcutButtonEnabled)
    }

    private fun handleNameRequirements(hasForbiddenCharacters: Boolean, hasMaxCharacters: Boolean) {
        binding.createShortcutDialogNameFileLayout.apply {
            error = when {
                hasMaxCharacters -> getString(R.string.uploader_upload_text_dialog_filename_error_length_max, MAX_FILENAME_LENGTH)
                hasForbiddenCharacters -> getString(R.string.filename_forbidden_characters)
                else -> null
            }
        }
    }

    private fun handleUrlRequirements(hasSpace: Boolean) {
        binding.createShortcutDialogUrlLayout.apply {
            if (hasSpace) {
                error = getString(R.string.create_shortcut_dialog_url_error_no_blanks)
            } else {
                error = null
            }
        }
    }

    private fun enableCreateButton(enable: Boolean) {
        binding.createButton.apply {
            isEnabled = enable
            if (enable) {
                setOnClickListener {
                    createShortcutListener.createShortcutFileFromApp(
                        fileName = binding.createShortcutDialogNameFileValue.text.toString(),
                        url = formatUrl(binding.createShortcutDialogUrlValue.text.toString()),
                    )
                    dialog?.dismiss()
                }
                setTextColor(resources.getColor(R.color.primary_button_background_color, null))
            } else {
                setOnClickListener(null)
                setTextColor(resources.getColor(R.color.grey, null))
            }
        }
    }

    private fun formatUrl(url: String): String {
        var formattedUrl = url
        if (!url.startsWith(FileDisplayActivity.PROTOCOL_HTTP) && !url.startsWith(FileDisplayActivity.PROTOCOL_HTTPS)) {
            formattedUrl = FileDisplayActivity.PROTOCOL_HTTPS + url
        }
        return formattedUrl
    }

    interface CreateShortcutListener {
        fun createShortcutFileFromApp(fileName: String, url: String)
    }

    companion object {

        fun newInstance(parentFolder: OCFile, listener: CreateShortcutListener): CreateShortcutDialogFragment {
            return CreateShortcutDialogFragment().apply {
                createShortcutListener = listener
                this.parentFolder = parentFolder
            }
        }
    }
}
