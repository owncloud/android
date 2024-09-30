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
import com.owncloud.android.data.providers.SharedPreferencesProvider
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
            ReleaseNote(
                title = R.string.release_notes_4_4_0_title_improved_from_original_folder_auto_upload,
                subtitle = R.string.release_notes_4_4_0_subtitle_improved_from_original_folder_auto_upload,
                type = ReleaseNoteType.ENHANCEMENT
            ),
            ReleaseNote(
                title = R.string.release_notes_4_4_0_title_resharing_capability,
                subtitle = R.string.release_notes_4_4_0_subtitle_resharing_capability,
                type = ReleaseNoteType.ENHANCEMENT
            ),
            ReleaseNote(
                title = R.string.release_notes_4_4_6_4_bugfixes_title,
                subtitle = R.string.release_notes_4_4_6_4_bugfixes_subtitle,
                type = ReleaseNoteType.BUGFIX
            ),
            ReleaseNote(
                title = R.string.release_notes_4_4_0_title_audio_player_android14,
                subtitle = R.string.release_notes_4_4_0_subtitle_audio_player_android14,
                type = ReleaseNoteType.BUGFIX
            ),
            ReleaseNote(
                title = R.string.release_notes_4_3_0_title_accessibility_improvements,
                subtitle = R.string.release_notes_4_3_0_subtitle_accessibility_improvements,
                type = ReleaseNoteType.ENHANCEMENT
            ),
            ReleaseNote(
                title = R.string.release_notes_bugfixes_title,
                subtitle = R.string.release_notes_bugfixes_subtitle,
                type = ReleaseNoteType.BUGFIX
            ),
        )
    }
}
