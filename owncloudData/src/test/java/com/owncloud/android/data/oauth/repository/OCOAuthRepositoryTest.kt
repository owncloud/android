/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author Jorge Aguado Recio
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

package com.owncloud.android.data.oauth.repository

import com.owncloud.android.data.oauth.datasources.RemoteOAuthDataSource
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_OIDC_AUTH
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION_REQUEST
import com.owncloud.android.testutil.oauth.OC_OIDC_SERVER_CONFIGURATION
import com.owncloud.android.testutil.oauth.OC_TOKEN_REQUEST_ACCESS
import com.owncloud.android.testutil.oauth.OC_TOKEN_RESPONSE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class OCOAuthRepositoryTest {

    private val remoteOAuthDataSource = mockk<RemoteOAuthDataSource>()
    private val oAuthRepository = OCOAuthRepository(remoteOAuthDataSource)

    @Test
    fun `performOIDCDiscovery returns an OIDCServerConfiguration`() {
        every {
            remoteOAuthDataSource.performOIDCDiscovery(OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl)
        } returns OC_OIDC_SERVER_CONFIGURATION

        val oidcServerConfiguration = oAuthRepository.performOIDCDiscovery(OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl)
        assertEquals(OC_OIDC_SERVER_CONFIGURATION, oidcServerConfiguration)

        verify(exactly = 1) {
            remoteOAuthDataSource.performOIDCDiscovery(OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl)
        }
    }

    @Test
    fun `performTokenRequest returns a TokenResponse`() {
        every {
            remoteOAuthDataSource.performTokenRequest(OC_TOKEN_REQUEST_ACCESS)
        } returns OC_TOKEN_RESPONSE

        val tokenResponse = oAuthRepository.performTokenRequest(OC_TOKEN_REQUEST_ACCESS)
        assertEquals(OC_TOKEN_RESPONSE, tokenResponse)

        verify(exactly = 1) {
            remoteOAuthDataSource.performTokenRequest(OC_TOKEN_REQUEST_ACCESS)
        }
    }

    @Test
    fun `registerClient returns a ClientRegistrationInfo`() {
        every {
            remoteOAuthDataSource.registerClient(OC_CLIENT_REGISTRATION_REQUEST)
        } returns OC_CLIENT_REGISTRATION

        val clientRegistrationInfo = oAuthRepository.registerClient(OC_CLIENT_REGISTRATION_REQUEST)
        assertEquals(OC_CLIENT_REGISTRATION, clientRegistrationInfo)

        verify(exactly = 1) {
            remoteOAuthDataSource.registerClient(OC_CLIENT_REGISTRATION_REQUEST)
        }
    }
}
