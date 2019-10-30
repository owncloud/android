/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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
import com.owncloud.android.domain.utils.DomainTestUtil
import io.mockk.every
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetStoredCapabilitiesUseCaseTest {
    @Rule
    @JvmField
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val capabilityRepository: CapabilityRepository = spyk()
    private val useCase = GetStoredCapabilitiesUseCase((capabilityRepository))
    private val useCaseParams = GetStoredCapabilitiesUseCase.Params("")
    private lateinit var capabilitiesEmitted: MutableList<OCCapability>

    @Before
    fun init() {
        capabilitiesEmitted = mutableListOf()
    }

    @Test
    fun getStoredCapabilitiesOk() {
        val capabilitiesLiveData = MutableLiveData<OCCapability>()
        every { capabilityRepository.getStoredCapabilities(any()) } returns capabilitiesLiveData

        val capabilitiesToEmit = listOf(DomainTestUtil.createCapability())

        useCase.execute(useCaseParams).observeForever {
            capabilitiesEmitted.add(it!!)
        }

        capabilitiesToEmit.forEach { capabilitiesLiveData.postValue(it) }

        Assert.assertEquals(capabilitiesToEmit, capabilitiesEmitted)
    }

    @Test(expected = Exception::class)
    fun getStoredCapabilitiesException() {
        every { capabilityRepository.getStoredCapabilities(any()) } throws Exception()

        useCase.execute(useCaseParams)
    }
}
