/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.ui.fragment;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment;
import com.owncloud.android.utils.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PublicShareDialogFragment extends DialogFragment {

    private static final String TAG = PublicShareDialogFragment.class.getSimpleName();

    /**
     * The fragment initialization parameters
     */
    private static final String ARG_FILE = "FILE";
    private static final String ARG_SHARE = "SHARE";
    private static final String ARG_ACCOUNT = "ACCOUNT";
    private static final String ARG_DEFAULT_LINK_NAME = "DEFAULT_LINK_NAME";
    private static final String KEY_EXPIRATION_DATE = "EXPIRATION_DATE";

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
     * Listener for changes in password switch
     */
    private OnPasswordInteractionListener mOnPasswordInteractionListener;

    /**
     * Listener for changes in expiration date switch
     */
    private OnExpirationDateInteractionListener mOnExpirationDateInteractionListener;

    /**
     * UI elements
     */

    private LinearLayout mNameSelectionLayout;
    private EditText mNameValueEdit;
    private TextView mPasswordLabel;
    private SwitchCompat mPasswordSwitch;
    private EditText mPasswordValueEdit;
    private TextView mExpirationDateLabel;
    private SwitchCompat mExpirationDateSwitch;
    private TextView mExpirationDateExplanationLabel;
    private TextView mExpirationDateValueLabel;
    private TextView mErrorMessageLabel;
    private RadioGroup mPermissionRadioGroup;
    private RadioButton mReadOnlyButton;
    private RadioButton mReadWriteButton;
    private RadioButton mUploadOnlyButton;

    /**
     * Create a new instance of PublicShareDialogFragment, providing fileToShare and account
     * as an argument.
     *
     * Dialog shown this way is intended to CREATE a new public share.
     *
     * @param   fileToShare     File to share with a new public share.
     * @param   account         Account to get capabilities
     */
    public static PublicShareDialogFragment newInstanceToCreate(
            OCFile fileToShare,
            Account account,
            String defaultLinkName
    ) {
        PublicShareDialogFragment publicShareDialogFragment = new PublicShareDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToShare);
        args.putParcelable(ARG_ACCOUNT, account);
        args.putString(ARG_DEFAULT_LINK_NAME, defaultLinkName);

        publicShareDialogFragment.setArguments(args);
        return publicShareDialogFragment;
    }

    /**
     * Update an instance of PublicShareDialogFragment, providing fileToShare, publicShare and
     * account as arguments.
     *
     * Dialog shown this way is intended to UPDATE an existing public share.
     *
     * @param   publicShare           Public share to update.
     */
    public static PublicShareDialogFragment newInstanceToUpdate(OCFile fileToShare,
                                                                OCShare publicShare,
                                                                Account account
    ) {
        PublicShareDialogFragment publicShareDialogFragment = new PublicShareDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToShare);
        args.putParcelable(ARG_SHARE, publicShare);
        args.putParcelable(ARG_ACCOUNT, account);
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

        View view = inflater.inflate(R.layout.share_public_dialog, container, false);

        Log_OC.d(TAG, "onCreateView");

        final TextView dialogTitleLabel = view.findViewById(R.id.publicShareDialogTitle);
        mNameSelectionLayout = view.findViewById(R.id.shareViaLinkNameSection);
        mNameValueEdit = view.findViewById(R.id.shareViaLinkNameValue);
        mPasswordLabel = view.findViewById(R.id.shareViaLinkPasswordLabel);
        mPasswordSwitch = view.findViewById(R.id.shareViaLinkPasswordSwitch);
        mPasswordValueEdit = view.findViewById(R.id.shareViaLinkPasswordValue);
        mExpirationDateLabel = view.findViewById(R.id.shareViaLinkExpirationLabel);
        mExpirationDateSwitch = view.findViewById(R.id.shareViaLinkExpirationSwitch);
        mExpirationDateExplanationLabel = view.findViewById(R.id.shareViaLinkExpirationExplanationLabel);
        mErrorMessageLabel = view.findViewById(R.id.public_link_error_message);
        mExpirationDateValueLabel = view.findViewById(R.id.shareViaLinkExpirationValue);
        mPermissionRadioGroup = view.findViewById(R.id.shareViaLinkEditPermissionGroup);
        mReadOnlyButton = view.findViewById(R.id.shareViaLinkEditPermissionReadOnly);
        mReadWriteButton = view.findViewById(R.id.shareViaLinkEditPermissionReadAndWrite);
        mUploadOnlyButton = view.findViewById(R.id.shareViaLinkEditPermissionUploadFiles);

        // Get and set the values saved previous to the screen rotation, if any
        if (savedInstanceState != null) {
            String expirationDate = savedInstanceState.getString(KEY_EXPIRATION_DATE);
            if (expirationDate.length() > 0) {
                mExpirationDateValueLabel.setVisibility(View.VISIBLE);
                mExpirationDateValueLabel.setText(expirationDate);
            }
        }

        if (updating()) {
            dialogTitleLabel.setText(R.string.share_via_link_edit_title);
            mNameValueEdit.setText(mPublicShare.getName());

            switch (mPublicShare.getPermissions()) {
                case (OCShare.CREATE_PERMISSION_FLAG
                        | OCShare.DELETE_PERMISSION_FLAG
                        | OCShare.UPDATE_PERMISSION_FLAG
                        | OCShare.READ_PERMISSION_FLAG):
                    mReadWriteButton.setChecked(true);
                    break;
                case OCShare.CREATE_PERMISSION_FLAG:
                    mUploadOnlyButton.setChecked(true);
                    break;
                default:
                    mReadOnlyButton.setChecked(true);
            }

            if (mPublicShare.isPasswordProtected()) {

                setPasswordSwitchChecked(true);
                mPasswordValueEdit.setVisibility(View.VISIBLE);
                mPasswordValueEdit.setHint(R.string.share_via_link_default_password);
            }

            if (mPublicShare.getExpirationDate() != 0) {
                setExpirationDateSwitchChecked(true);
                String formattedDate = ExpirationDatePickerDialogFragment.getDateFormat().format(
                        new Date(mPublicShare.getExpirationDate())
                );
                mExpirationDateValueLabel.setVisibility(View.VISIBLE);
                mExpirationDateValueLabel.setText(formattedDate);
            }

        } else {
            mNameValueEdit.setText(getArguments().getString(ARG_DEFAULT_LINK_NAME, ""));
        }

        initPasswordListener();
        initExpirationListener();
        initPasswordFocusChangeListener();
        initPasswordToggleListener();

        view.findViewById(R.id.saveButton)
                .setOnClickListener(v -> onSaveShareSetting());

        view.findViewById(R.id.cancelButton)
                .setOnClickListener(v -> dismiss());

        return view;
    }

    private void onSaveShareSetting() {

        // Get data filled by user
        final String publicLinkName = mNameValueEdit.getText().toString();
        String publicLinkPassword = mPasswordValueEdit.getText().toString();
        final long publicLinkExpirationDateInMillis = getExpirationDateValueInMillis();

        final int publicLinkPermissions;
        boolean publicUploadPermission;

        switch (mPermissionRadioGroup.getCheckedRadioButtonId()) {
            case R.id.shareViaLinkEditPermissionUploadFiles:
                publicLinkPermissions = OCShare.CREATE_PERMISSION_FLAG;
                publicUploadPermission = true;
                break;
            case R.id.shareViaLinkEditPermissionReadAndWrite:
                publicLinkPermissions = OCShare.CREATE_PERMISSION_FLAG
                        | OCShare.DELETE_PERMISSION_FLAG
                        | OCShare.UPDATE_PERMISSION_FLAG
                        | OCShare.READ_PERMISSION_FLAG;
                publicUploadPermission = true;
                break;
            case R.id.shareViaLinkEditPermissionReadOnly:
            default:
                publicLinkPermissions = OCShare.READ_PERMISSION_FLAG;
                publicUploadPermission = false;
                break;
        }

        // since the public link permission foo got a bit despagetified in the server somewhere
        // at 10.0.4 we don't need publicUploadPermission there anymore. By setting it to false
        // it will not be sent to the server.

        publicUploadPermission =
                (mCapabilities.getVersionMayor() >= 10
                        && (mCapabilities.getVersionMinor() > 1
                        || mCapabilities.getVersionMicro() > 3))
                        && publicUploadPermission;

        if (!updating()) { // Creating a new public share
            ((FileActivity) getActivity()).getFileOperationsHelper().
                    shareFileViaLink(mFile,
                            publicLinkName,
                            publicLinkPassword,
                            publicLinkExpirationDateInMillis,
                            false,
                            publicLinkPermissions);

        } else { // Updating an existing public share
            if (!mPasswordSwitch.isChecked()) {
                publicLinkPassword = "";
            } else if (mPasswordValueEdit.length() == 0) {
                // User has not added a new password, so do not update it
                publicLinkPassword = null;
            }

            ((FileActivity) getActivity()).getFileOperationsHelper().
                    updateShareViaLink(mPublicShare,
                            publicLinkName,
                            publicLinkPassword,
                            publicLinkExpirationDateInMillis,
                            publicUploadPermission,
                            publicLinkPermissions);
        }
    }

    private void initPasswordFocusChangeListener() {
        mPasswordValueEdit.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if (v.getId() == R.id.shareViaLinkPasswordValue) {
                onPasswordFocusChanged(hasFocus);
            }
        });
    }

    private void initPasswordToggleListener() {
        mPasswordValueEdit.setOnTouchListener(new RightDrawableOnTouchListener() {
            @Override
            public boolean onDrawableTouch(final MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    onViewPasswordClick();
                }
                return true;
            }
        });
    }

    private abstract static class RightDrawableOnTouchListener implements View.OnTouchListener {

        private int fuzz = 75;

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            Drawable rightDrawable = null;
            if (view instanceof TextView) {
                Drawable[] drawables = ((TextView) view).getCompoundDrawables();
                if (drawables.length > 2) {
                    rightDrawable = drawables[2];
                }
            }
            if (rightDrawable != null) {
                final int x = (int) event.getX();
                final int y = (int) event.getY();
                final Rect bounds = rightDrawable.getBounds();
                if (x >= (view.getRight() - bounds.width() - fuzz) &&
                        x <= (view.getRight() - view.getPaddingRight() + fuzz) &&
                        y >= (view.getPaddingTop() - fuzz) &&
                        y <= (view.getHeight() - view.getPaddingBottom()) + fuzz) {

                    return onDrawableTouch(event);
                }
            }
            return false;
        }

        public abstract boolean onDrawableTouch(final MotionEvent event);
    }

    /**
     * Handles changes in focus on the text input for the password (basic authorization).
     * When (hasFocus), the button to toggle password visibility is shown.
     * When (!hasFocus), the button is made invisible and the password is hidden.
     *
     * @param hasFocus          'True' if focus is received, 'false' if is lost
     */
    private void onPasswordFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            showViewPasswordButton();
        } else {
            hidePassword();
            hidePasswordButton();
        }
    }

    /**
     * Called when the eye icon in the password field is clicked.
     *
     * Toggles the visibility of the password in the field.
     */
    public void onViewPasswordClick() {
        if (getView() != null) {
            if (isPasswordVisible()) {
                hidePassword();
            } else {
                showPassword();
            }
            mPasswordValueEdit.setSelection(mPasswordValueEdit.getSelectionStart(),
                    mPasswordValueEdit.getSelectionEnd());
        }
    }

    private void showViewPasswordButton() {
        int drawable = isPasswordVisible()
                ? R.drawable.ic_view_black
                : R.drawable.ic_hide_black;
        if (getView() != null) {
            mPasswordValueEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0);
        }
    }

    private boolean isPasswordVisible() {
        return (getView() != null) &&
                ((mPasswordValueEdit.getInputType() & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                        == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    }

    private void hidePasswordButton() {
        if (getView() != null) {
            mPasswordValueEdit.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    private void showPassword() {
        if (getView() != null) {
            mPasswordValueEdit.setInputType(
                    InputType.TYPE_CLASS_TEXT |
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD |
                            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            showViewPasswordButton();
        }
    }

    private void hidePassword() {
        if (getView() != null) {
            mPasswordValueEdit.setInputType(
                    InputType.TYPE_CLASS_TEXT |
                            InputType.TYPE_TEXT_VARIATION_PASSWORD |
                            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            showViewPasswordButton();
        }
    }

    private long getExpirationDateValueInMillis() {
        long publicLinkExpirationDateInMillis = -1;
        String expirationDate = mExpirationDateValueLabel.getText().toString();
        if (expirationDate.length() > 0) {
            // Parse expiration date and convert it to milliseconds
            try {
                // remember: format is defined by date picker
                publicLinkExpirationDateInMillis =
                        ExpirationDatePickerDialogFragment.getDateFormat().
                                parse(expirationDate).getTime();
            } catch (ParseException e) {
                Log_OC.e(TAG, "Error reading expiration date from input field", e);
            }
        }
        return publicLinkExpirationDateInMillis;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log_OC.d(TAG, "onActivityCreated");

        // Load known capabilities of the server from DB
        refreshModelFromStorageManager();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_EXPIRATION_DATE, mExpirationDateValueLabel.getText().toString());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ShareFragmentListener) activity;
        } catch (IllegalStateException e) {
            throw new IllegalStateException(activity.toString()
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
     */
    private void initPasswordListener() {
        mOnPasswordInteractionListener = new OnPasswordInteractionListener();
        mPasswordSwitch.setOnCheckedChangeListener(mOnPasswordInteractionListener);
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
            if (isChecked) {
                mPasswordValueEdit.setVisibility(View.VISIBLE);
                mPasswordValueEdit.requestFocus();

                // Show keyboard to fill in the password
                InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                mgr.showSoftInput(mPasswordValueEdit, InputMethodManager.SHOW_IMPLICIT);

            } else {
                mPasswordValueEdit.setVisibility(View.GONE);
                mPasswordValueEdit.getText().clear();
            }
        }
    }

    /**
     * Binds listener for user actions that start any update on a expiration date
     * for the public link to the views receiving the user events.
     *
     */
    private void initExpirationListener() {
        mOnExpirationDateInteractionListener = new OnExpirationDateInteractionListener();
        mExpirationDateSwitch.setOnCheckedChangeListener(mOnExpirationDateInteractionListener);
        mExpirationDateLabel.setOnClickListener(mOnExpirationDateInteractionListener);
        mExpirationDateValueLabel.setOnClickListener(mOnExpirationDateInteractionListener);
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

            if (isChecked) {
                // Show calendar to set the expiration date
                ExpirationDatePickerDialogFragment dialog = ExpirationDatePickerDialogFragment.
                        newInstance(
                                getExpirationDateValueInMillis(),
                                getImposedExpirationDate());
                dialog.setDatePickerListener(this);
                dialog.show(
                        getActivity().getSupportFragmentManager(),
                        ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG);
            } else {
                mExpirationDateValueLabel.setVisibility(View.INVISIBLE);
                mExpirationDateValueLabel.setText("");
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

            // Show calendar to set the expiration date
            ExpirationDatePickerDialogFragment dialog = ExpirationDatePickerDialogFragment.
                    newInstance(
                            getExpirationDateValueInMillis(),
                            getImposedExpirationDate()
                    );
            dialog.setDatePickerListener(this);
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
            mExpirationDateValueLabel.setVisibility(View.VISIBLE);
            mExpirationDateValueLabel.setText(date);
        }

        @Override
        public void onCancelDatePicker() {

            SwitchCompat expirationToggle = getView().
                    findViewById(R.id.shareViaLinkExpirationSwitch);

            // If the date has not been set yet, uncheck the toggle
            if (expirationToggle.isChecked() && mExpirationDateValueLabel.getText() == "") {
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
    public void refreshModelFromStorageManager() {
        if (((FileActivity) mListener).getStorageManager() != null) {
            mCapabilities = ((FileActivity) mListener).getStorageManager().
                    getCapability(mAccount.name);

            updateInputFormAccordingToServerCapabilities();
        }
    }

    /**
     * Updates the UI according to enforcements and allowances set by the server administrator.
     *
     * Includes:
     *  - hide the link name section if multiple public share is not supported, showing the keyboard
     *    to fill in the public share name otherwise
     *  - hide show file listing option
     *  - hide or show the switch to disable the password if it is enforced or not;
     *  - hide or show the switch to disable the expiration date it it is enforced or not;
     *  - show or hide the switch to allow public uploads if it is allowed or not;
     *  - set the default value for expiration date if defined (only if creating a new share).
     */
    private void updateInputFormAccordingToServerCapabilities() {
        final OwnCloudVersion serverVersion = new OwnCloudVersion(mCapabilities.getVersionString());

        // Server version <= 9.x, multiple public sharing not supported
        if (!serverVersion.isMultiplePublicSharingSupported()) {
            mNameSelectionLayout.setVisibility(View.GONE);
        } else {
            getDialog().getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }

        if (mCapabilities.getFilesSharingPublicUpload().isTrue() && isSharedFolder()) {
            mPermissionRadioGroup.setVisibility(View.VISIBLE);
        }

        // Show file listing option if all the following is true:
        //  - The file to share is a folder
        //  - Upload only is supported by the server version
        //  - Upload only capability is set
        //  - Allow editing capability is set
        if (!(isSharedFolder() &&
                serverVersion.isPublicSharingWriteOnlySupported() &&
                mCapabilities.getFilesSharingPublicSupportsUploadOnly().isTrue() &&
                mCapabilities.getFilesSharingPublicUpload().isTrue())) {
            mPermissionRadioGroup.setVisibility(View.GONE);
        }

        // Show default date enforced by the server, if any
        if (!updating() && mCapabilities.getFilesSharingPublicExpireDateDays() > 0) {

            setExpirationDateSwitchChecked(true);

            String formattedDate = SimpleDateFormat.getDateInstance().format(
                    DateUtils.addDaysToDate(
                            new Date(),
                            mCapabilities.getFilesSharingPublicExpireDateDays()
                    )
            );

            mExpirationDateValueLabel.setVisibility(View.VISIBLE);

            mExpirationDateValueLabel.setText(formattedDate);
        }

        // Hide expiration date switch if date is enforced to prevent it is removed
        if (mCapabilities.getFilesSharingPublicExpireDateEnforced().isTrue()) {
            mExpirationDateLabel.setText(R.string.share_via_link_expiration_date_enforced_label);
            mExpirationDateSwitch.setVisibility(View.GONE);
            mExpirationDateExplanationLabel.setVisibility(View.VISIBLE);
            mExpirationDateExplanationLabel.setText(
                    getString(R.string.share_via_link_expiration_date_explanation_label,
                            mCapabilities.getFilesSharingPublicExpireDateDays()));
        }

        // Set password label when opening the dialog
        if (mReadOnlyButton.isChecked() &&
                mCapabilities.getFilesSharingPublicPasswordEnforcedReadOnly().isTrue() ||
                mReadWriteButton.isChecked() &&
                        mCapabilities.getFilesSharingPublicPasswordEnforcedReadWrite().isTrue() ||
                mUploadOnlyButton.isChecked() &&
                        mCapabilities.getFilesSharingPublicPasswordEnforcedUploadOnly().isTrue()) {
            setPasswordEnforced();
        }

        // Set password label depending on the checked permission option
        mPermissionRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    if (checkedId == mReadOnlyButton.getId()) {
                        if (mCapabilities.getFilesSharingPublicPasswordEnforcedReadOnly().isTrue()) {
                            setPasswordEnforced();
                        } else {
                            setPasswordNotEnforced();
                        }
                    } else if (checkedId == mReadWriteButton.getId()) {
                        if (mCapabilities.getFilesSharingPublicPasswordEnforcedReadWrite().isTrue()) {
                            setPasswordEnforced();
                        } else {
                            setPasswordNotEnforced();
                        }
                    } else if (checkedId == mUploadOnlyButton.getId()) {
                        if (mCapabilities.getFilesSharingPublicPasswordEnforcedUploadOnly().isTrue()) {
                            setPasswordEnforced();
                        } else {
                            setPasswordNotEnforced();
                        }
                    }
                }
        );

        // When there's no password enforced for capability
        boolean hasPasswordEnforcedFor =
                mCapabilities.getFilesSharingPublicPasswordEnforcedReadOnly().isTrue() ||
                        mCapabilities.getFilesSharingPublicPasswordEnforcedReadWrite().isTrue() ||
                        mCapabilities.getFilesSharingPublicPasswordEnforcedUploadOnly().isTrue();

        // hide password switch if password is enforced to prevent it is removed
        if (!hasPasswordEnforcedFor && mCapabilities.getFilesSharingPublicPasswordEnforced().isTrue()) {
            setPasswordEnforced();
        }
    }

    /**
     * Get expiration date imposed by the server, if any
     */
    private long getImposedExpirationDate() {

        if (mCapabilities != null && mCapabilities.
                getFilesSharingPublicExpireDateEnforced().isTrue()) {

            return DateUtils.addDaysToDate(
                    new Date(),
                    mCapabilities.getFilesSharingPublicExpireDateDays())
                    .getTime();
        }

        return -1;
    }

    private void setPasswordNotEnforced() {
        mPasswordLabel.setText(R.string.share_via_link_password_label);
        mPasswordSwitch.setVisibility(View.VISIBLE);
        mPasswordValueEdit.setVisibility(View.GONE);
    }

    private void setPasswordEnforced() {
        mPasswordLabel.setText(R.string.share_via_link_password_enforced_label);
        mPasswordSwitch.setVisibility(View.GONE);
        mPasswordValueEdit.setVisibility(View.VISIBLE);
    }

    /**
     * Show error when creating or updating the public share, if any
     * @param errorMessage
     */
    public void showError(String errorMessage) {
        mErrorMessageLabel.setVisibility(View.VISIBLE);
        mErrorMessageLabel.setText(errorMessage);
    }

    private void setPasswordSwitchChecked(boolean checked) {
        mPasswordSwitch.setOnCheckedChangeListener(null);
        mPasswordSwitch.setChecked(checked);
        mPasswordSwitch.setOnCheckedChangeListener(mOnPasswordInteractionListener);
    }

    private void setExpirationDateSwitchChecked(boolean checked) {
        mExpirationDateSwitch.setOnCheckedChangeListener(null);
        mExpirationDateSwitch.setChecked(checked);
        mExpirationDateSwitch.setOnCheckedChangeListener(mOnExpirationDateInteractionListener);
    }

}