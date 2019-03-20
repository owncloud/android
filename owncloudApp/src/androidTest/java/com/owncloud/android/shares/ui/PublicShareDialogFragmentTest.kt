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

package com.owncloud.android.shares.ui

import android.accounts.Account
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withInputType
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.ui.fragment.PublicShareDialogFragment
import com.owncloud.android.shares.viewmodel.OCShareViewModel
import com.owncloud.android.utils.TestUtil
import com.owncloud.android.utils.ViewModelUtil
import com.owncloud.android.vo.Resource
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class PublicShareDialogFragmentTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(TestShareFileActivity::class.java, true, true)

    private val sharesLiveData = MutableLiveData<Resource<List<OCShare>>>()
    private val file = mock(OCFile::class.java)

    private val publicShares = arrayListOf(
        TestUtil.createPublicShare(
            path = "/Documents/doc1",
            isFolder = false,
            name = "Document link",
            shareLink = "http://server:port/s/1"
        ),
        TestUtil.createPublicShare(
            path = "/Documents/doc2",
            isFolder = false,
            name = "Document link 2",
            shareLink = "http://server:port/s/2"
        )
    )

    @Before
    fun setUp() {
        val account = mock(Account::class.java)
        val defaultLinkName = "DOC_12112018.jpg link"

        val publicShareDialogFragment = PublicShareDialogFragment.newInstanceToCreate(
            file,
            account,
            defaultLinkName
        )

        val filePath = "/Documents/doc3"

        file.mimetype = ".txt"
        `when`(file.remotePath).thenReturn(filePath)

        val ocShareViewModel = mock(OCShareViewModel::class.java)
        `when`(
            ocShareViewModel.insertPublicShareForFile(
                filePath,
                1,
                defaultLinkName,
                "",
                -1,
                false
            )
        ).thenReturn(sharesLiveData)

        publicShareDialogFragment.viewModelFactory = ViewModelUtil.createFor(ocShareViewModel)
        activityRule.activity.setFragment(publicShareDialogFragment)
    }

    @Test
    fun showDialogTitle() {
        onView(withId(R.id.publicShareDialogTitle)).check(matches(withText(R.string.share_via_link_create_title)))
    }

    @Test
    fun showMandatoryFields() {
        onView(withId(R.id.shareViaLinkNameSection)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkPasswordSection)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkExpirationSection)).check(matches(isDisplayed()))
    }

    @Test
    fun showDialogButtons() {
        onView(withId(R.id.cancelButton)).check(matches(isDisplayed()))
        onView(withId(R.id.saveButton)).check(matches(isDisplayed()))
    }

    @Test
    fun showFolderAdditionalFields() {
        file.mimetype = "DIR"
        onView(withId(R.id.shareViaLinkEditPermissionGroup)).check(matches(isDisplayed()))
    }

    @Test
    fun enablePasswordSwitch() {
        onView(withId(R.id.shareViaLinkPasswordSwitch)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordValue)).check(matches(isDisplayed()))
    }

    @Test
    fun checkPasswordNotVisible() {
        onView(withId(R.id.shareViaLinkPasswordSwitch)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordValue)).perform(typeText("supersecure"))

        onView(withId(R.id.shareViaLinkPasswordValue)).check(
            matches(
                withInputType(
                    TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                )
            )
        )
    }

    @Test
    fun enableExpirationSwitch() {
//        onView(withId(R.id.shareViaLinkExpirationSwitch)).perform(click())
        // TODO Check date picker
    }

    @Test
    fun showError() {
        onView(withId(R.id.saveButton)).perform(click())

        sharesLiveData.postValue(
            Resource.error(
                RemoteOperationResult.ResultCode.SHARE_NOT_FOUND,
                data = publicShares
            )
        )

        onView(withId(R.id.public_link_error_message)).check(matches(isDisplayed()))
        onView(withId(R.id.public_link_error_message)).check(matches(withText(R.string.share_link_file_no_exist)))
    }

    @Test
    fun dismissDialog() {
        onView(withId(R.id.saveButton)).perform(click())

        sharesLiveData.postValue(
            Resource.success(publicShares)
        )

        Log.d("HEEY", activityRule.activity.supportFragmentManager.findFragmentByTag("TEST FRAGMENT")?.toString())

        onView(withText("Create link share")).check(matches(not(isDisplayed())))
    }
}
