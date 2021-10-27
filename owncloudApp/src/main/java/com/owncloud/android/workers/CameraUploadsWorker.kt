/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.workers

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCUpload
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.domain.camerauploads.usecases.GetCameraUploadsConfigurationUseCase
import com.owncloud.android.domain.camerauploads.usecases.SavePictureUploadsConfigurationUseCase
import com.owncloud.android.domain.camerauploads.usecases.SaveVideoUploadsConfigurationUseCase
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.operations.UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_PICTURE
import com.owncloud.android.operations.UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_VIDEO
import com.owncloud.android.presentation.ui.settings.SettingsActivity
import com.owncloud.android.usecases.UploadFileFromContentUriUseCase
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.NotificationUtils
import com.owncloud.android.utils.UPLOAD_NOTIFICATION_CHANNEL_ID
import org.koin.core.KoinComponent
import org.koin.core.inject

import timber.log.Timber
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

class CameraUploadsWorker(
    val appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
), KoinComponent {

    enum class SyncType(val prefixForType: String) {
        PICTURE_UPLOADS("image/"), VIDEO_UPLOADS("video/");

        fun getNotificationId(): Int =
            when (this) {
                PICTURE_UPLOADS -> pictureUploadsNotificationId
                VIDEO_UPLOADS -> videoUploadsNotificationId
            }
    }

    private val getCameraUploadsConfigurationUseCase: GetCameraUploadsConfigurationUseCase by inject()

    override suspend fun doWork(): Result {

        when (val useCaseResult = getCameraUploadsConfigurationUseCase.execute(Unit)) {
            is UseCaseResult.Success -> {
                val cameraUploadsConfiguration = useCaseResult.data
                if (cameraUploadsConfiguration == null || cameraUploadsConfiguration.areCameraUploadsDisabled()) {
                    cancelWorker()
                    return Result.success()
                }
                cameraUploadsConfiguration.pictureUploadsConfiguration?.let { pictureUploadsConfiguration ->
                    try {
                        checkSourcePathIsAValidUriOrThrowException(pictureUploadsConfiguration.sourcePath)
                        syncFolder(pictureUploadsConfiguration)
                    } catch (illegalArgumentException: IllegalArgumentException) {
                        showNotificationToUpdateUri(SyncType.PICTURE_UPLOADS)
                        return Result.failure()
                    }
                }
                cameraUploadsConfiguration.videoUploadsConfiguration?.let { videoUploadsConfiguration ->
                    try {
                        checkSourcePathIsAValidUriOrThrowException(videoUploadsConfiguration.sourcePath)
                        syncFolder(videoUploadsConfiguration)
                    } catch (illegalArgumentException: IllegalArgumentException) {
                        showNotificationToUpdateUri(SyncType.VIDEO_UPLOADS)
                        return Result.failure()
                    }
                }
            }
            is UseCaseResult.Error -> {
                Timber.e(useCaseResult.throwable, "Worker ${useCaseResult.throwable}")
            }
        }
        return Result.success()
    }

    @Throws(IllegalArgumentException::class)
    private fun checkSourcePathIsAValidUriOrThrowException(sourcePath: String) {
        val sourceUri: Uri = sourcePath.toUri()
        DocumentFile.fromTreeUri(applicationContext, sourceUri)
    }

    private fun cancelWorker() {
        WorkManager.getInstance(appContext).cancelUniqueWork(CAMERA_UPLOADS_WORKER)
    }

    private fun syncFolder(folderBackUpConfiguration: FolderBackUpConfiguration?) {
        if (folderBackUpConfiguration == null) return

        val syncType = when {
            folderBackUpConfiguration.isPictureUploads -> SyncType.PICTURE_UPLOADS
            folderBackUpConfiguration.isVideoUploads -> SyncType.VIDEO_UPLOADS
            // Else should not happen for the moment. Maybe in upcoming features..
            else -> SyncType.PICTURE_UPLOADS
        }

        val currentTimestamp = System.currentTimeMillis()

        val localPicturesDocumentFiles: List<DocumentFile> = getFilesReadyToUpload(
            syncType = syncType,
            sourcePath = folderBackUpConfiguration.sourcePath,
            lastSyncTimestamp = folderBackUpConfiguration.lastSyncTimestamp,
            currentTimestamp = currentTimestamp,
        )

        showNotification(syncType, localPicturesDocumentFiles.size)

        for (documentFile in localPicturesDocumentFiles) {
            val uploadId = storeInUploadsDatabase(
                documentFile = documentFile,
                uploadPath = folderBackUpConfiguration.uploadPath.plus(File.separator).plus(documentFile.name),
                accountName = folderBackUpConfiguration.accountName,
                behavior = folderBackUpConfiguration.behavior,
                createdByWorker = when (syncType) {
                    SyncType.PICTURE_UPLOADS -> CREATED_AS_CAMERA_UPLOAD_PICTURE
                    SyncType.VIDEO_UPLOADS -> CREATED_AS_CAMERA_UPLOAD_VIDEO
                }
            )
            enqueueSingleUpload(
                contentUri = documentFile.uri,
                uploadPath = folderBackUpConfiguration.uploadPath.plus(File.separator).plus(documentFile.name),
                lastModified = documentFile.lastModified(),
                behavior = folderBackUpConfiguration.behavior.toString(),
                accountName = folderBackUpConfiguration.accountName,
                uploadId = uploadId,
                wifiOnly = folderBackUpConfiguration.wifiOnly,
                chargingOnly = folderBackUpConfiguration.chargingOnly
            )
        }
        updateTimestamp(folderBackUpConfiguration, syncType, currentTimestamp)
    }

    private fun showNotification(
        syncType: SyncType,
        numberOfFilesToUpload: Int
    ) {
        if (numberOfFilesToUpload == 0) return

        val contentText = when (syncType) {
            SyncType.PICTURE_UPLOADS -> R.string.uploader_upload_picture_upload_files
            SyncType.VIDEO_UPLOADS -> R.string.uploader_upload_video_upload_files
        }

        NotificationUtils.createBasicNotification(
            context = appContext,
            contentTitle = appContext.getString(R.string.uploader_upload_camera_upload_files),
            contentText = appContext.getString(contentText, numberOfFilesToUpload),
            notificationChannelId = UPLOAD_NOTIFICATION_CHANNEL_ID,
            notificationId = syncType.getNotificationId(),
            intent = NotificationUtils.composePendingIntentToUploadList(appContext),
            onGoing = false,
            timeOut = 5_000
        )
    }

    private fun showNotificationToUpdateUri(
        syncType: SyncType
    ) {
        val contentText: Int = when (syncType) {
            SyncType.PICTURE_UPLOADS -> R.string.uploader_upload_picture_upload_error
            SyncType.VIDEO_UPLOADS -> R.string.uploader_upload_video_upload_error
        }
        val notificationKey: String = when (syncType) {
            SyncType.PICTURE_UPLOADS -> SettingsActivity.NOTIFICATION_INTENT_PICTURES
            SyncType.VIDEO_UPLOADS -> SettingsActivity.NOTIFICATION_INTENT_VIDEOS
        }
        NotificationUtils.createBasicNotification(
            context = appContext,
            contentTitle = appContext.getString(R.string.uploader_upload_camera_upload_source_path_error),
            contentText = appContext.getString(contentText),
            notificationChannelId = UPLOAD_NOTIFICATION_CHANNEL_ID,
            notificationId = syncType.getNotificationId(),
            intent = NotificationUtils.composePendingIntentToCameraUploads(appContext, notificationKey),
            onGoing = false,
            timeOut = null
        )
    }

    private fun updateTimestamp(
        folderBackUpConfiguration: FolderBackUpConfiguration,
        syncType: SyncType,
        currentTimestamp: Long,
    ) {

        when (syncType) {
            SyncType.PICTURE_UPLOADS -> {
                val savePictureUploadsConfigurationUseCase: SavePictureUploadsConfigurationUseCase by inject()
                savePictureUploadsConfigurationUseCase.execute(
                    SavePictureUploadsConfigurationUseCase.Params(folderBackUpConfiguration.copy(lastSyncTimestamp = currentTimestamp))
                )
            }
            SyncType.VIDEO_UPLOADS -> {
                val saveVideoUploadsConfigurationUseCase: SaveVideoUploadsConfigurationUseCase by inject()
                saveVideoUploadsConfigurationUseCase.execute(
                    SaveVideoUploadsConfigurationUseCase.Params(folderBackUpConfiguration.copy(lastSyncTimestamp = currentTimestamp))
                )
            }
        }
    }

    private fun getFilesReadyToUpload(
        syncType: SyncType,
        sourcePath: String,
        lastSyncTimestamp: Long,
        currentTimestamp: Long,
    ): List<DocumentFile> {
        val sourceUri: Uri = sourcePath.toUri()
        val documentTree = DocumentFile.fromTreeUri(applicationContext, sourceUri)
        val arrayOfLocalFiles = documentTree?.listFiles() ?: arrayOf()

        val filteredList: List<DocumentFile> = arrayOfLocalFiles
            .sortedBy { it.lastModified() }
            .filter { it.lastModified() >= lastSyncTimestamp }
            .filter { it.lastModified() < currentTimestamp }
            .filter { MimetypeIconUtil.getBestMimeTypeByFilename(it.name).startsWith(syncType.prefixForType) }

        Timber.i("Last sync ${syncType.name}: ${Date(lastSyncTimestamp)}")
        Timber.i("CurrentTimestamp ${Date(currentTimestamp)}")
        Timber.i("${arrayOfLocalFiles.size} files found in folder: ${sourceUri.path}")
        Timber.i("${filteredList.size} files are ${syncType.name} and were taken after last sync")

        return filteredList
    }

    private fun enqueueSingleUpload(
        contentUri: Uri,
        uploadPath: String,
        lastModified: Long,
        behavior: String,
        accountName: String,
        uploadId: Long,
        wifiOnly: Boolean,
        chargingOnly: Boolean
    ) {
        val lastModifiedInSeconds = (lastModified / 1000L).toString()

        UploadFileFromContentUriUseCase(WorkManager.getInstance(appContext)).execute(
            UploadFileFromContentUriUseCase.Params(
                accountName = accountName,
                contentUri = contentUri,
                lastModifiedInSeconds = lastModifiedInSeconds,
                behavior = behavior,
                uploadPath = uploadPath,
                uploadIdInStorageManager = uploadId,
                wifiOnly = wifiOnly,
                chargingOnly = chargingOnly
            )
        )
    }

    private fun storeInUploadsDatabase(
        documentFile: DocumentFile,
        uploadPath: String,
        accountName: String,
        behavior: FolderBackUpConfiguration.Behavior,
        createdByWorker: Int
    ): Long {
        val uploadStorageManager = UploadsStorageManager(appContext.contentResolver)

        val ocUpload = OCUpload(documentFile.uri.toString(), uploadPath, accountName).apply {
            fileSize = documentFile.length()
            isForceOverwrite = false
            createdBy = createdByWorker
            localAction = if (behavior == FolderBackUpConfiguration.Behavior.MOVE)
                FileUploader.LOCAL_BEHAVIOUR_MOVE
            else
                FileUploader.LOCAL_BEHAVIOUR_COPY
            uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
        }
        return uploadStorageManager.storeUpload(ocUpload)
    }

    companion object {
        const val CAMERA_UPLOADS_WORKER = "CAMERA_UPLOADS_WORKER"
        const val repeatInterval: Long = 15L
        val repeatIntervalTimeUnit: TimeUnit = TimeUnit.MINUTES
        private const val pictureUploadsNotificationId = 101
        private const val videoUploadsNotificationId = 102
    }
}
