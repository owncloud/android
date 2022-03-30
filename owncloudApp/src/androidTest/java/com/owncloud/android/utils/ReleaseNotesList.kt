package com.owncloud.android.testutil.ui

import com.owncloud.android.R
import com.owncloud.android.datamodel.ReleaseNote
import com.owncloud.android.enums.ReleaseNoteType

val releaseNotesList = listOf(
    ReleaseNote(
        title = R.string.release_note_account_list_title,
        subtitle = R.string.release_note_account_list_subtitle,
        type = ReleaseNoteType.BUGFIX
    ),
    ReleaseNote(
        title = R.string.release_note_account_biometrical_unlock_title,
        subtitle = R.string.release_note_account_biometrical_unlock_subtitle,
        type = ReleaseNoteType.BUGFIX
    ),
    ReleaseNote(
        title = R.string.release_note_account_list_title,
        subtitle = R.string.release_note_account_list_subtitle,
        type = ReleaseNoteType.ENHANCEMENT
    )
)