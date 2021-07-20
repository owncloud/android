/**
 *   ownCloud Android client application
 *
 *   @author Abel García de Prada
 *   Copyright (C) 2020 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FOLDER_BACKUP_TABLE_NAME
import com.owncloud.android.data.preferences.datasources.SharedPreferencesProvider
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration.Companion.pictureUploadsName
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration.Companion.videoUploadsName
import java.io.File

val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `$FOLDER_BACKUP_TABLE_NAME` (`accountName` TEXT NOT NULL, `behavior` TEXT NOT NULL, `sourcePath` TEXT NOT NULL, `uploadPath` TEXT NOT NULL, `wifiOnly` INTEGER NOT NULL, `name` TEXT NOT NULL, `lastSyncTimestamp` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
    }
}

@Deprecated("Legacy code. Only used to migrate old camera uploads configuration from ")
class CameraUploadsMigrationToRoom(val sharedPreferencesProvider: SharedPreferencesProvider) {

    fun getPictureUploadsConfigurationPreferences(timestamp: Long): FolderBackUpConfiguration? {

        if (!sharedPreferencesProvider.getBoolean(PREF__CAMERA_PICTURE_UPLOADS_ENABLED, false)) return null

        return FolderBackUpConfiguration(
            accountName = sharedPreferencesProvider.getString(PREF__CAMERA_PICTURE_UPLOADS_ACCOUNT_NAME, null) ?: "",
            wifiOnly = sharedPreferencesProvider.getBoolean(PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY, false),
            uploadPath = getUploadPathForPreference(PREF__CAMERA_PICTURE_UPLOADS_PATH),
            sourcePath = getSourcePathForPreference(PREF__CAMERA_PICTURE_UPLOADS_SOURCE),
            behavior = getBehaviorForPreference(PREF__CAMERA_PICTURE_UPLOADS_BEHAVIOUR),
            lastSyncTimestamp = timestamp,
            name = pictureUploadsName,
        )
    }

    fun getVideoUploadsConfigurationPreferences(timestamp: Long): FolderBackUpConfiguration? {
        if (!sharedPreferencesProvider.getBoolean(PREF__CAMERA_VIDEO_UPLOADS_ENABLED, false)) return null

        return FolderBackUpConfiguration(
            accountName = sharedPreferencesProvider.getString(PREF__CAMERA_VIDEO_UPLOADS_ACCOUNT_NAME, null) ?: "",
            wifiOnly = sharedPreferencesProvider.getBoolean(PREF__CAMERA_VIDEO_UPLOADS_WIFI_ONLY, false),
            uploadPath = getUploadPathForPreference(PREF__CAMERA_VIDEO_UPLOADS_PATH),
            sourcePath = getSourcePathForPreference(PREF__CAMERA_VIDEO_UPLOADS_SOURCE),
            behavior = getBehaviorForPreference(PREF__CAMERA_VIDEO_UPLOADS_BEHAVIOUR),
            lastSyncTimestamp = timestamp,
            name = videoUploadsName,
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
        return sharedPreferencesProvider.getString(keyPreference, null) ?: ""
    }

    private fun getBehaviorForPreference(keyPreference: String): FolderBackUpConfiguration.Behavior {
        val storedBehaviour = sharedPreferencesProvider.getString(keyPreference, null) ?: return FolderBackUpConfiguration.Behavior.COPY

        return FolderBackUpConfiguration.Behavior.fromString(storedBehaviour)
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
