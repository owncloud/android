/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 *
 * Modifications
 *
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2019 ownCloud GmbH.
 */

package com.owncloud.android.ui.dialog;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import com.owncloud.android.R;
import com.owncloud.android.authentication.FingerprintUIHelper;
import com.owncloud.android.authentication.PassCodeManager;
import com.owncloud.android.authentication.PatternManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FingerprintActivity;

/**
 * Dialog to authenticate the user using fingerprint APIs, enabling the user to use pass code authentication as well
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class FingerprintAuthDialogFragment extends DialogFragment implements FingerprintUIHelper.Callback {

    private FingerprintActivity mActivity;
    private FingerprintManager.CryptoObject mCryptoObject;
    private FingerprintUIHelper mFingerprintUiHelper;
    private Button mFingerprintCancelButton;

    private static final String TAG = FingerprintAuthDialogFragment.class.getSimpleName();

    /**
     * Public factory method to create new FingerprintAuthDialogFragment instances.
     *
     * @return Dialog ready to show.
     */
    public static FingerprintAuthDialogFragment newInstance() {
        return new FingerprintAuthDialogFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fingerprint_dialog, container, false);

        mFingerprintUiHelper = new FingerprintUIHelper(
                mActivity.getSystemService(FingerprintManager.class),
                (ImageView) v.findViewById(R.id.fingerprintIcon),
                (TextView) v.findViewById(R.id.fingerprintStatus), this);

        mFingerprintCancelButton = v.findViewById(R.id.fingerprintCancelButton);

        mFingerprintCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();

                if (PassCodeManager.getPassCodeManager().isPassCodeEnabled()) {
                    PassCodeManager.getPassCodeManager().onFingerprintCancelled(mActivity);
                } else if (PatternManager.getPatternManager().isPatternEnabled()) {
                    PatternManager.getPatternManager().onFingerprintCancelled(mActivity);
                }
                mActivity.finish();
            }
        });

        // Avoid pressing back button and skipping fingerprint lock
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(android.content.DialogInterface dialog, int keyCode, android.view.KeyEvent event) {

                return keyCode == android.view.KeyEvent.KEYCODE_BACK;
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mFingerprintUiHelper.startListening(mCryptoObject);
    }

    @Override
    public void onPause() {
        super.onPause();
        mFingerprintUiHelper.stopListening();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (FingerprintActivity) getActivity();
    }

    /**
     * Sets the crypto object to be passed in when authenticating with fingerprint.
     */
    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        mCryptoObject = cryptoObject;
    }

    @Override
    public void onAuthenticated() {
        dismiss();
        mActivity.finish();
    }

    @Override
    public void onError() {
        Log_OC.d(TAG, "Failed fingerprint authentication");
    }
}
