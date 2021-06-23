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
import com.owncloud.android.datamodel.CameraUploadsSyncStorageManager
import com.owncloud.android.datamodel.OCCameraUploadSync
import com.owncloud.android.datamodel.OCUpload
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.domain.camerauploads.usecases.GetCameraUploadsConfigurationUseCase
import com.owncloud.android.operations.UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_PICTURE
import com.owncloud.android.operations.UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_VIDEO
import com.owncloud.android.usecases.UploadFileFromContentUriUseCase
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.NotificationUtils
import com.owncloud.android.utils.UPLOAD_NOTIFICATION_CHANNEL_ID
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CameraUploadsWorker(
    val appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
), KoinComponent {

    enum class SyncType(val prefixForType: String) { PICTURE_UPLOADS("image/"), VIDEO_UPLOADS("video/") }

    private val getCameraUploadsConfigurationUseCase: GetCameraUploadsConfigurationUseCase by inject()

    override suspend fun doWork(): Result {

        when (val useCaseResult = getCameraUploadsConfigurationUseCase.execute(Unit)) {
            is UseCaseResult.Success -> {
                syncFolder(useCaseResult.data?.pictureUploadsConfiguration)
                syncFolder(useCaseResult.data?.videoUploadsConfiguration)
                updateTimestamp()
            }
            is UseCaseResult.Error -> {
                Timber.e(useCaseResult.throwable, "Worker ${useCaseResult.throwable}")
            }
        }
        return Result.success()
    }

    private fun syncFolder(folderBackUpConfiguration: FolderBackUpConfiguration?) {
        if (folderBackUpConfiguration == null) return

        val syncType = when (folderBackUpConfiguration) {
            is FolderBackUpConfiguration.PictureUploadsConfiguration -> SyncType.PICTURE_UPLOADS
            is FolderBackUpConfiguration.VideoUploadsConfiguration -> SyncType.VIDEO_UPLOADS
        }

        val localPicturesDocumentFiles: List<DocumentFile> = getFilesReadyToUpload(
            syncType = syncType,
            sourcePath = folderBackUpConfiguration.sourcePath
        )

        val contentText = when (syncType) {
            SyncType.PICTURE_UPLOADS -> R.string.uploader_upload_picture_upload_files
            SyncType.VIDEO_UPLOADS -> R.string.uploader_upload_video_upload_files
        }

        NotificationUtils.createBasicNotification(
            context = appContext,
            contentTitle = appContext.getString(R.string.uploader_upload_camera_upload_files),
            contentText = appContext.getString(contentText, localPicturesDocumentFiles.size),
            notificationChannelId = UPLOAD_NOTIFICATION_CHANNEL_ID,
            notificationId = System.currentTimeMillis().toInt(),
            intent = NotificationUtils.composePendingIntentToUploadList(appContext),
            onGoing = false,
            timeOut = 5_000
        )

        for (documentFile in localPicturesDocumentFiles) {
            val uploadId = storeInUploadsDatabase(
                documentFile = documentFile,
                uploadPath = folderBackUpConfiguration.uploadPath.plus(File.separator).plus(documentFile.name),
                accountName = folderBackUpConfiguration.accountName,
                behavior = folderBackUpConfiguration.behavior,
                createdByWorker = when (folderBackUpConfiguration) {
                    is FolderBackUpConfiguration.PictureUploadsConfiguration -> CREATED_AS_CAMERA_UPLOAD_PICTURE
                    is FolderBackUpConfiguration.VideoUploadsConfiguration -> CREATED_AS_CAMERA_UPLOAD_VIDEO
                }
            )
            enqueueSingleUpload(
                contentUri = documentFile.uri,
                uploadPath = folderBackUpConfiguration.uploadPath.plus(File.separator).plus(documentFile.name),
                lastModified = documentFile.lastModified(),
                behavior = folderBackUpConfiguration.behavior.toString(),
                accountName = folderBackUpConfiguration.accountName,
                uploadId = uploadId,
                wifiOnly = folderBackUpConfiguration.wifiOnly
            )
        }
    }

    private fun updateTimestamp() {
        val currentTimestamp = System.currentTimeMillis()
        val cameraUploadsSyncStorageManager = CameraUploadsSyncStorageManager(appContext.contentResolver)
        val cameraUploadSync = cameraUploadsSyncStorageManager.getCameraUploadSync(
            null,
            null,
            null
        )
        cameraUploadsSyncStorageManager.updateCameraUploadSync(
            OCCameraUploadSync(currentTimestamp, currentTimestamp).apply { id = cameraUploadSync.id })
    }

    private fun getFilesReadyToUpload(
        syncType: SyncType,
        sourcePath: String
    ): List<DocumentFile> {
        val lastFolderSync: Long = getLastSyncTimestamp(syncType)
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

        val sourceUri: Uri = sourcePath.toUri()
        val documentTree = DocumentFile.fromTreeUri(applicationContext, sourceUri)
        val arrayOfLocalFiles = documentTree?.listFiles() ?: arrayOf()

        val filteredList: List<DocumentFile> = arrayOfLocalFiles
            .sortedBy { it.lastModified() }
            .filter { it.lastModified() > lastFolderSync }
            .filter { MimetypeIconUtil.getBestMimeTypeByFilename(it.name).startsWith(syncType.prefixForType) }

        Timber.i("Last sync ${syncType.name}: ${simpleDateFormat.format(Date(lastFolderSync))}")
        Timber.i("${arrayOfLocalFiles.size} files found in folder: ${sourceUri.path}")
        Timber.i("${filteredList.size} files are ${syncType.name} and were taken after last sync")

        return filteredList
    }

    // We could move this to preferences as we store the picture configuration there.
    private fun getLastSyncTimestamp(syncType: SyncType): Long {
        val cameraUploadsSyncStorageManager = CameraUploadsSyncStorageManager(appContext.contentResolver)
        val cameraUploadSync = cameraUploadsSyncStorageManager.getCameraUploadSync(
            null,
            null,
            null
        )
        val currentTimestamp = System.currentTimeMillis()

        return if (cameraUploadSync == null) {

            val firstOcCameraUploadSync = OCCameraUploadSync(currentTimestamp, currentTimestamp)
            cameraUploadsSyncStorageManager.storeCameraUploadSync(firstOcCameraUploadSync)

            Timber.d("Camera uploads initialization at $currentTimestamp")

            currentTimestamp
        } else {
            when (syncType) {
                SyncType.PICTURE_UPLOADS -> cameraUploadSync.picturesLastSync
                SyncType.VIDEO_UPLOADS -> cameraUploadSync.videosLastSync
            }
        }
    }

    private fun enqueueSingleUpload(
        contentUri: Uri,
        uploadPath: String,
        lastModified: Long,
        behavior: String,
        accountName: String,
        uploadId: Long,
        wifiOnly: Boolean
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
                wifiOnly = wifiOnly
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
            localAction = behavior.ordinal
            uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
        }
        return uploadStorageManager.storeUpload(ocUpload)
    }

    companion object {
        const val CAMERA_UPLOADS_WORKER = "CAMERA_UPLOADS_WORKER"
        const val repeatInterval: Long = 15L
        val repeatIntervalTimeUnit: TimeUnit = TimeUnit.MINUTES
    }
}
