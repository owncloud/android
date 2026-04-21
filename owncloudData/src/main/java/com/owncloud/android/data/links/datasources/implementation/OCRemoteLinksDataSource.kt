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
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.links.datasources.RemoteLinksDataSource
import com.owncloud.android.domain.links.model.OCLinkType

class OCRemoteLinksDataSource(
    private val clientManager: ClientManager
): RemoteLinksDataSource {

    override fun addLink(accountName: String, spaceId: String, displayName: String, type: OCLinkType, expirationDate: String?, password: String?) {
        executeRemoteOperation {
            clientManager.getLinksService(accountName).addLink(spaceId, displayName, OCLinkType.toString(type), expirationDate, password)
        }
    }

    override fun editLink(accountName: String, spaceId: String, linkId: String, displayName: String, type: OCLinkType, expirationDate: String?) {
        executeRemoteOperation {
            clientManager.getLinksService(accountName).editLink(spaceId, linkId, displayName, OCLinkType.toString(type), expirationDate)
        }
    }

    override fun editPasswordLink(accountName: String, spaceId: String, linkId: String, password: String?) {
        executeRemoteOperation {
            clientManager.getLinksService(accountName).editPasswordLink(spaceId, linkId, password)
        }
    }

    override fun removeLink(accountName: String, spaceId: String, linkId: String) {
        executeRemoteOperation {
            clientManager.getLinksService(accountName).removeLink(spaceId, linkId)
        }
    }
}
