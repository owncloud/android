package com.owncloud.android.shares.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.MainApp.Companion.accountType
import com.owncloud.android.authentication.AccountAuthenticator.KEY_AUTH_TOKEN_TYPE
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.capabilities.viewmodel.OCCapabilityViewModel
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.viewmodel.OCShareViewModel
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.AccountsManager
import com.owncloud.android.utils.TestUtil
import com.owncloud.android.vo.Resource
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Spy

class CreatePublicShareTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(
        ShareActivity::class.java,
        true,
        false
    )

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private val capabilitiesLiveData = MutableLiveData<Resource<OCCapability>>()
    private val sharesLiveData = MutableLiveData<Resource<List<OCShare>>>()

    var ocCapabilityViewModel = mock(OCCapabilityViewModel::class.java)
    var ocShareViewModel = mock(OCShareViewModel::class.java)

    @Spy
    val account = Account("admin", "owncloud")

    @Spy
    val file = OCFile("/test")

    @Before
    fun setUp() {
        addAccount()

        val intent = spy(Intent::class.java)

        `when`(intent.getParcelableExtra(FileActivity.EXTRA_ACCOUNT) as? Parcelable).thenReturn(account)
        intent.putExtra(FileActivity.EXTRA_ACCOUNT, account)

        `when`(intent.getParcelableExtra(FileActivity.EXTRA_FILE) as? Parcelable).thenReturn(file)
        intent.putExtra(FileActivity.EXTRA_FILE, file)

        `when`(ocCapabilityViewModel.getCapabilityForAccount()).thenReturn(capabilitiesLiveData)
        `when`(ocShareViewModel.getSharesForFile()).thenReturn(sharesLiveData)

        loadKoinModules(module(override = true) {
            viewModel {
                ocCapabilityViewModel
            }
            viewModel {
                ocShareViewModel
            }
        })

        activityRule.launchActivity(intent)
    }

    @Test
    fun letsSee() {
        Thread.sleep(10000)
    }

    @After
    fun cleanUp() {
        stopKoin()
        AccountsManager.deleteAllAccounts(targetContext)
    }

    private val KEY_AUTH_TOKEN_TYPE = "AUTH_TOKEN_TYPE"
    private val KEY_AUTH_TOKEN = "AUTH_TOKEN"
    private val version = "10.2"

    private fun addAccount(): Account {
        // obtaining an AccountManager instance
        val accountManager = AccountManager.get(targetContext)

        Thread(Runnable {
            accountManager.addAccountExplicitly(account, "a", null)

            // include account version, user, server version and token with the new account
            accountManager.setUserData(
                account,
                AccountUtils.Constants.KEY_OC_VERSION,
                OwnCloudVersion(version).toString()
            )
            accountManager.setUserData(
                account,
                AccountUtils.Constants.KEY_OC_BASE_URL,
                "10.40.40.198:29000"
            )
            accountManager.setUserData(
                account,
                AccountUtils.Constants.KEY_DISPLAY_NAME,
                "user1"
            )

            accountManager.setAuthToken(
                account,
                KEY_AUTH_TOKEN_TYPE,
                KEY_AUTH_TOKEN
            )
        }).start()

        return account
    }
}
