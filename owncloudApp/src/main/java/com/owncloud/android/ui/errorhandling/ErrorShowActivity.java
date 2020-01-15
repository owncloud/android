/**
 * ownCloud Android client application
 *
 * @author LukeOwncloud
 * @author Christian Schabesberger
 * @author David González Verdugo
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
package com.owncloud.android.ui.errorhandling;

import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.utils.PreferenceUtils;
import timber.log.Timber;

public class ErrorShowActivity extends BaseActivity {

    TextView mError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.e("ErrorShowActivity was called. See above for StackTrace.");
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        setContentView(R.layout.errorhandling_showerror);

        // Allow or disallow touches with other visible windows
        ScrollView errorHandlingShowErrorScrollView = findViewById(R.id.errorHandlingShowErrorScrollView);
        errorHandlingShowErrorScrollView.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        );

        mError = findViewById(R.id.errorTextView);
        mError.setText(getIntent().getStringExtra("error"));
    }
}