/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author David González Verdugo
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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
import com.owncloud.android.testutil.OC_ACCESS_TOKEN
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_AUTH_TOKEN_TYPE
import com.owncloud.android.testutil.OC_BASIC_PASSWORD
import com.owncloud.android.testutil.OC_BASIC_USERNAME
import com.owncloud.android.testutil.OC_REDIRECTION_PATH
import com.owncloud.android.testutil.OC_REFRESH_TOKEN
import com.owncloud.android.testutil.OC_SCOPE
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_BASIC_AUTH
import com.owncloud.android.testutil.OC_USER_INFO
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OCAuthenticationRepositoryTest {

    private val localAuthenticationDataSource = mockk<LocalAuthenticationDataSource>()
    private val remoteAuthenticationDataSource = mockk<RemoteAuthenticationDataSource>()
    private val ocAuthenticationRepository: OCAuthenticationRepository =
        OCAuthenticationRepository(localAuthenticationDataSource, remoteAuthenticationDataSource)

    @Test
    fun `loginBasic returns String with the account name`() {
        every {
            remoteAuthenticationDataSource.loginBasic(
                serverPath = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                username = OC_BASIC_USERNAME,
                password = OC_BASIC_PASSWORD
            )
        } returns Pair(
            OC_USER_INFO,
            OC_REDIRECTION_PATH.lastPermanentLocation
        )

        every {
            localAuthenticationDataSource.addBasicAccount(
                userName = OC_BASIC_USERNAME,
                lastPermanentLocation = OC_REDIRECTION_PATH.lastPermanentLocation,
                password = OC_BASIC_PASSWORD,
                serverInfo = OC_SECURE_SERVER_INFO_BASIC_AUTH,
                userInfo = OC_USER_INFO,
                updateAccountWithUsername = null
            )
        } returns OC_ACCOUNT_NAME

        val accountName = ocAuthenticationRepository.loginBasic(
            serverInfo = OC_SECURE_SERVER_INFO_BASIC_AUTH,
            username = OC_BASIC_USERNAME,
            password = OC_BASIC_PASSWORD,
            updateAccountWithUsername = null
        )

        assertEquals(OC_ACCOUNT_NAME, accountName)

        verify(exactly = 1) {
            remoteAuthenticationDataSource.loginBasic(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, OC_BASIC_USERNAME, OC_BASIC_PASSWORD)
            localAuthenticationDataSource.addBasicAccount(
                userName = OC_BASIC_USERNAME,
                lastPermanentLocation = OC_REDIRECTION_PATH.lastPermanentLocation,
                password = OC_BASIC_PASSWORD,
                serverInfo = OC_SECURE_SERVER_INFO_BASIC_AUTH,
                userInfo = OC_USER_INFO,
                updateAccountWithUsername = null
            )
        }
    }

    @Test
    fun `loginOAuth returns String with the account name`() {
        every {
            remoteAuthenticationDataSource.loginOAuth(
                serverPath = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                username = OC_BASIC_USERNAME,
                accessToken = OC_ACCESS_TOKEN
            )
        } returns Pair(
            first = OC_USER_INFO,
            second = OC_REDIRECTION_PATH.lastPermanentLocation
        )

        every {
            localAuthenticationDataSource.addOAuthAccount(
                userName = OC_BASIC_USERNAME,
                lastPermanentLocation = OC_REDIRECTION_PATH.lastPermanentLocation,
                authTokenType = OC_AUTH_TOKEN_TYPE,
                accessToken = OC_ACCESS_TOKEN,
                serverInfo = OC_SECURE_SERVER_INFO_BASIC_AUTH,
                userInfo = OC_USER_INFO,
                refreshToken = OC_REFRESH_TOKEN,
                scope = OC_SCOPE,
                updateAccountWithUsername = null,
                clientRegistrationInfo = OC_CLIENT_REGISTRATION
            )
        } returns OC_ACCOUNT_NAME

        val accountName = ocAuthenticationRepository.loginOAuth(
            serverInfo = OC_SECURE_SERVER_INFO_BASIC_AUTH,
            username = OC_BASIC_USERNAME,
            authTokenType = OC_AUTH_TOKEN_TYPE,
            accessToken = OC_ACCESS_TOKEN,
            refreshToken = OC_REFRESH_TOKEN,
            scope = OC_SCOPE,
            updateAccountWithUsername = null,
            clientRegistrationInfo = OC_CLIENT_REGISTRATION
        )

        assertEquals(OC_ACCOUNT_NAME, accountName)

        verify(exactly = 1) {
            remoteAuthenticationDataSource.loginOAuth(
                serverPath = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                username = OC_BASIC_USERNAME,
                accessToken = OC_ACCESS_TOKEN
            )
            localAuthenticationDataSource.addOAuthAccount(
                userName = OC_BASIC_USERNAME,
                lastPermanentLocation = OC_REDIRECTION_PATH.lastPermanentLocation,
                authTokenType = OC_AUTH_TOKEN_TYPE,
                accessToken = OC_ACCESS_TOKEN,
                serverInfo = OC_SECURE_SERVER_INFO_BASIC_AUTH,
                userInfo = OC_USER_INFO,
                refreshToken = OC_REFRESH_TOKEN,
                scope = OC_SCOPE,
                updateAccountWithUsername = null,
                clientRegistrationInfo = OC_CLIENT_REGISTRATION
            )
        }
    }

    @Test
    fun `supportsOAuth2UseCase returns Boolean`() {
        every {
            localAuthenticationDataSource.supportsOAuth2(OC_ACCOUNT_NAME)
        } returns true

        val actualResult = ocAuthenticationRepository.supportsOAuth2UseCase(OC_ACCOUNT_NAME)

        assertTrue(actualResult)

        verify(exactly = 1) {
            localAuthenticationDataSource.supportsOAuth2(OC_ACCOUNT_NAME)
        }
    }

    @Test
    fun `getBaseUrl returns a String with the base URL`() {
        every {
            localAuthenticationDataSource.getBaseUrl(OC_ACCOUNT_NAME)
        } returns OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl

        val resultActual = ocAuthenticationRepository.getBaseUrl(OC_ACCOUNT_NAME)

        assertEquals(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, resultActual)

        verify(exactly = 1) {
            localAuthenticationDataSource.getBaseUrl(OC_ACCOUNT_NAME)
        }
    }

}
