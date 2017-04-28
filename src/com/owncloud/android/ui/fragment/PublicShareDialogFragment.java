/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2017 ownCloud GmbH.
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

package com.owncloud.android.ui.fragment;

import android.accounts.Account;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class PublicShareDialogFragment extends DialogFragment {

    private static final String TAG = PublicShareDialogFragment.class.getSimpleName();

    /**
     * The fragment initialization parameters
     */
    private static final String ARG_FILE = "FILE";

    private static final String ARG_SHARE = "SHARE";

    private static final String ARG_ACCOUNT = "ACCOUNT";

    /**
     * File to share, received as a parameter in construction time
     */
    private OCFile mFile;

    /**
     * Existing share to update. If NULL, the dialog will create a new share for mFile.
     */
    private OCShare mPublicShare;

    /*
     * OC account holding the file to share, received as a parameter in construction time
     */
    private Account mAccount;

    /**
     * Reference to parent listener
     */
    private ShareFragmentListener mListener;

    /**
     * Capabilities of the server
     */
    private OCCapability mCapabilities;

    /**
     * Create a new instance of PublicShareDialogFragment, providing fileToShare
     * as an argument.
     *
     * Dialog shown this way is intended to CREATE a new public share.
     *
     * @param   fileToShare     File to share with a new public share.
     * @param   account         Account to get capabilities
     */
    public static PublicShareDialogFragment newInstanceToCreate(OCFile fileToShare, Account account) {
        PublicShareDialogFragment publicShareDialogFragment = new PublicShareDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToShare);
        args.putParcelable(ARG_ACCOUNT, account);
        publicShareDialogFragment.setArguments(args);
        return publicShareDialogFragment;
    }

    /**
     * Update an instance of PublicShareDialogFragment, providing publicShare
     * as an argument.
     *
     * Dialog shown this way is intended to UPDATE an existing public share.
     *
     * @param   publicShare           Public share to update.
     */
    public static PublicShareDialogFragment newInstanceToUpdate(OCShare publicShare) {
        PublicShareDialogFragment publicShareDialogFragment = new PublicShareDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SHARE, publicShare);
        publicShareDialogFragment.setArguments(args);
        return publicShareDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mFile = getArguments().getParcelable(ARG_FILE);
            mAccount = getArguments().getParcelable(ARG_ACCOUNT);
            mPublicShare = getArguments().getParcelable(ARG_SHARE);
        }

        if (mFile == null && mPublicShare == null) {
            throw new IllegalStateException("Both ARG_FILE and ARG_SHARE cannot be NULL");
        }

        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    private boolean updating() {
        return (mPublicShare != null);
    }

    private boolean isSharedFolder() {
        return (
            (mFile != null && mFile.isFolder()) ||
            (mPublicShare != null && mPublicShare.isFolder())
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.add_public_link, container, false);

        RelativeLayout editPermissionSection = (RelativeLayout) view.
                findViewById(R.id.shareViaLinkEditPermissionSection);

        // Show or hide edit permission section
        if (isSharedFolder()){
            editPermissionSection.setVisibility(View.VISIBLE);
        } else {
            editPermissionSection.setVisibility(View.GONE);
        }

        // Set listener for user actions on password
        initPasswordListener(view);

        // Set listener for user actions on expiration date
        initExpirationListener(view);

        // Confirm add public link
        Button confirmAddPublicLinkButton = (Button) view.findViewById(R.id.confirmAddPublicLinkButton);
        if (updating()) {
            confirmAddPublicLinkButton.setText(R.string.common_ok);
        }

        confirmAddPublicLinkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // TO DO password enforced by server, request to the user before trying to create
                if (mCapabilities != null &&
                        mCapabilities.getFilesSharingPublicPasswordEnforced().isTrue()) {
                }

                String publicLinkName = getNameValue().getText().toString();

                String publicLinkPassword = getPasswordValue().getText().toString();

                String expirationDate = getExpirationDateValue().getText().toString();

                long publicLinkExpirationDateMillis = -1;

                // Parse expiration date and convert it to milliseconds
                if (expirationDate != null) {

                    DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

                    try {
                        publicLinkExpirationDateMillis = format.parse(getExpirationDateValue().
                                getText().toString()).getTime();
                    } catch (ParseException e) {
                        // DO NOTHING
                    }
                }

                Boolean publicLinkEditPermissions = getEditPermissionSwitch().isChecked();

                ((FileActivity) getActivity()).getFileOperationsHelper().
                        shareFileViaLink(
                                mFile,
                                publicLinkName,
                                publicLinkPassword,
                                publicLinkExpirationDateMillis,
                                publicLinkEditPermissions
                        );

                dismiss();
            }
        });

        // Cancel add public link
        Button cancelAddPublicLinkButton = (Button) view.findViewById(R.id.cancelAddPublicLinkButton);

        cancelAddPublicLinkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log_OC.d(TAG, "onActivityCreated");

        // Load known capabilities of the server from DB
        refreshCapabilitiesFromDB();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ShareFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnShareFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Binds listener for user actions that start any update on a password for the public link
     * to the views receiving the user events.
     *
     * @param shareView Root view in the fragment.
     */
    private void initPasswordListener(View shareView) {
        OnPasswordInteractionListener mOnPasswordInteractionListener = new OnPasswordInteractionListener();

        ((SwitchCompat) shareView.findViewById(R.id.shareViaLinkPasswordSwitch)).
                setOnCheckedChangeListener(mOnPasswordInteractionListener);
    }

    /**
     * Listener for user actions that start any update on a password for the public link.
     */
    private class OnPasswordInteractionListener
            implements CompoundButton.OnCheckedChangeListener {

        /**
         * Called by R.id.shareViaLinkPasswordSwitch to set or clear the password.
         *
         * @param switchView {@link SwitchCompat} toggled by the user, R.id.shareViaLinkPasswordSwitch
         * @param isChecked  New switch state.
         */
        @Override
        public void onCheckedChanged(CompoundButton switchView, boolean isChecked) {

            EditText shareViaLinkPasswordValue = (EditText) getView().findViewById(R.id.shareViaLinkPasswordValue);

            if (isChecked) {

                // Show input to set the password
                shareViaLinkPasswordValue.setVisibility(View.VISIBLE);

                shareViaLinkPasswordValue.requestFocus();

            } else {

                shareViaLinkPasswordValue.setVisibility(View.GONE);

                shareViaLinkPasswordValue.getText().clear();

            }
        }
    }

    /**
     * Binds listener for user actions that start any update on a expiration date
     * for the public link to the views receiving the user events.
     *
     * @param shareView Root view in the fragment.
     */
    private void initExpirationListener(View shareView) {
        OnExpirationDateInteractionListener mOnExpirationDateInteractionListener =
                new OnExpirationDateInteractionListener();

        ((SwitchCompat) shareView.findViewById(R.id.shareViaLinkExpirationSwitch)).
                setOnCheckedChangeListener(mOnExpirationDateInteractionListener);

        shareView.findViewById(R.id.shareViaLinkExpirationLabel).
                setOnClickListener(mOnExpirationDateInteractionListener);

        shareView.findViewById(R.id.shareViaLinkExpirationValue).
                setOnClickListener(mOnExpirationDateInteractionListener);
    }

    /**
     * Listener for user actions that start any update on the expiration date for the public link.
     */
    private class OnExpirationDateInteractionListener
            implements CompoundButton.OnCheckedChangeListener, View.OnClickListener,
            ExpirationDatePickerDialogFragment.DatePickerFragmentListener {

        /**
         * Called by R.id.shareViaLinkExpirationSwitch to set or clear the expiration date.
         *
         * @param switchView {@link SwitchCompat} toggled by the user, R.id.shareViaLinkExpirationSwitch
         * @param isChecked  New switch state.
         */
        @Override
        public void onCheckedChanged(CompoundButton switchView, boolean isChecked) {
            if (!isResumed()) {
                // very important, setCheched(...) is called automatically during
                // Fragment recreation on device rotations
                return;
            }

            ExpirationDatePickerDialogFragment dialog = ExpirationDatePickerDialogFragment.
                    newInstance(-1, this);

            if (isChecked) {

                // Show calendar to set the expiration date
                dialog.show(
                        getActivity().getSupportFragmentManager(),
                        ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
                );

            } else {

                getExpirationDateValue().setVisibility(View.INVISIBLE);

            }
        }

        /**
         * Called by R.id.shareViaLinkExpirationLabel or R.id.shareViaLinkExpirationValue
         * to change the current expiration date.
         *
         * @param expirationView Label or value view touched by the user.
         */
        @Override
        public void onClick(View expirationView) {

            ExpirationDatePickerDialogFragment dialog = ExpirationDatePickerDialogFragment.
                    newInstance(-1, this);

            // Show calendar to set the expiration date
            dialog.show(
                    getActivity().getSupportFragmentManager(),
                    ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
            );
        }

        /**
         * Update the selected date for the public link
         *
         * @param date date selected by the user
         */
        @Override
        public void onDateSet(String date) {

            TextView expirationDate = getExpirationDateValue();

            if (expirationDate.getVisibility() == View.INVISIBLE) {

                expirationDate.setVisibility(View.VISIBLE);

            }

            expirationDate.setText(date);
        }

        @Override
        public void onCancelDatePicker() {

            SwitchCompat expirationToggle = ((SwitchCompat) getView().
                    findViewById(R.id.shareViaLinkExpirationSwitch));

            if (expirationToggle.isChecked()) {
                expirationToggle.setChecked(false);
            }
        }
    }

    /**
     * Get known server capabilities from DB
     *
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshCapabilitiesFromDB() {
        if (((FileActivity) mListener).getStorageManager() != null) {
            mCapabilities = ((FileActivity) mListener).getStorageManager().
                    getCapability(mAccount.name);
        }
    }

    private EditText getNameValue() {
        return (EditText) getView().findViewById(R.id.shareViaLinkNameValue);
    }

    private View getPasswordSection() {
        return getView().findViewById(R.id.shareViaLinkPasswordSection);
    }

    private SwitchCompat getPasswordSwitch() {
        return (SwitchCompat) getView().findViewById(R.id.shareViaLinkPasswordSwitch);
    }

    private TextView getPasswordValue() {
        return (TextView) getView().findViewById(R.id.shareViaLinkPasswordValue);
    }

    private View getExpirationDateSection() {
        return getView().findViewById(R.id.shareViaLinkExpirationSection);
    }

    private SwitchCompat getExpirationDateSwitch() {
        return (SwitchCompat) getView().findViewById(R.id.shareViaLinkExpirationSwitch);
    }

    private TextView getExpirationDateValue() {
        return (TextView) getView().findViewById(R.id.shareViaLinkExpirationValue);
    }

    private View getEditPermissionSection() {
        return getView().findViewById(R.id.shareViaLinkEditPermissionSection);
    }

    private SwitchCompat getEditPermissionSwitch() {
        return (SwitchCompat) getView().findViewById(R.id.shareViaLinkEditPermissionSwitch);
    }
}