package com.owncloud.android.sharees.presentation.ui

import android.accounts.Account
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.shares.domain.OCShare
import com.owncloud.android.sharees.presentation.SearchShareesFragment
import com.owncloud.android.shares.presentation.ui.TestShareFileActivity
import com.owncloud.android.utils.TestUtil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class SearchShareesFragmentTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(
        TestShareFileActivity::class.java,
        true,
        true
    )

    private var privateShareList = arrayListOf(
        TestUtil.createPrivateShare(
            path = "/Docs/flat_agreement.jpg",
            isFolder = false,
            shareWith = "sheldon",
            sharedWithDisplayName = "Sheldon"
        ),
        TestUtil.createPrivateShare(
            path = "/Docs/flat_agreement.jpg",
            isFolder = false,
            shareWith = "leonard",
            sharedWithDisplayName = "Leonard"
        )
    )

    @Test
    fun showSearchBar() {
        loadSearchShareesFragment()
        onView(withId(R.id.search_mag_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.search_plate)).check(matches(isDisplayed()))
    }

    @Test
    fun showPrivateShares() {
        loadSearchShareesFragment()
        onView(withText("Sheldon")).check(matches(isDisplayed()))
        onView(withText("Sheldon")).check(matches(hasSibling(withId(R.id.unshareButton))))
            .check(matches(isDisplayed()))
        onView(withText("Sheldon")).check(matches(hasSibling(withId(R.id.editShareButton))))
            .check(matches(isDisplayed()))
        onView(withText("Leonard")).check(matches(isDisplayed()))
    }

    private fun loadSearchShareesFragment(
        capabilities: OCCapability = TestUtil.createCapability(),
        privateShares: ArrayList<OCShare> = privateShareList
    ) {
        val account = Mockito.mock(Account::class.java)
        val ownCloudVersion = Mockito.mock(OwnCloudVersion::class.java)
        Mockito.`when`(ownCloudVersion.isSearchUsersSupported).thenReturn(true)

        val searchShareesFragment = SearchShareesFragment.newInstance(
            getOCFileForTesting("image.jpg"),
            account
        )

        activityRule.activity.capabilities = capabilities
        activityRule.activity.privateShares = privateShares
        activityRule.activity.setFragment(searchShareesFragment)
    }

    private fun getOCFileForTesting(name: String = "default") = OCFile("/Docs").apply {
        availableOfflineStatus = OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE
        fileName = name
        fileId = 9456985479
        remoteId = "1"
        privateLink = "private link"
    }
}
