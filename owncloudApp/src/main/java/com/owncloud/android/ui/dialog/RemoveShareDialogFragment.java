/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2017 ownCloud GmbH.
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
 *  Dialog requiring confirmation before removing a share.
 *  Triggers the removal according to the user response.
 */

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.owncloud.android.ui.fragment.ShareFragmentListener;

public class RemoveShareDialogFragment extends ConfirmationDialogFragment
        implements ConfirmationDialogFragmentListener {

    private static final String TAG = RemoveShareDialogFragment.class.getName();

    private OCShare mTargetShare;

    private static final String ARG_TARGET_SHARE = "TARGET_SHARE";

    /**
     * Public factory method to create new RemoveFilesDialogFragment instances.
     *
     * @param share           {@link OCShare} to remove.
     * @return Dialog ready to show.
     */
    public static RemoveShareDialogFragment newInstance(OCShare share) {
        RemoveShareDialogFragment frag = new RemoveShareDialogFragment();
        Bundle args = new Bundle();

        args.putInt(ARG_MESSAGE_RESOURCE_ID, R.string.confirmation_remove_public_share_message);
        args.putStringArray(
                ARG_MESSAGE_ARGUMENTS,
                new String[]{
                        share.getName().length() > 0 ? share.getName() : share.getToken()
                }
        );
        args.putInt(ARG_TITLE_ID, R.string.confirmation_remove_public_share_title);
        args.putInt(ARG_POSITIVE_BTN_RES, R.string.common_yes);
        args.putInt(ARG_NEUTRAL_BTN_RES, R.string.common_no);
        args.putInt(ARG_NEGATIVE_BTN_RES, -1);
        args.putParcelable(ARG_TARGET_SHARE, share);
        frag.setArguments(args);

        return frag;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        mTargetShare = getArguments().getParcelable(ARG_TARGET_SHARE);

        setOnConfirmationListener(this);

        return dialog;
    }

    /**
     * Performs the removal of the target share, both locally and in the server.
     */
    @Override
    public void onConfirmation(String callerTag) {
        ShareFragmentListener listener = (ShareFragmentListener) getActivity();
        Log_OC.d(TAG, "Removing public share " + mTargetShare.getName());
        listener.removeShare(mTargetShare);
    }

    @Override
    public void onCancel(String callerTag) {
        // nothing to do here
    }

    @Override
    public void onNeutral(String callerTag) {
        // nothing to do here
    }
}