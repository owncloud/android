/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.shares.ui

import android.accounts.Account
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.capabilities.viewmodel.OCCapabilityViewModel
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.ui.fragment.PublicShareDialogFragment
import com.owncloud.android.shares.viewmodel.OCShareViewModel
import com.owncloud.android.utils.ViewModelUtil
import com.owncloud.android.vo.Resource
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class PublicShareEditionDialogFragmentTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(TestShareFileActivity::class.java, true, true)

    private val account = mock(Account::class.java)
    private val capabilitiesLiveData = MutableLiveData<Resource<OCCapability>>()
    private val sharesLiveData = MutableLiveData<Resource<List<OCShare>>>()
    private val file = mock(OCFile::class.java)
    private val publicShare = mock(OCShare::class.java)

    @Before
    fun setUp() {
        val linkName = "Docs link"

        `when`(publicShare.name).thenReturn(
            linkName
        )

        `when`(publicShare.permissions).thenReturn(
            RemoteShare.CREATE_PERMISSION_FLAG
        )

        `when`(publicShare.isPasswordProtected).thenReturn(
            true
        )

        `when`(publicShare.expirationDate).thenReturn(
            1556575200000
        )

        val publicShareDialogFragment = PublicShareDialogFragment.newInstanceToUpdate(
            file,
            publicShare,
            account
        )

        val filePath = "/Documents/doc3"

        file.mimetype = ".txt"
        `when`(file.remotePath).thenReturn(filePath)

        val ocCapabilityViewModel = mock(OCCapabilityViewModel::class.java)
        `when`(
            ocCapabilityViewModel.getCapabilityForAccount()
        ).thenReturn(capabilitiesLiveData)

        val ocShareViewModel = mock(OCShareViewModel::class.java)
        `when`(
            ocShareViewModel.insertPublicShareForFile(
                1,
                linkName,
                "",
                -1,
                false
            )
        ).thenReturn(sharesLiveData)

        publicShareDialogFragment.ocCapabilityViewModelFactory = ViewModelUtil.createFor(ocCapabilityViewModel)
        publicShareDialogFragment.ocShareViewModelFactory = ViewModelUtil.createFor(ocShareViewModel)

        activityRule.activity.setFragment(publicShareDialogFragment)
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
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).check(matches(isChecked()));
    }

    @Test
    fun checkPasswordSet() {
        onView(withId(R.id.shareViaLinkPasswordLabel)).check(matches(withText(R.string.share_via_link_password_label)))
        onView(withId(R.id.shareViaLinkPasswordSwitch))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        onView(withId(R.id.shareViaLinkPasswordValue))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        onView(withId(R.id.shareViaLinkPasswordValue))
            .check(matches(withHint(R.string.share_via_link_default_password)))
    }

    @Test
    fun checkExpirationDateSet() {
        onView(withId(R.id.shareViaLinkExpirationLabel)).check(
            matches(withText(R.string.share_via_link_expiration_date_label))
        )
        onView(withId(R.id.shareViaLinkExpirationSwitch))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        onView(withId(R.id.shareViaLinkExpirationValue))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        onView(withId(R.id.shareViaLinkExpirationValue))
            .check(matches(withText("Apr 30, 2019")))
    }
}
