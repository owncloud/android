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
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.viewmodel.OCShareViewModel
import com.owncloud.android.testing.TestShareFileActivity
import com.owncloud.android.ui.fragment.ShareFileFragment
import com.owncloud.android.utils.TestUtil
import com.owncloud.android.utils.ViewModelUtil
import com.owncloud.android.vo.Resource
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class ShareFileFragmentTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(TestShareFileActivity::class.java, true, true)

    private val sharesLiveData = MutableLiveData<Resource<List<OCShare>>>()
    private lateinit var shareFragment: ShareFileFragment
    private lateinit var ocShareViewModel: OCShareViewModel

    @Before
    fun setUp() {
        val account = mock(Account::class.java)
        val ownCloudVersion = mock(OwnCloudVersion::class.java)
        `when`(ownCloudVersion.isSearchUsersSupported).thenReturn(true)

        shareFragment = ShareFileFragment.newInstance(
            getOCFileForTesting("image.jpg"),
            account,
            ownCloudVersion
        )

        ocShareViewModel = mock(OCShareViewModel::class.java)
        `when`(ocShareViewModel.sharesForFile).thenReturn(sharesLiveData)

        shareFragment.mViewModelFactory = ViewModelUtil.createFor(ocShareViewModel)
        activityRule.activity.setFragment(shareFragment)
    }

    @Test
    fun showHeader() {
        onView(withId(R.id.shareFileName)).check(matches(withText("image.jpg")))
    }

    @Test
    fun showUsersAndGroupsSectionTitle() {
        onView(withText(R.string.share_with_user_section_title)).check(matches(isDisplayed()))
    }

    @Test
    fun showLoadingDialog() {
        sharesLiveData.postValue(Resource.loading())
        onView(withId(R.id.loadingLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun showNoPublicShares() {
        val publicShares = arrayListOf<OCShare>()
        sharesLiveData.postValue(Resource.success(publicShares))
        onView(withId(R.id.shareNoPublicLinks)).check(matches(withText(R.string.share_no_public_links)))
    }

    @Test
    fun showPublicShares() {
        val publicShares = arrayListOf(
            TestUtil.createPublicShare(
                path = "/Photos/image.jpg",
                isFolder = false,
                name = "Image link",
                shareLink = "http://server:port/s/1"
            ),
            TestUtil.createPublicShare(
                path = "/Photos/image.jpg",
                isFolder = false,
                name = "Image link 2",
                shareLink = "http://server:port/s/2"
            ),
            TestUtil.createPublicShare(
                path = "/Photos/image.jpg",
                isFolder = false,
                name = "Image link 3",
                shareLink = "http://server:port/s/3"
            )
        )

        sharesLiveData.postValue(Resource.success(publicShares))

        onView(withText("Image link")).check(matches(isDisplayed()))
        onView(withText("Image link 2")).check(matches(isDisplayed()))
        onView(withText("Image link 3")).check(matches(isDisplayed()))
    }

    @Test
    fun fileSizeVisible(){
        val publicShares = arrayListOf(
                TestUtil.createPublicShare(
                        path = "/Photos/image.jpg",
                        isFolder = false,
                        name = "Image link 1",
                        shareLink = "http://server:port/s/1"
                )
        )
        sharesLiveData.postValue(Resource.success(publicShares))
        onView(withId(R.id.shareFileSize)).check(matches(isDisplayed()))

    }


    @Test
    fun showError() {
        sharesLiveData.postValue(Resource.error(RemoteOperationResult.ResultCode.SERVICE_UNAVAILABLE))
        onView(withId(R.id.snackbar_text)).check(matches(withText(R.string.service_unavailable)))
    }

    fun getOCFileForTesting(name: String = "default"): OCFile {
        var file = OCFile("/Photos")
        file.availableOfflineStatus = OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
        file.fileName = name
        file.fileId = 9456985479
        file.remoteId = "1"
        return file
    }
}
