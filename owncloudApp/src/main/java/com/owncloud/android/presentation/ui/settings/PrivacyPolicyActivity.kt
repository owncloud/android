/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2021 ownCloud GmbH.
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

package com.owncloud.android.presentation.ui.settings

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.owncloud.android.R
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.utils.PreferenceUtils

/**
 * Activity to show the privacy policy to the user
 */
class PrivacyPolicyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        val toolbar = findViewById<Toolbar>(R.id.standard_toolbar).apply {
            setTitle(R.string.actionbar_privacy_policy)
            isVisible = true
        }
        findViewById<ConstraintLayout>(R.id.root_toolbar).isVisible = false

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Display the progress in a progress bar, like the browser app does.
        val progressBar = findViewById<ProgressBar>(R.id.syncProgressBar)

        // Allow or disallow touches with other visible windows
        findViewById<LinearLayout>(R.id.activityPrivacyPolicyLayout).apply {
            filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this@PrivacyPolicyActivity)
        }

        findViewById<WebView>(R.id.privacyPolicyWebview).apply {
            settings.javaScriptEnabled = true
            // Next two settings grant that non-responsive webs are zoomed out when loaded
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            // Enable zoom but hide display zoom controls
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, progress: Int) {
                    progressBar.progress = progress //Set the web page loading progress
                    if (progress == 100) {
                        progressBar.isVisible = false
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                    showMessageInSnackbar(message = getString(R.string.privacy_policy_error) + description)
                }
            }

            val urlPrivacyPolicy = resources.getString(R.string.url_privacy_policy)
            loadUrl(urlPrivacyPolicy)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> retval = super.onOptionsItemSelected(item)
        }
        return retval
    }

}
