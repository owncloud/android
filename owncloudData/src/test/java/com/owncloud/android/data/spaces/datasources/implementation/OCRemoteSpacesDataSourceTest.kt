/**
 * ownCloud Android client application
 *
 * @author Aitor Ballesteros Pavón
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.data.spaces.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.spaces.datasources.implementation.OCRemoteSpacesDataSource.Companion.toModel
import com.owncloud.android.lib.resources.spaces.services.OCSpacesService
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.SPACE_RESPONSE
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCRemoteSpacesDataSourceTest {

    private lateinit var ocRemoteSpacesDataSource: OCRemoteSpacesDataSource

    private val ocSpaceService: OCSpacesService = mockk()
    private val clientManager: ClientManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        ocRemoteSpacesDataSource = OCRemoteSpacesDataSource(clientManager)
        every { clientManager.getSpacesService(OC_ACCOUNT_NAME) } returns ocSpaceService
    }

    @Test
    fun `refreshSpacesForAccount returns a list of OCSpace`() {
        val getRemoteSpacesOperationResult = createRemoteOperationResultMock(
            listOf(SPACE_RESPONSE), isSuccess = true
        )

        every { ocSpaceService.getSpaces() } returns getRemoteSpacesOperationResult

        val resultActual = ocRemoteSpacesDataSource.refreshSpacesForAccount(OC_ACCOUNT_NAME)

        assertEquals(listOf(SPACE_RESPONSE.toModel(OC_ACCOUNT_NAME)), resultActual)

        verify(exactly = 1) {
            clientManager.getSpacesService(OC_ACCOUNT_NAME)
            ocSpaceService.getSpaces()
        }
    }
}
