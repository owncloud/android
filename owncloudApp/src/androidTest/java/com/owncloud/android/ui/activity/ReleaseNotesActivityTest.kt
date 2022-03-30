package com.owncloud.android.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.owncloud.android.R
import com.owncloud.android.testutil.ui.releaseNotesList
import com.owncloud.android.ui.viewmodels.ReleaseNotesViewModel
import com.owncloud.android.utils.click
import com.owncloud.android.utils.matchers.assertChildCount
import com.owncloud.android.utils.matchers.isDisplayed
import com.owncloud.android.utils.matchers.withText
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ReleaseNotesActivityTest {
    private lateinit var activityScenario: ActivityScenario<ReleaseNotesActivity>
    private lateinit var context: Context

    private lateinit var releaseNotesViewModel: ReleaseNotesViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        releaseNotesViewModel = mockk(relaxUnitFun = true)

        stopKoin()

        startKoin {
            context
            modules(
                module(override = true) {
                    viewModel {
                        releaseNotesViewModel
                    }
                }
            )
        }

        every { releaseNotesViewModel.getReleaseNotes() } returns releaseNotesList
    }

    @Test
    fun releaseNotesView() {
        openReleaseNotesActivity()

        val header = String.format(
            context.getString(R.string.release_notes_header),
            context.getString(R.string.app_name)
        )

        val footer = String.format(
            context.getString(R.string.release_notes_footer),
            context.getString(R.string.app_name)
        )

        with(R.id.txtHeader) {
            isDisplayed(true)
            withText(header)
        }

        R.id.releaseNotes.isDisplayed(true)

        with(R.id.txtFooter) {
            isDisplayed(true)
            withText(footer)
        }

        R.id.btnProceed.isDisplayed(true)
    }

    @Test
    fun releaseNotesProceedButton() {
        openReleaseNotesActivity()

        R.id.btnProceed.click()

        assertEquals(activityScenario.result.resultCode, Activity.RESULT_OK)
    }

    @Test
    fun test_childCount() {
        openReleaseNotesActivity()
        R.id.releaseNotes.assertChildCount(6)
    }

    private fun openReleaseNotesActivity() {
        val intent = Intent(context, ReleaseNotesActivity::class.java)
        activityScenario = ActivityScenario.launch(intent)
    }
}