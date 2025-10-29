/**
 * ownCloud Android client application
 *
 * @author Aitor Ballesteros Pavón
 * @author Juan Carlos Garrote Gascón
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

package com.owncloud.android.data.spaces.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.data.spaces.datasources.implementation.OCRemoteSpacesDataSource.Companion.toModel
import com.owncloud.android.lib.resources.spaces.services.OCSpacesService
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_SPACE_PROJECT_WITH_IMAGE
import com.owncloud.android.testutil.OC_USER_GROUPS
import com.owncloud.android.testutil.OC_USER_ID
import com.owncloud.android.testutil.SPACE_PERMISSIONS
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

        val resultActual = ocRemoteSpacesDataSource.refreshSpacesForAccount(OC_ACCOUNT_NAME, OC_USER_ID, OC_USER_GROUPS)

        assertEquals(listOf(SPACE_RESPONSE.toModel(OC_ACCOUNT_NAME)), resultActual)

        verify(exactly = 1) {
            clientManager.getSpacesService(OC_ACCOUNT_NAME)
            ocSpaceService.getSpaces()
        }
    }

    @Test
    fun `createSpace creates a new project space correctly`() {
        val createSpaceOperationResult = createRemoteOperationResultMock(SPACE_RESPONSE, isSuccess = true)

        every {
            ocSpaceService.createSpace(
                spaceName = OC_SPACE_PROJECT_WITH_IMAGE.name,
                spaceSubtitle = OC_SPACE_PROJECT_WITH_IMAGE.description!!,
                spaceQuota = OC_SPACE_PROJECT_WITH_IMAGE.quota?.total!!
            )
        } returns createSpaceOperationResult

        val spaceResult = ocRemoteSpacesDataSource.createSpace(
            accountName = OC_ACCOUNT_NAME,
            spaceName = OC_SPACE_PROJECT_WITH_IMAGE.name,
            spaceSubtitle = OC_SPACE_PROJECT_WITH_IMAGE.description!!,
            spaceQuota = OC_SPACE_PROJECT_WITH_IMAGE.quota?.total!!
        )
        assertEquals(SPACE_RESPONSE.toModel(OC_ACCOUNT_NAME), spaceResult)

        verify(exactly = 1) {
            clientManager.getSpacesService(OC_ACCOUNT_NAME)
            ocSpaceService.createSpace(
                spaceName = OC_SPACE_PROJECT_WITH_IMAGE.name,
                spaceSubtitle = OC_SPACE_PROJECT_WITH_IMAGE.description!!,
                spaceQuota = OC_SPACE_PROJECT_WITH_IMAGE.quota?.total!!
            )
        }
    }

    @Test
    fun `getSpacePermissions returns a list of String with project space permissions`() {
        val getSpacePermissionsResult = createRemoteOperationResultMock(SPACE_PERMISSIONS, isSuccess = true)

        every {
            ocSpaceService.getSpacePermissions(OC_SPACE_PROJECT_WITH_IMAGE.id)
        } returns getSpacePermissionsResult

        val spacePermissions = ocRemoteSpacesDataSource.getSpacePermissions(OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.id)
        assertEquals(SPACE_PERMISSIONS, spacePermissions)

        verify(exactly = 1) {
            clientManager.getSpacesService(OC_ACCOUNT_NAME)
            ocSpaceService.getSpacePermissions(OC_SPACE_PROJECT_WITH_IMAGE.id)
        }
    }

    @Test
    fun `editSpace updates a project space correctly`() {
        val editSpaceOperationResult = createRemoteOperationResultMock(SPACE_RESPONSE, isSuccess = true)

        every {
            ocSpaceService.editSpace(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                spaceName = OC_SPACE_PROJECT_WITH_IMAGE.name,
                spaceSubtitle = OC_SPACE_PROJECT_WITH_IMAGE.description!!,
                spaceQuota = OC_SPACE_PROJECT_WITH_IMAGE.quota?.total!!
            )
        } returns editSpaceOperationResult

        val spaceResult = ocRemoteSpacesDataSource.editSpace(
            accountName = OC_ACCOUNT_NAME,
            spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
            spaceName = OC_SPACE_PROJECT_WITH_IMAGE.name,
            spaceSubtitle = OC_SPACE_PROJECT_WITH_IMAGE.description!!,
            spaceQuota = OC_SPACE_PROJECT_WITH_IMAGE.quota?.total!!
        )
        assertEquals(SPACE_RESPONSE.toModel(OC_ACCOUNT_NAME), spaceResult)

        verify(exactly = 1) {
            clientManager.getSpacesService(OC_ACCOUNT_NAME)
            ocSpaceService.editSpace(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                spaceName = OC_SPACE_PROJECT_WITH_IMAGE.name,
                spaceSubtitle = OC_SPACE_PROJECT_WITH_IMAGE.description!!,
                spaceQuota = OC_SPACE_PROJECT_WITH_IMAGE.quota?.total!!
            )
        }
    }

    @Test
    fun `disableSpace disables a project space correctly when delete mode is false`() {
        val disableSpaceResult = createRemoteOperationResultMock(Unit, isSuccess = true)

        every {
            ocSpaceService.disableSpace(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                deleteMode = false
            )
        } returns disableSpaceResult

        ocRemoteSpacesDataSource.disableSpace(
            accountName = OC_ACCOUNT_NAME,
            spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
            deleteMode = false
        )

        verify(exactly = 1) {
            clientManager.getSpacesService(OC_ACCOUNT_NAME)
            ocSpaceService.disableSpace(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                deleteMode = false
            )
        }
    }

    @Test
    fun `disableSpace deletes a project space correctly when delete mode is true`() {
        val disableSpaceResult = createRemoteOperationResultMock(Unit, isSuccess = true)

        every {
            ocSpaceService.disableSpace(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                deleteMode = true
            )
        } returns disableSpaceResult

        ocRemoteSpacesDataSource.disableSpace(
            accountName = OC_ACCOUNT_NAME,
            spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
            deleteMode = true
        )

        verify(exactly = 1) {
            clientManager.getSpacesService(OC_ACCOUNT_NAME)
            ocSpaceService.disableSpace(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                deleteMode = true
            )
        }
    }

    @Test
    fun `enableSpace enables a project space correctly`() {
        val enableSpaceResult = createRemoteOperationResultMock(SPACE_RESPONSE, isSuccess = true)

        every {
            ocSpaceService.enableSpace(OC_SPACE_PROJECT_WITH_IMAGE.id)
        } returns enableSpaceResult

        ocRemoteSpacesDataSource.enableSpace(OC_ACCOUNT_NAME, OC_SPACE_PROJECT_WITH_IMAGE.id)

        verify(exactly = 1) {
            clientManager.getSpacesService(OC_ACCOUNT_NAME)
            ocSpaceService.enableSpace(OC_SPACE_PROJECT_WITH_IMAGE.id)
        }
    }

}
