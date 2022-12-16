/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * Copyright (C) 2021 ownCloud GmbH.
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
 *
 */

package com.owncloud.android.logging

import androidx.test.core.app.ActivityScenario
import com.owncloud.android.R
import com.owncloud.android.presentation.logging.LogsListActivity
import com.owncloud.android.presentation.logging.LogListViewModel
import com.owncloud.android.utils.matchers.assertChildCount
import com.owncloud.android.utils.matchers.isDisplayed
import com.owncloud.android.utils.matchers.withText
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File

@Ignore
class LogsListActivityTest {

    private lateinit var activityScenario: ActivityScenario<LogsListActivity>

    private lateinit var logListViewModel: LogListViewModel

    private fun launchTest(logs: List<File>) {
        every { logListViewModel.getLogsFiles() } returns logs
        activityScenario = ActivityScenario.launch(LogsListActivity::class.java)
    }

    @Before
    fun setUp() {
        logListViewModel = mockk(relaxed = true)

        stopKoin()

        startKoin {
            allowOverride(override = true)
            modules(
                module {
                    viewModel {
                        logListViewModel
                    }
                }
            )

        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun test_visibility_toolbar() {
        launchTest(logs = emptyList())
        R.id.toolbar_activity_logs_list.isDisplayed(true)
    }

    @Test
    fun test_isRecyclerViewEmpty_show_label() {
        launchTest(logs = emptyList())
        R.id.logs_list_empty.isDisplayed(true)
        R.id.list_empty_dataset_title.withText(R.string.prefs_log_no_logs_list_view)
        R.id.list_empty_dataset_sub_title.withText(R.string.prefs_log_empty_subtitle)
        R.id.recyclerView_activity_logs_list.isDisplayed(false)
    }

    @Test
    fun test_isRecyclerViewNotEmpty_hide_label() {
        launchTest(logs = listOf(File("path")))
        R.id.logs_list_empty.isDisplayed(false)
        R.id.recyclerView_activity_logs_list.isDisplayed(true)
    }

    @Test
    fun test_childCount() {
        launchTest(logs = listOf(File("owncloud.2021-01.01.log"), File("owncloud.2021-01-02.log")))
        R.id.recyclerView_activity_logs_list.assertChildCount(2)
    }
}
