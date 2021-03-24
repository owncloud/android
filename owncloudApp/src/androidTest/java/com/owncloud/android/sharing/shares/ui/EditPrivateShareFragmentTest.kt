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
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.R
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.ui.sharing.fragments.EditPrivateShareFragment
import com.owncloud.android.presentation.viewmodels.sharing.OCShareViewModel
import com.owncloud.android.testutil.OC_ACCOUNT
import com.owncloud.android.testutil.OC_SHARE
import com.owncloud.android.utils.AppTestUtil.OC_FILE
import com.owncloud.android.utils.AppTestUtil.OC_FOLDER
import com.owncloud.android.utils.Permissions
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.not
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class EditPrivateShareFragmentTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val defaultSharedWithDisplayName = "user"
    private val ocShareViewModel = mockk<OCShareViewModel>(relaxed = true)
    private val privateShareAsLiveData = MutableLiveData<Event<UIResult<OCShare>>>()

    private lateinit var activityScenario: ActivityScenario<TestShareFileActivity>

    @Before
    fun setUp() {
        every { ocShareViewModel.privateShare } returns privateShareAsLiveData

        stopKoin()

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext<Context>())
            modules(
                module(override = true) {
                    viewModel {
                        ocShareViewModel
                    }
                }
            )
        }
    }

    @Test
    fun showDialogTitle() {
        loadEditPrivateShareFragment()
        onView(withId(R.id.editShareTitle))
            .check(
                matches(
                    withText(
                        targetContext.getString(R.string.share_with_edit_title, defaultSharedWithDisplayName)
                    )
                )
            )
    }

    @Test
    fun closeDialog() {
        loadEditPrivateShareFragment()
        onView(withId(R.id.closeButton)).perform(click())
        activityScenario.onActivity { Assert.assertNull(it.getTestFragment()) }
    }

    @Test
    fun showToggles() {
        loadEditPrivateShareFragment()
        onView(withId(R.id.canEditSwitch)).check(matches(isDisplayed()))
        onView(withId(R.id.canShareSwitch)).check(matches(isDisplayed()))
    }

    @Test
    fun showFileShareWithNoPermissions() {
        loadEditPrivateShareFragment()
        onView(withId(R.id.canEditSwitch)).check(matches(isNotChecked()))
        onView(withId(R.id.canShareSwitch)).check(matches(isNotChecked()))
    }

    @Test
    fun showFileShareWithEditPermissions() {
        loadEditPrivateShareFragment(permissions = Permissions.EDIT_PERMISSIONS.value)
        onView(withId(R.id.canEditSwitch)).check(matches(isChecked()))
        onView(withId(R.id.canShareSwitch)).check(matches(isNotChecked()))
    }

    @Test
    fun showFileShareWithSharePermissions() {
        loadEditPrivateShareFragment(permissions = Permissions.SHARE_PERMISSIONS.value)
        onView(withId(R.id.canEditSwitch)).check(matches(isNotChecked()))
        onView(withId(R.id.canShareSwitch)).check(matches(isChecked()))
    }

    @Test
    fun showFileShareWithAllPermissions() {
        loadEditPrivateShareFragment(permissions = Permissions.ALL_PERMISSIONS.value)
        onView(withId(R.id.canEditSwitch)).check(matches(isChecked()))
        onView(withId(R.id.canShareSwitch)).check(matches(isChecked()))
    }

    @Test
    fun showFolderShareWithCreatePermissions() {
        loadEditPrivateShareFragment(true, permissions = Permissions.EDIT_CREATE_PERMISSIONS.value)
        onView(withId(R.id.canEditSwitch)).check(matches(isChecked()))
        onView(withId(R.id.canEditCreateCheckBox)).check(matches(isChecked()))
        onView(withId(R.id.canEditChangeCheckBox)).check(matches(isNotChecked()))
        onView(withId(R.id.canEditDeleteCheckBox)).check(matches(isNotChecked()))
        onView(withId(R.id.canShareSwitch)).check(matches(isNotChecked()))
    }

    @Test
    fun showFolderShareWithCreateChangePermissions() {
        loadEditPrivateShareFragment(true, permissions = Permissions.EDIT_CREATE_CHANGE_PERMISSIONS.value)
        onView(withId(R.id.canEditSwitch)).check(matches(isChecked()))
        onView(withId(R.id.canEditCreateCheckBox)).check(matches(isChecked()))
        onView(withId(R.id.canEditChangeCheckBox)).check(matches(isChecked()))
        onView(withId(R.id.canEditDeleteCheckBox)).check(matches(isNotChecked()))
        onView(withId(R.id.canShareSwitch)).check(matches(isNotChecked()))
    }

    @Test
    fun showFolderShareWithCreateDeletePermissions() {
        loadEditPrivateShareFragment(true, permissions = Permissions.EDIT_CREATE_DELETE_PERMISSIONS.value)
        onView(withId(R.id.canEditSwitch)).check(matches(isChecked()))
        onView(withId(R.id.canEditCreateCheckBox)).check(matches(isChecked()))
        onView(withId(R.id.canEditChangeCheckBox)).check(matches(isNotChecked()))
        onView(withId(R.id.canEditDeleteCheckBox)).check(matches(isChecked()))
        onView(withId(R.id.canShareSwitch)).check(matches(isNotChecked()))
    }

    @Test
    fun showFolderShareWithCreateChangeDeletePermissions() {
        loadEditPrivateShareFragment(true, permissions = Permissions.EDIT_CREATE_CHANGE_DELETE_PERMISSIONS.value)
        onView(withId(R.id.canEditSwitch)).check(matches(isChecked()))
        onView(withId(R.id.canEditCreateCheckBox)).check(matches(isChecked()))
        onView(withId(R.id.canEditChangeCheckBox)).check(matches(isChecked()))
        onView(withId(R.id.canEditDeleteCheckBox)).check(matches(isChecked()))
        onView(withId(R.id.canShareSwitch)).check(matches(isNotChecked()))
    }

    @Test
    fun showFolderShareWithChangePermissions() {
        loadEditPrivateShareFragment(true, permissions = Permissions.EDIT_CHANGE_PERMISSIONS.value)
        onView(withId(R.id.canEditSwitch)).check(matches(isChecked()))
        onView(withId(R.id.canEditCreateCheckBox)).check(matches(isNotChecked()))
        onView(withId(R.id.canEditChangeCheckBox)).check(matches(isChecked()))
        onView(withId(R.id.canEditDeleteCheckBox)).check(matches(isNotChecked()))
        onView(withId(R.id.canShareSwitch)).check(matches(isNotChecked()))
    }

    @Test
    fun showFolderShareWithChangeDeletePermissions() {
        loadEditPrivateShareFragment(true, permissions = Permissions.EDIT_CHANGE_DELETE_PERMISSIONS.value)
        onView(withId(R.id.canEditSwitch)).check(matches(isChecked()))
        onView(withId(R.id.canEditCreateCheckBox)).check(matches(isNotChecked()))
        onView(withId(R.id.canEditChangeCheckBox)).check(matches(isChecked()))
        onView(withId(R.id.canEditDeleteCheckBox)).check(matches(isChecked()))
        onView(withId(R.id.canShareSwitch)).check(matches(isNotChecked()))
    }

    @Test
    fun showFolderShareWithDeletePermissions() {
        loadEditPrivateShareFragment(true, permissions = Permissions.EDIT_DELETE_PERMISSIONS.value)
        onView(withId(R.id.canEditSwitch)).check(matches(isChecked()))
        onView(withId(R.id.canEditCreateCheckBox)).check(matches(isNotChecked()))
        onView(withId(R.id.canEditChangeCheckBox)).check(matches(isNotChecked()))
        onView(withId(R.id.canEditDeleteCheckBox)).check(matches(isChecked()))
        onView(withId(R.id.canShareSwitch)).check(matches(isNotChecked()))
    }

    @Test
    fun disableEditPermissionWithFile() {
        loadEditPrivateShareFragment(permissions = Permissions.EDIT_PERMISSIONS.value)
        onView(withId(R.id.canEditSwitch)).check(matches(isChecked()))
        onView(withId(R.id.canShareSwitch)).check(matches(isNotChecked()))

        onView(withId(R.id.canEditSwitch)).perform(click())

        onView(withId(R.id.canEditSwitch)).check(matches(isNotChecked()))  // "Can edit" changes
        onView(withId(R.id.canEditCreateCheckBox)).check(matches(not(isDisplayed())))  // No suboptions
        onView(withId(R.id.canEditChangeCheckBox)).check(matches(not(isDisplayed())))
        onView(withId(R.id.canEditDeleteCheckBox)).check(matches(not(isDisplayed())))
        onView(withId(R.id.canShareSwitch)).check(matches(isNotChecked()))  // "Can share" does not change
    }

    @Test
    fun disableEditPermissionWithFolder() {
        loadEditPrivateShareFragment(true, permissions = Permissions.EDIT_PERMISSIONS.value)
        onView(withId(R.id.canEditSwitch)).check(matches(isChecked()))
        onView(withId(R.id.canEditCreateCheckBox)).check(matches(isDisplayed()))  // Suboptions appear
        onView(withId(R.id.canEditChangeCheckBox)).check(matches(isDisplayed()))
        onView(withId(R.id.canEditDeleteCheckBox)).check(matches(isDisplayed()))
        onView(withId(R.id.canShareSwitch)).check(matches(isNotChecked()))

        onView(withId(R.id.canEditSwitch)).perform(click())

        onView(withId(R.id.canEditSwitch)).check(matches(isNotChecked()))  // "Can edit" changes
        onView(withId(R.id.canEditCreateCheckBox)).check(matches(not(isDisplayed())))  // Suboptions hidden
        onView(withId(R.id.canEditChangeCheckBox)).check(matches(not(isDisplayed())))
        onView(withId(R.id.canEditDeleteCheckBox)).check(matches(not(isDisplayed())))
        onView(withId(R.id.canShareSwitch)).check(matches(isNotChecked()))  // "Can share" does not change
    }

    private fun loadEditPrivateShareFragment(
        isFolder: Boolean = false,
        permissions: Int = Permissions.READ_PERMISSIONS.value
    ) {
        val shareToEdit = OC_SHARE.copy(
            sharedWithDisplayName = defaultSharedWithDisplayName,
            permissions = permissions
        )

        val sharedFile = if (isFolder) OC_FOLDER else OC_FILE

        val editPrivateShareFragment = EditPrivateShareFragment.newInstance(
            shareToEdit,
            sharedFile,
            OC_ACCOUNT
        )

        activityScenario = ActivityScenario.launch(TestShareFileActivity::class.java).onActivity {
            it.startFragment(editPrivateShareFragment)
        }

        privateShareAsLiveData.postValue(
            Event(
                UIResult.Success(
                    OC_SHARE.copy(
                        shareWith = "user",
                        sharedWithDisplayName = "User",
                        path = "/Videos",
                        isFolder = isFolder,
                        permissions = permissions
                    )
                )
            )
        )
    }
}
