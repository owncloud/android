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

package com.owncloud.android.data.server.datasources

import com.owncloud.android.data.server.datasources.implementation.OCRemoteServerInfoDataSource
import com.owncloud.android.domain.exceptions.OwncloudVersionNotSupportedException
import com.owncloud.android.domain.exceptions.SpecificServiceUnavailableException
import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.lib.common.http.HttpConstants.HTTP_SERVICE_UNAVAILABLE
import com.owncloud.android.lib.common.http.HttpConstants.HTTP_UNAUTHORIZED
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK_NO_SSL
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK_SSL
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.lib.resources.status.services.implementation.OCServerInfoService
import com.owncloud.android.testutil.OC_SERVER_INFO
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class OCRemoteServerInfoDataSourceTest {
    private lateinit var ocRemoteServerInfoDatasource: OCRemoteServerInfoDataSource

    private val ocInfoService: OCServerInfoService = mockk()

    private val ocOwncloudVersion = OwnCloudVersion("10.3.2")
    private val basicAuthHeader = "basic realm=\"owncloud\", charset=\"utf-8\""
    private val bearerHeader = "bearer realm=\"owncloud\""
    private val authHeadersBasic = listOf(basicAuthHeader)
    private val authHeaderBearer = listOf(basicAuthHeader, bearerHeader)
    private val redirectedLocation = "http://demo.owncloud.demo.com"

    @Before
    fun init() {
        ocRemoteServerInfoDatasource = OCRemoteServerInfoDataSource(ocInfoService)
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
            ocInfoService.checkPathExistence(redirectedLocation, false)
        } returns checkPathExistenceResultFollowRedirectionMocked

        every {
            ocInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false)
        } returns checkPathExistenceResultMocked

        val authenticationMethod = ocRemoteServerInfoDatasource.getAuthenticationMethod(redirectedLocation)

        assertNotNull(authenticationMethod)
        assertEquals(AuthenticationMethod.BASIC_HTTP_AUTH, authenticationMethod)

        verify { ocInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false) }
    }

    @Test
    fun getAuthenticationMethodBasic() {
        val expectedValue = AuthenticationMethod.BASIC_HTTP_AUTH
        prepareAuthorizationMethodToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getAuthenticationMethod(OC_SERVER_INFO.baseUrl)

        assertNotNull(expectedValue)
        assertEquals(expectedValue, currentValue)

        verify { ocInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false) }
    }

    @Test
    fun getAuthenticationMethodBearer() {
        val expectedValue = AuthenticationMethod.BEARER_TOKEN
        prepareAuthorizationMethodToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getAuthenticationMethod(OC_SERVER_INFO.baseUrl)

        assertNotNull(expectedValue)
        assertEquals(expectedValue, currentValue)

        verify { ocInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false) }
    }

    @Test
    fun getAuthenticationMethodNone() {
        val expectedValue = AuthenticationMethod.NONE
        prepareAuthorizationMethodToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getAuthenticationMethod(OC_SERVER_INFO.baseUrl)

        assertNotNull(expectedValue)
        assertEquals(expectedValue, currentValue)

        verify { ocInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false) }
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
            ocInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false)
        } throws Exception()

        ocRemoteServerInfoDatasource.getAuthenticationMethod(OC_SERVER_INFO.baseUrl)
    }

    @Test
    fun getRemoteStatusIsSecureConnection() {
        val expectedValue = Pair(ocOwncloudVersion, true)
        prepareRemoteStatusToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getRemoteStatus(OC_SERVER_INFO.baseUrl)

        assertNotNull(currentValue)
        assertEquals(expectedValue, currentValue)

        verify { ocInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl) }
    }

    @Test
    fun getRemoteStatusIsNotSecureConnection() {
        val expectedValue = Pair(ocOwncloudVersion, false)
        prepareRemoteStatusToBeRetrieved(expectedValue)

        val currentValue = ocRemoteServerInfoDatasource.getRemoteStatus(OC_SERVER_INFO.baseUrl)

        assertNotNull(currentValue)
        assertEquals(expectedValue, currentValue)

        verify { ocInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl) }
    }

    @Test(expected = OwncloudVersionNotSupportedException::class)
    fun getRemoteStatusOwncloudVersionNotSupported() {
        val expectedValue = Pair(OwnCloudVersion("9.0.0"), false)
        prepareRemoteStatusToBeRetrieved(expectedValue)

        ocRemoteServerInfoDatasource.getRemoteStatus(OC_SERVER_INFO.baseUrl)
    }

    @Test
    fun getRemoteStatusOwncloudVersionHidden() {
        val expectedValue = Pair(OwnCloudVersion(""), false)
        prepareRemoteStatusToBeRetrieved(expectedValue)

        ocRemoteServerInfoDatasource.getRemoteStatus(OC_SERVER_INFO.baseUrl)

        val remoteStatus = ocRemoteServerInfoDatasource.getRemoteStatus(OC_SERVER_INFO.baseUrl)

        assertEquals(true, remoteStatus.first.isVersionHidden)
        verify { ocInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl) }
    }

    @Test(expected = Exception::class)
    fun getRemoteStatusException() {
        every {
            ocInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl)
        } throws Exception()

        ocRemoteServerInfoDatasource.getRemoteStatus(OC_SERVER_INFO.baseUrl)
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
            ocInfoService.checkPathExistence(OC_SERVER_INFO.baseUrl, false)
        } returns checkPathExistenceResultMocked
    }

    private fun prepareRemoteStatusToBeRetrieved(expectedPair: Pair<OwnCloudVersion, Boolean>) {
        val expectedResultCode = when (expectedPair.second) {
            true -> OK_SSL
            false -> OK_NO_SSL
        }

        val remoteStatusResultMocked: RemoteOperationResult<OwnCloudVersion> =
            createRemoteOperationResultMock(
                data = expectedPair.first,
                isSuccess = true,
                resultCode = expectedResultCode
            )

        every {
            ocInfoService.getRemoteStatus(OC_SERVER_INFO.baseUrl)
        } returns remoteStatusResultMocked
    }
}
