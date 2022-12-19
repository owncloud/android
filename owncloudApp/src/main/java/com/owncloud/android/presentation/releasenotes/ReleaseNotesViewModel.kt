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

package com.owncloud.android.presentation.releasenotes

import androidx.lifecycle.ViewModel
import com.owncloud.android.MainApp
import com.owncloud.android.MainApp.Companion.versionCode
import com.owncloud.android.R
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
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
            ReleaseNote(R.string.release_notes_3_0_title1, R.string.release_notes_3_0_subtitle1, ReleaseNoteType.ENHANCEMENT),
            ReleaseNote(R.string.release_notes_3_0_title2, R.string.release_notes_3_0_subtitle2, ReleaseNoteType.ENHANCEMENT),
            ReleaseNote(R.string.release_notes_3_0_title3, R.string.release_notes_3_0_subtitle3, ReleaseNoteType.BUGFIX),
            ReleaseNote(R.string.release_notes_3_0_title4, R.string.release_notes_3_0_subtitle4, ReleaseNoteType.ENHANCEMENT),
            ReleaseNote(R.string.release_notes_3_0_title5, R.string.release_notes_3_0_subtitle5, ReleaseNoteType.ENHANCEMENT),
            ReleaseNote(R.string.release_notes_3_0_title6, R.string.release_notes_3_0_subtitle6, ReleaseNoteType.ENHANCEMENT)
        )
    }
}
