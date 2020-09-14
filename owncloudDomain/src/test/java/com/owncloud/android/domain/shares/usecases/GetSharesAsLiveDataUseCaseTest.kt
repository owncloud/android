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
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GetSharesAsLiveDataUseCaseTest {

    @Rule
    @JvmField
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val repository: ShareRepository = spyk()
    private val useCase = GetSharesAsLiveDataUseCase(repository)
    private val useCaseParams = GetSharesAsLiveDataUseCase.Params("", "")

    @Test
    fun `get shares as livedata - ok`() {
        val sharesLiveData = MutableLiveData<List<OCShare>>()
        every { repository.getSharesAsLiveData(any(), any()) } returns sharesLiveData

        val sharesToEmit = listOf(
            OC_SHARE,
            OC_SHARE.copy(id = 2),
            OC_SHARE.copy(id = 3)
        )

        val sharesEmitted = mutableListOf<OCShare>()

        useCase.execute(useCaseParams).observeForever {
            it?.forEach { ocShare -> sharesEmitted.add(ocShare) }
        }

        sharesLiveData.postValue(sharesToEmit)

        assertEquals(sharesToEmit, sharesEmitted)

        verify(exactly = 1) { repository.getSharesAsLiveData(any(), any()) }
    }

    @Test(expected = Exception::class)
    fun `get shares as livedata - ko`() {
        every { repository.getSharesAsLiveData(any(), any()) } throws Exception()

        useCase.execute(useCaseParams)
    }
}
