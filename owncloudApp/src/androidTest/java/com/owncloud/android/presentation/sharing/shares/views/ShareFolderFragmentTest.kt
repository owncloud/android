/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Jesus Recio Rincon
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

package com.owncloud.android.presentation.sharing.shares.views

import android.accounts.Account
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.presentation.ui.sharing.fragments.ShareFileFragment
import com.owncloud.android.utils.AppTestUtil
import io.mockk.every
import io.mockk.mockkClass
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareFolderFragmentTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(TestShareFileActivity::class.java, true, true)

    private lateinit var shareFragment: ShareFileFragment

    @Before
    fun setUp() {
        val account = mockkClass(Account::class)
        val ownCloudVersion = mockkClass(OwnCloudVersion::class)

        shareFragment = ShareFileFragment.newInstance(
            getOCFolderForTesting("Photos"),
            account,
            ownCloudVersion
        )

        activityRule.activity.capabilities = AppTestUtil.createCapability()
        activityRule.activity.publicShares = arrayListOf(
            AppTestUtil.createPublicShare(
                path = "/Photos",
                isFolder = true,
                name = "Photos link 1",
                shareLink = "http://server:port/s/1"
            )
        )
        activityRule.activity.privateShares = arrayListOf(
            AppTestUtil.createPrivateShare(
                path = "/Photos",
                isFolder = true,
                shareWith = "username",
                sharedWithDisplayName = "Bob"
            )
        )
        activityRule.activity.setFragment(shareFragment)
    }

    @Test
    fun folderSizeVisible() {
        onView(withId(R.id.shareFileSize)).check(matches(not(isDisplayed())))
    }

    @Test
    fun hidePrivateLink() {
        onView(withId(R.id.getPrivateLinkButton)).check(matches(not(isDisplayed())))
    }

    private fun getOCFolderForTesting(name: String = "default"): OCFile {
        val file = OCFile("/Photos")
        file.availableOfflineStatus = OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
        file.fileName = name
        file.fileId = 9456985479
        file.remoteId = "1"
        file.mimetype = "DIR"
        file.privateLink = null
        return file
    }
}
