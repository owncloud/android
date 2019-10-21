/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
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

package com.owncloud.android.flow

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountAuthenticator.KEY_AUTH_TOKEN_TYPE
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.viewmodels.capabilities.OCCapabilityViewModel
import com.owncloud.android.presentation.viewmodels.sharing.OCShareViewModel
import com.owncloud.android.presentation.ui.sharing.ShareActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.AccountsManager
import com.owncloud.android.utils.AppTestUtil.DUMMY_CAPABILITY
import com.owncloud.android.utils.AppTestUtil.DUMMY_SHARE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class CreatePublicShareTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(
        ShareActivity::class.java,
        true,
        false
    )

    private lateinit var file: OCFile

    private val publicShares = listOf(
        DUMMY_SHARE.copy(
            path = "/Photos/image.jpg",
            isFolder = false,
            name = "image.jpg link",
            shareLink = "http://server:port/s/1"
        ),
        DUMMY_SHARE.copy(
            path = "/Photos/image.jpg",
            isFolder = false,
            name = "image.jpg link (2)",
            shareLink = "http://server:port/s/2"
        ),
        DUMMY_SHARE.copy(
            path = "/Photos/image.jpg",
            isFolder = false,
            name = "image.jpg link (3)",
            shareLink = "http://server:port/s/3"
        )
    )

    private val capabilitiesLiveData = MutableLiveData<UIResult<OCCapability>>()
    private val publicSharesLiveData = MutableLiveData<UIResult<List<OCShare>>>()
    private val privateSharesLiveData = MutableLiveData<UIResult<OCShare>>()
    private val publicShareCreationStatusLiveData = MutableLiveData<UIResult<Unit>>()

    private val ocCapabilityViewModel = mockk<OCCapabilityViewModel>(relaxed = true)
    private val ocShareViewModel = mockk<OCShareViewModel>(relaxed = true)

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
        }
    }

    @Before
    fun setUp() {
        val intent = Intent()

        file = getOCFileForTesting("image.jpg")
        intent.putExtra(FileActivity.EXTRA_FILE, file)

        every { ocCapabilityViewModel.capabilities } returns capabilitiesLiveData
        every { ocShareViewModel.shares } returns publicSharesLiveData
        every { ocShareViewModel.privateShare } returns privateSharesLiveData
        every { ocShareViewModel.publicShareCreationStatus } returns publicShareCreationStatusLiveData

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
    fun createPublicShareWithNoPublicSharesYet() {
        loadCapabilitiesSuccessfully()
        publicSharesLiveData.postValue(UIResult.Success(listOf()))

        // Create share
        onView(withId(R.id.addPublicLinkButton)).perform(click())

        val newPublicShare = publicShares[0]

        onView(withId(R.id.saveButton)).perform(click())

        verify {
            ocShareViewModel.insertPublicShare(
                filePath = newPublicShare.path,
                permissions = 1,
                name = newPublicShare.name!!,
                password = "",
                expirationTimeInMillis = -1,
                publicUpload = false,
                accountName = "user@server"
            )
        }

        // New share properly created
        publicShareCreationStatusLiveData.postValue(UIResult.Success())
        publicSharesLiveData.postValue(
            UIResult.Success(
                listOf(newPublicShare)
            )
        )

        // Check whether the dialog to create the public share has been properly closed
        onView(withText(R.string.share_via_link_create_title)).check(doesNotExist())
        onView(withText(newPublicShare.name)).check(matches(isDisplayed()))
    }

    @Test
    fun createPublicShareWithAlreadyExistingShares() {
        loadCapabilitiesSuccessfully()
        val existingPublicShares = publicShares.take(2) as ArrayList<OCShare>
        publicSharesLiveData.postValue(
            UIResult.Success(
                existingPublicShares
            )
        )

        onView(withId(R.id.addPublicLinkButton)).perform(click())

        val newPublicShare = publicShares[2]

        verify {
            ocShareViewModel.insertPublicShare(
                filePath = newPublicShare.path,
                permissions = 1,
                name = newPublicShare.name!!,
                password = "",
                expirationTimeInMillis = -1,
                publicUpload = false,
                accountName = "user@server"
            )
        }

        // New share properly created
        publicShareCreationStatusLiveData.postValue(UIResult.Success())
        publicSharesLiveData.postValue(
            UIResult.Success(
                publicShares
            )
        )

        // Check whether the dialog to create the public share has been properly closed
        onView(withText(R.string.share_via_link_create_title)).check(doesNotExist())
        onView(withText(newPublicShare.name)).check(matches(isDisplayed()))
    }

    @Test
    fun createMultiplePublicShares() {
        loadCapabilitiesSuccessfully()
        publicSharesLiveData.postValue(UIResult.Success(listOf()))

        /**
         * 1st public share
         */
        onView(withId(R.id.addPublicLinkButton)).perform(click())

        val newPublicShare1 = publicShares[0]

        verify {
            ocShareViewModel.insertPublicShare(
                filePath = newPublicShare1.path,
                permissions = 1,
                name = newPublicShare1.name!!,
                password = "",
                expirationTimeInMillis = -1,
                publicUpload = false,
                accountName = "user@server"
            )
        }

        // New share properly created
        publicShareCreationStatusLiveData.postValue(UIResult.Success())
        publicSharesLiveData.postValue(
            UIResult.Success(
                listOf(newPublicShare1)
            )
        )

        // Check whether the dialog to create the public share has been properly closed
        onView(withText(R.string.share_via_link_create_title)).check(doesNotExist())
        onView(withText(newPublicShare1.name)).check(matches(isDisplayed()))

        /**
         * 2nd public share
         */
        onView(withId(R.id.addPublicLinkButton)).perform(click())

        val newPublicShare2 = publicShares[1]

        verify {
            ocShareViewModel.insertPublicShare(
                filePath = newPublicShare1.path,
                permissions = 1,
                name = newPublicShare1.name!!,
                password = "",
                expirationTimeInMillis = -1,
                publicUpload = false,
                accountName = "user@server"
            )
        }

        // New share properly created
        publicShareCreationStatusLiveData.postValue(UIResult.Success())
        publicSharesLiveData.postValue(
            UIResult.Success(
                listOf(newPublicShare1, newPublicShare2)
            )
        )

        // Check whether the dialog to create the public share has been properly closed
        onView(withText(R.string.share_via_link_create_title)).check(doesNotExist())
        onView(withText(newPublicShare2.name)).check(matches(isDisplayed()))

        /**
         * 3rd public share
         */
        onView(withId(R.id.addPublicLinkButton)).perform(click())

        val newPublicShare3 = publicShares[2]

        verify {
            ocShareViewModel.insertPublicShare(
                filePath = newPublicShare1.path,
                permissions = 1,
                name = newPublicShare1.name!!,
                password = "",
                expirationTimeInMillis = -1,
                publicUpload = false,
                accountName = "user@server"
            )
        }

        // New share properly created
        publicShareCreationStatusLiveData.postValue(UIResult.Success())
        publicSharesLiveData.postValue(
            UIResult.Success(
                publicShares
            )
        )

        // Check whether the dialog to create the public share has been properly closed
        onView(withText(R.string.share_via_link_create_title)).check(doesNotExist())
        onView(withText(newPublicShare3.name)).check(matches(isDisplayed()))
    }

    @Test
    fun createShareLoading() {
        loadCapabilitiesSuccessfully()
        publicSharesLiveData.postValue(UIResult.Success(listOf()))

        onView(withId(R.id.addPublicLinkButton)).perform(click())

        publicShareCreationStatusLiveData.postValue(UIResult.Loading())

        onView(withText(R.string.common_loading)).check(matches(isDisplayed()))
    }

    @Test
    fun createShareError() {
        loadCapabilitiesSuccessfully()
        publicSharesLiveData.postValue(UIResult.Success(listOf()))

        onView(withId(R.id.addPublicLinkButton)).perform(click())

        publicShareCreationStatusLiveData.postValue(
            UIResult.Error(
                Throwable()
            )
        )

        onView(withText(R.string.share_link_file_error)).check(matches(isDisplayed()))
    }

    private fun getOCFileForTesting(name: String = "default") = OCFile("/Photos").apply {
        availableOfflineStatus = OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
        fileName = name
        fileId = 9456985479
        remoteId = "1"
        privateLink = "private link"
    }

    private fun loadCapabilitiesSuccessfully() {
        capabilitiesLiveData.postValue(
            UIResult.Success(
                DUMMY_CAPABILITY.copy(
                    versionString = "10.1.1",
                    filesSharingPublicMultiple = CapabilityBooleanType.TRUE
                )
            )
        )
    }
}
