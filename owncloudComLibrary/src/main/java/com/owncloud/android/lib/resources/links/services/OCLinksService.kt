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

package com.owncloud.android.lib.resources.links.services

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.links.AddRemoteLinkOperation
import com.owncloud.android.lib.resources.links.EditRemoteLinkOperation
import com.owncloud.android.lib.resources.links.EditRemotePasswordLinkOperation
import com.owncloud.android.lib.resources.links.RemoveRemoteLinkOperation

class OCLinksService(override val client: OwnCloudClient) : LinksService {

    override fun addLink(
        spaceId: String,
        displayName: String,
        type: String,
        expirationDate: String?,
        password: String?
    ): RemoteOperationResult<Unit> =
        AddRemoteLinkOperation(
            spaceId = spaceId,
            displayName = displayName,
            type = type,
            expirationDate = expirationDate,
            password = password
        ).execute(client)

    override fun editLink(
        spaceId: String,
        linkId: String,
        displayName: String,
        type: String,
        expirationDate: String?
    ): RemoteOperationResult<Unit> =
        EditRemoteLinkOperation(
            spaceId = spaceId,
            linkId = linkId,
            displayName = displayName,
            type = type,
            expirationDate = expirationDate
        ).execute(client)

    override fun editPasswordLink(
        spaceId: String,
        linkId: String,
        password: String?
    ): RemoteOperationResult<Unit> =
        EditRemotePasswordLinkOperation(
            spaceId = spaceId,
            linkId = linkId,
            password = password
        ).execute(client)

    override fun removeLink(spaceId: String, linkId: String): RemoteOperationResult<Unit> =
        RemoveRemoteLinkOperation(
            spaceId = spaceId,
            linkId = linkId
        ).execute(client)
}
