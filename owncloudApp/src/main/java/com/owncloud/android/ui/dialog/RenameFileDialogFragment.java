/**
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

/**
 * Dialog to input a new name for an {@link OCFile} being renamed.
 * <p>
 * Triggers the rename operation.
 */

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.R;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.utils.PreferenceUtils;

/**
 *  Dialog to input a new name for a file or folder to rename.  
 *
 *  Triggers the rename operation when name is confirmed.
 */
public class RenameFileDialogFragment
        extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_TARGET_FILE = "TARGET_FILE";

    /**
     * Public factory method to create new RenameFileDialogFragment instances.
     *
     * @param file            File to rename.
     * @return Dialog ready to show.
     */
    public static RenameFileDialogFragment newInstance(OCFile file) {
        RenameFileDialogFragment frag = new RenameFileDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TARGET_FILE, file);
        frag.setArguments(args);
        return frag;

    }

    private OCFile mTargetFile;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mTargetFile = getArguments().getParcelable(ARG_TARGET_FILE);

        // Inflate the layout for the dialog
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.edit_box_dialog, null);

        // Allow or disallow touches with other visible windows
        v.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
        );

        // Setup layout 
        String currentName = mTargetFile.getFileName();
        EditText inputText = v.findViewById(R.id.user_input);
        inputText.setText(currentName);
        int selectionStart = 0;
        int extensionStart = mTargetFile.isFolder() ? -1 : currentName.lastIndexOf(".");
        int selectionEnd = (extensionStart >= 0) ? extensionStart : currentName.length();
        if (selectionStart >= 0 && selectionEnd >= 0) {
            inputText.setSelection(
                    Math.min(selectionStart, selectionEnd),
                    Math.max(selectionStart, selectionEnd));
        }
        inputText.requestFocus();

        // Build the dialog  
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setTitle(R.string.rename_dialog_title);
        Dialog d = builder.create();
        d.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return d;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            String newFileName =
                    ((TextView) (getDialog().findViewById(R.id.user_input)))
                            .getText().toString().trim();

            if (newFileName.length() <= 0) {
                showSnackMessage(R.string.filename_empty);
                return;
            }

            if (!FileUtils.isValidName(newFileName)) {
                showSnackMessage(R.string.filename_forbidden_charaters_from_server);
                return;
            }

            ((ComponentsGetter) getActivity()).getFileOperationsHelper().
                    renameFile(mTargetFile, newFileName);
        }
    }

    /**
     * Show a temporary message in a Snackbar bound to the content view of the parent Activity
     *
     * @param messageResource       Message to show.
     */
    private void showSnackMessage(int messageResource) {
        Snackbar snackbar = Snackbar.make(
                getActivity().findViewById(android.R.id.content),
                messageResource,
                Snackbar.LENGTH_LONG
        );
        snackbar.show();
    }
}
