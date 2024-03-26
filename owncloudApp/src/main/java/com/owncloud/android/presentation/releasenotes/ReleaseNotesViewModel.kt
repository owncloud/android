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
                title = R.string.release_notes_4_3_0_title_1,
                subtitle = R.string.release_notes_4_3_0_subtitle_1,
                type = ReleaseNoteType.ENHANCEMENT,
            ),
            ReleaseNote(
                title = R.string.release_notes_4_3_0_title_2,
                subtitle = R.string.release_notes_4_3_0_subtitle_2,
                type = ReleaseNoteType.ENHANCEMENT,
            ),
            ReleaseNote(
                title = R.string.release_notes_4_3_0_title_3,
                subtitle = R.string.release_notes_4_3_0_subtitle_3,
                type = ReleaseNoteType.ENHANCEMENT,
            ),
            ReleaseNote(
                title = R.string.release_notes_4_3_0_title_4,
                subtitle = R.string.release_notes_4_3_0_subtitle_4,
                type = ReleaseNoteType.ENHANCEMENT,
            ),
            ReleaseNote(
                title = R.string.release_notes_4_3_0_title_5,
                subtitle = R.string.release_notes_4_3_0_subtitle_5,
                type = ReleaseNoteType.BUGFIX,
            ),
            ReleaseNote(
                title = R.string.release_notes_4_3_0_title_6,
                subtitle = R.string.release_notes_4_3_0_subtitle_6,
                type = ReleaseNoteType.ENHANCEMENT,
            ),
            ReleaseNote(
                title = R.string.release_notes_4_3_0_title_7,
                subtitle = R.string.release_notes_4_3_0_subtitle_7,
                type = ReleaseNoteType.ENHANCEMENT
            ),
            ReleaseNote(
                title = R.string.release_notes_4_3_0_title_retried_successful_uploads_delete_temporary_folder,
                subtitle = R.string.release_notes_4_3_0_subtitle_retried_successful_uploads_delete_temporary_folder,
                type = ReleaseNoteType.BUGFIX,
            ),
            ReleaseNote(
                title = R.string.release_notes_4_3_0_title_manual_removal_local_storage,
                subtitle = R.string.release_notes_4_3_0_subtitle_manual_removal_local_storage,
                type = ReleaseNoteType.ENHANCEMENT,
            ),
            ReleaseNote(
                title = R.string.release_notes_4_3_0_title_clear_data_button_hard_reset,
                subtitle = R.string.release_notes_4_3_0_subtitle_clear_data_button_hard_reset,
                type = ReleaseNoteType.BUGFIX,
            ),
        )
    }
}
