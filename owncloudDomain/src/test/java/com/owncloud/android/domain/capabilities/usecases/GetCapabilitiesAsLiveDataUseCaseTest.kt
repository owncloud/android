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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.domain.capabilities.CapabilityRepository
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.testutil.OC_CAPABILITY
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class GetCapabilitiesAsLiveDataUseCaseTest {

    @Rule
    @JvmField
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val repository: CapabilityRepository = spyk()
    private val useCase = GetCapabilitiesAsLiveDataUseCase((repository))
    private val useCaseParams = GetCapabilitiesAsLiveDataUseCase.Params("")

    @Test
    fun `get capabilities as livedata - ok`() {
        val capabilitiesLiveData = MutableLiveData<OCCapability>()
        every { repository.getCapabilitiesAsLiveData(any()) } returns capabilitiesLiveData

        val capabilitiesToEmit = listOf(OC_CAPABILITY)

        val capabilitiesEmitted = mutableListOf<OCCapability>()

        useCase.execute(useCaseParams).observeForever {
            capabilitiesEmitted.add(it!!)
        }

        capabilitiesToEmit.forEach { capabilitiesLiveData.postValue(it) }

        Assert.assertEquals(capabilitiesToEmit, capabilitiesEmitted)

        verify(exactly = 1) { repository.getCapabilitiesAsLiveData(any()) }
    }

    @Test(expected = Exception::class)
    fun `get capabilities as livedata - ko`() {
        every { repository.getCapabilitiesAsLiveData(any()) } throws Exception()

        useCase.execute(useCaseParams)
    }
}
