/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.sharing.sharees.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.owncloud.android.R
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.ui.sharing.fragments.SearchShareesFragment
import com.owncloud.android.presentation.viewmodels.sharing.OCShareViewModel
import com.owncloud.android.sharing.shares.ui.TestShareFileActivity
import com.owncloud.android.testutil.OC_SHARE
import io.mockk.every
import io.mockk.mockkClass
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class SearchShareesFragmentTest {
    private val ocShareViewModel = mockkClass(OCShareViewModel::class, relaxed = true)
    private val sharesLiveData = MutableLiveData<Event<UIResult<List<OCShare>>>>()

    @Before
    fun init() {
        every { ocShareViewModel.shares } returns sharesLiveData

        stopKoin()

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext<Context>())
            modules(
                module(override = true) {
                    viewModel {
                        ocShareViewModel
                    }
                }
            )
        }

        ActivityScenario.launch(TestShareFileActivity::class.java).onActivity {
            val searchShareesFragment = SearchShareesFragment()
            it.startFragment(searchShareesFragment)
        }
    }

    @Test
    fun showSearchBar() {
        onView(withId(R.id.search_mag_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.search_plate)).check(matches(isDisplayed()))
    }

    @Test
    fun showUserShares() {
        sharesLiveData.postValue(
            Event(
                UIResult.Success(
                    listOf(
                        OC_SHARE.copy(sharedWithDisplayName = "Sheldon"),
                        OC_SHARE.copy(sharedWithDisplayName = "Penny")
                    )
                )
            )
        )

        onView(withText("Sheldon"))
            .check(matches(isDisplayed()))
            .check(matches(hasSibling(withId(R.id.unshareButton))))
            .check(matches(hasSibling(withId(R.id.editShareButton))))
        onView(withText("Penny")).check(matches(isDisplayed()))
    }

    @Test
    fun showGroupShares() {
        sharesLiveData.postValue(
            Event(
                UIResult.Success(
                    listOf(
                        OC_SHARE.copy(
                            shareType = ShareType.GROUP,
                            sharedWithDisplayName = "Friends"
                        )
                    )
                )
            )
        )

        onView(withText("Friends (group)"))
            .check(matches(isDisplayed()))
            .check(matches(hasSibling(withId(R.id.icon))))
        onView(ViewMatchers.withTagValue(CoreMatchers.equalTo(R.drawable.ic_group))).check(matches(isDisplayed()))
    }
}
