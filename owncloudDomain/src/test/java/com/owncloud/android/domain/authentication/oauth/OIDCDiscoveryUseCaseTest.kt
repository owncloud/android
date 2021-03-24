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
package com.owncloud.android.domain.authentication.oauth

import com.owncloud.android.domain.exceptions.ServerNotReachableException
import com.owncloud.android.testutil.OC_SERVER_INFO
import com.owncloud.android.testutil.oauth.OC_OIDC_SERVER_CONFIGURATION
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class OIDCDiscoveryUseCaseTest {

    private val repository: OAuthRepository = spyk()
    private val useCase = OIDCDiscoveryUseCase(repository)
    private val useCaseParams = OIDCDiscoveryUseCase.Params(OC_SERVER_INFO.baseUrl)

    @Test
    fun `test perform oidc discovery - ok`() {
        every { repository.performOIDCDiscovery(useCaseParams.baseUrl) } returns OC_OIDC_SERVER_CONFIGURATION

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isSuccess)
        Assert.assertEquals(OC_OIDC_SERVER_CONFIGURATION, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.performOIDCDiscovery(useCaseParams.baseUrl) }
    }

    @Test
    fun `test perform oidc discovery - ko`() {
        every { repository.performOIDCDiscovery(useCaseParams.baseUrl) } throws ServerNotReachableException()

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isError)
        Assert.assertTrue(useCaseResult.getThrowableOrNull() is ServerNotReachableException)

        verify(exactly = 1) { repository.performOIDCDiscovery(useCaseParams.baseUrl) }
    }
}
