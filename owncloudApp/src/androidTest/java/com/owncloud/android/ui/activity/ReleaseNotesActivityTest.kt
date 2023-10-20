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

package com.owncloud.android.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.owncloud.android.R
import com.owncloud.android.presentation.releasenotes.ReleaseNotesActivity
import com.owncloud.android.presentation.releasenotes.ReleaseNotesViewModel
import com.owncloud.android.utils.click
import com.owncloud.android.utils.matchers.assertChildCount
import com.owncloud.android.utils.matchers.isDisplayed
import com.owncloud.android.utils.matchers.withText
import com.owncloud.android.utils.releaseNotesList
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
        releaseNotesViewModel = mockk(relaxed = true)

        stopKoin()

        startKoin {
            context
            allowOverride(override = true)
            modules(
                module {
                    viewModel {
                        releaseNotesViewModel
                    }
                }
            )
        }

        every { releaseNotesViewModel.getReleaseNotes() } returns releaseNotesList

        val intent = Intent(context, ReleaseNotesActivity::class.java)
        activityScenario = ActivityScenario.launch(intent)
    }

    @Test
    fun releaseNotesView() {
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
        R.id.btnProceed.click()

        assertEquals(activityScenario.result.resultCode, Activity.RESULT_OK)
    }

    @Test
    fun test_childCount() {
        R.id.releaseNotes.assertChildCount(3)
    }
}
