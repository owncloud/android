/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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
package com.owncloud.android.data.oauth

import com.owncloud.android.data.oauth.datasource.RemoteOAuthDataSource
import com.owncloud.android.domain.authentication.oauth.OAuthRepository
import com.owncloud.android.testutil.OC_SERVER_INFO
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION_REQUEST
import com.owncloud.android.testutil.oauth.OC_OIDC_SERVER_CONFIGURATION
import com.owncloud.android.testutil.oauth.OC_TOKEN_REQUEST_ACCESS
import com.owncloud.android.testutil.oauth.OC_TOKEN_RESPONSE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class OAuthRepositoryTest {

    private val remoteOAuthDataSource = mockk<RemoteOAuthDataSource>(relaxed = true)
    private val oAuthRepository: OAuthRepository = OAuthRepositoryImpl(remoteOAuthDataSource)

    @Test
    fun `perform oidc discovery - ok`() {
        every { remoteOAuthDataSource.performOIDCDiscovery(OC_SERVER_INFO.baseUrl) } returns OC_OIDC_SERVER_CONFIGURATION

        oAuthRepository.performOIDCDiscovery(OC_SERVER_INFO.baseUrl)

        verify(exactly = 1) {
            remoteOAuthDataSource.performOIDCDiscovery(OC_SERVER_INFO.baseUrl)
        }
    }

    @Test(expected = Exception::class)
    fun `perform oidc discovery - ko`() {
        every { remoteOAuthDataSource.performOIDCDiscovery(OC_SERVER_INFO.baseUrl) } throws Exception()

        oAuthRepository.performOIDCDiscovery(OC_SERVER_INFO.baseUrl)
    }

    @Test
    fun `perform token request - ok`() {
        every { remoteOAuthDataSource.performTokenRequest(OC_TOKEN_REQUEST_ACCESS) } returns OC_TOKEN_RESPONSE

        oAuthRepository.performTokenRequest(OC_TOKEN_REQUEST_ACCESS)

        verify(exactly = 1) {
            remoteOAuthDataSource.performTokenRequest(OC_TOKEN_REQUEST_ACCESS)
        }
    }

    @Test(expected = Exception::class)
    fun `perform token request - ko`() {
        every { remoteOAuthDataSource.performTokenRequest(OC_TOKEN_REQUEST_ACCESS) } throws Exception()

        oAuthRepository.performTokenRequest(OC_TOKEN_REQUEST_ACCESS)
    }

    @Test
    fun `register client - ok`() {
        every { remoteOAuthDataSource.registerClient(OC_CLIENT_REGISTRATION_REQUEST) } returns OC_CLIENT_REGISTRATION

        oAuthRepository.registerClient(OC_CLIENT_REGISTRATION_REQUEST)

        verify(exactly = 1) {
            remoteOAuthDataSource.registerClient(OC_CLIENT_REGISTRATION_REQUEST)
        }
    }

    @Test(expected = Exception::class)
    fun `register client - ko`() {
        every { remoteOAuthDataSource.registerClient(OC_CLIENT_REGISTRATION_REQUEST) } throws Exception()

        remoteOAuthDataSource.registerClient(OC_CLIENT_REGISTRATION_REQUEST)
    }
}
