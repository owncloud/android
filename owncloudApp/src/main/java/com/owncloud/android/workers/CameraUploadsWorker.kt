package com.owncloud.android.workers

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.owncloud.android.datamodel.CameraUploadsSyncStorageManager
import com.owncloud.android.datamodel.OCCameraUploadSync
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.domain.camerauploads.usecases.GetCameraUploadsConfigurationUseCase
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.workers.UploadFileFromContentUriWorker.Companion.TRANSFER_TAG_CAMERA_UPLOAD
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

    private val getCameraUploadsConfigurationUseCase: GetCameraUploadsConfigurationUseCase by inject()

    override suspend fun doWork(): Result {

        when (val useCaseResult = getCameraUploadsConfigurationUseCase.execute(Unit)) {
            is UseCaseResult.Success -> {
                syncCameraPictures(useCaseResult.data?.pictureUploadsConfiguration)
                syncCameraVideos(useCaseResult.data?.videoUploadsConfiguration)
                updateTimestamp()
            }
            is UseCaseResult.Error -> {
                Timber.e(useCaseResult.throwable, "Worker ${useCaseResult.throwable}")
            }
        }
        return Result.success()
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

    private fun getImagesReadyToUpload(pictureUploadsConfiguration: FolderBackUpConfiguration.PictureUploadsConfiguration): List<DocumentFile> {
        val lastFolderSync: Long = getLastSyncTimestamp(pictures = true)
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

        val cameraPictureSourceUri: Uri = pictureUploadsConfiguration.sourcePath.toUri()
        val documentTree = DocumentFile.fromTreeUri(applicationContext, cameraPictureSourceUri)
        val arrayOfLocalFiles = documentTree?.listFiles() ?: arrayOf()

        val filteredList: List<DocumentFile> = arrayOfLocalFiles
            .sortedBy { it.lastModified() }
            .filter { it.lastModified() > lastFolderSync }
            .filter { MimetypeIconUtil.getBestMimeTypeByFilename(it.name).startsWith("image/") }

        Timber.i("Last sync picture uploads: ${simpleDateFormat.format(Date(lastFolderSync))}")
        Timber.i("${arrayOfLocalFiles.size} files found in folder: ${cameraPictureSourceUri.path}")
        Timber.i("${filteredList.size} files are images and were taken after last sync")

        return filteredList
    }

    private fun getVideosReadyToUpload(videoUploadsConfiguration: FolderBackUpConfiguration.VideoUploadsConfiguration): List<DocumentFile> {
        val lastFolderSync: Long = getLastSyncTimestamp(pictures = false)
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

        val cameraVideoSourceUri: Uri = videoUploadsConfiguration.sourcePath.toUri()
        val documentTree = DocumentFile.fromTreeUri(applicationContext, cameraVideoSourceUri)
        val arrayOfLocalFiles = documentTree?.listFiles() ?: arrayOf()

        val filteredList: List<DocumentFile> = arrayOfLocalFiles
            .sortedBy { it.lastModified() }
            .filter { it.lastModified() > lastFolderSync }
            .filter { MimetypeIconUtil.getBestMimeTypeByFilename(it.name).startsWith("video/") }

        Timber.i("Last sync video uploads: ${simpleDateFormat.format(Date(lastFolderSync))}")
        Timber.i("${arrayOfLocalFiles.size} files found in folder: ${cameraVideoSourceUri.path}")
        Timber.i("${filteredList.size} files are videos and were taken after last sync")

        return filteredList
    }

    private fun syncCameraPictures(
        pictureUploadsConfiguration: FolderBackUpConfiguration.PictureUploadsConfiguration?
    ) {
        if (pictureUploadsConfiguration == null) return

        val localPicturesDocumentFiles: List<DocumentFile> = getImagesReadyToUpload(pictureUploadsConfiguration)

        if (localPicturesDocumentFiles.isNotEmpty()) {
            for (documentFile in localPicturesDocumentFiles) {
                enqueueSingleUpload(
                    contentUri = documentFile.uri,
                    uploadPath = pictureUploadsConfiguration.uploadPath.plus(File.separator).plus(documentFile.name),
                    lastModified = documentFile.lastModified(),
                    behavior = pictureUploadsConfiguration.behavior.toString(),
                    accountName = pictureUploadsConfiguration.accountName
                )
            }
        }
    }

    // We could move this to preferences as we store the picture configuration there.
    private fun getLastSyncTimestamp(pictures: Boolean): Long {
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
            if (pictures) cameraUploadSync.picturesLastSync else cameraUploadSync.videosLastSync
        }
    }

    private fun syncCameraVideos(
        videoUploadsConfiguration: FolderBackUpConfiguration.VideoUploadsConfiguration?
    ) {
        if (videoUploadsConfiguration == null) return

        val localVideosDocumentFiles: List<DocumentFile> = getVideosReadyToUpload(videoUploadsConfiguration)

        if (localVideosDocumentFiles.isNotEmpty()) {
            for (documentFile in localVideosDocumentFiles) {
                enqueueSingleUpload(
                    contentUri = documentFile.uri,
                    uploadPath = videoUploadsConfiguration.uploadPath.plus(File.separator).plus(documentFile.name),
                    lastModified = documentFile.lastModified(),
                    behavior = videoUploadsConfiguration.behavior.toString(),
                    accountName = videoUploadsConfiguration.accountName
                )
            }
        }
    }

    private fun enqueueSingleUpload(
        contentUri: Uri,
        uploadPath: String,
        lastModified: Long,
        behavior: String,
        accountName: String
    ) {
        val lastModifiedInSeconds = (lastModified / 1000L).toString()

        val inputData = workDataOf(
            UploadFileFromContentUriWorker.KEY_PARAM_ACCOUNT_NAME to accountName,
            UploadFileFromContentUriWorker.KEY_PARAM_BEHAVIOR to behavior,
            UploadFileFromContentUriWorker.KEY_PARAM_CONTENT_URI to contentUri.toString(),
            UploadFileFromContentUriWorker.KEY_PARAM_LAST_MODIFIED to lastModifiedInSeconds,
            UploadFileFromContentUriWorker.KEY_PARAM_UPLOAD_PATH to uploadPath,
        )

        val uploadFileFromContentUriWorker = OneTimeWorkRequestBuilder<UploadFileFromContentUriWorker>()
            .setInputData(inputData)
            .addTag(accountName)
            .addTag(TRANSFER_TAG_CAMERA_UPLOAD)
            .build()

        WorkManager.getInstance(appContext).enqueue(uploadFileFromContentUriWorker)
        Timber.i("Upload of ${contentUri.path} has been enqueued.")
    }

    companion object {
        const val CAMERA_UPLOADS_WORKER = "CAMERA_UPLOADS_WORKER"
        const val repeatInterval: Long = 15L
        val repeatIntervalTimeUnit: TimeUnit = TimeUnit.MINUTES
    }
}
