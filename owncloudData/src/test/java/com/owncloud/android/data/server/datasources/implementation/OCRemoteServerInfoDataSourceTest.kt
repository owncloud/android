/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.data.server.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.domain.exceptions.OwncloudVersionNotSupportedException
import com.owncloud.android.domain.exceptions.SpecificServiceUnavailableException
import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants.HTTP_SERVICE_UNAVAILABLE
import com.owncloud.android.lib.common.http.HttpConstants.HTTP_UNAUTHORIZED
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK_NO_SSL
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK_SSL
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.lib.resources.status.RemoteServerInfo
import com.owncloud.android.lib.resources.status.services.implementation.OCServerInfoService
import com.owncloud.android.testutil.OC_INSECURE_SERVER_INFO_BASIC_AUTH
import com.owncloud.android.testutil.OC_INSECURE_SERVER_INFO_BEARER_AUTH
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_BASIC_AUTH
import com.owncloud.android.testutil.OC_SECURE_SERVER_INFO_BEARER_AUTH
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OCRemoteServerInfoDataSourceTest {
    private lateinit var ocRemoteServerInfoDatasource: OCRemoteServerInfoDataSource

    private val ocServerInfoService: OCServerInfoService = mockk()
    private val clientManager: ClientManager = mockk(relaxed = true)
    private val ocClientMocked: OwnCloudClient = mockk(relaxed = true)

    private val remoteServerInfo = RemoteServerInfo(
        ownCloudVersion = OwnCloudVersion(OC_SECURE_SERVER_INFO_BASIC_AUTH.ownCloudVersion),
        baseUrl = OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl,
        isSecureConnection = OC_SECURE_SERVER_INFO_BASIC_AUTH.isSecureConnection
    )
    private val basicAuthHeader = "basic realm=\"owncloud\", charset=\"utf-8\""
    private val bearerHeader = "bearer realm=\"owncloud\""
    private val authHeadersBasic = listOf(basicAuthHeader)
    private val authHeaderBearer = listOf(basicAuthHeader, bearerHeader)

    @Before
    fun setUp() {
        ocRemoteServerInfoDatasource = OCRemoteServerInfoDataSource(ocServerInfoService, clientManager)
        every { clientManager.getClientForAnonymousCredentials(any(), any()) } returns ocClientMocked
    }

    @Test
    fun `getAuthenticationMethod returns basic authentication`() {
        val expectedValue = AuthenticationMethod.BASIC_HTTP_AUTH
        prepareAuthorizationMethodToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getAuthenticationMethod(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)

        assertNotNull(expectedValue)
        assertEquals(expectedValue, currentValue)

        verify { ocServerInfoService.checkPathExistence(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, false, ocClientMocked) }
    }

    @Test
    fun `getAuthenticationMethod returns bearer authentication`() {
        val expectedValue = AuthenticationMethod.BEARER_TOKEN
        prepareAuthorizationMethodToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getAuthenticationMethod(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)

        assertNotNull(expectedValue)
        assertEquals(expectedValue, currentValue)

        verify { ocServerInfoService.checkPathExistence(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, false, ocClientMocked) }
    }

    @Test
    fun `getAuthenticationMethod returns none method`() {
        val expectedValue = AuthenticationMethod.NONE
        prepareAuthorizationMethodToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getAuthenticationMethod(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)

        assertNotNull(expectedValue)
        assertEquals(expectedValue, currentValue)

        verify { ocServerInfoService.checkPathExistence(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, false, ocClientMocked) }
    }

    @Test(expected = SpecificServiceUnavailableException::class)
    fun `getAuthenticationMethod throws exception when server is not available`() {
        prepareAuthorizationMethodToBeRetrieved(
            expectedAuthenticationMethod = AuthenticationMethod.BASIC_HTTP_AUTH,
            isServerAvailable = false
        )
        ocRemoteServerInfoDatasource.getAuthenticationMethod(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)
    }

    @Test
    fun `getRemoteStatus returns RemoteServerInfo with secure connection`() {
        val expectedValue = remoteServerInfo.copy(isSecureConnection = true)
        prepareRemoteStatusToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getRemoteStatus(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)

        assertNotNull(currentValue)
        assertEquals(expectedValue, currentValue)

        verify { ocServerInfoService.getRemoteStatus(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, ocClientMocked) }
    }

    @Test
    fun `getRemoteStatus returns RemoteServerInfo with insecure connection`() {
        val expectedValue = remoteServerInfo.copy(isSecureConnection = false)
        prepareRemoteStatusToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getRemoteStatus(OC_INSECURE_SERVER_INFO_BASIC_AUTH.baseUrl)

        assertNotNull(currentValue)
        assertEquals(expectedValue, currentValue)

        verify { ocServerInfoService.getRemoteStatus(OC_INSECURE_SERVER_INFO_BASIC_AUTH.baseUrl, ocClientMocked) }
    }

    @Test(expected = OwncloudVersionNotSupportedException::class)
    fun `getRemoteStatus throws exception when ownCloud version is not supported`() {
        val expectedValue = remoteServerInfo.copy(ownCloudVersion = OwnCloudVersion("9.0.0"))
        prepareRemoteStatusToBeRetrieved(expectedValue)

        ocRemoteServerInfoDatasource.getRemoteStatus(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)
    }

    @Test
    fun `getRemoteStatus returns RemoteServerInfo with hidden ownCloud version`() {
        val expectedValue = remoteServerInfo.copy(ownCloudVersion = OwnCloudVersion(""))
        prepareRemoteStatusToBeRetrieved(expectedValue)

        ocRemoteServerInfoDatasource.getRemoteStatus(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)

        val remoteStatus = ocRemoteServerInfoDatasource.getRemoteStatus(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)

        assertTrue(remoteStatus.ownCloudVersion.isVersionHidden)
        verify { ocServerInfoService.getRemoteStatus(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, ocClientMocked) }
    }

    @Test
    fun `getServerInfo returns ServerInfo with basic and secure connection`() {
        val expectedValue = OC_SECURE_SERVER_INFO_BASIC_AUTH

        prepareRemoteStatusToBeRetrieved(remoteServerInfo)
        prepareAuthorizationMethodToBeRetrieved(AuthenticationMethod.BASIC_HTTP_AUTH, true)

        val currentValue = ocRemoteServerInfoDatasource.getServerInfo(expectedValue.baseUrl)
        assertEquals(expectedValue, currentValue)

        verify(exactly = 1) { ocServerInfoService.getRemoteStatus(expectedValue.baseUrl, ocClientMocked) }
        verify(exactly = 1) { ocServerInfoService.checkPathExistence(expectedValue.baseUrl, false, ocClientMocked) }
    }

    @Test
    fun `getServerInfo returns ServerInfo with basic and insecure connection`() {
        val expectedValue = OC_INSECURE_SERVER_INFO_BASIC_AUTH

        prepareRemoteStatusToBeRetrieved(remoteServerInfo.copy(baseUrl = expectedValue.baseUrl, isSecureConnection = false))
        prepareAuthorizationMethodToBeRetrieved(AuthenticationMethod.BASIC_HTTP_AUTH, true)

        val currentValue = ocRemoteServerInfoDatasource.getServerInfo(expectedValue.baseUrl)
        assertEquals(expectedValue, currentValue)

        verify(exactly = 1) { ocServerInfoService.getRemoteStatus(expectedValue.baseUrl, ocClientMocked) }
        verify(exactly = 1) { ocServerInfoService.checkPathExistence(expectedValue.baseUrl, false, ocClientMocked) }
    }

    @Test
    fun `getServerInfo returns ServerInfo with bearer and secure connection`() {
        val expectedValue = OC_SECURE_SERVER_INFO_BEARER_AUTH

        prepareRemoteStatusToBeRetrieved(remoteServerInfo.copy(isSecureConnection = true))
        prepareAuthorizationMethodToBeRetrieved(AuthenticationMethod.BEARER_TOKEN, true)

        val currentValue = ocRemoteServerInfoDatasource.getServerInfo(expectedValue.baseUrl)
        assertEquals(expectedValue, currentValue)

        verify(exactly = 1) { ocServerInfoService.getRemoteStatus(expectedValue.baseUrl, ocClientMocked) }
        verify(exactly = 1) { ocServerInfoService.checkPathExistence(expectedValue.baseUrl, false, ocClientMocked) }
    }

    @Test
    fun `getServerInfo returns ServerInfo with bearer and insecure connection`() {
        val expectedValue = OC_INSECURE_SERVER_INFO_BEARER_AUTH

        prepareRemoteStatusToBeRetrieved(remoteServerInfo.copy(baseUrl = expectedValue.baseUrl, isSecureConnection = true))
        prepareAuthorizationMethodToBeRetrieved(AuthenticationMethod.BEARER_TOKEN, true)

        val currentValue = ocRemoteServerInfoDatasource.getServerInfo(expectedValue.baseUrl)
        assertEquals(expectedValue, currentValue)

        verify(exactly = 1) { ocServerInfoService.getRemoteStatus(expectedValue.baseUrl, ocClientMocked) }
        verify(exactly = 1) { ocServerInfoService.checkPathExistence(expectedValue.baseUrl, false, ocClientMocked) }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun `getServerInfo throws exception when there is no connection with the server`() {
        prepareRemoteStatusToBeRetrieved(remoteServerInfo, NoConnectionWithServerException())

        ocRemoteServerInfoDatasource.getServerInfo(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl)

        verify(exactly = 1) { ocServerInfoService.getRemoteStatus(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, ocClientMocked) }
        verify(exactly = 0) { ocServerInfoService.checkPathExistence(OC_SECURE_SERVER_INFO_BASIC_AUTH.baseUrl, false, ocClientMocked) }
    }

    private fun prepareAuthorizationMethodToBeRetrieved(
        expectedAuthenticationMethod: AuthenticationMethod,
        isServerAvailable: Boolean = true
    ) {
        val expectedAuthHeader = when (expectedAuthenticationMethod) {
            AuthenticationMethod.BEARER_TOKEN -> authHeaderBearer
            AuthenticationMethod.BASIC_HTTP_AUTH -> authHeadersBasic
            else -> listOf()
        }

        val checkPathExistenceResultMocked: RemoteOperationResult<Boolean> =
            createRemoteOperationResultMock(
                data = true,
                isSuccess = true,
                resultCode = OK_SSL,
                authenticationHeader = expectedAuthHeader,
                httpCode = if (isServerAvailable) HTTP_UNAUTHORIZED else HTTP_SERVICE_UNAVAILABLE
            )

        every {
            ocServerInfoService.checkPathExistence(any(), false, ocClientMocked)
        } returns checkPathExistenceResultMocked
    }

    private fun prepareRemoteStatusToBeRetrieved(
        expectedConfig: RemoteServerInfo,
        exception: Exception? = null
    ) {
        val expectedResultCode = when (expectedConfig.isSecureConnection) {
            true -> OK_SSL
            false -> OK_NO_SSL
        }

        val remoteStatusResultMocked: RemoteOperationResult<RemoteServerInfo> =
            createRemoteOperationResultMock(
                data = expectedConfig,
                isSuccess = true,
                resultCode = expectedResultCode,
                exception = exception
            )

        every {
            ocServerInfoService.getRemoteStatus(any(), ocClientMocked)
        } returns remoteStatusResultMocked
    }
}
