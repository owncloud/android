/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.testutil

import com.owncloud.android.data.spaces.db.SpaceQuotaEntity
import com.owncloud.android.data.spaces.db.SpaceRootEntity
import com.owncloud.android.data.spaces.db.SpaceSpecialEntity
import com.owncloud.android.data.spaces.db.SpacesEntity
import com.owncloud.android.data.spaces.db.SpacesWithSpecials
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.spaces.model.OCSpace.Companion.SPACE_ID_SHARES
import com.owncloud.android.domain.spaces.model.SpaceDeleted
import com.owncloud.android.domain.spaces.model.SpaceFile
import com.owncloud.android.domain.spaces.model.SpaceOwner
import com.owncloud.android.domain.spaces.model.SpaceQuota
import com.owncloud.android.domain.spaces.model.SpaceRoot
import com.owncloud.android.domain.spaces.model.SpaceSpecial
import com.owncloud.android.domain.spaces.model.SpaceSpecialFolder
import com.owncloud.android.domain.spaces.model.SpaceUser
import com.owncloud.android.lib.resources.spaces.responses.QuotaResponse
import com.owncloud.android.lib.resources.spaces.responses.RootResponse
import com.owncloud.android.lib.resources.spaces.responses.SpaceResponse

const val WEB_DAV_URL = "https://server.url/dav/spaces/8871f4f3-fc6f-4a66-8bed-62f175f76f3805bca744-d89f-4e9c-a990-25a0d7f03fe9"



val OC_SPACE_SPECIAL_README = SpaceSpecial(
    eTag = "71f78349c3598c9e431a67de5a283fc0",
    file = SpaceFile(
        mimeType = "text/markdown"
    ),
    id = "8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568199!1c7bbc13-469f-482c-8f13-55ae1402b4c3",
    lastModifiedDateTime = "2023-01-01T00:00:00.00000000Z",
    name = "readme.md",
    size = 50,
    specialFolder = SpaceSpecialFolder(
        name = "readme"
    ),
    webDavUrl = "https://server.com/dav/spaces/8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568199%210aa0e03c-ec36-498c-bb9f-857315568199/.space/readme.md"
)

val OC_SPACE_SPECIAL_IMAGE = SpaceSpecial(
    eTag = "26ad7e0b49f9c0f163a6f227af3f130a",
    file = SpaceFile(
        mimeType = "image/jpeg"
    ),
    id = "8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568199!2597f35a-350f-4cf0-ace1-54b0e6bc377c",
    lastModifiedDateTime = "2023-01-01T00:00:00.00000000Z",
    name = "image.jpg",
    size = 50000,
    specialFolder = SpaceSpecialFolder(
        name = "image"
    ),
    webDavUrl = "https://server.com/dav/spaces/8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568199%210aa0e03c-ec36-498c-bb9f-857315568199/.space/image.jpg"
)

val OC_SPACE_PROJECT_WITH_IMAGE = OCSpace(
    accountName = OC_ACCOUNT_NAME,
    driveAlias = "project/space",
    driveType = "project",
    id = "8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568199",
    lastModifiedDateTime = "2023-01-01T00:00:00.00000000Z",
    name = "Space",
    owner = SpaceOwner(
        user = SpaceUser(
            id = "0aa0e03c-ec36-498c-bb9f-857315568199"
        )
    ),
    quota = SpaceQuota(
        remaining = 999999995,
        state = "normal",
        total = 1000000000,
        used = 5
    ),
    root = SpaceRoot(
        eTag = "989c7968dbbbde8c5fd9849b9123c384",
        id = "8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568199",
        webDavUrl = "https://server.com/dav/spaces/8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568199",
        deleted = null
    ),
    webUrl = "https://server.com/f/8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568199",
    description = "This is the description of the space",
    special = listOf(
        OC_SPACE_SPECIAL_IMAGE,
        OC_SPACE_SPECIAL_README
    )
)

val OC_SPACE_PROJECT_WITHOUT_IMAGE = OC_SPACE_PROJECT_WITH_IMAGE.copy(
    id = "8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-1234566789",
    name = "Space without image",
    root = SpaceRoot(
        eTag = "989c7968dbbbde8c5fd9849b9123c384",
        id = "8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-1234566789",
        webDavUrl = "https://server.com/dav/spaces/8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-1234566789",
        deleted = null
    ),
    webUrl = "https://server.com/f/8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-1234566789",
    special = listOf(OC_SPACE_SPECIAL_README)
)

val OC_SPACE_PERSONAL = OC_SPACE_PROJECT_WITH_IMAGE.copy(
    driveAlias = "personal/admin",
    driveType = "personal",
    name = "Admin",
    description = null,
    special = null
)

val OC_SPACE_PROJECT_DISABLED = OC_SPACE_PROJECT_WITH_IMAGE.copy(
    quota = SpaceQuota(
        remaining = null,
        state = null,
        total = 1000000000,
        used = null
    ),
    root = SpaceRoot(
        eTag = "989c7968dbbbde8c5fd9849b9123c384",
        id = "8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568199",
        webDavUrl = "https://server.com/dav/spaces/8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568199",
        deleted = SpaceDeleted(
            state = "trashed"
        )
    ),
    special = null
)

val SPACE_ENTITY_WITH_SPECIALS = SpacesWithSpecials(
    SpacesEntity(
        accountName = OC_ACCOUNT_NAME,
        driveAlias = "driveAlias",
        driveType = "driveType",
        id = OC_ACCOUNT_ID,
        ownerId = OC_CLIENT_ID,
        lastModifiedDateTime = "lastModifiedDateTime",
        name = "name",
        quota = null,
        root = SpaceRootEntity(
            eTag = "eTag",
            id = "id",
            webDavUrl = WEB_DAV_URL,
            deleteState = "state"
        ),
        webUrl = "webUrl",
        description = "description"
    ),
    listOf(
        SpaceSpecialEntity(
            accountName = OC_ACCOUNT_NAME,
            eTag = "eTag",
            fileMimeType = "fileMimeType",
            id = OC_ACCOUNT_ID,
            spaceId = OC_SPACE_PERSONAL.id,
            lastModifiedDateTime = "lastModifiedDateTime",
            name = "name",
            webDavUrl = WEB_DAV_URL,
            size = 100,
            specialFolderName = OC_SPACE_SPECIAL_IMAGE.name
        )
    )
)

val SPACE_ENTITY_PERSONAL = SpacesEntity(
        accountName = OC_ACCOUNT_NAME,
        driveAlias = "personal/admin",
        driveType = "personal",
        id = "8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568199",
        ownerId = "0aa0e03c-ec36-498c-bb9f-857315568199",
        lastModifiedDateTime = "2023-01-01T00:00:00.00000000Z",
        name = "Admin",
        quota = SpaceQuotaEntity(
            remaining = 999999995,
            state = "normal",
            total = 1000000000,
            used = 5,
        ),
        root = SpaceRootEntity(
            eTag = "989c7968dbbbde8c5fd9849b9123c384",
            id = "8871f4f3-fc6f-4a66-8bed-62f175f76f38\$0aa0e03c-ec36-498c-bb9f-857315568199",
            webDavUrl = "https://server.com/dav/spaces/8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568199",
            deleteState = null
        ),
        webUrl = "https://server.com/f/8871f4f3-fc6f-4a66-8bed-62f175f76f38$0aa0e03c-ec36-498c-bb9f-857315568199",
        description = null
    )


val SPACE_ENTITY_SHARE = SpacesWithSpecials(
    SpacesEntity(
        accountName = OC_ACCOUNT_NAME,
        driveAlias = "driveAlias",
        driveType = "driveType",
        id = SPACE_ID_SHARES,
        ownerId = OC_CLIENT_ID,
        lastModifiedDateTime = "lastModifiedDateTime",
        name = "name",
        quota = null,
        root = SpaceRootEntity(
            eTag = "eTag",
            id = "id",
            webDavUrl = WEB_DAV_URL,
            deleteState = "state"
        ),
        webUrl = "webUrl",
        description = "description"
    ),
    listOf(
        SpaceSpecialEntity(
            accountName = OC_ACCOUNT_NAME,
            eTag = "eTag",
            fileMimeType = "fileMimeType",
            id = OC_ACCOUNT_ID,
            spaceId = OC_SPACE_PERSONAL.id,
            lastModifiedDateTime = "lastModifiedDateTime",
            name = "name",
            webDavUrl = WEB_DAV_URL,
            size = 100,
            specialFolderName = OC_SPACE_SPECIAL_IMAGE.name
        )
    )
)
val SPACE_RESPONSE =
    SpaceResponse(
        driveAlias = "driveAlias",
        driveType = "driveType",
        id = OC_ACCOUNT_ID,
        lastModifiedDateTime = "lastModifiedDateTime",
        name = "name",
        webUrl = "webUrl",
        description = "description",
        owner = null,
        root = RootResponse(
            eTag = "eTag",
            id = OC_ACCOUNT_ID,
            webDavUrl = "https://server.url/dav/spaces/8871f4f3-fc6f-4a66-8bed-62f175f76f3805bca744-d89f-4e9c-a990-25a0d7f03fe9",
            deleted = null
        ),
        quota = QuotaResponse(
            remaining = 1,
            state = "state",
            total = 10,
            used = 1
        ),
        special = null,
        )
