/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.presentation.ui.files.removefile

import android.app.Dialog
import android.os.Bundle
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.presentation.ui.files.operations.FileOperation
import com.owncloud.android.presentation.ui.files.operations.FileOperationsViewModel
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

/**
 * Dialog requiring confirmation before removing a collection of given OCFiles.
 *
 * Triggers the removal according to the user response.
 */
class RemoveFilesDialogFragment : ConfirmationDialogFragment(), ConfirmationDialogFragmentListener {

    private lateinit var targetFiles: ArrayList<OCFile>
    private val fileOperationViewModel: FileOperationsViewModel by sharedViewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        targetFiles = requireArguments().getParcelableArrayList(ARG_TARGET_FILES) ?: arrayListOf()
        setOnConfirmationListener(this)
        return dialog
    }

    /**
     * Performs the removal of the target file, both locally and in the server.
     */
    override fun onConfirmation(callerTag: String) {
        fileOperationViewModel.performOperation(FileOperation.RemoveOperation(targetFiles.toList(), removeOnlyLocalCopy = false))
        dismiss()
    }

    /**
     * Performs the removal of the local copy of the target file
     */
    override fun onCancel(callerTag: String) {
        fileOperationViewModel.performOperation(FileOperation.RemoveOperation(targetFiles.toList(), removeOnlyLocalCopy = true))
        dismiss()
    }

    override fun onNeutral(callerTag: String) {
        // nothing to do here
    }

    companion object {
        private const val ARG_TARGET_FILES = "TARGET_FILES"

        /**
         * Public factory method to create new RemoveFilesDialogFragment instances.
         *
         * @param files           Files to remove.
         * @return Dialog ready to show.
         */
        @JvmStatic
        fun newInstance(files: ArrayList<OCFile>): RemoveFilesDialogFragment {
            val messageStringId: Int
            var containsFolder = false
            var containsDown = false
            var containsAvailableOffline = false
            for (file in files) {
                if (file.isFolder) {
                    containsFolder = true
                }
                if (file.isAvailableLocally) {
                    containsDown = true
                }
                if (file.isAvailableOffline) {
                    containsAvailableOffline = true;
                }
            }
            messageStringId = if (files.size == 1) {
                // choose message for a single file
                val file = files.first()
                if (file.isFolder) {
                    R.string.confirmation_remove_folder_alert
                } else {
                    R.string.confirmation_remove_file_alert
                }
            } else {
                // choose message for more than one file
                if (containsFolder) {
                    R.string.confirmation_remove_folders_alert
                } else {
                    R.string.confirmation_remove_files_alert
                }
            }
            val localRemoveButton = if (!containsAvailableOffline && (containsFolder || containsDown)) {
                R.string.confirmation_remove_local
            } else {
                -1
            }

            val args = Bundle().apply {
                putInt(ARG_MESSAGE_RESOURCE_ID, messageStringId)
                if (files.size == 1) {
                    putStringArray(ARG_MESSAGE_ARGUMENTS, arrayOf(files.first().fileName))
                }
                putInt(ARG_POSITIVE_BTN_RES, R.string.common_yes)
                putInt(ARG_NEUTRAL_BTN_RES, R.string.common_no)
                putInt(ARG_NEGATIVE_BTN_RES, localRemoveButton)
                putParcelableArrayList(ARG_TARGET_FILES, files)
            }

            return RemoveFilesDialogFragment().apply {
                arguments = args
            }
        }

        /**
         * Convenience factory method to create new RemoveFilesDialogFragment instances for a single file
         *
         * @param file           File to remove.
         * @return Dialog ready to show.
         */
        @JvmStatic
        @JvmName("newInstanceForSingleFile")
        fun newInstance(file: OCFile): RemoveFilesDialogFragment {
            return newInstance(arrayListOf(file))
        }
    }
}
