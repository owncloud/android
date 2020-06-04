/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author Christian Schabesberger
 * Copyright (C) 2012 Bartek Przybylski
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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.owncloud.android.R;

/**
 * Dialog which will be displayed to user upon keep-in-sync file conflict.
 */
public class ConflictsResolveDialog extends DialogFragment {

    public enum Decision {
        CANCEL,
        KEEP_BOTH,
        OVERWRITE,
        SERVER
    }

    private OnConflictDecisionMadeListener mListener;

    public static ConflictsResolveDialog newInstance(String path, OnConflictDecisionMadeListener listener) {
        ConflictsResolveDialog f = new ConflictsResolveDialog();
        Bundle args = new Bundle();
        args.putString("remotepath", path);
        f.setArguments(args);
        f.mListener = listener;
        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.conflict_title)
                .setMessage(getString(R.string.conflict_message))
                .setPositiveButton(R.string.conflict_use_local_version,
                        (dialog, which) -> {
                            if (mListener != null) {
                                mListener.conflictDecisionMade(Decision.OVERWRITE);
                            }
                        })
                .setNeutralButton(R.string.conflict_keep_both,
                        (dialog, which) -> {
                            if (mListener != null) {
                                mListener.conflictDecisionMade(Decision.KEEP_BOTH);
                            }
                        })
                .setNegativeButton(R.string.conflict_use_server_version,
                        (dialog, which) -> {
                            if (mListener != null) {
                                mListener.conflictDecisionMade(Decision.SERVER);
                            }
                        })
                .create();
    }

    public void showDialog(AppCompatActivity activity) {
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag("dialog");
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        this.show(ft, "dialog");
    }

    public void dismissDialog(AppCompatActivity activity) {
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag(getTag());
        if (prev != null) {
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.remove(prev);
            ft.commit();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mListener.conflictDecisionMade(Decision.CANCEL);
    }

    public interface OnConflictDecisionMadeListener {
        void conflictDecisionMade(Decision decision);
    }
}
