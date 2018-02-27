/**
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   @author masensio
 *   @author David A. Velasco
 *   Copyright (C) 2011 Bartek Przybylski
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
package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.owncloud.android.BuildConfig;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.FingerprintAuthDialogFragment;

import java.util.Arrays;

public class FingerprintActivity extends AppCompatActivity {

    private static final String TAG = FingerprintActivity.class.getSimpleName();

    private static final String TAG_FINGERPRINT_FRAGMENT = "FINGERPRINT_LOCK";

    public final static String PREFERENCE_SET_FINGERPRINT = "set_fingerprint";

    /**
     * Initializes the activity.
     *
     * An intent with a valid ACTION is expected; if none is found, an
     * {@link IllegalArgumentException} will be thrown.
     *
     * @param savedInstanceState    Previously saved state - irrelevant in this case
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FingerprintAuthDialogFragment fingerprintAuthDialogFragment = new FingerprintAuthDialogFragment();

        fingerprintAuthDialogFragment.show(getFragmentManager(), TAG_FINGERPRINT_FRAGMENT);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}