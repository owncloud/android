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

package com.owncloud.android.presentation.viewmodels.releasenotes

import androidx.lifecycle.ViewModel
import com.owncloud.android.MainApp
import com.owncloud.android.MainApp.Companion.versionCode
import com.owncloud.android.R
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.presentation.ui.releasenotes.ReleaseNote
import com.owncloud.android.presentation.ui.releasenotes.ReleaseNoteType
import com.owncloud.android.providers.ContextProvider

class ReleaseNotesViewModel(
    private val preferencesProvider: SharedPreferencesProvider,
    private val contextProvider: ContextProvider
) : ViewModel() {

    fun getReleaseNotes(): List<ReleaseNote> {
        return releaseNotesList
    }

    fun updateVersionCode() {
        preferencesProvider.putInt(MainApp.PREFERENCE_KEY_LAST_SEEN_VERSION_CODE, versionCode)
    }

    fun shouldWhatsNewSectionBeVisible(): Boolean {
        return contextProvider.getBoolean(R.bool.release_notes_enabled) && getReleaseNotes().isNotEmpty()
    }

    companion object {
        val releaseNotesList = listOf(
            ReleaseNote(title = R.string.release_notes_title_2_21_1_oem_1, subtitle = R.string.release_notes_subtitle_2_21_1_oem_1, type = ReleaseNoteType.CHANGE),
            ReleaseNote(title = R.string.release_notes_title_2_21_1_oem_2, subtitle = R.string.release_notes_subtitle_2_21_1_oem_2, type = ReleaseNoteType.SECURITY),
            ReleaseNote(title = R.string.release_notes_title_2_21_1_oem_3, subtitle = R.string.release_notes_subtitle_2_21_1_oem_3, type = ReleaseNoteType.ENHANCEMENT),
            ReleaseNote(title = R.string.release_notes_title_2_21_1_oem_4, subtitle = R.string.release_notes_subtitle_2_21_1_oem_4, type = ReleaseNoteType.ENHANCEMENT),
            ReleaseNote(title = R.string.release_notes_title_2_21_1_oem_5, subtitle = R.string.release_notes_subtitle_2_21_1_oem_5, type = ReleaseNoteType.ENHANCEMENT),
            ReleaseNote(title = R.string.release_notes_title_2_21_1_oem_6, subtitle = R.string.release_notes_subtitle_2_21_1_oem_6, type = ReleaseNoteType.ENHANCEMENT),
            ReleaseNote(title = R.string.release_notes_title_2_21_1_oem_7, subtitle = R.string.release_notes_subtitle_2_21_1_oem_7, type = ReleaseNoteType.ENHANCEMENT),
            ReleaseNote(title = R.string.release_notes_title_2_21_1_oem_8, subtitle = R.string.release_notes_subtitle_2_21_1_oem_8, type = ReleaseNoteType.BUGFIX)
        )
    }
}
