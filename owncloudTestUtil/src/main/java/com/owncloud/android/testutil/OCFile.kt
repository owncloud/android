/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

import com.owncloud.android.domain.files.model.OCFile

val OC_FOLDER = OCFile(
    id = 122,
    parentId = 123,
    remotePath = "/Photos",
    owner = OC_ACCOUNT_NAME,
    permissions = "RDNVCK",
    remoteId = "00000003oci9p7er2hay",
    privateLink = "http://server.url/f/3",
    creationTimestamp = 0,
    modifiedTimestamp = 1593510589000,
    etag = "5efb0c13c688f",
    mimeType = "DIR",
    length = 123123123
)

val OC_FILE = OCFile(
    id = 124,
    parentId = 122,
    remotePath = "/Photos/image.jpt",
    owner = OC_ACCOUNT_NAME,
    permissions = "RDNVCK",
    remoteId = "00000003oci9p7er2how",
    privateLink = "http://server.url/f/4",
    creationTimestamp = 0,
    modifiedTimestamp = 1593510589000,
    etag = "5efb0c13c688f",
    mimeType = "image/jpeg",
    length = 3000000
)