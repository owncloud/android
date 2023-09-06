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

package com.owncloud.android.data.oauth.datasources

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.oauth.OC_REMOTE_CLIENT_REGISTRATION_PARAMS
import com.owncloud.android.data.oauth.OC_REMOTE_CLIENT_REGISTRATION_RESPONSE
import com.owncloud.android.data.oauth.OC_REMOTE_OIDC_DISCOVERY_RESPONSE
import com.owncloud.android.data.oauth.OC_REMOTE_TOKEN_REQUEST_PARAMS_ACCESS
import com.owncloud.android.data.oauth.OC_REMOTE_TOKEN_RESPONSE
import com.owncloud.android.data.oauth.datasources.implementation.OCRemoteOAuthDataSource
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.oauth.responses.ClientRegistrationResponse
import com.owncloud.android.lib.resources.oauth.responses.OIDCDiscoveryResponse
import com.owncloud.android.lib.resources.oauth.responses.TokenResponse
import com.owncloud.android.lib.resources.oauth.services.OIDCService
import com.owncloud.android.testutil.OC_SECURE_BASE_URL
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION_REQUEST
import com.owncloud.android.testutil.oauth.OC_OIDC_SERVER_CONFIGURATION
import com.owncloud.android.testutil.oauth.OC_TOKEN_REQUEST_ACCESS
import com.owncloud.android.testutil.oauth.OC_TOKEN_RESPONSE
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class RemoteOAuthDataSourceTest {
    private lateinit var remoteOAuthDataSource: RemoteOAuthDataSource

    private val clientManager: ClientManager = mockk(relaxed = true)
    private val ocClientMocked: OwnCloudClient = mockk()

    private val oidcService: OIDCService = mockk()

    @Before
    fun init() {
        every { clientManager.getClientForAnonymousCredentials(any(), any()) } returns ocClientMocked

        remoteOAuthDataSource = OCRemoteOAuthDataSource(
            clientManager = clientManager,
            oidcService = oidcService,
        )
    }

    @Test
    fun `performOIDCDiscovery returns a object of OIDCServerConfiguration`() {
        val oidcDiscoveryResult: RemoteOperationResult<OIDCDiscoveryResponse> =
            createRemoteOperationResultMock(data = OC_REMOTE_OIDC_DISCOVERY_RESPONSE, isSuccess = true)

        every {
            oidcService.getOIDCServerDiscovery(ocClientMocked)
        } returns oidcDiscoveryResult

        val oidcDiscovery = remoteOAuthDataSource.performOIDCDiscovery(OC_SECURE_BASE_URL)

        assertNotNull(oidcDiscovery)
        assertEquals(OC_OIDC_SERVER_CONFIGURATION, oidcDiscovery)

        verify(exactly = 1) {
            clientManager.getClientForAnonymousCredentials(OC_SECURE_BASE_URL, false)
            oidcService.getOIDCServerDiscovery(ocClientMocked)
        }
    }

    @Test(expected = Exception::class)
    fun `performOIDCDiscovery returns an exception when service receive an exception`() {
        every {
            oidcService.getOIDCServerDiscovery(ocClientMocked)
        } throws Exception()

        remoteOAuthDataSource.performOIDCDiscovery(OC_SECURE_BASE_URL)
    }

    @Test
    fun `performTokenRequest returns a TokenResponse`() {
        val tokenResponseResult: RemoteOperationResult<TokenResponse> =
            createRemoteOperationResultMock(data = OC_REMOTE_TOKEN_RESPONSE, isSuccess = true)

        every {
            oidcService.performTokenRequest(ocClientMocked, any())
        } returns tokenResponseResult

        val tokenResponse = remoteOAuthDataSource.performTokenRequest(OC_TOKEN_REQUEST_ACCESS)

        assertNotNull(tokenResponse)
        assertEquals(OC_TOKEN_RESPONSE, tokenResponse)

        verify(exactly = 1) {
            clientManager.getClientForAnonymousCredentials(OC_SECURE_BASE_URL, false)
            oidcService.performTokenRequest(ocClientMocked, any())
        }
    }

    @Test(expected = Exception::class)
    fun `performTokenRequest returns an exception when service receive an exception`() {
        every {
            oidcService.performTokenRequest(ocClientMocked, OC_REMOTE_TOKEN_REQUEST_PARAMS_ACCESS)
        } throws Exception()

        remoteOAuthDataSource.performTokenRequest(OC_TOKEN_REQUEST_ACCESS)
    }

    @Test
    fun `registerClient returns a ClientRegistrationInfo object`() {
        val clientRegistrationResponse: RemoteOperationResult<ClientRegistrationResponse> =
            createRemoteOperationResultMock(data = OC_REMOTE_CLIENT_REGISTRATION_RESPONSE, isSuccess = true)

        every {
            oidcService.registerClientWithRegistrationEndpoint(ocClientMocked, any())
        } returns clientRegistrationResponse

        val clientRegistrationInfo = remoteOAuthDataSource.registerClient(OC_CLIENT_REGISTRATION_REQUEST)

        assertNotNull(clientRegistrationInfo)
        assertEquals(OC_CLIENT_REGISTRATION, clientRegistrationInfo)

        verify(exactly = 1) {
            clientManager.getClientForAnonymousCredentials(any(), false)
            oidcService.registerClientWithRegistrationEndpoint(ocClientMocked, any())
        }
    }

    @Test(expected = Exception::class)
    fun `registerClient returns an exception when service receive an exception`() {
        every {
            oidcService.registerClientWithRegistrationEndpoint(ocClientMocked, OC_REMOTE_CLIENT_REGISTRATION_PARAMS)
        } throws Exception()

        remoteOAuthDataSource.registerClient(OC_CLIENT_REGISTRATION_REQUEST)
    }
}
