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
package com.owncloud.android.data.camerauploads.datasources.implementation

import com.owncloud.android.data.LocalStorageProvider
import com.owncloud.android.data.camerauploads.datasources.CameraUploadsLocalDataSource
import com.owncloud.android.data.camerauploads.db.CameraUploadsDao
import com.owncloud.android.data.camerauploads.db.FolderBackUpEntity
import com.owncloud.android.data.camerauploads.db.FolderBackUpEntity.Companion.pictureUploadsName
import com.owncloud.android.data.camerauploads.db.FolderBackUpEntity.Companion.videoUploadsName
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.domain.camerauploads.model.CameraUploadsConfiguration
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration.PictureUploadsConfiguration
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration.VideoUploadsConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class CameraUploadsLocalDataSourceImpl(
    private val sharedPreferencesProvider: SharedPreferencesProvider,
    private val localStorageProvider: LocalStorageProvider,
    private val cameraUploadsDao: CameraUploadsDao,
) : CameraUploadsLocalDataSource {

    override fun getCameraUploadsConfiguration(): CameraUploadsConfiguration? {
        val pictureUploadsConfiguration = getPictureUploadsConfiguration()
        val videoUploadsConfiguration = getVideoUploadsConfiguration()

        if (pictureUploadsConfiguration == null && videoUploadsConfiguration == null) return null

        return CameraUploadsConfiguration(
            pictureUploadsConfiguration = getPictureUploadsConfiguration(),
            videoUploadsConfiguration = getVideoUploadsConfiguration()
        )
    }

    override fun getPictureUploadsConfigurationStream(): Flow<PictureUploadsConfiguration?> =
        cameraUploadsDao.getFolderBackUpConfigurationByNameStream(name = pictureUploadsName).map { it?.toModel() as? PictureUploadsConfiguration }

    override fun getVideoUploadsConfigurationStream(): Flow<VideoUploadsConfiguration?> =
        cameraUploadsDao.getFolderBackUpConfigurationByNameStream(name = videoUploadsName).map { it?.toModel() as? VideoUploadsConfiguration }

    override fun savePictureUploadsConfiguration(pictureUploadsConfiguration: PictureUploadsConfiguration) {
        cameraUploadsDao.update(pictureUploadsConfiguration.toEntity())
    }

    override fun saveVideoUploadsConfiguration(videoUploadsConfiguration: VideoUploadsConfiguration) {
        cameraUploadsDao.insert(videoUploadsConfiguration.toEntity())
    }

    override fun resetPictureUploads() {
        cameraUploadsDao.delete(pictureUploadsName)
    }

    private fun getPictureUploadsConfiguration(): PictureUploadsConfiguration? =
        cameraUploadsDao.getFolderBackUpConfigurationByName(name = pictureUploadsName)
            ?.toModel() as? PictureUploadsConfiguration

    private fun getVideoUploadsConfiguration(): VideoUploadsConfiguration? =
        cameraUploadsDao.getFolderBackUpConfigurationByName(name = videoUploadsName)
            ?.toModel() as? VideoUploadsConfiguration

    @Deprecated("Use dao instead of preferences to retrieve the configuration")
    fun getPictureUploadsConfigurationPreferences(): PictureUploadsConfiguration? {

        if (!sharedPreferencesProvider.getBoolean(PREF__CAMERA_PICTURE_UPLOADS_ENABLED, false)) return null

        return PictureUploadsConfiguration(
            accountName = sharedPreferencesProvider.getString(PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME, null) ?: "",
            wifiOnly = sharedPreferencesProvider.getBoolean(PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY, false),
            uploadPath = getUploadPathForPreference(PREF__CAMERA_PICTURE_UPLOADS_PATH),
            sourcePath = getSourcePathForPreference(PREF__CAMERA_PICTURE_UPLOADS_SOURCE),
            behavior = getBehaviorForPreference(PREF__CAMERA_PICTURE_UPLOADS_BEHAVIOUR),
            lastSyncTimestamp = 0
        )
    }

    @Deprecated("Use dao instead of preferences to retrieve the configuration")
    fun getVideoUploadsConfigurationPreferences(): VideoUploadsConfiguration? {
        if (!sharedPreferencesProvider.getBoolean(PREF__CAMERA_VIDEO_UPLOADS_ENABLED, false)) return null

        return VideoUploadsConfiguration(
            accountName = sharedPreferencesProvider.getString(PREF__CAMERA_VIDEO_UPLOADS_ACCOUNT_NAME, null) ?: "",
            wifiOnly = sharedPreferencesProvider.getBoolean(PREF__CAMERA_VIDEO_UPLOADS_WIFI_ONLY, false),
            uploadPath = getUploadPathForPreference(PREF__CAMERA_VIDEO_UPLOADS_PATH),
            sourcePath = getSourcePathForPreference(PREF__CAMERA_VIDEO_UPLOADS_SOURCE),
            behavior = getBehaviorForPreference(PREF__CAMERA_VIDEO_UPLOADS_BEHAVIOUR),
            lastSyncTimestamp = 0
        )
    }

    private fun getUploadPathForPreference(keyPreference: String): String {
        val uploadPath = sharedPreferencesProvider.getString(
            key = keyPreference,
            defaultValue = DEFAULT_PATH_FOR_CAMERA_UPLOADS + File.separator
        )
        return if (uploadPath!!.endsWith(File.separator)) uploadPath else uploadPath + File.separator
    }

    private fun getSourcePathForPreference(keyPreference: String): String {
        return sharedPreferencesProvider.getString(keyPreference, null) ?: localStorageProvider.getDefaultCameraSourcePath()
    }

    private fun getBehaviorForPreference(keyPreference: String): FolderBackUpConfiguration.Behavior {
        val storedBehaviour = sharedPreferencesProvider.getString(keyPreference, null) ?: return FolderBackUpConfiguration.Behavior.COPY

        return FolderBackUpConfiguration.Behavior.fromString(storedBehaviour)
    }

    /**************************************************************************************************************
     ************************************************* Mappers ****************************************************
     **************************************************************************************************************/
    private fun FolderBackUpEntity.toModel() = when (name) {
        pictureUploadsName -> PictureUploadsConfiguration(
            accountName = accountName,
            behavior = FolderBackUpConfiguration.Behavior.fromString(behavior),
            sourcePath = sourcePath,
            uploadPath = uploadPath,
            wifiOnly = wifiOnly,
            lastSyncTimestamp = lastSyncTimestamp,
        )
        videoUploadsName -> VideoUploadsConfiguration(
            accountName = accountName,
            behavior = FolderBackUpConfiguration.Behavior.fromString(behavior),
            sourcePath = sourcePath,
            uploadPath = uploadPath,
            wifiOnly = wifiOnly,
            lastSyncTimestamp = lastSyncTimestamp,
        )
        else -> null
    }

    private fun FolderBackUpConfiguration.toEntity(): FolderBackUpEntity {
        val name = when (this) {
            is PictureUploadsConfiguration -> pictureUploadsName
            is VideoUploadsConfiguration -> videoUploadsName
        }
        return FolderBackUpEntity(accountName, behavior.toString(), sourcePath, uploadPath, wifiOnly, name, lastSyncTimestamp)
    }

    companion object {
        private const val PREF__CAMERA_PICTURE_UPLOADS_ENABLED = "enable_picture_uploads"
        private const val PREF__CAMERA_VIDEO_UPLOADS_ENABLED = "enable_video_uploads"
        private const val PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY = "picture_uploads_on_wifi"
        private const val PREF__CAMERA_VIDEO_UPLOADS_WIFI_ONLY = "video_uploads_on_wifi"
        private const val PREF__CAMERA_PICTURE_UPLOADS_PATH = "picture_uploads_path"
        private const val PREF__CAMERA_VIDEO_UPLOADS_PATH = "video_uploads_path"
        private const val PREF__CAMERA_PICTURE_UPLOADS_BEHAVIOUR = "picture_uploads_behaviour"
        private const val PREF__CAMERA_PICTURE_UPLOADS_SOURCE = "picture_uploads_source_path"
        private const val PREF__CAMERA_VIDEO_UPLOADS_BEHAVIOUR = "video_uploads_behaviour"
        private const val PREF__CAMERA_VIDEO_UPLOADS_SOURCE = "video_uploads_source_path"
        private const val PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME = "picture_uploads_account_name"
        private const val PREF__CAMERA_VIDEO_UPLOADS_ACCOUNT_NAME = "video_uploads_account_name"

        private const val DEFAULT_PATH_FOR_CAMERA_UPLOADS = "/CameraUpload"
    }
}
