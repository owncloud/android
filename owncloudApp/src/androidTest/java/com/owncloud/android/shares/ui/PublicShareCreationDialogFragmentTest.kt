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
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withInputType
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.capabilities.viewmodel.OCCapabilityViewModel
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.shares.ui.fragment.PublicShareDialogFragment
import com.owncloud.android.shares.viewmodel.OCShareViewModel
import com.owncloud.android.utils.DateUtils
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
import java.text.SimpleDateFormat
import java.util.Date

@RunWith(AndroidJUnit4::class)
class PublicShareCreationDialogFragmentTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(TestShareFileActivity::class.java, true, true)

    private val account = mock(Account::class.java)
    private val capabilitiesLiveData = MutableLiveData<Resource<OCCapability>>()
    private val sharesLiveData = MutableLiveData<Resource<List<OCShare>>>()
    private val file = mock(OCFile::class.java)

    private val publicShares = arrayListOf(
        TestUtil.createPublicShare(
            path = "/Documents/",
            isFolder = true,
            name = "Document link",
            shareLink = "http://server:port/s/1"
        ),
        TestUtil.createPublicShare(
            path = "/Documents/doc1",
            isFolder = false,
            name = "Document link",
            shareLink = "http://server:port/s/2"
        ),
        TestUtil.createPublicShare(
            shareWith = "1fa2P4QAq3412",
            path = "/Documents/doc2",
            isFolder = false,
            name = "Document link 2",
            shareLink = "http://server:port/s/3"
        )
    )

    @Before
    fun setUp() {
        val defaultLinkName = "DOC_12112018.jpg link"

        val publicShareDialogFragment = PublicShareDialogFragment.newInstanceToCreate(
            file,
            account,
            defaultLinkName
        )

        val filePath = "/Documents/doc3"

        file.mimetype = ".txt"
        `when`(file.remotePath).thenReturn(filePath)
        val ocCapabilityViewModel = mock(OCCapabilityViewModel::class.java)
        `when`(
            ocCapabilityViewModel.getCapabilityForAccount()
        ).thenReturn(capabilitiesLiveData)

        val ocShareViewModel = mock(OCShareViewModel::class.java)
        `when`(
            ocShareViewModel.insertPublicShareForFile(
                1,
                defaultLinkName,
                "",
                -1,
                false
            )
        ).thenReturn(sharesLiveData)

        publicShareDialogFragment.ocCapabilityViewModelFactory = ViewModelUtil.createFor(ocCapabilityViewModel)
        publicShareDialogFragment.ocShareViewModelFactory = ViewModelUtil.createFor(ocShareViewModel)

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
    fun showDefaultLinkName() {
        onView(withId(R.id.shareViaLinkNameValue)).check(matches(withText("DOC_12112018.jpg link")))
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
    fun checkPasswordEnforced() {
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(sharingPublicPasswordEnforced = CapabilityBooleanType.TRUE.value)
            )
        )

        onView(withId(R.id.shareViaLinkPasswordLabel)).check(
            matches(withText(R.string.share_via_link_password_enforced_label))
        )
        onView(withId(R.id.shareViaLinkPasswordSwitch))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        onView(withId(R.id.shareViaLinkPasswordValue))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    @Test
    fun checkExpireDateEnforced() {
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(
                    sharingPublicExpireDateEnforced = CapabilityBooleanType.TRUE.value
                )
            )
        )

        onView(withId(R.id.shareViaLinkExpirationLabel))
            .check(matches(withText(R.string.share_via_link_expiration_date_enforced_label)))
        onView(withId(R.id.shareViaLinkExpirationSwitch))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        onView(withId(R.id.shareViaLinkExpirationExplanationLabel))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    @Test
    fun checkExpireDateNotEnforced() {
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(
                    sharingPublicExpireDateEnforced = CapabilityBooleanType.FALSE.value
                )
            )
        )

        onView(withId(R.id.shareViaLinkExpirationLabel))
            .check(matches(withText(R.string.share_via_link_expiration_date_label)))
        onView(withId(R.id.shareViaLinkExpirationSwitch))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        onView(withId(R.id.shareViaLinkExpirationExplanationLabel))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    @Test
    fun enableExpirationSwitch() {
        onView(withId(R.id.shareViaLinkExpirationSwitch)).perform(click())
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.shareViaLinkExpirationValue))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        //TODO: check the date form the picker
    }

    @Test
    fun cancelExpirationSwitch() {
        onView(withId(R.id.shareViaLinkExpirationSwitch)).perform(click())
        onView(withId(android.R.id.button2)).perform(click());
        onView(withId(R.id.shareViaLinkExpirationValue))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)))
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
    fun uploadPermissionsWithFolderDisplayed() {
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(
                    versionString = "10.1.1",
                    sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                    sharingPublicUpload = CapabilityBooleanType.TRUE.value
                )
            )
        )

        `when`(file.isFolder).thenReturn(true)
        onView(withId(R.id.shareViaLinkEditPermissionGroup)).check(matches(isDisplayed()))
    }

    @Test
    fun uploadPermissionsWithFolderNotDisplayed() {
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(
                    versionString = "10.1.1",
                    sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                    sharingPublicUpload = CapabilityBooleanType.FALSE.value
                )
            )
        )

        `when`(file.isFolder).thenReturn(true)
        onView(withId(R.id.shareViaLinkEditPermissionGroup)).check(matches(not(isDisplayed())))
    }

    @Test
    fun expirationDateDays() {
        val daysToTest = 15
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(
                    versionString = "10.1.1",
                    sharingPublicExpireDateDays = daysToTest
                )
            )
        )
        val formattedDate = SimpleDateFormat.getDateInstance().format(
            DateUtils.addDaysToDate(
                Date(),
                daysToTest
            )
        )
        onView(withId(R.id.shareViaLinkExpirationSwitch))
            .check(matches(isEnabled()))
        onView(withId(R.id.shareViaLinkExpirationValue))
            .check(matches(withText(formattedDate)))
    }

    @Test
    fun passwordNotEnforced() {
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(
                    versionString = "10.1.1",
                    sharingPublicPasswordEnforced = CapabilityBooleanType.FALSE.value
                )
            )
        )
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_label)))
    }

    @Test
    fun passwordEnforced() {
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(
                    versionString = "10.1.1",
                    sharingPublicPasswordEnforced = CapabilityBooleanType.TRUE.value
                )
            )
        )
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_enforced_label)))
    }

    @Test
    fun passwordEnforcedReadOnlyFolders() {
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(
                    versionString = "10.1.1",
                    sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                    sharingPublicUpload = CapabilityBooleanType.TRUE.value,
                    sharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.TRUE.value,
                    sharingPublicPasswordEnforced = CapabilityBooleanType.TRUE.value
                )
            )
        )

        `when`(file.isFolder).thenReturn(true)
        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_enforced_label)))
    }

    @Test
    fun passwordNotEnforcedReadOnlyFolders() {
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(
                    versionString = "10.1.1",
                    sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                    sharingPublicUpload = CapabilityBooleanType.TRUE.value,
                    sharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.FALSE.value,
                    sharingPublicPasswordEnforced = CapabilityBooleanType.FALSE.value
                )
            )
        )

        `when`(file.isFolder).thenReturn(true)
        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_label)))
    }

    @Test
    fun passwordEnforcedReadWriteFolders() {
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(
                    versionString = "10.1.1",
                    sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                    sharingPublicUpload = CapabilityBooleanType.TRUE.value,
                    sharingPublicPasswordEnforcedReadWrite = CapabilityBooleanType.TRUE.value,
                    sharingPublicPasswordEnforced = CapabilityBooleanType.TRUE.value
                )
            )
        )

        `when`(file.isFolder).thenReturn(true)
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_enforced_label)))
    }

    @Test
    fun passwordNotEnforcedReadWriteFolders() {
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(
                    versionString = "10.1.1",
                    sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                    sharingPublicUpload = CapabilityBooleanType.TRUE.value,
                    sharingPublicPasswordEnforcedReadWrite = CapabilityBooleanType.FALSE.value,
                    sharingPublicPasswordEnforced = CapabilityBooleanType.FALSE.value
                )
            )
        )

        `when`(file.isFolder).thenReturn(true)
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_label)))
    }

    @Test
    fun passwordEnforcedUploadOnlyFolders() {
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(
                    versionString = "10.1.1",
                    sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                    sharingPublicUpload = CapabilityBooleanType.TRUE.value,
                    sharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.TRUE.value,
                    sharingPublicPasswordEnforced = CapabilityBooleanType.FALSE.value
                )
            )
        )

        `when`(file.isFolder).thenReturn(true)
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_enforced_label)))
    }

    @Test
    fun passwordNotEnforcedUploadOnlyFolders() {
        capabilitiesLiveData.postValue(
            Resource.success(
                TestUtil.createCapability(
                    versionString = "10.1.1",
                    sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                    sharingPublicUpload = CapabilityBooleanType.TRUE.value,
                    sharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.FALSE.value,
                    sharingPublicPasswordEnforced = CapabilityBooleanType.FALSE.value
                )
            )
        )

        `when`(file.isFolder).thenReturn(true)
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_label)))
    }


}
