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

import com.owncloud.android.R;

public class AddPublicLinkFragment extends DialogFragment {

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

        View v = inflater.inflate(R.layout.add_public_link, container, false);

        // Confirm add public link
        Button confirmAddPublicLinkButton = (Button)v.findViewById(R.id.confirmAddPublicLinkButton);

        confirmAddPublicLinkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // When button is clicked, call up to owning activity.

            }
        });

        // Cancel add public link
        Button cancelAddPublicLinkButton = (Button)v.findViewById(R.id.cancelAddPublicLinkButton);

        cancelAddPublicLinkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });


        // Password toggle
        SwitchCompat passwordToggle = ((SwitchCompat) v.findViewById(R.id.shareViaLinkPasswordSwitch));

        // Password value
        final EditText shareViaLinkPasswordValue = (EditText) v.findViewById(R.id.shareViaLinkPasswordValue);

        passwordToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                // Show input to set the password
                if (isChecked) {

                    shareViaLinkPasswordValue.setVisibility(View.VISIBLE);

                    shareViaLinkPasswordValue.requestFocus();

                } else {

                    shareViaLinkPasswordValue.setVisibility(View.GONE);

                    shareViaLinkPasswordValue.getText().clear();

                }
            }
        });

        return v;
    }
}
