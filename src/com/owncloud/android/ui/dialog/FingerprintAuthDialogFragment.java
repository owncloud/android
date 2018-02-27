/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2018 ownCloud GmbH.
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

package com.owncloud.android.ui.dialog;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.owncloud.android.R;

/**
 * Dialog to authenticate the user using fingerprint APIs, enabling the user to use pass code authentication as well
 */
public class FingerprintAuthDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private Button mFingerprintCancelButton;

    /**
     * Public factory method to create new FingerprintAuthDialogFragment instances.
     *
     * @return Dialog ready to show.
     */
    public static FingerprintAuthDialogFragment newInstance() {
        return new FingerprintAuthDialogFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View v = inflater.inflate(R.layout.fingerprint_dialog, container, false);

        mFingerprintCancelButton = (Button) v.findViewById(R.id.fingerprintCancelButton);

        mFingerprintCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        return v;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

    }
}