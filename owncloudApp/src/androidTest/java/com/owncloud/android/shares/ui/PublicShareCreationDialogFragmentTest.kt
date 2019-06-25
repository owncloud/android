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

import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
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
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.shares.presentation.fragment.PublicShareDialogFragment
import com.owncloud.android.utils.DateUtils
import com.owncloud.android.utils.TestUtil
import org.hamcrest.CoreMatchers.not
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

    private val file = mock(OCFile::class.java)

    @Test
    fun showDialogTitle() {
        loadPublicShareDialogFragment()
        onView(withId(R.id.publicShareDialogTitle)).check(matches(withText(R.string.share_via_link_create_title)))
    }

    @Test
    fun showMandatoryFields() {
        loadPublicShareDialogFragment()
        onView(withId(R.id.shareViaLinkNameSection)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkPasswordSection)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkExpirationSection)).check(matches(isDisplayed()))
    }

    @Test
    fun showDialogButtons() {
        loadPublicShareDialogFragment()
        onView(withId(R.id.cancelButton)).check(matches(isDisplayed()))
        onView(withId(R.id.saveButton)).check(matches(isDisplayed()))
    }

    @Test
    fun showFolderAdditionalFields() {
        loadPublicShareDialogFragment(
            TestUtil.createCapability(
                versionString = "10.0.1",
                sharingPublicUpload = CapabilityBooleanType.TRUE.value,
                sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value
            ),
            isFolder = true
        )
        onView(withId(R.id.shareViaLinkEditPermissionGroup)).check(matches(isDisplayed()))
    }

    @Test
    fun showDefaultLinkName() {
        loadPublicShareDialogFragment()
        onView(withId(R.id.shareViaLinkNameValue)).check(matches(withText("DOC_12112018.jpg link")))
    }

    @Test
    fun enablePasswordSwitch() {
        loadPublicShareDialogFragment()
        onView(withId(R.id.shareViaLinkPasswordSwitch)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordValue)).check(matches(isDisplayed()))
    }

    @Test
    fun checkPasswordNotVisible() {
        loadPublicShareDialogFragment()
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
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(sharingPublicPasswordEnforced = CapabilityBooleanType.TRUE.value)
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
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(
                sharingPublicExpireDateEnforced = CapabilityBooleanType.TRUE.value
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
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(
                sharingPublicExpireDateEnforced = CapabilityBooleanType.FALSE.value
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
        loadPublicShareDialogFragment()
        onView(withId(R.id.shareViaLinkExpirationSwitch)).perform(click())
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.shareViaLinkExpirationValue))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        //TODO: check the date form the picker
    }

    @Test
    fun cancelExpirationSwitch() {
        loadPublicShareDialogFragment()
        onView(withId(R.id.shareViaLinkExpirationSwitch)).perform(click())
        onView(withId(android.R.id.button2)).perform(click());
        onView(withId(R.id.shareViaLinkExpirationValue))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)))
    }

    @Test
    fun showError() {
        loadPublicShareDialogFragment(
            errorMessage = "Unable to share. Please check whether the file exists"
        )
        onView(withId(R.id.saveButton)).perform(click())
        onView(withId(R.id.public_link_error_message)).check(matches(isDisplayed()))
        onView(withId(R.id.public_link_error_message)).check(matches(withText(R.string.share_link_file_no_exist)))
    }

    @Test
    fun uploadPermissionsWithFolderDisplayed() {
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(
                versionString = "10.1.1",
                sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                sharingPublicUpload = CapabilityBooleanType.TRUE.value
            )
        )

        `when`(file.isFolder).thenReturn(true)
        onView(withId(R.id.shareViaLinkEditPermissionGroup)).check(matches(isDisplayed()))
    }

    @Test
    fun uploadPermissionsWithFolderNotDisplayed() {
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(
                versionString = "10.1.1",
                sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                sharingPublicUpload = CapabilityBooleanType.FALSE.value
            )
        )

        `when`(file.isFolder).thenReturn(true)
        onView(withId(R.id.shareViaLinkEditPermissionGroup)).check(matches(not(isDisplayed())))
    }

    @Test
    fun expirationDateDays() {
        val daysToTest = 15
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(
                versionString = "10.1.1",
                sharingPublicExpireDateDays = daysToTest
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
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(
                versionString = "10.1.1",
                sharingPublicPasswordEnforced = CapabilityBooleanType.FALSE.value
            )
        )
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_label)))
    }

    @Test
    fun passwordEnforced() {
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(
                versionString = "10.1.1",
                sharingPublicPasswordEnforced = CapabilityBooleanType.TRUE.value
            )
        )
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_enforced_label)))
    }

    @Test
    fun passwordEnforcedReadOnlyFolders() {
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(
                versionString = "10.1.1",
                sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                sharingPublicUpload = CapabilityBooleanType.TRUE.value,
                sharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.TRUE.value,
                sharingPublicPasswordEnforced = CapabilityBooleanType.TRUE.value
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
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(
                versionString = "10.1.1",
                sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                sharingPublicUpload = CapabilityBooleanType.TRUE.value,
                sharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.FALSE.value,
                sharingPublicPasswordEnforced = CapabilityBooleanType.FALSE.value
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
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(
                versionString = "10.1.1",
                sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                sharingPublicUpload = CapabilityBooleanType.TRUE.value,
                sharingPublicPasswordEnforcedReadWrite = CapabilityBooleanType.TRUE.value,
                sharingPublicPasswordEnforced = CapabilityBooleanType.TRUE.value
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
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(
                versionString = "10.1.1",
                sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                sharingPublicUpload = CapabilityBooleanType.TRUE.value,
                sharingPublicPasswordEnforcedReadWrite = CapabilityBooleanType.FALSE.value,
                sharingPublicPasswordEnforced = CapabilityBooleanType.FALSE.value
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
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(
                versionString = "10.1.1",
                sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                sharingPublicUpload = CapabilityBooleanType.TRUE.value,
                sharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.TRUE.value,
                sharingPublicPasswordEnforced = CapabilityBooleanType.FALSE.value
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
        loadPublicShareDialogFragment(
            capabilities = TestUtil.createCapability(
                versionString = "10.1.1",
                sharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE.value,
                sharingPublicUpload = CapabilityBooleanType.TRUE.value,
                sharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.FALSE.value,
                sharingPublicPasswordEnforced = CapabilityBooleanType.FALSE.value
            )
        )
        `when`(file.isFolder).thenReturn(true)
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_label)))
    }

    private fun loadPublicShareDialogFragment(
        capabilities: OCCapability = TestUtil.createCapability(),
        errorMessage: String = "Common error",
        isFolder: Boolean = false
    ) {
        val defaultLinkName = "DOC_12112018.jpg link"
        val filePath = "/Documents/doc3"
        val fileMimeType = ".txt"

        `when`(file.remotePath).thenReturn(filePath)
        `when`(file.mimetype).thenReturn(fileMimeType)
        `when`(file.isFolder).thenReturn(isFolder)

        val publicShareDialogFragment = PublicShareDialogFragment.newInstanceToCreate(
            file,
            defaultLinkName
        )

        activityRule.activity.capabilities = capabilities
        activityRule.activity.errorMessage = errorMessage
        activityRule.activity.setFragment(publicShareDialogFragment)
    }
}
