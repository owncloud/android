/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.sharing.shares.ui

import android.content.Context
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withInputType
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.owncloud.android.R
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.ui.sharing.fragments.PublicShareDialogFragment
import com.owncloud.android.presentation.viewmodels.capabilities.OCCapabilityViewModel
import com.owncloud.android.presentation.viewmodels.sharing.OCShareViewModel
import com.owncloud.android.testutil.OC_ACCOUNT
import com.owncloud.android.testutil.OC_CAPABILITY
import com.owncloud.android.testutil.OC_FILE
import com.owncloud.android.testutil.OC_FOLDER
import com.owncloud.android.utils.DateUtils
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.text.SimpleDateFormat
import java.util.Date

class PublicShareCreationDialogFragmentTest {
    private val ocCapabilityViewModel = mockk<OCCapabilityViewModel>(relaxed = true)
    private val capabilitiesLiveData = MutableLiveData<Event<UIResult<OCCapability>>>()
    private val ocShareViewModel = mockk<OCShareViewModel>(relaxed = true)
    private val publicShareCreationStatus = MutableLiveData<Event<UIResult<Unit>>>()

    @Before
    fun setUp() {
        every { ocCapabilityViewModel.capabilities } returns capabilitiesLiveData
        every { ocShareViewModel.publicShareCreationStatus } returns publicShareCreationStatus

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
    }

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
            isFolder = true,
            capabilities = OC_CAPABILITY.copy(
                versionString = "10.0.1",
                filesSharingPublicUpload = CapabilityBooleanType.TRUE,
                filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE
            )
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
            capabilities = OC_CAPABILITY.copy(
                filesSharingPublicPasswordEnforced = CapabilityBooleanType.TRUE
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
        loadPublicShareDialogFragment(
            capabilities = OC_CAPABILITY.copy(
                filesSharingPublicExpireDateEnforced = CapabilityBooleanType.TRUE
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
            capabilities = OC_CAPABILITY.copy(
                filesSharingPublicExpireDateEnforced = CapabilityBooleanType.FALSE
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
        onView(withId(android.R.id.button1)).perform(click())
        onView(withId(R.id.shareViaLinkExpirationValue))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        //TODO: check the date form the picker
    }

    @Test
    fun cancelExpirationSwitch() {
        loadPublicShareDialogFragment()
        onView(withId(R.id.shareViaLinkExpirationSwitch)).perform(click())
        onView(withId(android.R.id.button2)).perform(click())
        onView(withId(R.id.shareViaLinkExpirationValue))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)))
    }

    @Test
    fun showError() {
        loadPublicShareDialogFragment()

        onView(withId(R.id.saveButton)).perform(click())

        publicShareCreationStatus.postValue(
            Event(
                UIResult.Error(
                    error = Throwable("It was not possible to share this file or folder")
                )
            )
        )

        onView(withId(R.id.public_link_error_message)).check(matches(isDisplayed()))
        onView(withId(R.id.public_link_error_message)).check(
            matches(
                withText(R.string.share_link_file_error)
            )
        )
    }

    @Test
    fun uploadPermissionsWithFolderDisplayed() {
        loadPublicShareDialogFragment(
            isFolder = true,
            capabilities = OC_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE,
                filesSharingPublicUpload = CapabilityBooleanType.TRUE
            )
        )

        onView(withId(R.id.shareViaLinkEditPermissionGroup)).check(matches(isDisplayed()))
    }

    @Test
    fun uploadPermissionsWithFolderNotDisplayed() {
        loadPublicShareDialogFragment(
            isFolder = true,
            capabilities = OC_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE,
                filesSharingPublicUpload = CapabilityBooleanType.FALSE
            )
        )

        onView(withId(R.id.shareViaLinkEditPermissionGroup)).check(matches(not(isDisplayed())))
    }

    @Test
    fun expirationDateDays() {
        val daysToTest = 15
        loadPublicShareDialogFragment(
            capabilities = OC_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicExpireDateDays = daysToTest
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
            capabilities = OC_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicPasswordEnforced = CapabilityBooleanType.FALSE
            )
        )
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_label)))
    }

    @Test
    fun passwordEnforced() {
        loadPublicShareDialogFragment(
            capabilities = OC_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicPasswordEnforced = CapabilityBooleanType.TRUE
            )
        )
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_enforced_label)))
    }

    @Test
    fun passwordEnforcedReadOnlyFolders() {
        loadPublicShareDialogFragment(
            isFolder = true,
            capabilities = OC_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE,
                filesSharingPublicUpload = CapabilityBooleanType.TRUE,
                filesSharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.TRUE,
                filesSharingPublicPasswordEnforced = CapabilityBooleanType.TRUE
            )
        )

        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).perform(scrollTo())
        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_enforced_label)))
    }

    @Test
    fun passwordNotEnforcedReadOnlyFolders() {
        loadPublicShareDialogFragment(
            isFolder = true,
            capabilities = OC_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE,
                filesSharingPublicUpload = CapabilityBooleanType.TRUE,
                filesSharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.FALSE,
                filesSharingPublicPasswordEnforced = CapabilityBooleanType.FALSE
            )
        )
        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkEditPermissionReadOnly)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_label)))
    }

    @Test
    fun passwordEnforcedReadWriteFolders() {
        loadPublicShareDialogFragment(
            isFolder = true,
            capabilities = OC_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE,
                filesSharingPublicUpload = CapabilityBooleanType.TRUE,
                filesSharingPublicPasswordEnforcedReadWrite = CapabilityBooleanType.TRUE,
                filesSharingPublicPasswordEnforced = CapabilityBooleanType.TRUE
            )
        )
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_enforced_label)))
    }

    @Test
    fun passwordNotEnforcedReadWriteFolders() {
        loadPublicShareDialogFragment(
            isFolder = true,
            capabilities = OC_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE,
                filesSharingPublicUpload = CapabilityBooleanType.TRUE,
                filesSharingPublicPasswordEnforcedReadWrite = CapabilityBooleanType.FALSE,
                filesSharingPublicPasswordEnforced = CapabilityBooleanType.FALSE
            )
        )
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkEditPermissionReadAndWrite)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_label)))
    }

    @Test
    fun passwordEnforcedUploadOnlyFolders() {
        loadPublicShareDialogFragment(
            isFolder = true,
            capabilities = OC_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE,
                filesSharingPublicUpload = CapabilityBooleanType.TRUE,
                filesSharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.TRUE,
                filesSharingPublicPasswordEnforced = CapabilityBooleanType.FALSE
            )
        )
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_enforced_label)))
    }

    @Test
    fun passwordNotEnforcedUploadOnlyFolders() {
        loadPublicShareDialogFragment(
            isFolder = true,
            capabilities = OC_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE,
                filesSharingPublicUpload = CapabilityBooleanType.TRUE,
                filesSharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.FALSE,
                filesSharingPublicPasswordEnforced = CapabilityBooleanType.FALSE
            )
        )
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).check(matches(isDisplayed()))
        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).perform(click())
        onView(withId(R.id.shareViaLinkPasswordLabel))
            .check(matches(withText(R.string.share_via_link_password_label)))
    }

    @Test
    fun passwordEnforcedClearErrorMessageIfSwitchsToNotEnforced() {
        val commonError = "Common error"

        //One permission with password enforced. Error is cleaned after switching permission
        //to a non-forced one
        loadPublicShareDialogFragment(
            isFolder = true,
            capabilities = OC_CAPABILITY.copy(
                versionString = "10.1.1",
                filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.TRUE,
                filesSharingPublicUpload = CapabilityBooleanType.TRUE,
                filesSharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.FALSE,
                filesSharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.FALSE,
                filesSharingPublicPasswordEnforced = CapabilityBooleanType.TRUE
            )
        )

        onView(withId(R.id.saveButton)).perform(scrollTo())
        onView(withId(R.id.saveButton)).perform(click())

        publicShareCreationStatus.postValue(
            Event(
                UIResult.Error(
                    error = Throwable(commonError)
                )
            )
        )

        onView(withId(R.id.public_link_error_message)).perform(scrollTo())
        onView(withText(commonError)).check(matches(isDisplayed()))

        onView(withId(R.id.shareViaLinkEditPermissionUploadFiles)).perform(scrollTo(), click())

        onView(withText(commonError)).check(matches(not(isDisplayed())))
    }

    private fun loadPublicShareDialogFragment(
        isFolder: Boolean = false,
        capabilities: OCCapability = OC_CAPABILITY
    ) {
        val file = if (isFolder) OC_FOLDER else OC_FILE

        val publicShareDialogFragment = PublicShareDialogFragment.newInstanceToCreate(
            file,
            OC_ACCOUNT,
            "DOC_12112018.jpg link"
        )

        ActivityScenario.launch(TestShareFileActivity::class.java).onActivity {
            it.startFragment(publicShareDialogFragment)
        }

        capabilitiesLiveData.postValue(Event(
            UIResult.Success(
                capabilities
            ))
        )
    }
}
