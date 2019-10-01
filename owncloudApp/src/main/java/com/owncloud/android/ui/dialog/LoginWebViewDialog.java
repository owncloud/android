/**
 * ownCloud Android client application
 *
 * @author Maria Asensio
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

package com.owncloud.android.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.BaseWebViewClient;
import com.owncloud.android.authentication.SAMLWebViewClient;
import com.owncloud.android.authentication.SAMLWebViewClient.SsoWebViewClientListener;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.AuthenticationMethod;
import com.owncloud.android.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

import static com.owncloud.android.operations.AuthenticationMethod.SAML_WEB_SSO;

/**
 * Dialog to show the WebView for SAML authentication
 */
public class LoginWebViewDialog extends DialogFragment {

    private final static String TAG = LoginWebViewDialog.class.getSimpleName();

    private static final String ARG_INITIAL_URL = "INITIAL_URL";
    private static final String ARG_TARGET_URLS = "TARGET_URLS";
    private static final String ARG_AUTHENTICATION_METHOD = "AUTHENTICATION_METHOD";

    private WebView mWebView;
    private BaseWebViewClient mWebViewClient;

    private String mInitialUrl;
    private List<String> mTargetUrls;

    private SsoWebViewClientListener mSsoWebViewClientListener;

    /**
     * Public factory method to get dialog instances.
     *
     * @param url           Url to open at WebView.
     * @param targetUrls     Url signaling the success of the authentication, when loaded.
     * @return New dialog instance, ready to show.
     */
    public static LoginWebViewDialog newInstance(
            String url,
            ArrayList<String> targetUrls,
            AuthenticationMethod authenticationMethod
    ) {
        if (SAML_WEB_SSO != authenticationMethod) {
            throw new IllegalArgumentException(
                    "Only SAML_WEB_SSO authentication method is supported"
            );
        }
        LoginWebViewDialog fragment = new LoginWebViewDialog();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_URL, url);
        args.putStringArrayList(ARG_TARGET_URLS, targetUrls);
        args.putInt(ARG_AUTHENTICATION_METHOD, authenticationMethod.getValue());
        fragment.setArguments(args);
        return fragment;
    }

    public LoginWebViewDialog() {
        super();
    }

    @Override
    public void onAttach(Activity activity) {
        Log_OC.v(TAG, "onAttach");
        super.onAttach(activity);
        try {
            AuthenticationMethod authenticationMethod = AuthenticationMethod.fromValue(
                    getArguments().getInt(ARG_AUTHENTICATION_METHOD)
            );
            if (authenticationMethod == null) {
                throw new IllegalStateException("Null authentication method got to onAttach");
            }
            Handler handler = new Handler();

            if (SAML_WEB_SSO == authenticationMethod) {
                mSsoWebViewClientListener = (SsoWebViewClientListener) activity;
                mWebViewClient = new SAMLWebViewClient(activity, handler, mSsoWebViewClientListener);
            } else {
                throw new IllegalStateException("Invalid authentication method got to onAttach");
            }

        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " +
                    SsoWebViewClientListener.class.getSimpleName()
            );
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate, savedInstanceState is " + savedInstanceState);
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(getActivity().getApplicationContext());
        }

        mInitialUrl = getArguments().getString(ARG_INITIAL_URL);
        mTargetUrls = getArguments().getStringArrayList(ARG_TARGET_URLS);

        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreateView, savedInsanceState is " + savedInstanceState);

        // Inflate layout of the dialog  
        RelativeLayout ssoRootView = (RelativeLayout) inflater.inflate(R.layout.webview_dialog,
                container, false);  // null parent view because it will go in the dialog layout

        // Allow or disallow touches with other visible windows
        ssoRootView.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
        );

        if (mWebView == null) {
            // initialize the WebView
            mWebView = new SsoWebView(getActivity().getApplicationContext());
            mWebView.setFocusable(true);
            mWebView.setFocusableInTouchMode(true);
            mWebView.setClickable(true);

            WebSettings webSettings = mWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setSavePassword(false);
            webSettings.setUserAgentString(MainApp.Companion.getUserAgent());
            webSettings.setSaveFormData(false);
            // next two settings grant that non-responsive webs are zoomed out when loaded
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);
            // next three settings allow the user use pinch gesture to zoom in / out
            webSettings.setSupportZoom(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
            webSettings.setAllowFileAccess(false);

            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.removeAllCookie();

            mWebView.loadUrl(mInitialUrl);
        }

        mWebViewClient.addTargetUrls(mTargetUrls);
        mWebView.setWebViewClient(mWebViewClient);

        // add the webview into the layout
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        ssoRootView.addView(mWebView, layoutParams);
        ssoRootView.requestLayout();

        return ssoRootView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return dialog;
    }

    @Override
    public void onDestroyView() {
        Log_OC.v(TAG, "onDestroyView");

        if (mWebView.getParent() != null) {
            ((ViewGroup) mWebView.getParent()).removeView(mWebView);
        }

        mWebView.setWebViewClient(null);

        // Work around bug: http://code.google.com/p/android/issues/detail?id=17423
        Dialog dialog = getDialog();
        if ((dialog != null)) {
            dialog.setOnDismissListener(null);
        }

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log_OC.v(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Log_OC.v(TAG, "onDetach");

        mSsoWebViewClientListener = null;
        mWebViewClient = null;
        super.onDetach();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Log_OC.d(TAG, "onCancel");
        super.onCancel(dialog);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log_OC.d(TAG, "onDismiss");
        super.onDismiss(dialog);
    }

    @Override
    public void onStart() {
        Log_OC.v(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onStop() {
        Log_OC.v(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onResume() {
        Log_OC.v(TAG, "onResume");
        super.onResume();
        mWebView.onResume();
    }

    @Override
    public void onPause() {
        Log_OC.v(TAG, "onPause");
        mWebView.onPause();
        super.onPause();
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {
        Log_OC.v(TAG, "show (transaction)");
        return super.show(transaction, tag);
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        Log_OC.v(TAG, "show (manager)");
        super.show(manager, tag);
    }

}
