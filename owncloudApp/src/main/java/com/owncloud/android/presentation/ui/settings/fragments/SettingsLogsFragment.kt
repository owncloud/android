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

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.owncloud.android.R
import com.owncloud.android.presentation.viewmodels.settings.SettingsLogsViewModel
import com.owncloud.android.presentation.ui.settings.LogHistoryActivity
import com.owncloud.android.utils.PermissionUtil
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsLogsFragment : PreferenceFragmentCompat() {

    // ViewModel
    private val logsViewModel by viewModel<SettingsLogsViewModel>()

    private var prefEnableLogging: SwitchPreferenceCompat? = null
    private var prefHttpLogs: CheckBoxPreference? = null
    private var prefLogsView: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_logs, rootKey)

        prefEnableLogging = findPreference(PREFERENCE_ENABLE_LOGGING)
        prefHttpLogs = findPreference(PREFERENCE_LOG_HTTP)
        prefLogsView = findPreference(PREFERENCE_LOGGER)

        if (PermissionUtil.isPermissionNotGranted(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionUtil.requestWriteExternalStoreagePermission(requireActivity())
        }

        with(logsViewModel.isLoggingEnabled()) {
            prefHttpLogs?.isEnabled = this
            prefLogsView?.isEnabled = this
        }

        prefEnableLogging?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any ->
            val value = newValue as Boolean
            logsViewModel.setEnableLogging(value)

            prefHttpLogs?.isEnabled = value
            prefLogsView?.isEnabled = value
            if (!value) {
                // Disable http logs when global logs are disabled.
                logsViewModel.shouldLogHttpRequests(value)
                prefHttpLogs?.isChecked = false
            }
            true
        }

        prefHttpLogs?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any ->
            logsViewModel.shouldLogHttpRequests(newValue as Boolean)
            true
        }

        prefLogsView?.let {
            it.setOnPreferenceClickListener {
                val intent = Intent(context, LogHistoryActivity::class.java)
                startActivity(intent)
                true
            }
        }
    }

    companion object {
        const val PREFERENCE_ENABLE_LOGGING = "enable_logging"
        const val PREFERENCE_LOG_HTTP = "set_httpLogs"
        const val PREFERENCE_LOGGER = "logger"
    }

}
