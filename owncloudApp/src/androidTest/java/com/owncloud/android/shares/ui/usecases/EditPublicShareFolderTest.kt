/**
 * ownCloud Android client application
 *
 * @author Jesús Recio (@jesmrec)
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

package com.owncloud.android.shares.ui.usecases

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountAuthenticator.KEY_AUTH_TOKEN_TYPE
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.capabilities.viewmodel.OCCapabilityViewModel
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.ui.ShareActivity
import com.owncloud.android.shares.viewmodel.OCShareViewModel
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.AccountsManager
import com.owncloud.android.utils.TestUtil
import com.owncloud.android.vo.Resource
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class EditPublicShareFolderTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(
        ShareActivity::class.java,
        true,
        false
    )

    private lateinit var file: OCFile

    private val publicShares = arrayListOf(
        TestUtil.createPublicShare(
            path = "/Photos/",
            expirationDate = 0L,
            permissions = 1,
            isFolder = true,
            name = "Photos link",
            shareLink = "http://server:port/s/1"
        ),
        TestUtil.createPublicShare(  // With name updated
            path = "/Photos/",
            expirationDate = 0L,
            permissions = 1,
            isFolder = true,
            name = "Photos updated link",
            shareLink = "http://server:port/s/1"
        ),
        TestUtil.createPublicShare( // With permission Download/View/Upload
            path = "/Photos/",
            expirationDate = 0L,
            permissions = 15,
            isFolder = true,
            name = "Photos link",
            shareLink = "http://server:port/s/1"
        ),
        TestUtil.createPublicShare( // With permission Upload only
            path = "/Photos/",
            expirationDate = 0L,
            permissions = 4,
            isFolder = true,
            name = "Photos link",
            shareLink = "http://server:port/s/1"
        ),
        TestUtil.createPublicShare( // With password
            path = "/Photos/",
            expirationDate = 0L,
            isFolder = true,
            name = "Photos link",
            shareLink = "http://server:port/s/1",
            shareWith = "1"
        )
    )

    private val capabilitiesLiveData = MutableLiveData<Resource<OCCapability>>()
    private val sharesLiveData = MutableLiveData<Resource<List<OCShare>>>()

    private val ocCapabilityViewModel = mock(OCCapabilityViewModel::class.java)
    private val ocShareViewModel = mock(OCShareViewModel::class.java)

    companion object {
        private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        private val account = Account("admin", "owncloud")

        @BeforeClass
        @JvmStatic
        fun init() {
            addAccount()
        }

        @AfterClass
        @JvmStatic
        fun cleanUp() {
            AccountsManager.deleteAllAccounts(targetContext)
        }

        private fun addAccount() {
            // obtaining an AccountManager instance
            val accountManager = AccountManager.get(targetContext)

            Thread(Runnable {
                accountManager.addAccountExplicitly(account, "a", null)

                // include account version, user, server version and token with the new account
                accountManager.setUserData(
                    account,
                    AccountUtils.Constants.KEY_OC_VERSION,
                    OwnCloudVersion("10.2").toString()
                )
                accountManager.setUserData(
                    account,
                    AccountUtils.Constants.KEY_OC_BASE_URL,
                    "serverUrl:port"
                )
                accountManager.setUserData(
                    account,
                    AccountUtils.Constants.KEY_DISPLAY_NAME,
                    "admin"
                )
                accountManager.setUserData(
                    account,
                    AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                    "1"
                )

                accountManager.setAuthToken(
                    account,
                    KEY_AUTH_TOKEN_TYPE,
                    "AUTH_TOKEN"
                )
            }).start()
        }
    }

    @Before
    fun setUp() {
        val intent = spy(Intent::class.java)

        file = getOCFileForTesting("Photos")

        `when`(intent.getParcelableExtra(FileActivity.EXTRA_FILE) as? Parcelable).thenReturn(file)
        intent.putExtra(FileActivity.EXTRA_FILE, file)

        `when`(ocCapabilityViewModel.getCapabilityForAccount(false)).thenReturn(capabilitiesLiveData)
        `when`(ocCapabilityViewModel.getCapabilityForAccount(true)).thenReturn(capabilitiesLiveData)
        `when`(ocShareViewModel.getSharesForFile()).thenReturn(sharesLiveData)

        stopKoin()

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext<Context>())
            modules(
                module(override = true) {
                    viewModel {
                        ocCapabilityViewModel
                    }
                    viewModel {
                        ocShareViewModel
                    }
                }
            )
        }

        activityRule.launchActivity(intent)
    }

    @Test
    fun editNameFolder() {
        loadCapabilitiesSuccessfully()

        val existingPublicShare = publicShares[0]
        loadSharesSuccessfully(arrayListOf(existingPublicShare))

        val updatedPublicShare = publicShares[1]

        `when`(
            ocShareViewModel.updatePublicShareForFile(
                1,
                updatedPublicShare.name!!,
                "",
                -1,
                1,
                false
            )
        ).thenReturn(sharesLiveData)

        // 1. Open dialog to edit an existing public share
        onView(withId(R.id.editPublicLinkButton)).perform(click())

        // 2. Update fields
        onView(withId(R.id.shareViaLinkNameValue)).perform(replaceText(updatedPublicShare.name))

        // 3. Save updated share
        onView(withId(R.id.saveButton)).perform(click())

        // 4. Share properly updated
        sharesLiveData.postValue(
            Resource.success(
                arrayListOf(updatedPublicShare)
            )
        )

        // Check whether the dialog to create the public share has been properly closed
        onView(withText(R.string.share_via_link_edit_title)).check(doesNotExist())
        onView(withText(updatedPublicShare.name)).check(matches(isDisplayed()))
    }

    @Test
    fun editPermissionToDownloadViewUpload() {
        loadCapabilitiesSuccessfully()

        val existingPublicShare = publicShares[0]
        loadSharesSuccessfully(arrayListOf(existingPublicShare))

        val updatedPublicShare = publicShares[2]
        `when`(
            ocShareViewModel.updatePublicShareForFile(
                1,
                updatedPublicShare.name!!,
                "",
                -1,
                15,
                true
            )
        ).thenReturn(sharesLiveData)

        // 1. Open dialog to edit an existing public share
        onView(withId(R.id.editPublicLinkButton)).perform(click())

        // 2. Update fields
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).perform(click())

        // 3. Save updated share
        onView(withId(R.id.saveButton)).perform(click())

        // 4. Share properly updated
        sharesLiveData.postValue(
            Resource.success(
                arrayListOf(updatedPublicShare)
            )
        )

        // Open Dialog to check correct permission change
        onView(withId(R.id.editPublicLinkButton)).perform(click())
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).check(matches(isChecked()))

    }

    @Test
    fun editPermissionToUploadOnly() {
        loadCapabilitiesSuccessfully()

        val existingPublicShare = publicShares[0]
        loadSharesSuccessfully(arrayListOf(existingPublicShare))

        val updatedPublicShare = publicShares[3]
        `when`(
            ocShareViewModel.updatePublicShareForFile(
                1,
                updatedPublicShare.name!!,
                "",
                -1,
                4,
                true
            )
        ).thenReturn(sharesLiveData)

        // 1. Open dialog to edit an existing public share
        onView(withId(R.id.editPublicLinkButton)).perform(click())

        // 2. Update fields
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).perform(click())

        // 3. Save updated share
        onView(withId(R.id.saveButton)).perform(click())

        // 4. Share properly updated
        sharesLiveData.postValue(
            Resource.success(
                arrayListOf(updatedPublicShare)
            )
        )

        // Open Dialog to check correct permission change
        onView(withId(R.id.editPublicLinkButton)).perform(click())
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).check(matches(isChecked()))

    }

    @Test
    fun editPermissionToDownloadView() {
        loadCapabilitiesSuccessfully()

        val existingPublicShare = publicShares[2]
        loadSharesSuccessfully(arrayListOf(existingPublicShare))

        val updatedPublicShare = publicShares[0]
        `when`(
            ocShareViewModel.updatePublicShareForFile(
                1,
                updatedPublicShare.name!!,
                "",
                -1,
                1,
                false
            )
        ).thenReturn(sharesLiveData)

        // 1. Open dialog to edit an existing public share
        onView(withId(R.id.editPublicLinkButton)).perform(click())

        // 2. Update fields
        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).perform(click())

        // 3. Save updated share
        onView(withId(R.id.saveButton)).perform(click())

        // 4. Share properly updated
        sharesLiveData.postValue(
            Resource.success(
                arrayListOf(updatedPublicShare)
            )
        )

        // Open Dialog to check correct permission change
        onView(withId(R.id.editPublicLinkButton)).perform(click())
        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).check(matches(isChecked()))

    }


    private fun getOCFileForTesting(name: String = "default"): OCFile {
        val file = OCFile("/Photos")
        file.availableOfflineStatus = OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
        file.fileName = name
        file.fileId = 9456985479
        file.remoteId = "1"
        file.mimetype = "DIR"
        file.privateLink = "private link"
        return file
    }

    private fun loadCapabilitiesSuccessfully(
        capability: OCCapability = TestUtil.createCapability(
            versionString = "10.1.1",
            sharingPublicMultiple = CapabilityBooleanType.TRUE.value,
            sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
            sharingPublicUpload = CapabilityBooleanType.TRUE.value
        )
    ) {
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
