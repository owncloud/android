/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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
package com.owncloud.android.data.folderbackup.datasources.implementation

import androidx.annotation.VisibleForTesting
import com.owncloud.android.data.folderbackup.datasources.LocalFolderBackupDataSource
import com.owncloud.android.data.folderbackup.db.FolderBackUpEntity
import com.owncloud.android.data.folderbackup.db.FolderBackupDao
import com.owncloud.android.domain.camerauploads.model.CameraUploadsConfiguration
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration.Companion.pictureUploadsName
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration.Companion.videoUploadsName
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OCLocalFolderBackupDataSource(
    private val folderBackupDao: FolderBackupDao,
) : LocalFolderBackupDataSource {

    override fun getCameraUploadsConfiguration(): CameraUploadsConfiguration? {
        val pictureUploadsConfiguration = folderBackupDao.getFolderBackUpConfigurationByName(pictureUploadsName)
        val videoUploadsConfiguration = folderBackupDao.getFolderBackUpConfigurationByName(videoUploadsName)

        if (pictureUploadsConfiguration == null && videoUploadsConfiguration == null) return null

        return CameraUploadsConfiguration(
            pictureUploadsConfiguration = pictureUploadsConfiguration?.toModel(),
            videoUploadsConfiguration = videoUploadsConfiguration?.toModel(),
        )
    }

    override fun getFolderBackupConfigurationByNameAsFlow(name: String): Flow<FolderBackUpConfiguration?> =
        folderBackupDao.getFolderBackUpConfigurationByNameAsFlow(name = name).map { it?.toModel() }

    override fun saveFolderBackupConfiguration(folderBackUpConfiguration: FolderBackUpConfiguration) {
        folderBackupDao.update(folderBackUpConfiguration.toEntity())
    }

    override fun resetFolderBackupConfigurationByName(name: String) {
        folderBackupDao.delete(name)
    }

    /**************************************************************************************************************
     ************************************************* Mappers ****************************************************
     **************************************************************************************************************/

    companion object {
        @VisibleForTesting
        fun FolderBackUpEntity.toModel() =
            FolderBackUpConfiguration(
                accountName = accountName,
                behavior = UploadBehavior.fromString(behavior),
                sourcePath = sourcePath,
                uploadPath = uploadPath,
                wifiOnly = wifiOnly,
                chargingOnly = chargingOnly,
                lastSyncTimestamp = lastSyncTimestamp,
                name = name,
                spaceId = spaceId,
            )
    }


    private fun FolderBackUpConfiguration.toEntity(): FolderBackUpEntity =
        FolderBackUpEntity(
            accountName = accountName,
            behavior = behavior.toString(),
            sourcePath = sourcePath,
            uploadPath = uploadPath,
            wifiOnly = wifiOnly,
            chargingOnly = chargingOnly,
            name = name,
            lastSyncTimestamp = lastSyncTimestamp,
            spaceId = spaceId,
        )
}
