package com.owncloud.android.ui.activity

import androidx.lifecycle.ViewModel
import com.owncloud.android.datamodel.ReleaseNote
import com.owncloud.android.features.ReleaseNotesList

class ReleaseNotesViewModel : ViewModel() {

    fun getReleaseNotes(): List<ReleaseNote> {
        return ReleaseNotesList().getReleaseNotes()
    }
}