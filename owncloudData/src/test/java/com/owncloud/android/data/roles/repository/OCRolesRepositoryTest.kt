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

package com.owncloud.android.data.roles.repository

import com.owncloud.android.data.roles.datasources.RemoteRolesDataSource
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.SPACE_MEMBERS
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.junit.Test

class OCRolesRepositoryTest {

    private val remoteRolesDataSource = mockk<RemoteRolesDataSource>()
    private val ocRolesRepository = OCRolesRepository(remoteRolesDataSource)

    @Test
    fun `getRoles returns a list of OCRole`() {
        every {
            remoteRolesDataSource.getRoles(OC_ACCOUNT_NAME)
        } returns SPACE_MEMBERS.roles

        val roles = ocRolesRepository.getRoles(OC_ACCOUNT_NAME)
        assertEquals(SPACE_MEMBERS.roles, roles)

        verify(exactly = 1) {
            remoteRolesDataSource.getRoles(OC_ACCOUNT_NAME)
        }
    }

}
