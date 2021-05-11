/**
 * ownCloud Android client application
 * <p>
 * Copyright (C) 2020 ownCloud GmbH.
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
package com.owncloud.android.ui.dialog;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.owncloud.android.AppRater;
import com.owncloud.android.R;
import com.owncloud.android.extensions.ActivityExtKt;
import com.owncloud.android.utils.PreferenceUtils;
import timber.log.Timber;

public class RateMeDialog extends DialogFragment {
    private Dialog dialog;

    private static final String ARG_CANCELABLE = RateMeDialog.class.getCanonicalName() + ".ARG_CANCELABLE";
    private static final String APP_PACKAGE_NAME = RateMeDialog.class.getCanonicalName() + ".APP_PACKAGE_NAME";

    private static final String MARKET_DETAILS_URI = "market://details?id=";
    private static final String PLAY_STORE_URI = "http://play.google.com/store/apps/details?id=";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setCancelable(false);
    }

    /**
     * Public factory method to get dialog instances.
     *
     * @param packageName The package name of the application
     * @param cancelable  If 'true', the dialog can be cancelled by the user input (BACK button, touch outside...)
     * @return New dialog instance, ready to show.
     */
    public static RateMeDialog newInstance(String packageName, boolean cancelable) {
        RateMeDialog fragment = new RateMeDialog();
        Bundle args = new Bundle();
        args.putBoolean(ARG_CANCELABLE, cancelable);
        args.putString(APP_PACKAGE_NAME, packageName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create a view by inflating desired layout
        View view = inflater.inflate(R.layout.rate_me_dialog, container, false);

        // Allow or disallow touches with other visible windows
        view.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
        );

        Button rateNowButton = view.findViewById(R.id.button_rate_now);
        Button laterButton = view.findViewById(R.id.button_later);
        Button noThanksButton = view.findViewById(R.id.button_no_thanks);
        TextView titleView = view.findViewById(R.id.rate_me_dialog_title_view);

        titleView.setText(String.format(getString(R.string.rate_dialog_title), getString(R.string.app_name)));

        rateNowButton.setOnClickListener(rateNowButtonView -> {
            Timber.d("Rate now button was pressed");
            String packageName = null;
            if (getArguments() != null) {
                packageName = getArguments().getString(APP_PACKAGE_NAME);
            }
            /// To count with Play market back stack, After pressing back button,
            /// to taken back to our application, we need to add following flags to intent.
            int flags =
                    Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
            try {
                ActivityExtKt.goToUrl(requireActivity(), MARKET_DETAILS_URI + packageName, flags);
            } catch (ActivityNotFoundException e) {
                ActivityExtKt.goToUrl(requireActivity(), PLAY_STORE_URI + packageName, null);
            }
            dialog.dismiss();
        });

        laterButton.setOnClickListener(laterButtonView -> {
            Timber.d("Rate later button was pressed");
            SharedPreferences preferences = getActivity().getSharedPreferences
                    (AppRater.APP_RATER_PREF_TITLE, 0);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(AppRater.APP_RATER_PREF_DATE_NEUTRAL, System.currentTimeMillis());
            editor.apply();
            dialog.dismiss();
        });

        noThanksButton.setOnClickListener(noThanksButtonView -> {
            Timber.d("Button to not show the rate dialog anymore was pressed");
            SharedPreferences preferences = getActivity().getSharedPreferences
                    (AppRater.APP_RATER_PREF_TITLE, 0);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(AppRater.APP_RATER_PREF_DONT_SHOW, true);
            editor.apply();
            dialog.dismiss();
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        /// set cancellation behavior
        boolean cancelable = false;
        if (getArguments() != null) {
            cancelable = getArguments().getBoolean(ARG_CANCELABLE, false);
        }
        dialog.setCancelable(cancelable);
        if (!cancelable) {
            // disable the back button
            DialogInterface.OnKeyListener keyListener = (dialog, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK;
            dialog.setOnKeyListener(keyListener);
        }
        return dialog;
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }
}
