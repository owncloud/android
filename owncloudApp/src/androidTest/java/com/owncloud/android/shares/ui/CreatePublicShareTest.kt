package com.owncloud.android.shares.ui

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
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.shares.db.OCShare
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

        file = getOCFileForTesting("image.jpg")

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
    fun createPublicShareWithNoPublicSharesYet() {
        loadCapabilitiesSuccessfully()
        loadSharesSuccessfully(arrayListOf())

        val newPublicShare = publicShares[0]

        `when`(
            ocShareViewModel.insertPublicShareForFile(
                1,
                newPublicShare.name!!,
                "",
                -1,
                false
            )
        ).thenReturn(sharesLiveData)

        // 1. Open dialog to create new public share
        onView(withId(R.id.addPublicLinkButton)).perform(click())

        // 2. Save share
        onView(withId(R.id.saveButton)).perform(click())

        // 3. New share properly created
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

        val newPublicShare = publicShares[2]

        `when`(
            ocShareViewModel.insertPublicShareForFile(
                1,
                newPublicShare.name!!,
                "",
                -1,
                false
            )
        ).thenReturn(sharesLiveData)

        onView(withId(R.id.addPublicLinkButton)).perform(click())
        onView(withId(R.id.saveButton)).perform(click())

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
        val newPublicShare1 = publicShares[0]

        createPublicShareSuccesfully(newPublicShare1, arrayListOf(newPublicShare1))

        // Check whether the dialog to create the public share has been properly closed
        onView(withText(R.string.share_via_link_create_title)).check(doesNotExist())
        onView(withText(newPublicShare1.name)).check(matches(isDisplayed()))

        /**
         * 2nd public share
         */
        val newPublicShare2 = publicShares[1]

        createPublicShareSuccesfully(newPublicShare2, publicShares.take(2))

        // Check whether the dialog to create the public share has been properly closed
        onView(withText(R.string.share_via_link_create_title)).check(doesNotExist())
        onView(withText(newPublicShare2.name)).check(matches(isDisplayed()))

        /**
         * 3rd public share
         */
        val newPublicShare3 = publicShares[2]

        createPublicShareSuccesfully(newPublicShare3, publicShares)

        // Check whether the dialog to create the public share has been properly closed
        onView(withText(R.string.share_via_link_create_title)).check(doesNotExist())
        onView(withText(newPublicShare3.name)).check(matches(isDisplayed()))
    }

    private fun getOCFileForTesting(name: String = "default") = OCFile("/Photos").apply {
        availableOfflineStatus = OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
        fileName = name
        fileId = 9456985479
        remoteId = "1"
        privateLink = "private link"
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

    private fun createPublicShareSuccesfully(newShare: OCShare, sharesAfterCreation: List<OCShare>) {
        `when`(
            ocShareViewModel.insertPublicShareForFile(
                1,
                newShare.name!!,
                "",
                -1,
                false
            )
        ).thenReturn(sharesLiveData)

        // 1. Open dialog to create new public share
        onView(withId(R.id.addPublicLinkButton)).perform(click())

        // 2. Save share
        onView(withId(R.id.saveButton)).perform(click())

        // 3. New share properly created
        sharesLiveData.postValue(
            Resource.success(
                sharesAfterCreation
            )
        )
    }
}
