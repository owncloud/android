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

package com.owncloud.android.domain.shares.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.owncloud.android.domain.sharing.shares.ShareRepository
import com.owncloud.android.domain.sharing.shares.model.OCShare
import com.owncloud.android.domain.sharing.shares.usecases.GetSharesAsLiveDataUseCase
import com.owncloud.android.testutil.OC_SHARE
import io.mockk.every
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetSharesAsLiveDataUseCaseTest {
    @Rule
    @JvmField
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val shareRepository: ShareRepository = spyk()
    private val useCase = GetSharesAsLiveDataUseCase((shareRepository))
    private val useCaseParams = GetSharesAsLiveDataUseCase.Params("", "")
    private lateinit var sharesEmitted: MutableList<OCShare>

    @Before
    fun init() {
        sharesEmitted = mutableListOf()
    }

    @Test
    fun getSharesAsLiveDataOk() {
        val sharesLiveData = MutableLiveData<List<OCShare>>()
        every { shareRepository.getSharesAsLiveData(any(), any()) } returns sharesLiveData

        val sharesToEmit = listOf(OC_SHARE, OC_SHARE.copy(id = 2), OC_SHARE.copy(id = 3))

        useCase.execute(useCaseParams).observeForever {
            it?.forEach { ocShare -> sharesEmitted.add(ocShare) }
        }

        sharesLiveData.postValue(sharesToEmit)

        Assert.assertEquals(sharesToEmit, sharesEmitted)
    }

    @Test(expected = Exception::class)
    fun getSharesAsLiveDataException() {
        every { shareRepository.getSharesAsLiveData(any(), any()) } throws Exception()

        useCase.execute(useCaseParams)
    }
}
