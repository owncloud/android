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

package com.owncloud.android.presentation.sharing.sharees.views

import android.accounts.Account
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.presentation.sharing.shares.views.TestShareFileActivity
import com.owncloud.android.presentation.ui.sharing.fragments.SearchShareesFragment
import com.owncloud.android.utils.AppTestUtil
import io.mockk.mockkClass
import org.hamcrest.CoreMatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchShareesFragmentTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(
        TestShareFileActivity::class.java,
        true,
        true
    )

    private var userSharesList = arrayListOf(
        AppTestUtil.createPrivateShare(
            shareType = ShareType.USER.value,
            path = "/Docs",
            isFolder = true,
            shareWith = "sheldon",
            sharedWithDisplayName = "Sheldon"
        ),
        AppTestUtil.createPrivateShare(
            shareType = ShareType.USER.value,
            path = "/Docs",
            isFolder = true,
            shareWith = "penny",
            sharedWithDisplayName = "Penny"
        )
    )

    private var groupSharesList = arrayListOf(
        AppTestUtil.createPrivateShare(
            shareType = ShareType.GROUP.value,
            path = "/Photos",
            isFolder = false,
            shareWith = "friends",
            sharedWithDisplayName = "Friends"
        )
    )

    @Test
    fun showSearchBar() {
        loadSearchShareesFragment()
        onView(withId(R.id.search_mag_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.search_plate)).check(matches(isDisplayed()))
    }

    @Test
    fun showUserShares() {
        loadSearchShareesFragment(privateShares = userSharesList)
        onView(withText("Sheldon")).check(matches(isDisplayed()))
        onView(withText("Sheldon")).check(matches(hasSibling(withId(R.id.unshareButton))))
            .check(matches(isDisplayed()))
        onView(withText("Sheldon")).check(matches(hasSibling(withId(R.id.editShareButton))))
            .check(matches(isDisplayed()))
        onView(withText("Penny")).check(matches(isDisplayed()))
    }

    @Test
    fun showGroupShares() {
        loadSearchShareesFragment(privateShares = groupSharesList)
        onView(withText("Friends (group)")).check(matches(isDisplayed()))
        onView(withText("Friends (group)")).check(matches(hasSibling(withId(R.id.icon))))
            .check(matches(isDisplayed()))
        onView(ViewMatchers.withTagValue(CoreMatchers.equalTo(R.drawable.ic_group))).check(matches(isDisplayed()))
    }

    private fun loadSearchShareesFragment(
        capabilities: OCCapabilityEntity = AppTestUtil.createCapability(),
        privateShares: ArrayList<OCShareEntity> = arrayListOf()
    ) {
        val account = mockkClass(Account::class)

        val searchShareesFragment =
            SearchShareesFragment.newInstance(
                getOCFileForTesting("image.jpg"),
                account
            )

        activityRule.activity.capabilities = capabilities
        activityRule.activity.privateShares = privateShares
        activityRule.activity.setFragment(searchShareesFragment)
    }

    private fun getOCFileForTesting(name: String = "default") = OCFile("/Docs").apply {
        availableOfflineStatus = OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
        fileName = name
        fileId = 9456985479
        remoteId = "1"
        privateLink = "private link"
    }
}
