/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @authos Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
 * <p>
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
package com.owncloud.android.presentation.ui.files.createfolder

import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.utils.PreferenceUtils

/**
 * Dialog to input the name for a new folder to create.
 *
 *
 * Triggers the folder creation when name is confirmed.
 */
class CreateFolderDialogFragment : DialogFragment() {
    private lateinit var parentFolder: OCFile
    private lateinit var createFolderListener: CreateFolderListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        // Inflate the layout for the dialog
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.edit_box_dialog, null)

        // Allow or disallow touches with other visible windows
        view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
        val coordinatorLayout: CoordinatorLayout = requireActivity().findViewById(R.id.coordinator_layout)

        coordinatorLayout.filterTouchesWhenObscured =
            PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)

        // Request focus
        val inputText: EditText = view.findViewById(R.id.user_input)
        inputText.requestFocus()

        // Build the dialog  
        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(view)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                createFolderListener.onFolderNameSet(
                    newFolderName = inputText.text.toString(),
                    parentFolder = parentFolder
                )
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setTitle(R.string.uploader_info_dirname)
        val alertDialog: Dialog = builder.create()
        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        return alertDialog
    }

    interface CreateFolderListener {
        fun onFolderNameSet(newFolderName: String, parentFolder: OCFile)
    }

    companion object {
        const val CREATE_FOLDER_FRAGMENT = "CREATE_FOLDER_FRAGMENT"

        /**
         * Public factory method to create new CreateFolderDialogFragment instances.
         *
         * @param parentFolder Folder to create
         * @return Dialog ready to show.
         */
        @JvmStatic
        fun newInstance(parent: OCFile, listener: CreateFolderListener): CreateFolderDialogFragment {
            return CreateFolderDialogFragment().apply {
                createFolderListener = listener
                parentFolder = parent
            }
        }
    }
}
