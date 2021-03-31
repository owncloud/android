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

import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.owncloud.android.R
import com.owncloud.android.presentation.viewmodels.settings.SettingsPictureUploadsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsPictureUploadsFragment : PreferenceFragmentCompat() {

    // ViewModel
    private val picturesViewModel by viewModel<SettingsPictureUploadsViewModel>()

    private var prefEnablePictureUploads: SwitchPreferenceCompat? = null
    private var prefPictureUploadsPath: Preference? = null
    private var prefPictureUploadsOnWifi: CheckBoxPreference? = null
    private var prefPictureUploadsSourcePath: Preference? = null
    private var prefPictureUploadsBehaviour: ListPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_picture_uploads, rootKey)
    }

}
