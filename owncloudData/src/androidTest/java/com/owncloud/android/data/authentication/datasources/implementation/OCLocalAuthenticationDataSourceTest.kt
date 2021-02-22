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

package com.owncloud.android.data.authentication.datasources.implementation

import android.accounts.Account
import android.accounts.AccountManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.data.authentication.KEY_CLIENT_REGISTRATION_CLIENT_EXPIRATION_DATE
import com.owncloud.android.data.authentication.KEY_CLIENT_REGISTRATION_CLIENT_ID
import com.owncloud.android.data.authentication.KEY_CLIENT_REGISTRATION_CLIENT_SECRET
import com.owncloud.android.data.authentication.KEY_OAUTH2_REFRESH_TOKEN
import com.owncloud.android.data.authentication.KEY_OAUTH2_SCOPE
import com.owncloud.android.data.authentication.SELECTED_ACCOUNT
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.domain.authentication.oauth.model.ClientRegistrationInfo
import com.owncloud.android.domain.exceptions.AccountNotFoundException
import com.owncloud.android.domain.exceptions.AccountNotNewException
import com.owncloud.android.domain.exceptions.AccountNotTheSameException
import com.owncloud.android.domain.server.model.ServerInfo
import com.owncloud.android.domain.user.model.UserInfo
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.ACCOUNT_VERSION
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_DISPLAY_NAME
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_BASE_URL
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_VERSION
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_SUPPORTS_OAUTH2
import com.owncloud.android.testutil.OC_ACCESS_TOKEN
import com.owncloud.android.testutil.OC_ACCOUNT
import com.owncloud.android.testutil.OC_ACCOUNT_ID
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_AUTH_TOKEN_TYPE
import com.owncloud.android.testutil.OC_BASE_URL
import com.owncloud.android.testutil.OC_BASIC_USERNAME
import com.owncloud.android.testutil.OC_OAUTH_SUPPORTED_TRUE
import com.owncloud.android.testutil.OC_REDIRECTION_PATH
import com.owncloud.android.testutil.OC_REFRESH_TOKEN
import com.owncloud.android.testutil.OC_SCOPE
import com.owncloud.android.testutil.OC_SERVER_INFO
import com.owncloud.android.testutil.OC_USER_INFO
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
class OCLocalAuthenticationDataSourceTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var ocLocalAuthenticationDataSource: OCLocalAuthenticationDataSource
    private val accountManager = mockkClass(AccountManager::class)
    private val preferencesProvider = spyk<SharedPreferencesProvider>()

    @Before
    fun init() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        ocLocalAuthenticationDataSource = OCLocalAuthenticationDataSource(
            context,
            accountManager,
            preferencesProvider,
            OC_ACCOUNT.type
        )
    }

    @Test
    fun addBasicAccountOk() {
        mockRegularAccountCreationFlow()
        mockSelectedAccountNameInPreferences()

        val newAccountName = ocLocalAuthenticationDataSource.addBasicAccount(
            OC_ACCOUNT_ID,
            OC_REDIRECTION_PATH.lastPermanentLocation,
            "password",
            OC_SERVER_INFO,
            OC_USER_INFO,
            null
        )

        val newAccount = Account(OC_ACCOUNT_NAME, "owncloud")

        // One for checking if the account exists and another one for getting the new account
        verifyAccountsByTypeAreGot(newAccount.type, 2)

        verifyAccountIsExplicitlyAdded(newAccount, "password", 1)
        verifyAccountInfoIsUpdated(newAccount, OC_SERVER_INFO, OC_USER_INFO, 1)

        assertEquals(newAccount.name, newAccountName)
    }

    @Test(expected = AccountNotNewException::class)
    fun addBasicAccountAlreadyExistsNoUpdate() {
        every {
            accountManager.getAccountsByType(OC_ACCOUNT.type)
        } returns arrayOf(OC_ACCOUNT) // The account is already there

        ocLocalAuthenticationDataSource.addBasicAccount(
            OC_ACCOUNT_ID,
            OC_REDIRECTION_PATH.lastPermanentLocation,
            "password",
            OC_SERVER_INFO,
            OC_USER_INFO.copy(id = OC_ACCOUNT_ID),
            null
        )
    }

    @Test
    fun addBasicAccountAlreadyExistsUpdateSameUsername() {
        every {
            accountManager.getAccountsByType(OC_ACCOUNT.type)
        } returns arrayOf(OC_ACCOUNT) // The account is already there

        every {
            accountManager.setPassword(any(), any())
        } returns Unit

        every {
            accountManager.setUserData(any(), any(), any())
        } returns Unit

        mockSelectedAccountNameInPreferences()

        ocLocalAuthenticationDataSource.addBasicAccount(
            OC_ACCOUNT_ID,
            OC_REDIRECTION_PATH.lastPermanentLocation,
            "password",
            OC_SERVER_INFO,
            OC_USER_INFO.copy(id = OC_ACCOUNT_ID),
            OC_ACCOUNT_NAME
        )

        // One for getting account to update
        verifyAccountsByTypeAreGot(OC_ACCOUNT.type, 1)

        // The account already exists so do not create it
        verifyAccountIsExplicitlyAdded(OC_ACCOUNT, "password", 0)

        // The account already exists, so update it
        verifyAccountInfoIsUpdated(OC_ACCOUNT, OC_SERVER_INFO, OC_USER_INFO, 1)
    }

    @Test
    fun addBasicAccountAlreadyExistsUpdateDifferentUsername() {

        every {
            accountManager.setUserData(any(), any(), any())
        } returns Unit

        mockSelectedAccountNameInPreferences()

        try {
            ocLocalAuthenticationDataSource.addBasicAccount(
                OC_BASIC_USERNAME,
                OC_REDIRECTION_PATH.lastPermanentLocation,
                "password",
                OC_SERVER_INFO,
                OC_USER_INFO,
                "NotTheSameAccount"
            )
        } catch (exception: Exception) {
            assertTrue(exception is AccountNotTheSameException)
        } finally {
            // The account already exists so do not create a new one
            verifyAccountIsExplicitlyAdded(OC_ACCOUNT, "password", 0)

            // The account is not the same, so no update needed
            verifyAccountInfoIsUpdated(OC_ACCOUNT, OC_SERVER_INFO, OC_USER_INFO, 0)
        }
    }

    @Test
    fun addOAuthAccountOk() {
        mockRegularAccountCreationFlow()
        mockSelectedAccountNameInPreferences()

        every {
            accountManager.setAuthToken(any(), any(), any())
        } returns Unit

        val newAccountName = ocLocalAuthenticationDataSource.addOAuthAccount(
            OC_ACCOUNT_ID,
            OC_REDIRECTION_PATH.lastPermanentLocation,
            OC_AUTH_TOKEN_TYPE,
            OC_ACCESS_TOKEN,
            OC_SERVER_INFO,
            OC_USER_INFO,
            OC_REFRESH_TOKEN,
            OC_SCOPE,
            null,
            OC_CLIENT_REGISTRATION
        )

        val newAccount = Account(OC_ACCOUNT_NAME, "owncloud")

        // One for checking if the account exists and another one for getting the new account
        verifyAccountsByTypeAreGot(newAccount.type, 2)

        verifyAccountIsExplicitlyAdded(newAccount, "", 1)
        verifyAccountInfoIsUpdated(newAccount, OC_SERVER_INFO, OC_USER_INFO, 1)

        // OAuth params are updated
        verifyOAuthParamsAreUpdated(newAccount, OC_ACCESS_TOKEN, OC_OAUTH_SUPPORTED_TRUE, OC_REFRESH_TOKEN, OC_SCOPE, OC_CLIENT_REGISTRATION, 1)

        assertEquals(newAccount.name, newAccountName)
    }

    @Test(expected = AccountNotNewException::class)
    fun addOAuthAccountAlreadyExistsNoUpdate() {
        every {
            accountManager.getAccountsByType(OC_ACCOUNT.type)
        } returns arrayOf(OC_ACCOUNT) // The account is already there

        ocLocalAuthenticationDataSource.addOAuthAccount(
            OC_ACCOUNT_ID,
            OC_REDIRECTION_PATH.lastPermanentLocation,
            OC_AUTH_TOKEN_TYPE,
            OC_ACCESS_TOKEN,
            OC_SERVER_INFO,
            OC_USER_INFO.copy(id = OC_ACCOUNT_ID),
            OC_REFRESH_TOKEN,
            OC_SCOPE,
            null,
            OC_CLIENT_REGISTRATION
        )
    }

    @Test
    fun addOAuthAccountAlreadyExistsUpdateSameUsername() {
        every {
            accountManager.getAccountsByType(OC_ACCOUNT.type)
        } returns arrayOf(OC_ACCOUNT) // The account is already there

        every {
            accountManager.setUserData(any(), any(), any())
        } returns Unit

        every {
            accountManager.setAuthToken(any(), any(), any())
        } returns Unit

        mockSelectedAccountNameInPreferences()

        ocLocalAuthenticationDataSource.addOAuthAccount(
            OC_ACCOUNT_ID,
            OC_REDIRECTION_PATH.lastPermanentLocation,
            OC_AUTH_TOKEN_TYPE,
            OC_ACCESS_TOKEN,
            OC_SERVER_INFO,
            OC_USER_INFO.copy(id = OC_ACCOUNT_ID),
            OC_REFRESH_TOKEN,
            OC_SCOPE,
            OC_ACCOUNT_NAME,
            OC_CLIENT_REGISTRATION
        )

        // One for getting account to update
        verifyAccountsByTypeAreGot(OC_ACCOUNT.type, 1)

        // The account already exists so do not create it
        verifyAccountIsExplicitlyAdded(OC_ACCOUNT, "password", 0)

        // The account already exists, so update it
        verifyAccountInfoIsUpdated(OC_ACCOUNT, OC_SERVER_INFO, OC_USER_INFO, 1)
        verifyOAuthParamsAreUpdated(OC_ACCOUNT, OC_ACCESS_TOKEN, OC_OAUTH_SUPPORTED_TRUE, OC_REFRESH_TOKEN, OC_SCOPE, OC_CLIENT_REGISTRATION, 1)
    }

    @Test
    fun addOAuthAccountAlreadyExistsUpdateDifferentUsername() {

        every {
            accountManager.setUserData(any(), any(), any())
        } returns Unit

        every {
            accountManager.setAuthToken(any(), any(), any())
        } returns Unit

        mockSelectedAccountNameInPreferences()

        try {
            ocLocalAuthenticationDataSource.addOAuthAccount(
                OC_BASIC_USERNAME,
                OC_REDIRECTION_PATH.lastPermanentLocation,
                OC_AUTH_TOKEN_TYPE,
                OC_ACCESS_TOKEN,
                OC_SERVER_INFO,
                OC_USER_INFO,
                OC_REFRESH_TOKEN,
                OC_SCOPE,
                "AccountNotTheSame",
                OC_CLIENT_REGISTRATION
            )
        } catch (exception: Exception) {
            assertTrue(exception is AccountNotTheSameException)
        } finally {
            // The account already exists so do not create it
            verifyAccountIsExplicitlyAdded(OC_ACCOUNT, "password", 0)

            // The account already exists, so update it
            verifyAccountInfoIsUpdated(OC_ACCOUNT, OC_SERVER_INFO, OC_USER_INFO, 0)
            verifyOAuthParamsAreUpdated(
                OC_ACCOUNT,
                OC_ACCESS_TOKEN,
                OC_OAUTH_SUPPORTED_TRUE,
                OC_REFRESH_TOKEN,
                OC_SCOPE,
                OC_CLIENT_REGISTRATION,
                0
            )
        }
    }

    @Test
    fun supportsOAuthOk() {
        every {
            accountManager.getAccountsByType(OC_ACCOUNT.type)
        } returns arrayOf(OC_ACCOUNT)

        every {
            accountManager.getUserData(OC_ACCOUNT, KEY_SUPPORTS_OAUTH2)
        } returns OC_OAUTH_SUPPORTED_TRUE

        val supportsOAuth2 = ocLocalAuthenticationDataSource.supportsOAuth2(OC_ACCOUNT.name)

        verifyAccountsByTypeAreGot(OC_ACCOUNT.type, 1)
        verifyUserDataIsGot(OC_ACCOUNT, KEY_SUPPORTS_OAUTH2, 1)

        assertEquals(true, supportsOAuth2)
    }

    @Test(expected = AccountNotFoundException::class)
    fun supportsOAuthAccountNotFound() {
        every {
            accountManager.getAccountsByType(OC_ACCOUNT.type)
        } returns arrayOf() // That account does not exist

        ocLocalAuthenticationDataSource.supportsOAuth2(OC_ACCOUNT.name)
    }

    @Test
    fun getBaseUrlOk() {
        every {
            accountManager.getAccountsByType(OC_ACCOUNT.type)
        } returns arrayOf(OC_ACCOUNT)

        every {
            accountManager.getUserData(OC_ACCOUNT, KEY_OC_BASE_URL)
        } returns OC_BASE_URL

        val baseUrl = ocLocalAuthenticationDataSource.getBaseUrl(OC_ACCOUNT.name)

        verifyAccountsByTypeAreGot(OC_ACCOUNT.type, 1)
        verifyUserDataIsGot(OC_ACCOUNT, KEY_OC_BASE_URL, 1)

        assertEquals(OC_BASE_URL, baseUrl)
    }

    @Test(expected = AccountNotFoundException::class)
    fun getBaseUrlAccountNotFound() {
        every {
            accountManager.getAccountsByType(OC_ACCOUNT.type)
        } returns arrayOf() // That account does not exist

        ocLocalAuthenticationDataSource.getBaseUrl(OC_ACCOUNT.name)
    }

    private fun mockSelectedAccountNameInPreferences(
        selectedAccountName: String = OC_ACCOUNT.name
    ) {
        every {
            preferencesProvider.getString(SELECTED_ACCOUNT, any())
        } returns selectedAccountName
        every {
            preferencesProvider.putString(any(), any())
        } returns Unit
    }

    private fun mockRegularAccountCreationFlow() {
        // Step 1: Get accounts to know if the current account exists
        every {
            accountManager.getAccountsByType("owncloud")
        } returns arrayOf() // There's no accounts yet

        // Step 2: Add new account
        every {
            accountManager.addAccountExplicitly(any(), any(), any())
        } returns true

        // Step 3: Update just created account
        every {
            accountManager.setUserData(any(), any(), any())
        } returns Unit
    }

    private fun verifyAccountsByTypeAreGot(accountType: String, exactly: Int) {
        verify(exactly = exactly) {
            accountManager.getAccountsByType(accountType)
        }
    }

    private fun verifyAccountIsExplicitlyAdded(
        account: Account,
        password: String,
        exactly: Int
    ) {
        verify(exactly = exactly) {
            accountManager.addAccountExplicitly(
                account,
                password,
                null
            )
        }
    }

    private fun verifyAccountInfoIsUpdated(
        account: Account,
        serverInfo: ServerInfo,
        userInfo: UserInfo,
        exactly: Int
    ) {
        verify(exactly = exactly) {
            // The account info is updated
            accountManager.setUserData(account, KEY_OC_ACCOUNT_VERSION, ACCOUNT_VERSION.toString())
            accountManager.setUserData(account, KEY_OC_VERSION, serverInfo.ownCloudVersion)
            accountManager.setUserData(account, KEY_OC_BASE_URL, serverInfo.baseUrl)
            accountManager.setUserData(account, KEY_DISPLAY_NAME, userInfo.displayName)
        }
    }

    private fun verifyOAuthParamsAreUpdated(
        account: Account,
        accessToken: String,
        supportsOAuth2: String,
        refreshToken: String,
        scope: String,
        clientInfo: ClientRegistrationInfo,
        exactly: Int
    ) {
        verify(exactly = exactly) {
            accountManager.setAuthToken(account, OC_AUTH_TOKEN_TYPE, accessToken)
            accountManager.setUserData(account, KEY_SUPPORTS_OAUTH2, supportsOAuth2)
            accountManager.setUserData(account, KEY_OAUTH2_REFRESH_TOKEN, refreshToken)
            accountManager.setUserData(account, KEY_OAUTH2_SCOPE, scope)
            accountManager.setUserData(account, KEY_CLIENT_REGISTRATION_CLIENT_SECRET, clientInfo.clientSecret)
            accountManager.setUserData(account, KEY_CLIENT_REGISTRATION_CLIENT_ID, clientInfo.clientId)
            accountManager.setUserData(account, KEY_CLIENT_REGISTRATION_CLIENT_EXPIRATION_DATE, clientInfo.clientSecretExpiration.toString())
        }
    }

    private fun verifyUserDataIsGot(account: Account, key: String, exactly: Int) {
        verify(exactly = exactly) {
            accountManager.getUserData(account, key)
        }
    }
}
