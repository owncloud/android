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

package com.owncloud.android.data.server.repository

import com.owncloud.android.data.oauth.datasources.RemoteOAuthDataSource
import com.owncloud.android.data.server.datasources.RemoteServerInfoDataSource
import com.owncloud.android.data.webfinger.datasources.RemoteWebFingerDataSource
import com.owncloud.android.domain.webfinger.model.WebFingerRel
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_BASIC_AUTH
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_BEARER_AUTH
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_OIDC_AUTH
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_OIDC_AUTH_WEBFINGER_INSTANCE
import com.owncloud.android.testutil.OC_WEBFINGER_INSTANCE_URL
import com.owncloud.android.testutil.oauth.OC_OIDC_SERVER_CONFIGURATION
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class OCServerInfoRepositoryTest {

    private val remoteServerInfoDataSource = mockk<RemoteServerInfoDataSource>()
    private val remoteWebFingerDataSource = mockk<RemoteWebFingerDataSource>()
    private val remoteOAuthDataSource = mockk<RemoteOAuthDataSource>()
    private val ocServerInfoRepository = OCServerInfoRepository(remoteServerInfoDataSource, remoteWebFingerDataSource, remoteOAuthDataSource)

    @Test
    fun `getServerInfo returns a BasicServer when creatingAccount parameter is false`() {
        every {
            remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, false)
        } returns OC_SECURE_SERVER_INFO_BASIC_AUTH

        val basicServer = ocServerInfoRepository.getServerInfo(
            path = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
            creatingAccount = false,
            enforceOIDC = false
        )
        assertEquals(OC_SECURE_SERVER_INFO_BASIC_AUTH, basicServer)

        verify(exactly = 1) {
            remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, false)
        }
    }

    @Test
    fun `getServerInfo returns a BasicServer when creatingAccount parameter is true and webfinger datasource throws an exception`() {
        every {
            remoteWebFingerDataSource.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
                resource = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl
            )
        } throws Exception()

        every {
            remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, false)
        } returns OC_SECURE_SERVER_INFO_BASIC_AUTH

        val basicServer = ocServerInfoRepository.getServerInfo(
            path = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
            creatingAccount = true,
            enforceOIDC = false
        )
        assertEquals(OC_SECURE_SERVER_INFO_BASIC_AUTH, basicServer)

        verify(exactly = 1) {
            remoteWebFingerDataSource.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
                resource = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl
            )
            remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, false)
        }
    }

    @Test
    fun `getServerInfo returns an OAuth2Server when creatingAccount parameter is false`() {
        every {
            remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl, false)
        } returns OC_SECURE_SERVER_INFO_BEARER_AUTH

        every {
            remoteOAuthDataSource.performOIDCDiscovery(OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl)
        } throws Exception()

        val oAuthServer = ocServerInfoRepository.getServerInfo(
            path = OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl,
            creatingAccount = false,
            enforceOIDC = false
        )
        assertEquals(OC_SECURE_SERVER_INFO_BEARER_AUTH, oAuthServer)

        verify(exactly = 1) {
            remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl, false)
            remoteOAuthDataSource.performOIDCDiscovery(OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl)
        }
    }

    @Test
    fun `getServerInfo returns an OAuth2Server when creatingAccount parameter is true and webfinger datasource throws an exception`() {
        every {
            remoteWebFingerDataSource.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
                resource = OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl
            )
        } throws Exception()

        every {
            remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl, false)
        } returns OC_SECURE_SERVER_INFO_BEARER_AUTH

        every {
            remoteOAuthDataSource.performOIDCDiscovery(OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl)
        } throws Exception()

        val oAuthServer = ocServerInfoRepository.getServerInfo(
            path = OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl,
            creatingAccount = true,
            enforceOIDC = false
        )
        assertEquals(OC_SECURE_SERVER_INFO_BEARER_AUTH, oAuthServer)

        verify(exactly = 1) {
            remoteWebFingerDataSource.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
                resource = OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl
            )
            remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl, false)
            remoteOAuthDataSource.performOIDCDiscovery(OC_SECURE_SERVER_INFO_BEARER_AUTH.baseUrl)
        }
    }

    @Test
    fun `getServerInfo returns an OIDCServer when creatingAccount parameter is false`() {
        every {
            remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl, false)
        } returns OC_SECURE_SERVER_INFO_OIDC_AUTH

        every {
            remoteOAuthDataSource.performOIDCDiscovery(OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl)
        } returns OC_OIDC_SERVER_CONFIGURATION

        val oIDCServer = ocServerInfoRepository.getServerInfo(
            path = OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl,
            creatingAccount = false,
            enforceOIDC = false
        )
        assertEquals(OC_SECURE_SERVER_INFO_OIDC_AUTH, oIDCServer)

        verify(exactly = 1) {
            remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl, false)
            remoteOAuthDataSource.performOIDCDiscovery(OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl)
        }
    }

    @Test
    fun `getServerInfo returns an OIDCServer when creatingAccount parameter is true and webfinger datasource throws an exception`() {
        every {
            remoteWebFingerDataSource.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
                resource = OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl
            )
        } throws Exception()

        every {
            remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl, false)
        } returns OC_SECURE_SERVER_INFO_OIDC_AUTH

        every {
            remoteOAuthDataSource.performOIDCDiscovery(OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl)
        } returns OC_OIDC_SERVER_CONFIGURATION

        val oIDCServer = ocServerInfoRepository.getServerInfo(
            path = OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl,
            creatingAccount = true,
            enforceOIDC = false
        )
        assertEquals(OC_SECURE_SERVER_INFO_OIDC_AUTH, oIDCServer)

        verify(exactly = 1) {
            remoteWebFingerDataSource.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
                resource = OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl
            )
            remoteServerInfoDataSource.getServerInfo(OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl, false)
            remoteOAuthDataSource.performOIDCDiscovery(OC_SECURE_SERVER_INFO_OIDC_AUTH.baseUrl)
        }
    }

    @Test
    fun `getServerInfo returns an OIDCServer when creatingAccount is true and webfinger datasource returns an OIDC issuer`() {
        every {
            remoteWebFingerDataSource.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_OIDC_AUTH_WEBFINGER_INSTANCE.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
                resource = OC_SECURE_SERVER_INFO_OIDC_AUTH_WEBFINGER_INSTANCE.baseUrl
            )
        } returns listOf(OC_WEBFINGER_INSTANCE_URL)

        every {
            remoteOAuthDataSource.performOIDCDiscovery(OC_WEBFINGER_INSTANCE_URL)
        } returns OC_OIDC_SERVER_CONFIGURATION

        val oIDCServerWebfinger = ocServerInfoRepository.getServerInfo(
            path = OC_SECURE_SERVER_INFO_OIDC_AUTH_WEBFINGER_INSTANCE.baseUrl,
            creatingAccount = true,
            enforceOIDC = false
        )
        assertEquals(OC_SECURE_SERVER_INFO_OIDC_AUTH_WEBFINGER_INSTANCE, oIDCServerWebfinger)

        verify(exactly = 1) {
            remoteWebFingerDataSource.getInstancesFromWebFinger(
                lookupServer = OC_SECURE_SERVER_INFO_OIDC_AUTH_WEBFINGER_INSTANCE.baseUrl,
                rel = WebFingerRel.OIDC_ISSUER_DISCOVERY,
                resource = OC_SECURE_SERVER_INFO_OIDC_AUTH_WEBFINGER_INSTANCE.baseUrl
            )
            remoteOAuthDataSource.performOIDCDiscovery(OC_WEBFINGER_INSTANCE_URL)
        }
    }
}
