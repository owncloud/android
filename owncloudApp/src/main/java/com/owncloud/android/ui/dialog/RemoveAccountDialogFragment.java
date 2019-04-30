/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2016 ownCloud GmbH.
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
 * Dialog requiring confirmation before removing an OC Account.
 * <p>
 * Removes the account if the user confirms.
 * <p>
 * Container Activity needs to implement AccountManagerCallback<Boolean>.
 */

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.app.Activity;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import com.owncloud.android.R;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;

import java.util.Objects;

public class RemoveAccountDialogFragment extends ConfirmationDialogFragment
        implements ConfirmationDialogFragmentListener {

    private Account mTargetAccount;

    private static final String ARG_TARGET_ACCOUNT = "TARGET_ACCOUNT";

    /**
     * Public factory method to create new RemoveAccountDialogFragment instances.
     *
     * @param account Account to remove.
     * @return Dialog ready to show.
     */
    public static RemoveAccountDialogFragment newInstance(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Cannot remove a NULL account");
        }

        RemoveAccountDialogFragment frag = new RemoveAccountDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MESSAGE_RESOURCE_ID, R.string.confirmation_remove_account_alert);
        args.putStringArray(ARG_MESSAGE_ARGUMENTS, new String[]{account.name});
        args.putInt(ARG_POSITIVE_BTN_RES, R.string.common_yes);
        args.putInt(ARG_NEUTRAL_BTN_RES, R.string.common_no);
        args.putInt(ARG_NEGATIVE_BTN_RES, -1);
        args.putParcelable(ARG_TARGET_ACCOUNT, account);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // checked here to fail soon in case of wrong usage
        try {
            AccountManagerCallback<Boolean> a = (AccountManagerCallback<Boolean>) getActivity();
        } catch (ClassCastException c) {
            throw new IllegalStateException(
                    "Container Activity needs to implement (AccountManagerCallback<Boolean>)", c
            );
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        mTargetAccount = getArguments().getParcelable(ARG_TARGET_ACCOUNT);

        setOnConfirmationListener(this);

        return dialog;
    }

    /**
     * Performs the removal of the target account.
     */
    @Override
    public void onConfirmation(String callerTag) {
        Activity parentActivity = getActivity();
        AccountManager am = AccountManager.get(parentActivity);
        AccountManagerCallback<Boolean> callback = (AccountManagerCallback<Boolean>) parentActivity;
        am.removeAccount(mTargetAccount, callback, new Handler());

        // Notify removal to Document Provider
        String authority = getResources().getString(R.string.document_provider_authority);
        Uri rootsUri = DocumentsContract.buildRootsUri(authority);
        Objects.requireNonNull(getContext()).getContentResolver().notifyChange(rootsUri, null);
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