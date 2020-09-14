/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.domain.capabilities.usecases

import com.owncloud.android.domain.capabilities.CapabilityRepository
import com.owncloud.android.domain.exceptions.UnauthorizedException
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

class RefreshCapabilitiesFromServerAsyncUseCaseTest {

    private val repository: CapabilityRepository = spyk()
    private val useCase = RefreshCapabilitiesFromServerAsyncUseCase((repository))
    private val useCaseParams = RefreshCapabilitiesFromServerAsyncUseCase.Params("")

    @Test
    fun `refresh capabilities from server - ok`() {
        every { repository.refreshCapabilitiesForAccount(any()) } returns Unit

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isSuccess)
        Assert.assertEquals(Unit, useCaseResult.getDataOrNull())

        verify(exactly = 1) { repository.refreshCapabilitiesForAccount("") }
    }

    @Test
    fun `refresh capabilities from server - ko`() {
        every { repository.refreshCapabilitiesForAccount(any()) } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParams)

        Assert.assertTrue(useCaseResult.isError)
        Assert.assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { repository.refreshCapabilitiesForAccount("") }
    }
}
