/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
import com.owncloud.android.testutil.OC_SERVER_INFO
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

    private val OC_REMOTE_SERVER_INFO = RemoteServerInfo(
        ownCloudVersion = OwnCloudVersion(OC_SERVER_INFO.ownCloudVersion),
        baseUrl = OC_SERVER_INFO.baseUrl,
        isSecureConnection = OC_SERVER_INFO.isSecureConnection
    )
    private val basicAuthHeader = "basic realm=\"owncloud\", charset=\"utf-8\""
    private val bearerHeader = "bearer realm=\"owncloud\""
    private val authHeadersBasic = listOf(basicAuthHeader)
    private val authHeaderBearer = listOf(basicAuthHeader, bearerHeader)
    private val redirectedLocation = "http://demo.owncloud.demo.com"

    @Before
    fun init() {
        ocRemoteServerInfoDatasource = OCRemoteServerInfoDataSource(ocServerInfoService, clientManager)

        every { clientManager.getClientForUnExistingAccount(any(), any()) } returns ocClientMocked
    }

    @Test
    fun getAuthenticationMethodFollowRedirections() {
        val checkPathExistenceResultFollowRedirectionMocked: RemoteOperationResult<Boolean> =
            createRemoteOperationResultMock(
                data = true,
                isSuccess = true,
                redirectedLocation = OC_SERVER_INFO.baseUrl
            )
        val checkPathExistenceResultMocked: RemoteOperationResult<Boolean> =
            createRemoteOperationResultMock(
                data = true,
                isSuccess = true,
                resultCode = OK_SSL,
                authenticationHeader = authHeadersBasic,
                httpCode = HTTP_UNAUTHORIZED
            )

        every {
            ocServerInfoService.checkPathExistence(redirectedLocation, false, ocClientMocked)
        } returns checkPathExistenceResultFollowRedirectionMocked

        every {
            ocServerInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false, ocClientMocked)
        } returns checkPathExistenceResultMocked

        val authenticationMethod = ocRemoteServerInfoDatasource.getAuthenticationMethod(redirectedLocation)

        assertNotNull(authenticationMethod)
        assertEquals(AuthenticationMethod.BASIC_HTTP_AUTH, authenticationMethod)

        verify { ocServerInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false, ocClientMocked) }
    }

    @Test
    fun getAuthenticationMethodBasic() {
        val expectedValue = AuthenticationMethod.BASIC_HTTP_AUTH
        prepareAuthorizationMethodToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getAuthenticationMethod(OC_SERVER_INFO.baseUrl)

        assertNotNull(expectedValue)
        assertEquals(expectedValue, currentValue)

        verify { ocServerInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false, ocClientMocked) }
    }

    @Test
    fun getAuthenticationMethodBearer() {
        val expectedValue = AuthenticationMethod.BEARER_TOKEN
        prepareAuthorizationMethodToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getAuthenticationMethod(OC_SERVER_INFO.baseUrl)

        assertNotNull(expectedValue)
        assertEquals(expectedValue, currentValue)

        verify { ocServerInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false, ocClientMocked) }
    }

    @Test
    fun getAuthenticationMethodNone() {
        val expectedValue = AuthenticationMethod.NONE
        prepareAuthorizationMethodToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getAuthenticationMethod(OC_SERVER_INFO.baseUrl)

        assertNotNull(expectedValue)
        assertEquals(expectedValue, currentValue)

        verify { ocServerInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false, ocClientMocked) }
    }

    @Test(expected = SpecificServiceUnavailableException::class)
    fun getAuthenticationMethodNotAvailable() {
        prepareAuthorizationMethodToBeRetrieved(
            expectedAuthenticationMethod = AuthenticationMethod.BASIC_HTTP_AUTH,
            isServerAvailable = false
        )
        ocRemoteServerInfoDatasource.getAuthenticationMethod(OC_SERVER_INFO.baseUrl)
    }

    @Test(expected = Exception::class)
    fun getAuthenticationMethodException() {
        every {
            ocServerInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false, ocClientMocked)
        } throws Exception()

        ocRemoteServerInfoDatasource.getAuthenticationMethod(OC_SERVER_INFO.baseUrl)
    }

    @Test
    fun getRemoteStatusIsSecureConnection() {
        val expectedValue = OC_REMOTE_SERVER_INFO.copy(isSecureConnection = true)
        prepareRemoteStatusToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getRemoteStatus(OC_SERVER_INFO.baseUrl)

        assertNotNull(currentValue)
        assertEquals(expectedValue, currentValue)

        verify { ocServerInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl, ocClientMocked) }
    }

    @Test
    fun getRemoteStatusIsNotSecureConnection() {
        val expectedValue = OC_REMOTE_SERVER_INFO.copy(isSecureConnection = false)
        prepareRemoteStatusToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getRemoteStatus(OC_SERVER_INFO.baseUrl)

        assertNotNull(currentValue)
        assertEquals(expectedValue, currentValue)

        verify { ocServerInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl, ocClientMocked) }
    }

    @Test(expected = OwncloudVersionNotSupportedException::class)
    fun getRemoteStatusOwncloudVersionNotSupported() {
        val expectedValue = OC_REMOTE_SERVER_INFO.copy(ownCloudVersion = OwnCloudVersion("9.0.0"))
        prepareRemoteStatusToBeRetrieved(expectedValue)

        ocRemoteServerInfoDatasource.getRemoteStatus(OC_SERVER_INFO.baseUrl)
    }

    @Test
    fun getRemoteStatusOwncloudVersionHidden() {
        val expectedValue = OC_REMOTE_SERVER_INFO.copy(ownCloudVersion = OwnCloudVersion(""))
        prepareRemoteStatusToBeRetrieved(expectedValue)

        ocRemoteServerInfoDatasource.getRemoteStatus(OC_SERVER_INFO.baseUrl)

        val remoteStatus = ocRemoteServerInfoDatasource.getRemoteStatus(OC_SERVER_INFO.baseUrl)

        assertTrue(remoteStatus.ownCloudVersion.isVersionHidden)
        verify { ocServerInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl, ocClientMocked) }
    }

    @Test(expected = Exception::class)
    fun getRemoteStatusException() {
        every {
            ocServerInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl, ocClientMocked)
        } throws Exception()

        ocRemoteServerInfoDatasource.getRemoteStatus(OC_SERVER_INFO.baseUrl)
    }

    @Test
    fun getServerInfoBasicAndSecureConnection() {
        val expectedValue =
            OC_SERVER_INFO.copy(authenticationMethod = AuthenticationMethod.BASIC_HTTP_AUTH, isSecureConnection = true)

        prepareRemoteStatusToBeRetrieved(OC_REMOTE_SERVER_INFO.copy(isSecureConnection = true))
        prepareAuthorizationMethodToBeRetrieved(expectedValue.authenticationMethod, true)

        val currentValue = ocRemoteServerInfoDatasource.getServerInfo(OC_SERVER_INFO.baseUrl)
        assertEquals(expectedValue, currentValue)

        verify(exactly = 1) { ocServerInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl, ocClientMocked) }
        verify(exactly = 1) { ocServerInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false, ocClientMocked) }
    }

    @Test
    fun getServerInfoBasicAndInSecureConnection() {
        val expectedValue =
            OC_SERVER_INFO.copy(authenticationMethod = AuthenticationMethod.BASIC_HTTP_AUTH, isSecureConnection = false)

        prepareRemoteStatusToBeRetrieved(OC_REMOTE_SERVER_INFO.copy(isSecureConnection = false))
        prepareAuthorizationMethodToBeRetrieved(expectedValue.authenticationMethod, true)

        val currentValue = ocRemoteServerInfoDatasource.getServerInfo(expectedValue.baseUrl)
        assertEquals(expectedValue, currentValue)

        verify(exactly = 1) { ocServerInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl, ocClientMocked) }
        verify(exactly = 1) { ocServerInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false, ocClientMocked) }
    }

    @Test
    fun getServerInfoBearerAndSecureConnection() {
        val expectedValue =
            OC_SERVER_INFO.copy(authenticationMethod = AuthenticationMethod.BEARER_TOKEN, isSecureConnection = true)

        prepareRemoteStatusToBeRetrieved(OC_REMOTE_SERVER_INFO.copy(isSecureConnection = true))
        prepareAuthorizationMethodToBeRetrieved(expectedValue.authenticationMethod, true)

        val currentValue = ocRemoteServerInfoDatasource.getServerInfo(OC_SERVER_INFO.baseUrl)
        assertEquals(expectedValue, currentValue)

        verify(exactly = 1) { ocServerInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl, ocClientMocked) }
        verify(exactly = 1) { ocServerInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false, ocClientMocked) }
    }

    @Test
    fun getServerInfoBearerAndInSecureConnection() {
        val expectedValue =
            OC_SERVER_INFO.copy(authenticationMethod = AuthenticationMethod.BASIC_HTTP_AUTH, isSecureConnection = true)

        prepareRemoteStatusToBeRetrieved(OC_REMOTE_SERVER_INFO.copy(isSecureConnection = true))
        prepareAuthorizationMethodToBeRetrieved(expectedValue.authenticationMethod, true)

        val currentValue = ocRemoteServerInfoDatasource.getServerInfo(OC_SERVER_INFO.baseUrl)
        assertEquals(expectedValue, currentValue)

        verify(exactly = 1) { ocServerInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl, ocClientMocked) }
        verify(exactly = 1) { ocServerInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false, ocClientMocked) }
    }

    @Test(expected = NoConnectionWithServerException::class)
    fun getServerInfoNoConnection() {
        prepareRemoteStatusToBeRetrieved(OC_REMOTE_SERVER_INFO, NoConnectionWithServerException())

        ocRemoteServerInfoDatasource.getServerInfo(OC_SERVER_INFO.baseUrl)

        verify(exactly = 1) { ocServerInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl, ocClientMocked) }
        verify(exactly = 0) { ocServerInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false, ocClientMocked) }
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
            ocServerInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false, ocClientMocked)
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
            ocServerInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl, ocClientMocked)
        } returns remoteStatusResultMocked
    }
}
