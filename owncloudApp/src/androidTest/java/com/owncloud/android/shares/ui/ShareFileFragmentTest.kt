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
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.capabilities.viewmodel.OCCapabilityViewModel
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.ui.fragment.ShareFileFragment
import com.owncloud.android.shares.viewmodel.OCShareViewModel
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

    private val capabilitiesLiveData = MutableLiveData<Resource<OCCapability>>()
    private val sharesLiveData = MutableLiveData<Resource<List<OCShare>>>()

    private val publicShares = arrayListOf(
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

    @Before
    fun setUp() {
        val account = mock(Account::class.java)
        val ownCloudVersion = mock(OwnCloudVersion::class.java)
        `when`(ownCloudVersion.isSearchUsersSupported).thenReturn(true)

        val shareFragment = ShareFileFragment.newInstance(
            getOCFileForTesting("image.jpg"),
            account,
            ownCloudVersion
        )

        val ocShareViewModel = mock(OCShareViewModel::class.java)
        `when`(ocShareViewModel.getSharesForFile()).thenReturn(sharesLiveData)
        shareFragment.ocShareViewModelFactory = ViewModelUtil.createFor(ocShareViewModel)

        val ocCapabilityViewModel = mock(OCCapabilityViewModel::class.java)
        `when`(ocCapabilityViewModel.getCapabilityForAccount()).thenReturn(capabilitiesLiveData)
        shareFragment.ocCapabilityViewModelFactory = ViewModelUtil.createFor(ocCapabilityViewModel)

        activityRule.activity.setFragment(shareFragment)
    }

    @Test
    fun showHeader() {
        onView(withId(R.id.shareFileName)).check(matches(withText("image.jpg")))
    }

    @Test
    fun showPrivateLink() {
        onView(withId(R.id.getPrivateLinkButton)).check(matches(isDisplayed()))
    }

    @Test
    fun showUsersAndGroupsSectionTitle() {
        onView(withText(R.string.share_with_user_section_title)).check(matches(isDisplayed()))
    }

    @Test
    fun showLoadingCapabilitiesDialog() {
        capabilitiesLiveData.postValue(Resource.loading(TestUtil.createCapability()))
        onView(withId(R.id.loadingLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun showLoadingSharesDialog() {
        loadCapabilitiesSuccessfully()
        sharesLiveData.postValue(Resource.loading(publicShares))
        onView(withId(R.id.loadingLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun showNoPublicShares() {
        val publicShares = arrayListOf<OCShare>()
        loadSharesSuccessfully(publicShares)
        onView(withId(R.id.shareNoPublicLinks)).check(matches(withText(R.string.share_no_public_links)))
    }

    @Test
    fun showPublicShares() {
        loadCapabilitiesSuccessfully()
        loadSharesSuccessfully()

        onView(withText("Image link")).check(matches(isDisplayed()))
        onView(withText("Image link 2")).check(matches(isDisplayed()))
        onView(withText("Image link 3")).check(matches(isDisplayed()))
    }

    @Test
    fun fileSizeVisible() {
        loadSharesSuccessfully()
        onView(withId(R.id.shareFileSize)).check(matches(isDisplayed()))
    }

    @Test
    fun showErrorWhenLoadingCapabilities() {
        capabilitiesLiveData.postValue(
            Resource.error(
                RemoteOperationResult.ResultCode.SERVICE_UNAVAILABLE
            )
        )

        onView(withId(R.id.snackbar_text)).check(matches(withText(R.string.service_unavailable)))
    }

    @Test
    fun showErrorWhenLoadingShares() {
        loadCapabilitiesSuccessfully()

        sharesLiveData.postValue(
            Resource.error(
                RemoteOperationResult.ResultCode.SERVICE_UNAVAILABLE,
                data = publicShares
            )
        )
        onView(withId(R.id.snackbar_text)).check(matches(withText(R.string.service_unavailable)))
    }

    @Test
    fun showPublicSharesSharingEnabled() {
        loadCapabilitiesSuccessfully(
            TestUtil.createCapability(sharingPublicEnabled = CapabilityBooleanType.TRUE.value)
        )
        loadSharesSuccessfully()

        onView(withText("Image link")).check(matches(isDisplayed()))
        onView(withText("Image link 2")).check(matches(isDisplayed()))
        onView(withText("Image link 3")).check(matches(isDisplayed()))
    }

    @Test
    fun hidePublicSharesSharingDisabled() {
        loadCapabilitiesSuccessfully(
            TestUtil.createCapability(sharingPublicEnabled = CapabilityBooleanType.FALSE.value)
        )
        loadSharesSuccessfully()

        onView(withId(R.id.shareViaLinkSection))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    @Test
    fun createPublicShareMultipleCapability() {
        loadCapabilitiesSuccessfully(
            TestUtil.createCapability(
                versionString = "10.1.1",
                sharingPublicMultiple = CapabilityBooleanType.TRUE.value
            )
        )

        loadSharesSuccessfully(arrayListOf(publicShares.get(0)))

        onView(withId(R.id.addPublicLinkButton))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    @Test
    fun cannotCreatePublicShareMultipleCapability() {
        loadCapabilitiesSuccessfully(
            TestUtil.createCapability(
                versionString = "10.1.1",
                sharingPublicMultiple = CapabilityBooleanType.FALSE.value
            )
        )

        loadSharesSuccessfully(arrayListOf(publicShares.get(0)))

        onView(withId(R.id.addPublicLinkButton))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)))
    }

    @Test
    fun cannotCreatePublicShareServerCapability() {
        loadCapabilitiesSuccessfully(
            TestUtil.createCapability(
                versionString = "9.3.1"
            )
        )

        loadSharesSuccessfully(arrayListOf(publicShares.get(0)))

        onView(withId(R.id.addPublicLinkButton))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)))
    }

    private fun getOCFileForTesting(name: String = "default") = OCFile("/Photos").apply {
        availableOfflineStatus = OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
        fileName = name
        fileId = 9456985479
        remoteId = "1"
        privateLink = "private link"
    }

    private fun loadCapabilitiesSuccessfully(capability: OCCapability = TestUtil.createCapability()) {
        capabilitiesLiveData.postValue(
            Resource.success(
                capability
            )
        )
    }

    private fun loadSharesSuccessfully(shares: ArrayList<OCShare> = publicShares) {
        sharesLiveData.postValue(Resource.success(shares))
    }
}
