/**
 * ownCloud Android client application
 *
 * @author Jesús Recio (@jesmrec)
 * @author David González (@davigonz)
 * @author Abel García (@abelgardep)
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
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
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

class EditPublicShareFolderTest {
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
            path = "/Photos/",
            expirationDate = 0L,
            permissions = 1,
            isFolder = true,
            name = "Photos link",
            shareLink = "http://server:port/s/1"
        ),
        DUMMY_SHARE.copy(  // With name updated
            path = "/Photos/",
            expirationDate = 0L,
            permissions = 1,
            isFolder = true,
            name = "Photos updated link",
            shareLink = "http://server:port/s/1"
        ),
        DUMMY_SHARE.copy( // With permission Download/View/Upload
            path = "/Photos/",
            expirationDate = 0L,
            permissions = 15,
            isFolder = true,
            name = "Photos link",
            shareLink = "http://server:port/s/1"
        ),
        DUMMY_SHARE.copy( // With permission Upload only
            path = "/Photos/",
            expirationDate = 0L,
            permissions = 4,
            isFolder = true,
            name = "Photos link",
            shareLink = "http://server:port/s/1"
        ),
        DUMMY_SHARE.copy( // With password
            path = "/Photos/",
            expirationDate = 0L,
            isFolder = true,
            name = "Photos link",
            shareLink = "http://server:port/s/1",
            shareWith = "1"
        )
    )

    private val capabilitiesLiveData = MutableLiveData<UIResult<OCCapability>>()
    private val sharesLiveData = MutableLiveData<UIResult<List<OCShare>>>()
    private val publicShareEditionStatusLiveData = MutableLiveData<UIResult<Unit>>()

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

        file = getOCFileForTesting("Photos")
        intent.putExtra(FileActivity.EXTRA_FILE, file)

        every { ocCapabilityViewModel.capabilities } returns capabilitiesLiveData
        every { ocShareViewModel.shares } returns sharesLiveData
        every { ocShareViewModel.publicShareEditionStatus } returns publicShareEditionStatusLiveData
        every { ocShareViewModel.privateShare } returns MutableLiveData()

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
        sharesLiveData.postValue(UIResult.Success(listOf(existingPublicShare)))

        val updatedPublicShare = publicShares[1]

        // 1. Open dialog to edit an existing public share
        onView(withId(R.id.editPublicLinkButton)).perform(click())

        // 2. Update fields
        onView(withId(R.id.shareViaLinkNameValue)).perform(replaceText(updatedPublicShare.name))

        // 3. Save updated share
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())

        verify {
            ocShareViewModel.updatePublicShare(
                remoteId = 1,
                name = updatedPublicShare.name!!,
                password = "",
                expirationDateInMillis = -1,
                permissions = 1,
                publicUpload = false,
                accountName = "user@server"
            )
        }

        // 4. Share properly updated
        publicShareEditionStatusLiveData.postValue(UIResult.Success())
        sharesLiveData.postValue(
            UIResult.Success(
                listOf(updatedPublicShare)
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
        sharesLiveData.postValue(UIResult.Success(listOf(existingPublicShare)))

        val updatedPublicShare = publicShares[2]

        // 1. Open dialog to edit an existing public share
        onView(withId(R.id.editPublicLinkButton)).perform(click())

        // 2. Update fields
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).perform(click())

        // 3. Save updated share
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())

        verify {
            ocShareViewModel.updatePublicShare(
                remoteId = 1,
                name = updatedPublicShare.name!!,
                password = "",
                expirationDateInMillis = -1,
                permissions = 15,
                publicUpload = true,
                accountName = "user@server"
            )
        }

        // 4. Share properly updated
        publicShareEditionStatusLiveData.postValue(UIResult.Success())
        sharesLiveData.postValue(
            UIResult.Success(
                listOf(updatedPublicShare)
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
        sharesLiveData.postValue(UIResult.Success(listOf(existingPublicShare)))

        val updatedPublicShare = publicShares[3]

        // 1. Open dialog to edit an existing public share
        onView(withId(R.id.editPublicLinkButton)).perform(click())

        // 2. Update fields
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).perform(click())

        // 3. Save updated share
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())

        verify {
            ocShareViewModel.updatePublicShare(
                remoteId = 1,
                name = updatedPublicShare.name!!,
                password = "",
                expirationDateInMillis = -1,
                permissions = 4,
                publicUpload = true,
                accountName = "user@server"
            )
        }

        // 4. Share properly updated
        publicShareEditionStatusLiveData.postValue(UIResult.Success())
        sharesLiveData.postValue(
            UIResult.Success(
                listOf(updatedPublicShare)
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
        sharesLiveData.postValue(UIResult.Success(listOf(existingPublicShare)))

        val updatedPublicShare = publicShares[0]

        // 1. Open dialog to edit an existing public share
        onView(withId(R.id.editPublicLinkButton)).perform(click())

        // 2. Update fields
        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).perform(click())

        // 3. Save updated share
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())

        verify {
            ocShareViewModel.updatePublicShare(
                remoteId = 1,
                name = updatedPublicShare.name!!,
                password = "",
                expirationDateInMillis = -1,
                permissions = 1,
                publicUpload = false,
                accountName = "user@server"
            )
        }

        // 4. Share properly updated
        publicShareEditionStatusLiveData.postValue(UIResult.Success())
        sharesLiveData.postValue(
            UIResult.Success(
                listOf(updatedPublicShare)
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

    private fun loadCapabilitiesSuccessfully() {
        capabilitiesLiveData.postValue(
            UIResult.Success(
                DUMMY_CAPABILITY.copy(
                    versionString = "10.1.1",
                    filesSharingPublicMultiple = CapabilityBooleanType.TRUE,
                    filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE,
                    filesSharingPublicUpload = CapabilityBooleanType.TRUE
                )
            )
        )
    }
}
