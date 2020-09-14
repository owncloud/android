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

package com.owncloud.android.domain.capabilities.usecases

import com.owncloud.android.domain.capabilities.CapabilityRepository
import com.owncloud.android.testutil.OC_CAPABILITY
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetStoredCapabilitiesUseCaseTest {

    private val repository: CapabilityRepository = spyk()
    private val useCase = GetStoredCapabilitiesUseCase((repository))
    private val useCaseParams = GetStoredCapabilitiesUseCase.Params("user@server")

    @Test
    fun `get stored capabilities - ok`() {
        every { repository.getStoredCapabilities(any()) } returns OC_CAPABILITY

        val capability = useCase.execute(useCaseParams)

        assertEquals(OC_CAPABILITY, capability)

        verify(exactly = 1) { repository.getStoredCapabilities(any()) }
    }

    @Test
    fun `get stored capabilities - ok - null`() {
        every { repository.getStoredCapabilities(any()) } returns null

        val capability = useCase.execute(useCaseParams)

        assertNull(capability)

        verify(exactly = 1) { repository.getStoredCapabilities(any()) }
    }

    @Test(expected = Exception::class)
    fun `get stored capabilities - ko`() {
        every { repository.getStoredCapabilities(any()) } throws Exception()

        useCase.execute(useCaseParams)
    }
}
