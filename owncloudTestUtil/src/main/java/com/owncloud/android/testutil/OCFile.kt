/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo

val OC_FOLDER = OCFile(
    id = 122,
    parentId = 123,
    remotePath = "/Photos",
    owner = OC_ACCOUNT_NAME,
    permissions = "RDNVCK",
    remoteId = "00000003oci9p7er2hay",
    privateLink = "http://server.url/f/3",
    creationTimestamp = 0,
    modificationTimestamp = 1593510589000,
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
    creationTimestamp = 1593510589000,
    modificationTimestamp = 1593510589000,
    etag = "5efb0c13c688f",
    mimeType = "image/jpeg",
    length = 3000000,
    availableOfflineStatus = AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE,
)

val OC_FILE_AVAILABLE_OFFLINE = OCFile(
    id = 124,
    parentId = 122,
    remotePath = "/Photos/image.jpt",
    owner = OC_ACCOUNT_NAME,
    permissions = "RDNVCK",
    remoteId = "00000003oci9p7er2how",
    privateLink = "http://server.url/f/4",
    creationTimestamp = 1593510589000,
    modificationTimestamp = 1593510589000,
    etag = "5efb0c13c688f",
    mimeType = "image/jpeg",
    length = 3000000,
    availableOfflineStatus = AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT
)

val OC_FILE_WITH_SYNC_INFO = OCFileWithSyncInfo(
    file = OC_FILE,
    uploadWorkerUuid = null,
    downloadWorkerUuid = null,
    isSynchronizing = false,
)

val OC_FILE_WITH_SYNC_INFO_AND_SPACE = OCFileWithSyncInfo(
    file = OC_FILE,
    uploadWorkerUuid = null,
    downloadWorkerUuid = null,
    isSynchronizing = false,
    space = OC_SPACE_PERSONAL
)

val OC_FILE_WITH_SYNC_INFO_AND_WITHOUT_PERSONAL_SPACE = OCFileWithSyncInfo(
    file = OC_FILE,
    uploadWorkerUuid = null,
    downloadWorkerUuid = null,
    isSynchronizing = false,
    space = OC_SPACE_PROJECT_WITH_IMAGE
)

val OC_FILE_OC_AVAILABLE_OFFLINE_FILE = OCFileWithSyncInfo(
    file = OC_FILE_AVAILABLE_OFFLINE,
    uploadWorkerUuid = null,
    downloadWorkerUuid = null,
    isSynchronizing = false,
    space = OC_SPACE_PROJECT_WITH_IMAGE
)

val OC_AVAILABLE_OFFLINE_FILE = OCFile(
    id = 125,
    parentId = 122,
    remotePath = "/Photos/image.jpt",
    owner = OC_ACCOUNT_NAME,
    permissions = "RDNVCK",
    remoteId = "00000003oci9p7er2how",
    privateLink = "http://server.url/f/4",
    creationTimestamp = 0,
    modificationTimestamp = 1593510589000,
    etag = "5efb0c13c688f",
    mimeType = "image/jpeg",
    availableOfflineStatus = AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT,
    length = 3000000
)

val OC_FILES_WITH_SYNC_INFO = listOf(OC_FILE_WITH_SYNC_INFO, OC_FILE_WITH_SYNC_INFO, OC_FILE_WITH_SYNC_INFO)
val OC_AVAILABLE_OFFLINE_FILES = listOf(OC_AVAILABLE_OFFLINE_FILE, OC_AVAILABLE_OFFLINE_FILE, OC_AVAILABLE_OFFLINE_FILE)
val OC_FILES_EMPTY = emptyList<OCFile>()
val OC_FILES_WITH_SYNC_INFO_EMPTY = emptyList<OCFileWithSyncInfo>()
