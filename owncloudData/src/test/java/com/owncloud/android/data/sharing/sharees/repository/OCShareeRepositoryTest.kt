/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.data.sharing.sharees.repository

import com.owncloud.android.data.sharing.sharees.datasources.RemoteShareeDataSource
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_SHAREE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class OCShareeRepositoryTest {

    private val remoteShareeDataSource = mockk<RemoteShareeDataSource>()
    private val ocShareeRepository = OCShareeRepository(remoteShareeDataSource)

    @Test
    fun `getSharees returns a list of OCSharees`() {
        val searchString = "user"
        val requestedPage = 1
        val resultsPerPage = 30

        every {
            remoteShareeDataSource.getSharees(searchString, requestedPage, resultsPerPage, OC_ACCOUNT_NAME)
        } returns listOf(OC_SHAREE)

        val listOfSharees = ocShareeRepository.getSharees(searchString, requestedPage, resultsPerPage, OC_ACCOUNT_NAME)
        assertEquals(listOf(OC_SHAREE), listOfSharees)

        verify(exactly = 1) {
            remoteShareeDataSource.getSharees(searchString, requestedPage, resultsPerPage, OC_ACCOUNT_NAME)
        }
    }
}
