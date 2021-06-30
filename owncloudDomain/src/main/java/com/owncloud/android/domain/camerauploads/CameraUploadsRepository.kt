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
package com.owncloud.android.domain.camerauploads

import com.owncloud.android.domain.camerauploads.model.CameraUploadsConfiguration
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import kotlinx.coroutines.flow.Flow

interface CameraUploadsRepository {
    fun getCameraUploadsConfiguration(): CameraUploadsConfiguration?
    fun getPictureUploadsConfigurationStream(): Flow<FolderBackUpConfiguration.PictureUploadsConfiguration?>
    fun getVideoUploadsConfigurationStream(): Flow<FolderBackUpConfiguration.VideoUploadsConfiguration?>

    fun savePictureUploadConfiguration(pictureUploadsConfiguration: FolderBackUpConfiguration.PictureUploadsConfiguration)
    fun saveVideoUploadConfiguration(videoUploadsConfiguration: FolderBackUpConfiguration.VideoUploadsConfiguration)

    fun resetPictureUpload()
    fun resetVideoUpload()
}
