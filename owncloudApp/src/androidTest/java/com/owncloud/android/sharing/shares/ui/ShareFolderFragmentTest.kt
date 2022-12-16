/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Jesus Recio Rincon
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

package com.owncloud.android.sharing.shares.ui

import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.owncloud.android.R
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.sharing.ShareFileFragment
import com.owncloud.android.presentation.capabilities.CapabilityViewModel
import com.owncloud.android.presentation.sharing.ShareViewModel
import com.owncloud.android.testutil.OC_ACCOUNT
import com.owncloud.android.testutil.OC_CAPABILITY
import com.owncloud.android.testutil.OC_FOLDER
import com.owncloud.android.testutil.OC_SHARE
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ShareFolderFragmentTest {
    private val capabilityViewModel = mockk<CapabilityViewModel>(relaxed = true)
    private val capabilitiesLiveData = MutableLiveData<Event<UIResult<OCCapability>>>()
    private val shareViewModel = mockk<ShareViewModel>(relaxed = true)
    private val sharesLiveData = MutableLiveData<Event<UIResult<List<OCShare>>>>()

    @Before
    fun setUp() {
        every { capabilityViewModel.capabilities } returns capabilitiesLiveData
        every { shareViewModel.shares } returns sharesLiveData

        stopKoin()

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            allowOverride(override = true)
            modules(
                module {
                    viewModel {
                        capabilityViewModel
                    }
                    viewModel {
                        shareViewModel
                    }
                }
            )
        }

        val shareFileFragment = ShareFileFragment.newInstance(
            OC_FOLDER.copy(privateLink = null),
            OC_ACCOUNT
        )

        ActivityScenario.launch(TestShareFileActivity::class.java).onActivity {
            it.startFragment(shareFileFragment)
        }

        capabilitiesLiveData.postValue(Event(UIResult.Success(OC_CAPABILITY)))

        sharesLiveData.postValue(Event(UIResult.Success(listOf(OC_SHARE))))
    }

    @Test
    fun folderSizeVisible() {
        onView(withId(R.id.shareFileSize)).check(matches(not(isDisplayed())))
    }

    @Test
    fun hidePrivateLink() {
        onView(withId(R.id.getPrivateLinkButton)).check(matches(not(isDisplayed())))
    }
}
