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

    fun getReleaseNotes(): List<ReleaseNote> =
        releaseNotesList

    fun updateVersionCode() {
        preferencesProvider.putInt(MainApp.PREFERENCE_KEY_LAST_SEEN_VERSION_CODE, versionCode)
    }

    fun shouldWhatsNewSectionBeVisible(): Boolean =
        contextProvider.getBoolean(R.bool.release_notes_enabled) && getReleaseNotes().isNotEmpty()

    companion object {
        val releaseNotesList = listOf(
            ReleaseNote(
                title = R.string.release_notes_4_6_1_title_duplicated_automatic_uploads,
                subtitle = R.string.release_notes_4_6_1_subtitle_duplicated_automatic_uploads,
                type = ReleaseNoteType.BUGFIX
            ),
            ReleaseNote(
                title = R.string.release_notes_4_6_1_title_bearer_token_handling,
                subtitle = R.string.release_notes_4_6_1_subtitle_bearer_token_handling,
                type = ReleaseNoteType.BUGFIX
            ),
            ReleaseNote(
                title = R.string.release_notes_4_6_1_title_shares_space_docs_provider,
                subtitle = R.string.release_notes_4_6_1_subtitle_shares_space_docs_provider,
                type = ReleaseNoteType.ENHANCEMENT
            ),
            ReleaseNote(
                title = R.string.release_notes_4_7_0_title_new_layout_for_spaces,
                subtitle = R.string.release_notes_4_7_0_subtitle_new_layout_for_spaces,
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
