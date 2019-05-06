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

package com.owncloud.android.shares.ui

import android.accounts.Account
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.capabilities.viewmodel.OCCapabilityViewModel
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.ui.fragment.ShareFileFragment
import com.owncloud.android.shares.viewmodel.OCShareViewModel
import com.owncloud.android.utils.TestUtil
import com.owncloud.android.utils.ViewModelUtil
import com.owncloud.android.vo.Resource
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class ShareFolderFragmentTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(TestShareFileActivity::class.java, true, true)

    private val sharesLiveData = MutableLiveData<Resource<List<OCShare>>>()
    private val capabilitiesLiveData = MutableLiveData<Resource<OCCapability>>()
    private lateinit var shareFragment: ShareFileFragment
    private lateinit var ocShareViewModel: OCShareViewModel

    @Before
    fun setUp() {
        val account = Mockito.mock(Account::class.java)
        val ownCloudVersion = Mockito.mock(OwnCloudVersion::class.java)
        Mockito.`when`(ownCloudVersion.isSearchUsersSupported).thenReturn(true)

        shareFragment = ShareFileFragment.newInstance(
                getOCFolderForTesting("Photos"),
                account,
                ownCloudVersion
        )

        val ocCapabilityViewModel = Mockito.mock(OCCapabilityViewModel::class.java)
        Mockito.`when`(
            ocCapabilityViewModel.getCapabilityForAccount()
        ).thenReturn(capabilitiesLiveData)

        ocShareViewModel = Mockito.mock(OCShareViewModel::class.java)
        Mockito.`when`(ocShareViewModel.getSharesForFile()).thenReturn(sharesLiveData)

        shareFragment.ocShareViewModelFactory = ViewModelUtil.createFor(ocShareViewModel)
        shareFragment.ocCapabilityViewModelFactory = ViewModelUtil.createFor(ocCapabilityViewModel)
        activityRule.activity.setFragment(shareFragment)
    }

    @Test
    fun folderSizeVisible(){
        val publicShares = arrayListOf(
                TestUtil.createPublicShare(
                        path = "/Photos",
                        isFolder = true,
                        name = "Photos link 1",
                        shareLink = "http://server:port/s/1"
                )
        )
        sharesLiveData.postValue(Resource.success(publicShares))
        onView(withId(R.id.shareFileSize)).check(matches(not(isDisplayed())))

    }

    @Test
    fun hidePrivateLink() {
        onView(withId(R.id.getPrivateLinkButton)).check(matches(not(isDisplayed())))
    }


    fun getOCFolderForTesting(name: String = "default"): OCFile {
        var file = OCFile("/Photos")
        file.availableOfflineStatus = OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
        file.fileName = name
        file.fileId = 9456985479
        file.remoteId = "1"
        file.mimetype = "DIR"
        file.privateLink = null
        return file
    }
}
