/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
 * Copyright (C) 2021 ownCloud GmbH.
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.presentation.files.renamefile

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputLayout
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.extensions.avoidScreenshotsIfNeeded
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.presentation.files.operations.FileOperation
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel
import com.owncloud.android.utils.PreferenceUtils
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

/**
 * Dialog to input a new name for a file or folder to rename.
 *
 * Triggers the rename operation when name is confirmed.
 */

class RenameFileDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    private var targetFile: OCFile? = null
    private val filesViewModel: FileOperationsViewModel by sharedViewModel()
    private var isButtonEnabled = true
    private val MAX_FILENAME_LENGTH = 223
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState != null) {
            isButtonEnabled = savedInstanceState.getBoolean(IS_BUTTON_ENABLED_FLAG_KEY)
        }

        targetFile = requireArguments().getParcelable(ARG_TARGET_FILE)

        // Inflate the layout for the dialog
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.edit_box_dialog, null)

        // Allow or disallow touches with other visible windows
        view.filterTouchesWhenObscured =
            PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)

        // Setup layout
        val currentName = targetFile!!.fileName
        var error: String? = null
        val inputLayout: TextInputLayout = view.findViewById(R.id.edit_box_input_text_layout)
        val inputText = view.findViewById<EditText>(R.id.user_input)
        inputText.setText(currentName)
        val selectionStart = 0
        val extensionStart = if (targetFile!!.isFolder) -1 else currentName.lastIndexOf(".")
        val selectionEnd = if (extensionStart >= 0) extensionStart else currentName.length
        if (selectionStart >= 0 && selectionEnd >= 0) {
            inputText.setSelection(
                selectionStart.coerceAtMost(selectionEnd),
                selectionStart.coerceAtLeast(selectionEnd)
            )
        }

        inputText.requestFocus()

        // Build the dialog
        return AlertDialog.Builder(requireActivity()).apply {
            setView(view)
            setPositiveButton(android.R.string.ok, this@RenameFileDialogFragment)
            setNegativeButton(android.R.string.cancel, this@RenameFileDialogFragment)
            setTitle(R.string.rename_dialog_title)
        }.create().apply {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            avoidScreenshotsIfNeeded()

            setOnShowListener {
                val okButton = getButton(AlertDialog.BUTTON_POSITIVE)
                okButton.isEnabled = isButtonEnabled

            }

            inputText.doOnTextChanged { text, _, _, _ ->
                val okButton = getButton(AlertDialog.BUTTON_POSITIVE)
                if (text.isNullOrBlank()) {
                    okButton.isEnabled = false
                    error = getString(R.string.uploader_upload_text_dialog_filename_error_empty)
                } else if (text.length > MAX_FILENAME_LENGTH) {
                    error = String.format(
                        getString(R.string.uploader_upload_text_dialog_filename_error_length_max),
                        MAX_FILENAME_LENGTH
                    )
                } else if (forbiddenChars.any { text.contains(it) }) {
                    error = getString(R.string.filename_forbidden_characters)
                } else {
                    okButton.isEnabled = true
                    error = null
                    inputLayout.error = error
                }

                if (error != null) {
                    okButton.isEnabled = false
                    inputLayout.error = error
                }

            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_BUTTON_ENABLED_FLAG_KEY, isButtonEnabled)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            // These checks are done in the RenameFileUseCase too, we could remove them too.
            val newFileName = (getDialog()!!.findViewById<View>(R.id.user_input) as TextView).text.toString()
            filesViewModel.performOperation(FileOperation.RenameOperation(targetFile!!, newFileName))
        }
    }

    /**
     * Show a temporary message in a Snackbar bound to the content view of the parent Activity
     *
     * @param messageResource Message to show.
     */
    private fun showSnackMessage(messageResource: Int) {
        showMessageInSnackbar(
            message = getString(messageResource)
        )
    }

    companion object {
        const val FRAGMENT_TAG_RENAME_FILE = "RENAME_FILE_FRAGMENT"
        private const val ARG_TARGET_FILE = "TARGET_FILE"
        private const val IS_BUTTON_ENABLED_FLAG_KEY = "IS_BUTTON_ENABLED_FLAG_KEY"
        private val forbiddenChars = listOf('/', '\\')

        /**
         * Public factory method to create new RenameFileDialogFragment instances.
         *
         * @param file            File to rename.
         * @return Dialog ready to show.
         */
        @JvmStatic
        fun newInstance(file: OCFile): RenameFileDialogFragment {
            val args = Bundle().apply {
                putParcelable(ARG_TARGET_FILE, file)
            }
            return RenameFileDialogFragment().apply { arguments = args }
        }
    }
}
