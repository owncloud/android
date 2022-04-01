/**
 * ownCloud Android client application
 *
 * @author David Crespo Ríos
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.owncloud.android.MainApp.Companion.versionCode
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.datamodel.ReleaseNote
import com.owncloud.android.features.ReleaseNotesList

class ReleaseNotesViewModel(
    private val preferencesProvider: SharedPreferencesProvider
) : ViewModel() {
    private val KEY_LAST_SEEN_VERSION_CODE = "lastSeenVersionCode"

    fun getReleaseNotes(): List<ReleaseNote> {
        return ReleaseNotesList.getReleaseNotes()
    }

    fun updateVersionCode() {
        preferencesProvider.putInt(KEY_LAST_SEEN_VERSION_CODE, versionCode)
    }
}