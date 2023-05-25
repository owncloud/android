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

package com.owncloud.android.sharing.shares.ui

import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.owncloud.android.R
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.sharing.shares.PublicShareDialogFragment
import com.owncloud.android.presentation.capabilities.CapabilityViewModel
import com.owncloud.android.presentation.sharing.ShareViewModel
import com.owncloud.android.testutil.OC_ACCOUNT
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_SHARE
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.TimeZone

class PublicShareEditionDialogFragmentTest {
    private val capabilityViewModel = mockk<CapabilityViewModel>(relaxed = true)
    private val capabilitiesLiveData = MutableLiveData<Event<UIResult<OCCapability>>>()
    private val shareViewModel = mockk<ShareViewModel>(relaxed = true)

    private val expirationDate = 1556575200000 // GMT: Monday, April 29, 2019 10:00:00 PM

    @Before
    fun setUp() {
        every { capabilityViewModel.capabilities } returns capabilitiesLiveData

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

        val publicShareDialogFragment = PublicShareDialogFragment.newInstanceToUpdate(
            OC_FILE,
            OC_ACCOUNT,
            OC_SHARE.copy(
                shareType = ShareType.PUBLIC_LINK,
                shareWith = "user",
                name = "Docs link",
                permissions = RemoteShare.CREATE_PERMISSION_FLAG,
                expirationDate = expirationDate,
                isFolder = true
            )
        )

        ActivityScenario.launch(TestShareFileActivity::class.java).onActivity {
            it.startFragment(publicShareDialogFragment)
        }
    }

    @Test
    fun showEditionDialogTitle() {
        onView(withId(R.id.publicShareDialogTitle)).check(matches(withText(R.string.share_via_link_edit_title)))
    }

    @Test
    fun checkLinkNameSet() {
        onView(withText(R.string.share_via_link_name_label)).check(matches(ViewMatchers.isDisplayed()))
        onView(withId(R.id.shareViaLinkNameValue)).check(matches(withText("Docs link")))
    }

    @Test
    fun checkUploadOnly() {
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).check(matches(isChecked()))
    }

    @Test
    fun checkPasswordSet() {
        onView(withId(R.id.shareViaLinkPasswordLabel)).check(matches(withText(R.string.share_via_link_password_label)))
        onView(withId(R.id.shareViaLinkPasswordSwitch)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.shareViaLinkPasswordValue)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.shareViaLinkPasswordValue)).check(matches(withHint(R.string.share_via_link_default_password)))
    }

    @Test
    fun checkExpirationDateSet() {
        val calendar = GregorianCalendar()
        calendar.timeInMillis = expirationDate

        val formatter: DateFormat = SimpleDateFormat.getDateInstance()
        formatter.timeZone = TimeZone.getDefault()

        val time = formatter.format(calendar.time)

        onView(withId(R.id.shareViaLinkExpirationLabel)).check(matches(withText(R.string.share_via_link_expiration_date_label)))
        onView(withId(R.id.shareViaLinkExpirationSwitch)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.shareViaLinkExpirationValue)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.shareViaLinkExpirationValue)).check(matches(withText(time)))
    }
}
