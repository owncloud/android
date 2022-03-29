package com.owncloud.android.presentation.viewmodels.ui

import com.owncloud.android.datamodel.ReleaseNote
import com.owncloud.android.features.ReleaseNotesList
import com.owncloud.android.presentation.viewmodels.ViewModelTest
import com.owncloud.android.ui.viewmodels.ReleaseNotesViewModel
import org.junit.Assert.assertEquals

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ReleaseNotesViewModelTest : ViewModelTest() {
    private lateinit var releaseNotesViewModel: ReleaseNotesViewModel

    @Before
    fun setUp() {
        releaseNotesViewModel = ReleaseNotesViewModel()
    }

    @Test
    fun `get release notes - ok`() {
        val notes = releaseNotesViewModel.getReleaseNotes()

        assertEquals(listOf<ReleaseNote>(), notes)
        assertEquals(
            ReleaseNotesList().getReleaseNotes().size,
            notes.size
        )
    }
}