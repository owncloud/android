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

package com.owncloud.android.flow

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountAuthenticator.KEY_AUTH_TOKEN_TYPE
import com.owncloud.android.data.capabilities.db.OCCapabilityEntity
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.presentation.viewmodels.capabilities.OCCapabilityViewModel
import com.owncloud.android.presentation.viewmodels.sharing.OCShareViewModel
import com.owncloud.android.presentation.ui.sharing.ShareActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.AccountsManager
import com.owncloud.android.utils.AppTestUtil
import io.mockk.every
import io.mockk.mockk
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

class LoadSharesTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(
        ShareActivity::class.java,
        true,
        false
    )

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

        val file = getOCFileForTesting("image.jpg")
        intent.putExtra(FileActivity.EXTRA_FILE, file)

        every { ocCapabilityViewModel.getCapabilityForAccount() } returns capabilitiesLiveData
        every { ocShareViewModel.getPublicShares(file.remotePath) } returns publicSharesLiveData
        every { ocShareViewModel.getPrivateShares(file.remotePath) } returns privateSharesLiveData

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

    /******************************************************************************************************
     ******************************************** CAPABILITIES ********************************************
     ******************************************************************************************************/

    private val ocCapabilityViewModel = mockk<OCCapabilityViewModel>(relaxed = true)
    private val capabilitiesLiveData = MutableLiveData<DataResult<OCCapabilityEntity>>()

    @Test
    fun showLoadingCapabilitiesDialog() {
        capabilitiesLiveData.postValue(DataResult.loading(AppTestUtil.createCapability()))
        onView(withId(R.id.loadingLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun showErrorWhenLoadingCapabilities() {
        capabilitiesLiveData.postValue(
            DataResult.error(
                RemoteOperationResult.ResultCode.SERVICE_UNAVAILABLE
            )
        )

        onView(withId(R.id.snackbar_text)).check(matches(withText(R.string.service_unavailable)))
    }

    /******************************************************************************************************
     ******************************************* PRIVATE SHARES *******************************************
     ******************************************************************************************************/

    private val privateSharesLiveData = MutableLiveData<DataResult<List<OCShareEntity>>>()
    private val privateShares = arrayListOf(
        AppTestUtil.createPrivateShare(
            path = "/Photos/image.jpg",
            isFolder = false,
            shareWith = "work",
            sharedWithDisplayName = "Work"
        ),
        AppTestUtil.createPrivateShare(
            path = "/Photos/image.jpg",
            isFolder = false,
            shareWith = "family",
            sharedWithDisplayName = "Family"
        )
    )

    @Test
    fun showLoadingPrivateSharesDialog() {
        loadCapabilitiesSuccessfully()
        privateSharesLiveData.postValue(DataResult.loading(privateShares))
        onView(withId(R.id.loadingLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun showErrorWhenLoadingPrivateShares() {
        loadCapabilitiesSuccessfully()

        privateSharesLiveData.postValue(
            DataResult.error(
                RemoteOperationResult.ResultCode.FORBIDDEN,
                data = privateShares
            )
        )
        onView(withId(R.id.snackbar_text)).check(matches(withText(R.string.get_shares_error)))
    }

    /******************************************************************************************************
     ******************************************* PUBLIC SHARES ********************************************
     ******************************************************************************************************/

    private val publicSharesLiveData = MutableLiveData<DataResult<List<OCShareEntity>>>()
    private val publicShares = arrayListOf(
        AppTestUtil.createPublicShare(
            path = "/Photos/image.jpg",
            isFolder = false,
            name = "Image link",
            shareLink = "http://server:port/s/1"
        ),
        AppTestUtil.createPublicShare(
            path = "/Photos/image.jpg",
            isFolder = false,
            name = "Image link 2",
            shareLink = "http://server:port/s/2"
        )
    )

    @Test
    fun showLoadingPublicSharesDialog() {
        loadCapabilitiesSuccessfully()
        publicSharesLiveData.postValue(DataResult.loading(publicShares))
        onView(withId(R.id.loadingLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun showErrorWhenLoadingPublicShares() {
        loadCapabilitiesSuccessfully()

        publicSharesLiveData.postValue(
            DataResult.error(
                RemoteOperationResult.ResultCode.SERVICE_UNAVAILABLE,
                data = publicShares
            )
        )
        onView(withId(R.id.snackbar_text)).check(matches(withText(R.string.service_unavailable)))
    }

    /******************************************************************************************************
     *********************************************** COMMON ***********************************************
     ******************************************************************************************************/

    private fun getOCFileForTesting(name: String = "default") = OCFile("/Photos").apply {
        availableOfflineStatus = OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
        fileName = name
        fileId = 9456985479
        remoteId = "1"
        privateLink = "private link"
    }

    private fun loadCapabilitiesSuccessfully(capability: OCCapabilityEntity = AppTestUtil.createCapability()) {
        capabilitiesLiveData.postValue(
            DataResult.success(
                capability
            )
        )
    }
}
