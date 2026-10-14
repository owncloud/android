/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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
import com.owncloud.android.domain.sharing.shares.usecases.GetOcsSharesAsLiveDataUseCase
import com.owncloud.android.testutil.OC_SHARE
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GetOcsSharesAsLiveDataUseCaseTest {

    @Rule
    @JvmField
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val repository: ShareRepository = spyk()
    private val useCase = GetOcsSharesAsLiveDataUseCase(repository)
    private val useCaseParams = GetOcsSharesAsLiveDataUseCase.Params("", "")

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

        useCase(useCaseParams).observeForever {
            it?.forEach { ocShare -> sharesEmitted.add(ocShare) }
        }

        sharesLiveData.postValue(sharesToEmit)

        assertEquals(sharesToEmit, sharesEmitted)

        verify(exactly = 1) { repository.getSharesAsLiveData(any(), any()) }
    }

    @Test(expected = Exception::class)
    fun `get shares as livedata - ko`() {
        every { repository.getSharesAsLiveData(any(), any()) } throws Exception()

        useCase(useCaseParams)
    }
}
