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
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.owncloud.android.R
import com.owncloud.android.presentation.viewmodels.settings.SettingsViewModel
import com.owncloud.android.ui.activity.LogHistoryActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsLogsFragment : PreferenceFragmentCompat() {

    // ViewModel
    private val settingsViewModel by viewModel<SettingsViewModel>()

    private var prefLogsCategory: PreferenceCategory? = null
    private var prefHttpLogs: CheckBoxPreference? = null
    private var prefLogsView: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_logs, rootKey)

        prefLogsCategory = findPreference(PREFERENCE_LOGS_CATEGORY)
        prefHttpLogs = findPreference(PREFERENCE_LOG_HTTP)
        prefLogsView = findPreference(PREFERENCE_LOGGER)

        prefHttpLogs?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any ->
            settingsViewModel.shouldLogHttpRequests(newValue as Boolean)
            true
        }

        prefLogsView?.let {
            it.setOnPreferenceClickListener {
                val intent = Intent(context, LogHistoryActivity::class.java)
                startActivity(intent)
                true
            }

            /*if (settingsViewModel.isDeveloperByClicks()) {
                prefScreen?.addPreference(prefLogsCategory)
            } else if (!settingsViewModel.isDeveloperByMainApp()) {
                prefScreen?.removePreference(prefLogsCategory)
            }*/
        }
    }

    companion object {
        private const val PREFERENCE_SCREEN = "preference_screen"
        private const val PREFERENCE_LOGS_CATEGORY = "logs_category"

        private const val PREFERENCE_LOG_HTTP = "set_httpLogs"
        private const val PREFERENCE_LOGGER = "logger"
    }

}
