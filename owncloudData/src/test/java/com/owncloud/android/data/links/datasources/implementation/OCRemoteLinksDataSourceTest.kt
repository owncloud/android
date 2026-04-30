/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.data.links.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.domain.links.model.OCLinkType
import com.owncloud.android.lib.resources.links.services.OCLinksService
import com.owncloud.android.testutil.OC_ACCOUNT_NAME
import com.owncloud.android.testutil.OC_SPACE_PROJECT_WITH_IMAGE
import com.owncloud.android.testutil.SPACE_MEMBERS
import com.owncloud.android.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class OCRemoteLinksDataSourceTest {

    private lateinit var ocRemoteLinksDataSource: OCRemoteLinksDataSource

    private val ocLinksService: OCLinksService = mockk()
    private val clientManager: ClientManager = mockk(relaxed = true)

    private val password = "testPasswordForPublicLink"

    @Before
    fun setUp() {
        ocRemoteLinksDataSource = OCRemoteLinksDataSource(clientManager)
        every { clientManager.getLinksService(OC_ACCOUNT_NAME) } returns ocLinksService
    }

    @Test
    fun `addLink adds a public link over a project space correctly`() {
        val addLinkResult = createRemoteOperationResultMock(Unit, isSuccess = true)

        every {
            ocLinksService.addLink(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                displayName = SPACE_MEMBERS.links[0].displayName,
                type = OCLinkType.toString(SPACE_MEMBERS.links[0].type),
                expirationDate = SPACE_MEMBERS.links[0].expirationDateTime,
                password = password
            )
        } returns addLinkResult

        ocRemoteLinksDataSource.addLink(
            accountName = OC_ACCOUNT_NAME,
            spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
            displayName = SPACE_MEMBERS.links[0].displayName,
            type = SPACE_MEMBERS.links[0].type,
            expirationDate = SPACE_MEMBERS.links[0].expirationDateTime,
            password = password
        )

        verify(exactly = 1) {
            clientManager.getLinksService(OC_ACCOUNT_NAME)
            ocLinksService.addLink(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                displayName = SPACE_MEMBERS.links[0].displayName,
                type = OCLinkType.toString(SPACE_MEMBERS.links[0].type),
                expirationDate = SPACE_MEMBERS.links[0].expirationDateTime,
                password = password
            )
        }
    }

    @Test
    fun `editLink edits a public link from a project space correctly`() {
        val editLinkResult = createRemoteOperationResultMock(Unit, isSuccess = true)

        every {
            ocLinksService.editLink(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                linkId = SPACE_MEMBERS.links[0].id,
                displayName = SPACE_MEMBERS.links[0].displayName,
                type = OCLinkType.toString(SPACE_MEMBERS.links[0].type),
                expirationDate = SPACE_MEMBERS.links[0].expirationDateTime
            )
        } returns editLinkResult

        ocRemoteLinksDataSource.editLink(
            accountName = OC_ACCOUNT_NAME,
            spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
            linkId = SPACE_MEMBERS.links[0].id,
            displayName = SPACE_MEMBERS.links[0].displayName,
            type = SPACE_MEMBERS.links[0].type,
            expirationDate = SPACE_MEMBERS.links[0].expirationDateTime
        )

        verify(exactly = 1) {
            clientManager.getLinksService(OC_ACCOUNT_NAME)
            ocLinksService.editLink(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                linkId = SPACE_MEMBERS.links[0].id,
                displayName = SPACE_MEMBERS.links[0].displayName,
                type = OCLinkType.toString(SPACE_MEMBERS.links[0].type),
                expirationDate = SPACE_MEMBERS.links[0].expirationDateTime
            )
        }
    }

    @Test
    fun `editPasswordLink edits the password of a public link from a project space correctly`() {
        val editPasswordLinkResult = createRemoteOperationResultMock(Unit, isSuccess = true)

        every {
            ocLinksService.editPasswordLink(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                linkId = SPACE_MEMBERS.links[0].id,
                password = password
            )
        } returns editPasswordLinkResult

        ocRemoteLinksDataSource.editPasswordLink(
            accountName = OC_ACCOUNT_NAME,
            spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
            linkId = SPACE_MEMBERS.links[0].id,
            password = password
        )

        verify(exactly = 1) {
            clientManager.getLinksService(OC_ACCOUNT_NAME)
            ocLinksService.editPasswordLink(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                linkId = SPACE_MEMBERS.links[0].id,
                password = password
            )
        }
    }

    @Test
    fun `removeLink removes a public link from a project space correctly`() {
        val removeLinkResult = createRemoteOperationResultMock(Unit, isSuccess = true)

        every {
            ocLinksService.removeLink(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                linkId = SPACE_MEMBERS.links[0].id
            )
        } returns removeLinkResult

        ocRemoteLinksDataSource.removeLink(
            accountName = OC_ACCOUNT_NAME,
            spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
            linkId = SPACE_MEMBERS.links[0].id
        )

        verify(exactly = 1) {
            clientManager.getLinksService(OC_ACCOUNT_NAME)
            ocLinksService.removeLink(
                spaceId = OC_SPACE_PROJECT_WITH_IMAGE.id,
                linkId = SPACE_MEMBERS.links[0].id
            )
        }
    }
}
