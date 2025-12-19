/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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

package com.owncloud.android.data.roles.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.roles.datasources.implementation.OCRemoteRolesDataSource.Companion.toModel
import com.owncloud.android.lib.resources.roles.services.OCRolesService
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.SPACE_PERMISSIONS_RESPONSE
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test

class OCRemoteRolesDataSourceTest {

    private lateinit var ocRemoteRolesDataSource: OCRemoteRolesDataSource

    private val ocRolesService: OCRolesService = mockk()
    private val clientManager: ClientManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        ocRemoteRolesDataSource = OCRemoteRolesDataSource(clientManager)
        every { clientManager.getRolesService(OC_ACCOUNT_NAME) } returns ocRolesService
    }

    @Test
    fun `getRoles returns a list of OCRole with all available roles from the server`() {
        val getRolesResult = createRemoteOperationResultMock(SPACE_PERMISSIONS_RESPONSE.roles, isSuccess = true)

        every {
            ocRolesService.getRoles()
        } returns getRolesResult

        val roles = ocRemoteRolesDataSource.getRoles(OC_ACCOUNT_NAME)
        assertEquals(SPACE_PERMISSIONS_RESPONSE.roles.map { it.toModel() }, roles)

        verify(exactly = 1) {
            clientManager.getRolesService(OC_ACCOUNT_NAME)
            ocRolesService.getRoles()
        }
    }

}
