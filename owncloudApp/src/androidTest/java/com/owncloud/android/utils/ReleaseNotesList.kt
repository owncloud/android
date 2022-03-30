package com.owncloud.android.testutil.ui

import com.owncloud.android.R
import com.owncloud.android.enums.ReleaseNoteType

val releaseNotesList = listOf(
    com.owncloud.android.datamodel.ReleaseNote(
        title = R.string.release_note_account_list_title,
        subtitle = R.string.release_note_account_list_subtitle,
        type = ReleaseNoteType.BUGFIX
    ),
    com.owncloud.android.datamodel.ReleaseNote(
        title = R.string.release_note_account_biometrical_unlock_title,
        subtitle = R.string.release_note_account_biometrical_unlock_subtitle,
        type = ReleaseNoteType.BUGFIX
    ),
    com.owncloud.android.datamodel.ReleaseNote(
        title = R.string.release_note_account_list_title,
        subtitle = R.string.release_note_account_list_subtitle,
        type = ReleaseNoteType.ENHANCEMENT
    ),
    com.owncloud.android.datamodel.ReleaseNote(
        title = R.string.release_note_account_biometrical_unlock_title,
        subtitle = R.string.release_note_account_biometrical_unlock_subtitle,
        type = ReleaseNoteType.ENHANCEMENT
    ),
    com.owncloud.android.datamodel.ReleaseNote(
        title = R.string.release_note_account_list_title,
        subtitle = R.string.release_note_account_list_subtitle,
        type = ReleaseNoteType.SECURITY
    ),
    com.owncloud.android.datamodel.ReleaseNote(
        title = R.string.release_note_account_biometrical_unlock_title,
        subtitle = R.string.release_note_account_biometrical_unlock_subtitle,
        type = ReleaseNoteType.CHANGE
    )
)