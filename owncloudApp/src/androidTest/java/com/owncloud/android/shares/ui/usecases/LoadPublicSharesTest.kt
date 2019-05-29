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

package com.owncloud.android.shares.ui.usecases

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Parcelable
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
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.capabilities.viewmodel.OCCapabilityViewModel
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.operations.RemoteOperationResult
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
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy

class LoadPublicSharesTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(
        ShareActivity::class.java,
        true,
        false
    )

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

        val file = getOCFileForTesting("image.jpg")

        `when`(intent.getParcelableExtra(FileActivity.EXTRA_FILE) as? Parcelable).thenReturn(file)
        intent.putExtra(FileActivity.EXTRA_FILE, file)

        `when`(ocCapabilityViewModel.getCapabilityForAccount()).thenReturn(capabilitiesLiveData)
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
}
