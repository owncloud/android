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

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.extensions.goToUrl
import com.owncloud.android.extensions.sendEmail
import com.owncloud.android.presentation.ui.settings.PrivacyPolicyActivity
import com.owncloud.android.presentation.viewmodels.settings.SettingsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsMoreFragment : PreferenceFragmentCompat() {

    // ViewModel
    private val settingsViewModel by viewModel<SettingsViewModel>()

    private var moreScreen: PreferenceScreen? = null
    private var prefHelp: Preference? = null
    private var prefSync: Preference? = null
    private var prefRecommend: Preference? = null
    private var prefFeedback: Preference? = null
    private var prefPrivacyPolicy: Preference? = null
    private var prefImprint: Preference? = null
    private var prefAboutApp: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_more, rootKey)

        moreScreen = findPreference(SCREEN_MORE)
        prefHelp = findPreference(PREFERENCE_HELP)
        prefSync = findPreference(PREFERENCE_SYNC_CALENDAR_CONTACTS)
        prefRecommend = findPreference(PREFERENCE_RECOMMEND)
        prefFeedback = findPreference(PREFERENCE_FEEDBACK)
        prefPrivacyPolicy = findPreference(PREFERENCE_PRIVACY_POLICY)
        prefImprint = findPreference(PREFERENCE_IMPRINT)
        prefAboutApp = findPreference(PREFERENCE_ABOUT_APP)

        // Help
        if (settingsViewModel.isHelpEnabled()) {
            prefHelp?.setOnPreferenceClickListener {
                val helpUrl = settingsViewModel.getHelpUrl()
                requireActivity().goToUrl(helpUrl)
                true
            }
        } else {
            moreScreen?.removePreference(prefHelp)
        }

        // Sync contacts, calendars and tasks
        if (settingsViewModel.isSyncEnabled()) {
            prefSync?.setOnPreferenceClickListener {
                val syncUrl = settingsViewModel.getSyncUrl()
                requireActivity().goToUrl(syncUrl)
                true
            }
        } else {
            moreScreen?.removePreference(prefSync)
        }

        // Recommend
        if (settingsViewModel.isRecommendEnabled()) {
            prefRecommend?.setOnPreferenceClickListener {
                val appName = getString(R.string.app_name)
                val downloadUrl = getString(R.string.url_app_download)

                val recommendEmail = getString(R.string.mail_recommend)
                val recommendSubject = String.format(getString(R.string.recommend_subject), appName)
                val recommendText = String.format(getString(R.string.recommend_text), appName, downloadUrl)

                requireActivity().sendEmail(email = recommendEmail, subject = recommendSubject, text = recommendText)
                true
            }
        } else {
            moreScreen?.removePreference(prefRecommend)
        }

        // Feedback
        if (settingsViewModel.isFeedbackEnabled()) {
            prefFeedback?.setOnPreferenceClickListener {
                val feedbackMail = getString(R.string.mail_feedback)
                val feedback = "Android v" + BuildConfig.VERSION_NAME + " - " + getString(R.string.prefs_feedback)

                requireActivity().sendEmail(email = feedbackMail, subject = feedback)
                true
            }
        } else {
            moreScreen?.removePreference(prefFeedback)
        }

        // Privacy policy
        if (settingsViewModel.isPrivacyPolicyEnabled()) {
            prefPrivacyPolicy?.setOnPreferenceClickListener {
                val intent = Intent(context, PrivacyPolicyActivity::class.java)
                startActivity(intent)
                true
            }
        } else {
            moreScreen?.removePreference(prefPrivacyPolicy)
        }

        // Imprint
        if (settingsViewModel.isImprintEnabled()) {
            prefImprint?.setOnPreferenceClickListener {
                val imprintUrl = settingsViewModel.getImprintUrl()
                requireActivity().goToUrl(imprintUrl)
                true
            }
        } else {
            moreScreen?.removePreference(prefImprint)
        }

        // About app
        prefAboutApp?.let {
            it.title = String.format(getString(R.string.about_android), getString(R.string.app_name))
            val appVersion = "${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE} ${BuildConfig.COMMIT_SHA1}"
            it.summary = String.format(getString(R.string.about_version), appVersion)
        }
    }

    companion object {
        private const val SCREEN_MORE = "more_screen"
        private const val PREFERENCE_HELP = "help"
        private const val PREFERENCE_SYNC_CALENDAR_CONTACTS = "syncCalendarContacts"
        private const val PREFERENCE_RECOMMEND = "recommend"
        private const val PREFERENCE_FEEDBACK = "feedback"
        private const val PREFERENCE_PRIVACY_POLICY = "privacyPolicy"
        private const val PREFERENCE_IMPRINT = "imprint"
        private const val PREFERENCE_ABOUT_APP = "about_app"
    }

}
