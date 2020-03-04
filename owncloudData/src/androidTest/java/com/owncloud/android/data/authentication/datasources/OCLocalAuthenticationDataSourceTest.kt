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

package com.owncloud.android.data.authentication.datasources

import android.accounts.AccountManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.data.authentication.datasources.implementation.OCLocalAuthenticationDataSource
import com.owncloud.android.domain.exceptions.AccountNotNewException
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OAUTH2_REFRESH_TOKEN
import com.owncloud.android.testutil.OC_ACCOUNT
import com.owncloud.android.testutil.OC_REDIRECTION_PATH
import com.owncloud.android.testutil.OC_SERVER_INFO
import com.owncloud.android.testutil.OC_USER_INFO
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OCLocalAuthenticationDataSourceTest {
    private lateinit var ocLocalAuthenticationDataSource: OCLocalAuthenticationDataSource
    private val accountManager = mockkClass(AccountManager::class)

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        ocLocalAuthenticationDataSource = OCLocalAuthenticationDataSource(
            context,
            accountManager,
            OC_ACCOUNT.type
        )
    }

    @Test
    fun addBasicAccountOk() {
        every {
            accountManager.addAccountExplicitly(any(), any(), any())
        } returns true

        every {
            accountManager.getAccountsByType(OC_ACCOUNT.type)
        } returns arrayOf() // There's no accounts yet

        every {
            accountManager.setUserData(any(), any(), any())
        } returns Unit

        val newAccountName = ocLocalAuthenticationDataSource.addBasicAccount(
            OC_REDIRECTION_PATH.lastPermanentLocation,
            "username",
            "password",
            OC_SERVER_INFO,
            OC_USER_INFO,
            false
        )

        verify(exactly = 1) {
            accountManager.getAccountsByType(OC_ACCOUNT.type)
//            accountManager.addAccountExplicitly(
//                OC_ACCOUNT,
//                "password",
//                null
//            )
//            accountManager.setUserData(OC_ACCOUNT, KEY_OAUTH2_REFRESH_TOKEN, )
//            accountManager.setUserData()
        }

        assertEquals(OC_ACCOUNT.name, newAccountName)
    }

    @Test(expected = AccountNotNewException::class)
    fun addBasicAccountAlreadyExistsNoUpdate() {
        every {
            accountManager.getAccountsByType(OC_ACCOUNT.type)
        } returns arrayOf(OC_ACCOUNT) // The account is already there

        ocLocalAuthenticationDataSource.addBasicAccount(
            OC_REDIRECTION_PATH.lastPermanentLocation,
            "username",
            "password",
            OC_SERVER_INFO,
            OC_USER_INFO,
            false
        )

        verify(exactly = 1) {
            accountManager.addAccountExplicitly(
                OC_ACCOUNT,
                "password",
                null
            )
        }
    }

    @Test()
    fun addBasicAccountAlreadyExistsUpdate() {
        every {
            accountManager.getAccountsByType(OC_ACCOUNT.type)
        } returns arrayOf(OC_ACCOUNT) // The account is already there

        ocLocalAuthenticationDataSource.addBasicAccount(
            OC_REDIRECTION_PATH.lastPermanentLocation,
            "username",
            "passwordUpdated",
            OC_SERVER_INFO,
            OC_USER_INFO,
            false
        )

        verify(exactly = 1) {
            accountManager.addAccountExplicitly(
                OC_ACCOUNT,
                "password",
                null
            )
        }
    }
}
