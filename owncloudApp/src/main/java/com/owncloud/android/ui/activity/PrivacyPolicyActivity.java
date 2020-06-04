/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
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

package com.owncloud.android.ui.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.R;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PreferenceUtils;

/**
 * Activity to show the privacy policy to the user
 */
public class PrivacyPolicyActivity extends ToolbarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_privacy_policy);

        setupToolbar();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(getText(R.string.actionbar_privacy_policy));

        // Display the progress in a progress bar, like the browser app does.
        final ProgressBar mProgressBar = findViewById(R.id.syncProgressBar);

        // Allow or disallow touches with other visible windows
        LinearLayout activityPrivacyPolicyLayout = findViewById(R.id.activityPrivacyPolicyLayout);
        activityPrivacyPolicyLayout.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        );

        WebView webview = findViewById(R.id.privacyPolicyWebview);
        webview.getSettings().setJavaScriptEnabled(true);

        // next two settings grant that non-responsive webs are zoomed out when loaded
        webview.getSettings().setUseWideViewPort(true);
        webview.getSettings().setLoadWithOverviewMode(true);

        //Enable zoom but hide display zoom controls
        webview.getSettings().setBuiltInZoomControls(true);
        webview.getSettings().setDisplayZoomControls(false);

        webview.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                mProgressBar.setProgress(progress); //Set the web page loading progress

                if (progress == 100) {
                    mProgressBar.setVisibility(View.GONE);
                }
            }
        });
        webview.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

                Snackbar snackbar = Snackbar.make(
                        findViewById(android.R.id.content),
                        getString(R.string.privacy_policy_error) + description,
                        Snackbar.LENGTH_LONG
                );
                snackbar.show();
            }
        });

        String urlPrivacyPolicy = getResources().getString(R.string.url_privacy_policy);
        webview.loadUrl(urlPrivacyPolicy);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }
}
