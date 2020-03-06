/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
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

package com.owncloud.android.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.utils.PreferenceUtils;

/**
 * Dialog to input the name for a new folder to create.
 * <p>
 * Triggers the folder creation when name is confirmed.
 */
public class CreateFolderDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_PARENT_FOLDER = "PARENT_FOLDER";

    public static final String CREATE_FOLDER_FRAGMENT = "CREATE_FOLDER_FRAGMENT";

    /**
     * Public factory method to create new CreateFolderDialogFragment instances.
     *
     * @param parentFolder Folder to create
     * @return Dialog ready to show.
     */
    public static CreateFolderDialogFragment newInstance(OCFile parentFolder) {
        CreateFolderDialogFragment frag = new CreateFolderDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARENT_FOLDER, parentFolder);
        frag.setArguments(args);
        return frag;
    }

    private OCFile mParentFolder;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mParentFolder = requireArguments().getParcelable(ARG_PARENT_FOLDER);

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams")
        View v = inflater.inflate(R.layout.edit_box_dialog, null);

        // Allow or disallow touches with other visible windows
        v.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
        );

        CoordinatorLayout coordinatorLayout = requireActivity().findViewById(R.id.coordinator_layout);

        coordinatorLayout.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
        );

        // Setup layout 
        EditText inputText = v.findViewById(R.id.user_input);
        inputText.setText("");
        inputText.requestFocus();

        // Build the dialog  
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(v)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setTitle(R.string.uploader_info_dirname);
        Dialog d = builder.create();
        d.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return d;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            String newFolderName =
                    ((TextView) (requireDialog().findViewById(R.id.user_input))).getText().toString().trim();

            if (newFolderName.length() <= 0) {
                showSnackMessage(R.string.filename_empty);
                return;
            }

            if (!FileUtils.isValidName(newFolderName)) {
                showSnackMessage(R.string.filename_forbidden_charaters_from_server);
                return;
            }

            String path = mParentFolder.getRemotePath();
            path += newFolderName + OCFile.PATH_SEPARATOR;
            ((ComponentsGetter) requireActivity()).getFileOperationsHelper().createFolder(path, false);
        }
    }

    /**
     * Show a temporary message in a Snackbar bound to the content view of the parent Activity
     *
     * @param messageResource Message to show.
     */
    private void showSnackMessage(int messageResource) {
        Snackbar snackbar = Snackbar.make(
                requireActivity().findViewById(R.id.coordinator_layout),
                messageResource,
                Snackbar.LENGTH_LONG
        );
        snackbar.show();
    }

}
