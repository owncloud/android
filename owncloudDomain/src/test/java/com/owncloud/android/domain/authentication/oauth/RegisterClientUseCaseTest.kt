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
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION
import com.owncloud.android.testutil.oauth.OC_CLIENT_REGISTRATION_REQUEST
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class RegisterClientUseCaseTest {

    private val repository: OAuthRepository = spyk()
    private val useCase = RegisterClientUseCase(repository)
    private val useCaseParams = RegisterClientUseCase.Params(OC_CLIENT_REGISTRATION_REQUEST)

    @Test
    fun `test register client - ok`() {
        every { repository.registerClient(useCaseParams.clientRegistrationRequest) } returns OC_CLIENT_REGISTRATION

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isSuccess)
        Assert.assertEquals(OC_CLIENT_REGISTRATION, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.registerClient(useCaseParams.clientRegistrationRequest) }
    }

    @Test
    fun `test register client - ko`() {
        every { repository.registerClient(useCaseParams.clientRegistrationRequest) } throws ServerNotReachableException()

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isError)
        Assert.assertTrue(useCaseResult.getThrowableOrNull() is ServerNotReachableException)

        verify(exactly = 1) { repository.registerClient(useCaseParams.clientRegistrationRequest) }
    }
}