/**
 * ownCloud Android client application
 *
 * @author David Crespo Ríos
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.presentation.settings.advanced

import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.owncloud.android.R
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsAdvancedFragment : PreferenceFragmentCompat() {

    // ViewModel
    private val advancedViewModel by viewModel<SettingsAdvancedViewModel>()

    private var prefShowHiddenFiles: SwitchPreferenceCompat? = null
    private var prefDeleteLocalFiles: ListPreference? = null
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_advanced, rootKey)

        prefShowHiddenFiles = findPreference(PREF_SHOW_HIDDEN_FILES)
        prefDeleteLocalFiles = findPreference<ListPreference>(PREFERENCE_DELETE_LOCAL_FILES)?.apply {
            entries = listOf(
                getString(R.string.prefs_delete_local_files_entries_never),
                getString(R.string.prefs_delete_local_files_entries_1hour),
                getString(R.string.prefs_delete_local_files_entries_12hours),
                getString(R.string.prefs_delete_local_files_entries_24hours),
                getString(R.string.prefs_delete_local_files_entries_30days)
            ).toTypedArray()
            entryValues = listOf(
                DeleteLocalFiles.NEVER.name,
                DeleteLocalFiles.ONE_HOUR.name,
                DeleteLocalFiles.TWELVE_HOURS.name,
                DeleteLocalFiles.TWENTY_FOUR_HOURS.name,
                DeleteLocalFiles.THIRTY_DAYS.name
            ).toTypedArray()
        }
        initPreferenceListeners()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefShowHiddenFiles?.isChecked = advancedViewModel.isHiddenFilesShown()
    }

    private fun initPreferenceListeners() {
        prefShowHiddenFiles?.setOnPreferenceChangeListener { _: Preference?, newValue: Any ->
            advancedViewModel.setShowHiddenFiles(newValue as Boolean)
            true
        }

        prefDeleteLocalFiles?.setOnPreferenceChangeListener { _: Preference?, newValue: Any ->
            advancedViewModel.scheduleDeleteLocalFiles(newValue as String)
            true
        }
    }

    companion object {
        const val PREF_SHOW_HIDDEN_FILES = "show_hidden_files"
    }
}
