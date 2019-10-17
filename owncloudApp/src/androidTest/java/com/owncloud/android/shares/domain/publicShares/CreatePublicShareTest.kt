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

package com.owncloud.android.shares.domain.publicShares

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Parcelable
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
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.capabilities.viewmodel.OCCapabilityViewModel
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.shares.domain.OCShare
import com.owncloud.android.shares.presentation.OCShareViewModel
import com.owncloud.android.shares.presentation.ShareActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.AccountsManager
import com.owncloud.android.utils.TestUtil
import com.owncloud.android.vo.Resource
import org.junit.After
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
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy

class CreatePublicShareTest {
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
            path = "/Photos/image.jpg",
            isFolder = false,
            name = "image.jpg link",
            shareLink = "http://server:port/s/1"
        ),
        TestUtil.createPublicShare(
            path = "/Photos/image.jpg",
            isFolder = false,
            name = "image.jpg link (2)",
            shareLink = "http://server:port/s/2"
        ),
        TestUtil.createPublicShare(
            path = "/Photos/image.jpg",
            isFolder = false,
            name = "image.jpg link (3)",
            shareLink = "http://server:port/s/3"
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
        val intent = spy(Intent::class.java)

        file = TestUtil.createFile("image.jpg")

        `when`(intent.getParcelableExtra(FileActivity.EXTRA_FILE) as? Parcelable).thenReturn(file)
        intent.putExtra(FileActivity.EXTRA_FILE, file)

        `when`(ocCapabilityViewModel.getCapabilityForAccountAsLiveData(false)).thenReturn(capabilitiesLiveData)
        `when`(ocCapabilityViewModel.getCapabilityForAccountAsLiveData(true)).thenReturn(capabilitiesLiveData)
        `when`(ocShareViewModel.getPublicShares(file.remotePath)).thenReturn(sharesLiveData)
        `when`(ocShareViewModel.getPrivateShares(file.remotePath)).thenReturn(MutableLiveData())

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

    @After
    fun clean() {
        stopKoin()
    }

    @Test
    fun createPublicShareWithNoPublicSharesYet() {
        loadCapabilitiesSuccessfully()
        loadSharesSuccessfully(arrayListOf())

        // Create share
        onView(withId(R.id.addPublicLinkButton)).perform(click())

        val newPublicShare = publicShares[0]
        savePublicShare(newPublicShare)

        // New share properly created
        sharesLiveData.postValue(
            Resource.success(
                arrayListOf(newPublicShare)
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
        loadSharesSuccessfully(
            existingPublicShares
        )

        onView(withId(R.id.addPublicLinkButton)).perform(click())

        val newPublicShare = publicShares[2]
        savePublicShare(newPublicShare)

        // New share properly created
        sharesLiveData.postValue(
            Resource.success(
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
        loadSharesSuccessfully(arrayListOf())

        /**
         * 1st public share
         */
        onView(withId(R.id.addPublicLinkButton)).perform(click())

        val newPublicShare1 = publicShares[0]
        savePublicShare(newPublicShare1)

        // New share properly created
        sharesLiveData.postValue(
            Resource.success(
                arrayListOf(newPublicShare1)
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
        savePublicShare(newPublicShare2)

        // New share properly created
        sharesLiveData.postValue(
            Resource.success(
                publicShares.take(2)
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
        savePublicShare(newPublicShare3)

        // New share properly created
        sharesLiveData.postValue(
            Resource.success(
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
        loadSharesSuccessfully(arrayListOf())

        onView(withId(R.id.addPublicLinkButton)).perform(click())

        savePublicShare(publicShares[0], Resource.loading())

        onView(withText(R.string.common_loading)).check(matches(isDisplayed()))
    }

    @Test
    fun createShareError() {
        loadCapabilitiesSuccessfully()
        loadSharesSuccessfully(arrayListOf())

        onView(withId(R.id.addPublicLinkButton)).perform(click())

        savePublicShare(
            publicShares[0],
            Resource.error(
                RemoteOperationResult.ResultCode.FORBIDDEN,
                exception = Exception("Error when retrieving shares")
            )
        )

        onView(withText(R.string.share_link_file_error)).check(matches(isDisplayed()))
    }

    private fun loadCapabilitiesSuccessfully(
        capability: OCCapability = TestUtil.createCapability(
            versionString = "10.1.1",
            sharingPublicMultiple = CapabilityBooleanType.TRUE.value
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

    private fun savePublicShare(newShare: OCShare, resource: Resource<Unit> = Resource.success()) {
        `when`(
            ocShareViewModel.insertPublicShare(
                file.remotePath,
                1,
                newShare.name!!,
                "",
                -1,
                false
            )
        ).thenReturn(
            MutableLiveData<Resource<Unit>>().apply {
                postValue(resource)
            }
        )

        onView(withId(R.id.saveButton)).perform(click())
    }
}
