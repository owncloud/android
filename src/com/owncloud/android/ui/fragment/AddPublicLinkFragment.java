package com.owncloud.android.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment;

public class AddPublicLinkFragment extends DialogFragment {


    /**
     * Listener for user actions to set, update or clear password on public link
     */
    private OnPasswordInteractionListener mOnPasswordInteractionListener = null;

    /**
     * Listener for user actions to set, update or clear expiration date on public link
     */
    private OnExpirationDateInteractionListener mOnExpirationDateInteractionListener = null;

    /**
     * Create a new instance of MyDialogFragment, providing "num"
     * as an argument.
     */
    public static AddPublicLinkFragment newInstance() {
        AddPublicLinkFragment addPublicLinkFragment = new AddPublicLinkFragment();

        return addPublicLinkFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NORMAL, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Set title for this dialog
        getDialog().setTitle(R.string.share_add_public_link_title);

        View view = inflater.inflate(R.layout.add_public_link, container, false);

        // Confirm add public link
        Button confirmAddPublicLinkButton = (Button) view.findViewById(R.id.confirmAddPublicLinkButton);

        confirmAddPublicLinkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // When button is clicked, call up to owning activity.

            }
        });

        // Cancel add public link
        Button cancelAddPublicLinkButton = (Button) view.findViewById(R.id.cancelAddPublicLinkButton);

        cancelAddPublicLinkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });

        // Set listener for user actions on password
        initPasswordListener(view);

        // Set listener for user actions on expiration date
        initExpirationListener(view);

        return view;
    }


    /**
     * Binds listener for user actions that start any update on a password for the public link
     * to the views receiving the user events.
     *
     * @param shareView Root view in the fragment.
     */
    private void initPasswordListener(View shareView) {
        mOnPasswordInteractionListener = new OnPasswordInteractionListener();

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
        mOnExpirationDateInteractionListener = new OnExpirationDateInteractionListener();

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


            TextView shareViaLinkExpirationValue = (TextView) getView().findViewById(R.id.shareViaLinkExpirationValue);

            if (isChecked) {

                // Show calendar to set the expiration date
                dialog.show(
                        getActivity().getSupportFragmentManager(),
                        ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
                );

            } else {

                shareViaLinkExpirationValue.setVisibility(View.GONE);

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

            TextView expirationDate = (TextView) getView().findViewById(R.id.shareViaLinkExpirationValue);

            if (expirationDate.getVisibility() == View.GONE) {

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
}
