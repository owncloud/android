/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
package com.owncloud.android.data.authentication.repository

import com.owncloud.android.data.authentication.datasources.LocalAuthenticationDataSource
import com.owncloud.android.data.authentication.datasources.RemoteAuthenticationDataSource
import com.owncloud.android.domain.exceptions.AccountNotFoundException
import com.owncloud.android.domain.exceptions.AccountNotNewException
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.testutil.OC_ACCESS_TOKEN
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_AUTH_TOKEN_TYPE
import com.owncloud.android.testutil.OC_REDIRECTION_PATH
import com.owncloud.android.testutil.OC_REFRESH_TOKEN
import com.owncloud.android.testutil.OC_SCOPE
import com.owncloud.android.testutil.OC_SERVER_INFO
import com.owncloud.android.testutil.OC_USER_INFO
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class OCAuthenticationRepositoryTest {

    private val localAuthenticationDataSource = mockk<LocalAuthenticationDataSource>(relaxed = true)
    private val remoteAuthenticationDataSource = mockk<RemoteAuthenticationDataSource>(relaxed = true)
    private val ocAuthenticationRepository: OCAuthenticationRepository =
        OCAuthenticationRepository(localAuthenticationDataSource, remoteAuthenticationDataSource)

    @Test
    fun loginBasicOk() {
        every { remoteAuthenticationDataSource.loginBasic(any(), any(), any()) } returns Pair(
            OC_USER_INFO,
            OC_REDIRECTION_PATH.lastPermanentLocation
        )

        every {
            localAuthenticationDataSource.addBasicAccount(any(), any(), any(), any(), any(), any())
        } returns OC_ACCOUNT_NAME

        val accountName = ocAuthenticationRepository.loginBasic(
            OC_SERVER_INFO,
            "username",
            "password",
            null
        )

        verify(exactly = 1) {
            remoteAuthenticationDataSource.loginBasic(OC_SERVER_INFO.baseUrl, "username", "password")
            localAuthenticationDataSource.addBasicAccount(
                "username",
                OC_REDIRECTION_PATH.lastPermanentLocation,
                "password",
                OC_SERVER_INFO,
                OC_USER_INFO,
                null
            )
        }

        assertEquals(OC_ACCOUNT_NAME, accountName)
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun loginBasicRemoteException() {
        every {
            remoteAuthenticationDataSource.loginBasic(any(), any(), any())
        } throws NoConnectionWithServerException()

        every {
            localAuthenticationDataSource.addBasicAccount(any(), any(), any(), any(), any(), any())
        } returns OC_ACCOUNT_NAME

        ocAuthenticationRepository.loginBasic(OC_SERVER_INFO, "test", "test", null)

        verify(exactly = 1) {
            remoteAuthenticationDataSource.loginBasic(any(), any(), any())
            localAuthenticationDataSource.addBasicAccount(any(), any(), any(), any(), any(), any())
        }
    }

    @Test(expected = AccountNotNewException::class)
    fun loginBasicLocalException() {
        every { remoteAuthenticationDataSource.loginBasic(any(), any(), any()) } returns Pair(
            OC_USER_INFO,
            OC_REDIRECTION_PATH.lastPermanentLocation
        )

        every {
            localAuthenticationDataSource.addBasicAccount(any(), any(), any(), any(), any(), any())
        } throws AccountNotNewException()

        ocAuthenticationRepository.loginBasic(OC_SERVER_INFO, "test", "test", null)

        verify(exactly = 1) {
            remoteAuthenticationDataSource.loginBasic(any(), any(), any())
            localAuthenticationDataSource.addBasicAccount(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun loginBasicOAuth() {
        every { remoteAuthenticationDataSource.loginOAuth(any(), any(), any()) } returns Pair(
            OC_USER_INFO,
            OC_REDIRECTION_PATH.lastPermanentLocation
        )

        every {
            localAuthenticationDataSource.addOAuthAccount(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns OC_ACCOUNT_NAME

        val accountName = ocAuthenticationRepository.loginOAuth(
            OC_SERVER_INFO,
            "username",
            OC_AUTH_TOKEN_TYPE,
            OC_ACCESS_TOKEN,
            OC_REFRESH_TOKEN,
            OC_SCOPE,
            null
        )

        verify(exactly = 1) {
            remoteAuthenticationDataSource.loginOAuth(OC_SERVER_INFO.baseUrl, "username", OC_ACCESS_TOKEN)
            localAuthenticationDataSource.addOAuthAccount(
                "username",
                OC_REDIRECTION_PATH.lastPermanentLocation,
                OC_AUTH_TOKEN_TYPE,
                OC_ACCESS_TOKEN,
                OC_SERVER_INFO,
                OC_USER_INFO,
                OC_REFRESH_TOKEN,
                OC_SCOPE,
                null
            )
        }

        assertEquals(OC_ACCOUNT_NAME, accountName)
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun loginOAuthRemoteException() {
        every {
            remoteAuthenticationDataSource.loginOAuth(any(), any(), any())
        } throws NoConnectionWithServerException()

        every {
            localAuthenticationDataSource.addOAuthAccount(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns OC_ACCOUNT_NAME

        ocAuthenticationRepository.loginOAuth(
            OC_SERVER_INFO,
            "test",
            OC_AUTH_TOKEN_TYPE,
            OC_ACCESS_TOKEN,
            OC_REFRESH_TOKEN,
            OC_SCOPE,
            null
        )

        verify(exactly = 1) {
            remoteAuthenticationDataSource.loginOAuth(any(), any(), any())
            localAuthenticationDataSource.addOAuthAccount(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test(expected = AccountNotNewException::class)
    fun loginOAuthLocalException() {
        every { remoteAuthenticationDataSource.loginOAuth(any(), any(), any()) } returns Pair(
            OC_USER_INFO,
            OC_REDIRECTION_PATH.lastPermanentLocation
        )

        every {
            localAuthenticationDataSource.addOAuthAccount(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } throws AccountNotNewException()

        ocAuthenticationRepository.loginOAuth(
            OC_SERVER_INFO,
            "test",
            OC_AUTH_TOKEN_TYPE,
            OC_ACCESS_TOKEN,
            OC_REFRESH_TOKEN,
            OC_SCOPE,
            null
        )

        verify(exactly = 1) {
            remoteAuthenticationDataSource.loginOAuth(any(), any(), any())
            localAuthenticationDataSource.addOAuthAccount(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun supportsOAuth2Ok() {
        every {
            localAuthenticationDataSource.supportsOAuth2(any())
        } returns true

        ocAuthenticationRepository.supportsOAuth2UseCase(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            localAuthenticationDataSource.supportsOAuth2(OC_ACCOUNT_NAME)
        }
    }

    @Test(expected = AccountNotFoundException::class)
    fun supportsOAuth2Exception() {
        every {
            localAuthenticationDataSource.supportsOAuth2(any())
        } throws AccountNotFoundException()

        ocAuthenticationRepository.supportsOAuth2UseCase(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            localAuthenticationDataSource.supportsOAuth2(any())
        }
    }

    @Test
    fun getBaseUrlOk() {
        every {
            localAuthenticationDataSource.getBaseUrl(any())
        } returns OC_SERVER_INFO.baseUrl

        ocAuthenticationRepository.getBaseUrl(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            localAuthenticationDataSource.getBaseUrl(OC_ACCOUNT_NAME)
        }
    }

    @Test(expected = AccountNotFoundException::class)
    fun getBaseUrlException() {
        every {
            localAuthenticationDataSource.getBaseUrl(any())
        } throws AccountNotFoundException()

        ocAuthenticationRepository.getBaseUrl(OC_ACCOUNT_NAME)

        verify(exactly = 1) {
            localAuthenticationDataSource.getBaseUrl(any())
        }
    }
}
