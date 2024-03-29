/**
 * ownCloud Android client application
 *
 * @author Javier Rodríguez Pérez
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.accounts

import android.accounts.Account
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.owncloud.android.R
import com.owncloud.android.presentation.accounts.AccountsManagementActivity
import com.owncloud.android.presentation.accounts.AccountsManagementViewModel
import com.owncloud.android.presentation.avatar.AvatarManager
import com.owncloud.android.presentation.common.DrawerViewModel
import com.owncloud.android.utils.matchers.assertChildCount
import com.owncloud.android.utils.matchers.isDisplayed
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@Ignore("Ignored temporary till we fix some conflicts with tests for activities that extends FileActivity")
class AccountsManagementActivityTest {

    private lateinit var activityScenario: ActivityScenario<AccountsManagementActivity>

    private lateinit var accountsManagementViewModel: AccountsManagementViewModel
    private lateinit var drawerViewModel: DrawerViewModel
    private lateinit var avatarManager: AvatarManager

    private fun launchTest(accounts: Array<Account>, account: Account?) {
        every { accountsManagementViewModel.getLoggedAccounts() } returns accounts
        every { accountsManagementViewModel.getCurrentAccount() } returns account
        every { drawerViewModel.getAccounts(any()) } returns accounts.toList()
        every { drawerViewModel.getCurrentAccount(any()) } returns account
        every { avatarManager.getAvatarForAccount(any(), any(), any()) } returns null
        activityScenario = ActivityScenario.launch(AccountsManagementActivity::class.java)
    }

    @Before
    fun setUp() {
        accountsManagementViewModel = mockk(relaxed = true)
        drawerViewModel = mockk(relaxed = true)
        avatarManager = mockk(relaxed = true)

        stopKoin()

        startKoin {
            allowOverride(override = true)
            modules(
                module {
                    viewModel { accountsManagementViewModel }
                    viewModel { drawerViewModel }
                    factory { avatarManager }
                }
            )

        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testChildCount() {
        launchTest(
            accounts = arrayOf(
                Account("jrodper@owncloud.com", "owncloud"),
                Account("master@owncloud.com", "owncloud")
            ),
            Account("jrodper@owncloud.com", "owncloud")
        )
        R.id.account_list_recycler_view.assertChildCount(3)
    }

    @Test
    fun testChildCountEmptyAddAccountStays() {
        launchTest(accounts = emptyArray(), account = null)
        R.id.account_list_recycler_view.assertChildCount(1)
        onView(withId(R.id.account_list_recycler_view)).check(matches(hasDescendant(withText(R.string.prefs_add_account))))

    }

    @Test
    fun testVisibilityToolbar() {
        launchTest(accounts = emptyArray(), account = null)
        R.id.standard_toolbar.isDisplayed(true)
    }

    @Test
    fun testCheckData() {
        launchTest(
            accounts = arrayOf(
                Account("jrodper@owncloud.com", "owncloud")
            ),
            Account("jrodper@owncloud.com", "owncloud")
        )
        onView(withId(R.id.account_list_recycler_view)).check(matches(hasDescendant(withText("jrodper"))))
    }
}
