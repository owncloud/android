package com.owncloud.android.ui.shares

import android.accounts.Account
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.testing.SingleFragmentActivity
import com.owncloud.android.ui.fragment.ShareFileFragment
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class ShareFragmentTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(SingleFragmentActivity::class.java, true, true)
    private val account = mock(Account::class.java)

    private val shareFragment = ShareFileFragment.newInstance(getOCFileForTesting(), account)

    @Before
    fun init() {
        activityRule.activity.setFragment(shareFragment)
    }

    @Test
    fun test() {
        onView(withText(R.string.share_with_user_section_title)).check(matches(isDisplayed()))
    }

    fun getOCFileForTesting(name: String = "default"): OCFile {
        var file = OCFile("/Photos")
        file.availableOfflineStatus = OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
        file.fileName = name
        file.fileId = 9456985479
        return file
    }
}