/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2016 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.ui.dialog;

import com.owncloud.android.R;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoadingDialog extends DialogFragment {

    private static final String ARG_MESSAGE_ID = LoadingDialog.class.getCanonicalName() + ".ARG_MESSAGE_ID";
    private static final String ARG_CANCELABLE = LoadingDialog.class.getCanonicalName() + ".ARG_CANCELABLE";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setCancelable(false);
    }

    /**
     * Public factory method to get dialog instances.
     *
     * @param messageId     Resource id for a message to show in the dialog.
     * @param cancelable    If 'true', the dialog can be cancelled by the user input (BACK button, touch outside...)
     * @return              New dialog instance, ready to show.
     */
    public static LoadingDialog newInstance(int messageId, boolean cancelable) {
        LoadingDialog fragment = new LoadingDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_MESSAGE_ID, messageId);
        args.putBoolean(ARG_CANCELABLE, cancelable);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create a view by inflating desired layout
        View v = inflater.inflate(R.layout.loading_dialog, container,  false);
        
        // set message
        TextView tv  = (TextView) v.findViewById(R.id.loadingText);
        int messageId = getArguments().getInt(ARG_MESSAGE_ID, R.string.placeholder_sentence);
        tv.setText(messageId);

        // set progress wheel color
        ProgressBar progressBar  = (ProgressBar) v.findViewById(R.id.loadingBar);
        progressBar.getIndeterminateDrawable().setColorFilter(
            ContextCompat.getColor(getActivity(), R.color.color_accent),
            PorterDuff.Mode.SRC_IN
        );
        
        return v;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        /// set cancellation behavior
        boolean cancelable = getArguments().getBoolean(ARG_CANCELABLE, false);
        dialog.setCancelable(cancelable);
        if (!cancelable) {
            // disable the back button
            DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode,
                                     KeyEvent event) {

                    if( keyCode == KeyEvent.KEYCODE_BACK) {
                        return true;
                    }
                    return false;
                }
            };
            dialog.setOnKeyListener(keyListener);
        }
        return dialog;
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }
}
