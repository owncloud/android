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
package com.owncloud.android.data.oauth.mapper

import com.owncloud.android.data.oauth.OC_REMOTE_TOKEN_REQUEST_PARAMS_ACCESS
import com.owncloud.android.data.oauth.OC_REMOTE_TOKEN_REQUEST_PARAMS_REFRESH
import com.owncloud.android.lib.resources.oauth.params.TokenRequestParams
import com.owncloud.android.testutil.oauth.OC_TOKEN_REQUEST_ACCESS
import com.owncloud.android.testutil.oauth.OC_TOKEN_REQUEST_REFRESH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteTokenRequestMapperTest {

    private val remoteTokenRequestMapper = RemoteTokenRequestMapper()

    @Test
    fun `to model - ok - null`() {
        assertNull(remoteTokenRequestMapper.toModel(null))
    }

    @Test
    fun `to remote - ok - access token`() {
        val tokenRequestParams =
            remoteTokenRequestMapper.toRemote(OC_TOKEN_REQUEST_ACCESS) as TokenRequestParams.Authorization

        assertEquals(OC_REMOTE_TOKEN_REQUEST_PARAMS_ACCESS.authorizationCode, tokenRequestParams.authorizationCode)
        assertEquals(OC_REMOTE_TOKEN_REQUEST_PARAMS_ACCESS.redirectUri, tokenRequestParams.redirectUri)
        assertEquals(OC_REMOTE_TOKEN_REQUEST_PARAMS_ACCESS.grantType, tokenRequestParams.grantType)
        assertEquals(OC_REMOTE_TOKEN_REQUEST_PARAMS_ACCESS.clientAuth, tokenRequestParams.clientAuth)
        assertEquals(OC_REMOTE_TOKEN_REQUEST_PARAMS_ACCESS.tokenEndpoint, tokenRequestParams.tokenEndpoint)
    }

    @Test
    fun `to remote - ok - refresh token`() {
        val tokenRequestParams =
            remoteTokenRequestMapper.toRemote(OC_TOKEN_REQUEST_REFRESH) as TokenRequestParams.RefreshToken

        assertEquals(OC_REMOTE_TOKEN_REQUEST_PARAMS_REFRESH.refreshToken, tokenRequestParams.refreshToken)
        assertEquals(OC_REMOTE_TOKEN_REQUEST_PARAMS_REFRESH.tokenEndpoint, tokenRequestParams.tokenEndpoint)
        assertEquals(OC_REMOTE_TOKEN_REQUEST_PARAMS_REFRESH.clientAuth, tokenRequestParams.clientAuth)
        assertEquals(OC_REMOTE_TOKEN_REQUEST_PARAMS_REFRESH.grantType, tokenRequestParams.grantType)
    }

    @Test
    fun `to remote - ok - null`() {
        assertNull(remoteTokenRequestMapper.toRemote(null))
    }
}
