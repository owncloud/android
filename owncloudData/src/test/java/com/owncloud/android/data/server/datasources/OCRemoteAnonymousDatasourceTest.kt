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

import com.owncloud.android.data.server.datasources.implementation.OCRemoteAnonymousDataSource
import com.owncloud.android.data.server.network.OCAnonymousServerService
import com.owncloud.android.domain.server.model.AuthenticationMethod
import com.owncloud.android.lib.common.http.HttpConstants.HTTP_UNAUTHORIZED
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK_NO_SSL
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK_SSL
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.testutil.OC_ServerInfo
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class OCRemoteAnonymousDatasourceTest {
    private lateinit var ocRemoteAnonymousDatasource: OCRemoteAnonymousDataSource

    private val ocAnonymousService: OCAnonymousServerService = mockk()

    private val OC_OWNCLOUD_VERSION = OwnCloudVersion("10.3.2")
    private val basicAuthHeader = "basic realm=\"owncloud\", charset=\"utf-8\""
    private val bearerHeader = "bearer realm=\"owncloud\""
    private val redirectedLocation = "http://demo.owncloud.demo.com"

    @Before
    fun init() {
        ocRemoteAnonymousDatasource = OCRemoteAnonymousDataSource(ocAnonymousService)
    }

    @Test
    fun getAuthenticationMethodFollowRedirections() {
        val checkPathExistenceResultFollowRedirectionMocked: RemoteOperationResult<Boolean> =
            createRemoteOperationResultMock(
                data = true,
                isSuccess = true,
                redirectedLocation = OC_ServerInfo.baseUrl
            )
        val checkPathExistenceResultMocked: RemoteOperationResult<Boolean> =
            createRemoteOperationResultMock(
                data = true,
                isSuccess = true,
                resultCode = OK_SSL,
                authenticationHeader = basicAuthHeader,
                httpCode = HTTP_UNAUTHORIZED
            )

        every {
            ocAnonymousService.checkPathExistence(redirectedLocation, false)
        } returns checkPathExistenceResultFollowRedirectionMocked

        every {
            ocAnonymousService.checkPathExistence(OC_ServerInfo.baseUrl, false)
        } returns checkPathExistenceResultMocked

        val authenticationMethod = ocRemoteAnonymousDatasource.getAuthenticationMethod(redirectedLocation)

        assertNotNull(authenticationMethod)
        assertEquals(AuthenticationMethod.BASIC_HTTP_AUTH, authenticationMethod)

        verify { ocAnonymousService.checkPathExistence(OC_ServerInfo.baseUrl, false) }
    }

    @Test
    fun getAuthenticationMethodBasic() {
        val checkPathExistenceResultMocked: RemoteOperationResult<Boolean> =
            createRemoteOperationResultMock(
                data = true,
                isSuccess = true,
                resultCode = OK_SSL,
                authenticationHeader = basicAuthHeader,
                httpCode = HTTP_UNAUTHORIZED,
                redirectedLocation = null
            )

        every {
            ocAnonymousService.checkPathExistence(OC_ServerInfo.baseUrl, false)
        } returns checkPathExistenceResultMocked

        val authenticationMethod = ocRemoteAnonymousDatasource.getAuthenticationMethod(OC_ServerInfo.baseUrl)

        assertNotNull(authenticationMethod)
        assertEquals(AuthenticationMethod.BASIC_HTTP_AUTH, authenticationMethod)

        verify { ocAnonymousService.checkPathExistence(OC_ServerInfo.baseUrl, false) }
    }

    @Test
    fun getAuthenticationMethodBearer() {
        val checkPathExistenceResultMocked: RemoteOperationResult<Boolean> =
            createRemoteOperationResultMock(
                data = true,
                isSuccess = true,
                resultCode = OK_SSL,
                authenticationHeader = bearerHeader,
                httpCode = HTTP_UNAUTHORIZED,
                redirectedLocation = null
            )

        every {
            ocAnonymousService.checkPathExistence(OC_ServerInfo.baseUrl, false)
        } returns checkPathExistenceResultMocked

        val authenticationMethod = ocRemoteAnonymousDatasource.getAuthenticationMethod(OC_ServerInfo.baseUrl)

        assertNotNull(authenticationMethod)
        assertEquals(AuthenticationMethod.BEARER_TOKEN, authenticationMethod)

        verify { ocAnonymousService.checkPathExistence(OC_ServerInfo.baseUrl, false) }
    }

    @Test(expected = Exception::class)
    fun getAuthenticationMethodException() {
        every {
            ocAnonymousService.checkPathExistence(OC_ServerInfo.baseUrl, false)
        } throws Exception()

        val authenticationMethod = ocRemoteAnonymousDatasource.getAuthenticationMethod(OC_ServerInfo.baseUrl)

        assertNotNull(authenticationMethod)
        assertEquals(AuthenticationMethod.BASIC_HTTP_AUTH, authenticationMethod)

        verify { ocAnonymousService.checkPathExistence(OC_ServerInfo.baseUrl, false) }
    }

    @Test
    fun getRemoteStatusIsSecureConnection() {
        val remoteStatusResultMocked: RemoteOperationResult<OwnCloudVersion> =
            createRemoteOperationResultMock(data = OC_OWNCLOUD_VERSION, isSuccess = true, resultCode = OK_SSL)

        every {
            ocAnonymousService.getRemoteStatus(OC_ServerInfo.baseUrl)
        } returns remoteStatusResultMocked

        val remoteStatus = ocRemoteAnonymousDatasource.getRemoteStatus(OC_ServerInfo.baseUrl)

        assertNotNull(remoteStatus)
        assertEquals(Pair(OC_OWNCLOUD_VERSION, true), remoteStatus)

        verify { ocAnonymousService.getRemoteStatus(OC_ServerInfo.baseUrl) }
    }

    @Test
    fun getRemoteStatusIsNotSecureConnection() {
        val remoteStatusResultMocked: RemoteOperationResult<OwnCloudVersion> =
            createRemoteOperationResultMock(data = OC_OWNCLOUD_VERSION, isSuccess = true, resultCode = OK_NO_SSL)

        every {
            ocAnonymousService.getRemoteStatus(OC_ServerInfo.baseUrl)
        } returns remoteStatusResultMocked

        val remoteStatus = ocRemoteAnonymousDatasource.getRemoteStatus(OC_ServerInfo.baseUrl)

        assertNotNull(remoteStatus)
        assertEquals(Pair(OC_OWNCLOUD_VERSION, false), remoteStatus)

        verify { ocAnonymousService.getRemoteStatus(OC_ServerInfo.baseUrl) }
    }

    @Test(expected = Exception::class)
    fun getRemoteStatusException() {
        every {
            ocAnonymousService.getRemoteStatus(OC_ServerInfo.baseUrl)
        } throws Exception()

        val remoteStatus = ocRemoteAnonymousDatasource.getRemoteStatus(OC_ServerInfo.baseUrl)

        assertNotNull(remoteStatus)
        assertEquals(Pair(OC_OWNCLOUD_VERSION, true), remoteStatus)

        verify { ocAnonymousService.getRemoteStatus(OC_ServerInfo.baseUrl) }
    }
}
