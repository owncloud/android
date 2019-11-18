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

package com.owncloud.android.sharing.shares.ui

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
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.presentation.ui.sharing.fragments.PublicShareDialogFragment
import com.owncloud.android.utils.AppTestUtil
import io.mockk.every
import io.mockk.mockkClass
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class PublicShareEditionDialogFragmentTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(TestShareFileActivity::class.java, true, true)

    private val file = mockkClass(OCFile::class)
    private val publicShare = mockkClass(OCShareEntity::class)
    private val expirationDate = 1556575200000 // GMT: Monday, April 29, 2019 10:00:00 PM

    @Before
    fun setUp() {
        every { file.mimetype } returns ".txt"
        every { file.remotePath } returns "/Documents/doc3"
        every { file.isFolder } returns false

        every { publicShare.name } returns "Docs link"
        every { publicShare.permissions } returns RemoteShare.CREATE_PERMISSION_FLAG
        every { publicShare.isPasswordProtected } returns true
        every { publicShare.expirationDate } returns expirationDate
        every { publicShare.isFolder } returns false

        val publicShareDialogFragment = PublicShareDialogFragment.newInstanceToUpdate(
            file,
            publicShare
        )

        activityRule.activity.capabilities = AppTestUtil.createCapability()
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
        val calendar = GregorianCalendar()
        calendar.timeInMillis = expirationDate

        val formatter: DateFormat = SimpleDateFormat("MMM dd, yyyy");
        formatter.timeZone = TimeZone.getDefault();

        val time = formatter.format(calendar.time);

        onView(withId(R.id.shareViaLinkExpirationLabel)).check(
            matches(withText(R.string.share_via_link_expiration_date_label))
        )
        onView(withId(R.id.shareViaLinkExpirationSwitch))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        onView(withId(R.id.shareViaLinkExpirationValue))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        onView(withId(R.id.shareViaLinkExpirationValue))
            .check(matches(withText(time)))
    }
}
