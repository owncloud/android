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

package com.owncloud.android.shares.presentation.ui

import android.accounts.Account
import android.accounts.AccountManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountAuthenticator
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.shares.domain.OCShare
import com.owncloud.android.shares.presentation.fragment.EditPrivateShareFragment
import com.owncloud.android.utils.AccountsManager
import com.owncloud.android.utils.TestUtil
import org.hamcrest.CoreMatchers.not
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class EditPrivateShareFragmentTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(
        TestShareFileActivity::class.java,
        true,
        true
    )

    private val defaultSharedWithDisplayName = "user"

    enum class Permissions(val value: Int) {
        READ_PERMISSIONS(1),
        EDIT_PERMISSIONS(3),
        SHARE_PERMISSIONS(17),
        ALL_PERMISSIONS(19),
        // FOLDERS
        EDIT_CREATE_PERMISSIONS(5),
        EDIT_CREATE_CHANGE_PERMISSIONS(7),
        EDIT_CREATE_DELETE_PERMISSIONS(13),
        EDIT_CREATE_CHANGE_DELETE_PERMISSIONS(15),
        EDIT_CHANGE_PERMISSIONS(3),
        EDIT_CHANGE_DELETE_PERMISSIONS(11),
        EDIT_DELETE_PERMISSIONS(9),
    }

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

            accountManager.addAccountExplicitly(account, "1234", null)

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
                AccountAuthenticator.KEY_AUTH_TOKEN_TYPE,
                "AUTH_TOKEN"
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
        val shareToEdit = mock(OCShare::class.java)
        `when`(shareToEdit.sharedWithDisplayName).thenReturn(defaultSharedWithDisplayName)
        `when`(shareToEdit.permissions).thenReturn(permissions)

        val sharedFile = mock(OCFile::class.java)
        `when`(sharedFile.isFolder).thenReturn(isFolder)

        val editPrivateShareFragment = EditPrivateShareFragment.newInstance(shareToEdit, sharedFile, account)

        activityRule.activity.privateShares = arrayListOf(
            TestUtil.createPrivateShare(
                shareWith = "user",
                sharedWithDisplayName = "User",
                path = "/Videos",
                isFolder = isFolder,
                permissions = permissions
            )
        )
        activityRule.activity.setFragment(editPrivateShareFragment)
    }
}
