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
 * Copyright (C) 2020 ownCloud GmbH.
 */

package com.owncloud.android.authentication;

import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import com.owncloud.android.R;

/**
 * Small helper class to manage text/icon around fingerprint authentication UI.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class FingerprintUIHelper extends FingerprintManager.AuthenticationCallback {

    private static final long ERROR_TIMEOUT_MILLIS = 3000;
    private static final long SUCCESS_DELAY_MILLIS = 1300;

    private final FingerprintManager mFingerprintManager;
    private final ImageView mIcon;
    private final TextView mErrorTextView;
    private final Callback mCallback;
    private CancellationSignal mCancellationSignal;

    private boolean mSelfCancelled;

    /**
     * Constructor for {@link FingerprintUIHelper}.
     */
    public FingerprintUIHelper(FingerprintManager fingerprintManager,
                               ImageView icon, TextView errorTextView, Callback callback) {
        mFingerprintManager = fingerprintManager;
        mIcon = icon;
        mErrorTextView = errorTextView;
        mCallback = callback;
    }

    public void startListening(FingerprintManager.CryptoObject cryptoObject) {
        mCancellationSignal = new CancellationSignal();
        mSelfCancelled = false;
        mFingerprintManager.authenticate(cryptoObject, mCancellationSignal, 0 /* flags */, this,
                null);
    }

    public void stopListening() {
        if (mCancellationSignal != null) {
            mSelfCancelled = true;
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        mErrorTextView.removeCallbacks(mResetErrorTextRunnable);
        mIcon.setImageResource(R.drawable.ic_fingerprint_success);
        mErrorTextView.setTextColor(mErrorTextView.getResources().getColor(R.color.success, null));
        mErrorTextView.setText(mErrorTextView.getResources().getString(R.string.fingerprint_success));
        mIcon.postDelayed(new Runnable() {
            @Override
            public void run() {
                mCallback.onAuthenticated();
            }
        }, SUCCESS_DELAY_MILLIS);
    }

    @Override
    public void onAuthenticationFailed() {
        showError(mIcon.getResources().getString(R.string.fingerprint_not_recognized));
    }

    private void showError(CharSequence error) {
        mIcon.setImageResource(R.drawable.ic_fingerprint_error);
        mErrorTextView.setText(error);
        mErrorTextView.setTextColor(mErrorTextView.getResources().getColor(R.color.warning, null));
        mErrorTextView.removeCallbacks(mResetErrorTextRunnable);
        mErrorTextView.postDelayed(mResetErrorTextRunnable, ERROR_TIMEOUT_MILLIS);
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        if (!mSelfCancelled) {
            showError(errString);
            mIcon.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCallback.onError();
                }
            }, ERROR_TIMEOUT_MILLIS);
        }
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        showError(helpString);
    }

    private Runnable mResetErrorTextRunnable = new Runnable() {
        @Override
        public void run() {
            mErrorTextView.setTextColor(mErrorTextView.getResources().getColor(R.color.grey, null));
            mErrorTextView.setText(mErrorTextView.getResources().getString(R.string.fingerprint_hint));
            mIcon.setImageResource(R.drawable.ic_fp_40px);
        }
    };

    public interface Callback {

        void onAuthenticated();

        void onError();
    }
}