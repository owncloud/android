/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

import com.owncloud.android.data.files.db.OCFileAndFileSync
import com.owncloud.android.data.files.db.OCFileEntity
import com.owncloud.android.data.files.db.OCFileSyncEntity
import com.owncloud.android.domain.availableoffline.model.AvailableOfflineStatus
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import com.owncloud.android.lib.resources.files.RemoteFile
import com.owncloud.android.lib.resources.files.RemoteMetaFile

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
    availableOfflineStatus = AvailableOfflineStatus.AVAILABLE_OFFLINE
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

val OC_FILE_WITH_SYNC_INFO_AVAILABLE_OFFLINE = OCFileWithSyncInfo(
    file = OC_FILE_AVAILABLE_OFFLINE,
    space = OC_SPACE_PROJECT_WITH_IMAGE
)

val OC_FILES_WITH_SYNC_INFO = listOf(OC_FILE_WITH_SYNC_INFO, OC_FILE_WITH_SYNC_INFO, OC_FILE_WITH_SYNC_INFO)
val OC_AVAILABLE_OFFLINE_FILES = listOf(OC_FILE_AVAILABLE_OFFLINE, OC_FILE_AVAILABLE_OFFLINE, OC_FILE_AVAILABLE_OFFLINE)
val OC_FILES_EMPTY = emptyList<OCFile>()
val OC_FILES_WITH_SYNC_INFO_EMPTY = emptyList<OCFileWithSyncInfo>()

val OC_FOLDER_ENTITY = OCFileEntity(
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
    length = 123123123,
    availableOfflineStatus = AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE.ordinal,
    fileIsDownloading = false,
    modifiedAtLastSyncForData = 0,
    lastSyncDateForData = 0,
    treeEtag = "",
    name = "Photos",
).apply { this.id = 122 }

val OC_FILE_ENTITY = OCFileEntity(
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
    availableOfflineStatus = AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE.ordinal,
    fileIsDownloading = false,
    modifiedAtLastSyncForData = 0,
    lastSyncDateForData = 0,
    treeEtag = "",
    name = "image.jpt",
).apply { this.id = 124 }

val OC_FILE_AVAILABLE_OFFLINE_ENTITY = OCFileEntity(
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
    availableOfflineStatus = AvailableOfflineStatus.AVAILABLE_OFFLINE.ordinal,
    fileIsDownloading = false,
    modifiedAtLastSyncForData = 0,
    lastSyncDateForData = 0,
    treeEtag = "",
    name = "image.jpt",
).apply { this.id = 124 }

val OC_FILE_SYNC_ENTITY = OCFileSyncEntity(
    fileId = OC_FILE.id!!,
    uploadWorkerUuid = null,
    downloadWorkerUuid = null,
    isSynchronizing = false,
)

val OC_FILE_AND_FILE_SYNC = OCFileAndFileSync(
    OC_FILE_ENTITY,
    OC_FILE_SYNC_ENTITY,
    SPACE_ENTITY_PERSONAL,
)

val REMOTE_FILE = RemoteFile(
    remotePath = OC_FILE.remotePath,
    mimeType = OC_FILE.mimeType,
    length = OC_FILE.length,
    creationTimestamp = OC_FILE.creationTimestamp!!,
    modifiedTimestamp = OC_FILE.modificationTimestamp,
    etag = OC_FILE.etag,
    permissions = OC_FILE.permissions,
    remoteId = OC_FILE.remoteId,
    privateLink = OC_FILE.privateLink,
    owner = OC_FILE.owner,
    sharedByLink = OC_FILE.sharedByLink,
    sharedWithSharee = OC_FILE.sharedWithSharee!!,
)

val REMOTE_META_FILE = RemoteMetaFile(
    metaPathForUser = OC_FILE.remotePath,
)
