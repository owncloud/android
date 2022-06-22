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

package com.owncloud.android.presentation.ui.settings.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.extensions.goToUrl
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.presentation.ui.releasenotes.ReleaseNotesActivity
import com.owncloud.android.presentation.ui.settings.PrivacyPolicyActivity
import com.owncloud.android.presentation.viewmodels.releasenotes.ReleaseNotesViewModel
import com.owncloud.android.presentation.viewmodels.settings.SettingsMoreViewModel
import com.owncloud.android.presentation.viewmodels.settings.SettingsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : PreferenceFragmentCompat() {

    // ViewModel
    private val settingsViewModel by viewModel<SettingsViewModel>()
    private val moreViewModel by viewModel<SettingsMoreViewModel>()
    private val releaseNotesViewModel by viewModel<ReleaseNotesViewModel>()

    private var settingsScreen: PreferenceScreen? = null
    private var subsectionPictureUploads: Preference? = null
    private var subsectionVideoUploads: Preference? = null
    private var subsectionMore: Preference? = null
    private var prefPrivacyPolicy: Preference? = null
    private var subsectionWhatsNew: Preference? = null
    private var prefAboutApp: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        settingsScreen = findPreference(SCREEN_SETTINGS)
        subsectionPictureUploads = findPreference(SUBSECTION_PICTURE_UPLOADS)
        subsectionVideoUploads = findPreference(SUBSECTION_VIDEO_UPLOADS)
        subsectionMore = findPreference(SUBSECTION_MORE)
        prefPrivacyPolicy = findPreference(PREFERENCE_PRIVACY_POLICY)
        subsectionWhatsNew = findPreference(SUBSECTION_WHATSNEW)
        prefAboutApp = findPreference(PREFERENCE_ABOUT_APP)

        subsectionPictureUploads?.isVisible = settingsViewModel.isThereAttachedAccount()
        subsectionVideoUploads?.isVisible = settingsViewModel.isThereAttachedAccount()
        subsectionMore?.isVisible = moreViewModel.shouldMoreSectionBeVisible()
        subsectionWhatsNew?.isVisible = releaseNotesViewModel.shouldWhatsNewSectionBeVisible()

        if (moreViewModel.isPrivacyPolicyEnabled()) {
            prefPrivacyPolicy?.setOnPreferenceClickListener {
                val urlPrivacyPolicy = requireContext().resources.getString(R.string.url_privacy_policy)

                val cantBeOpenedWithWebView = urlPrivacyPolicy.endsWith("pdf")
                if (cantBeOpenedWithWebView) {
                    requireActivity().goToUrl(urlPrivacyPolicy)
                } else {
                    val intent = Intent(context, PrivacyPolicyActivity::class.java)
                    startActivity(intent)
                }
                true
            }
        } else {
            settingsScreen?.removePreferenceFromScreen(prefPrivacyPolicy)
        }

        subsectionWhatsNew?.setOnPreferenceClickListener {
            val intent = Intent(context, ReleaseNotesActivity::class.java)
            startActivity(intent)
            true
        }

        subsectionWhatsNew?.setOnPreferenceClickListener {
            val intent = Intent(context, ReleaseNotesActivity::class.java)
            startActivity(intent)
            true
        }

        prefAboutApp?.apply {
            summary = String.format(
                getString(R.string.prefs_app_version_summary),
                getString(R.string.app_name),
                BuildConfig.BUILD_TYPE,
                BuildConfig.VERSION_NAME,
                BuildConfig.COMMIT_SHA1
            )
            setOnPreferenceClickListener {
                val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("ownCloud app version", summary)
                clipboard.setPrimaryClip(clip)
                showMessageInSnackbar(getString(R.string.clipboard_text_copied))
                true
            }
        }
    }

    companion object {
        private const val SCREEN_SETTINGS = "settings_screen"
        private const val PREFERENCE_PRIVACY_POLICY = "privacyPolicy"
        private const val PREFERENCE_ABOUT_APP = "about_app"
        private const val SUBSECTION_PICTURE_UPLOADS = "picture_uploads_subsection"
        private const val SUBSECTION_VIDEO_UPLOADS = "video_uploads_subsection"
        private const val SUBSECTION_MORE = "more_subsection"

        // Remove preference with nullability check
        fun PreferenceScreen?.removePreferenceFromScreen(preference: Preference?) {
            preference?.let { this?.removePreference(it) }
        }

        private const val SUBSECTION_WHATSNEW = "whatsNew"
    }
}
