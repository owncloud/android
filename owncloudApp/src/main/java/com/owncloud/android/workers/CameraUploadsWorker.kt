package com.owncloud.android.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.domain.camerauploads.usecases.GetCameraUploadsConfigurationUseCase
import com.owncloud.android.utils.MimetypeIconUtil
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class CameraUploadsWorker(
    appContext: Context,
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
            }
            is UseCaseResult.Error -> {
                Timber.e(useCaseResult.throwable, "Worker ${useCaseResult.throwable}")
            }
        }
        return Result.success()
    }

    private fun syncCameraPictures(
        pictureUploadsConfiguration: FolderBackUpConfiguration.PictureUploadsConfiguration?
    ) {
        if (pictureUploadsConfiguration == null) return

        val cameraPictureSourcePath: String = pictureUploadsConfiguration.sourcePath
        val localPictureFiles: MutableList<File> = mutableListOf()
        val cameraPicturesFolder = File(cameraPictureSourcePath)
        localPictureFiles.addAll(cameraPicturesFolder.listFiles() ?: arrayOf())
        localPictureFiles.sortBy { it.lastModified() }

        if (localPictureFiles.isNotEmpty()) {
            for (localFile in localPictureFiles) {
                val fileName = localFile.name
                val mimeType = MimetypeIconUtil.getBestMimeTypeByFilename(fileName)
                val isImage = mimeType.startsWith("image/")
                if (isImage) {
                    // TODO: Upload localFile
                    Timber.d("Upload ${localFile.name}")
                }
            }
        }
    }

    private fun syncCameraVideos(
        videoUploadsConfiguration: FolderBackUpConfiguration.VideoUploadsConfiguration?
    ) {
        if (videoUploadsConfiguration == null) return

        val cameraVideoSourcePath: String = videoUploadsConfiguration.sourcePath
        val localVideoFiles: MutableList<File> = mutableListOf()
        val cameraVideoFolder = File(cameraVideoSourcePath)
        localVideoFiles.addAll(cameraVideoFolder.listFiles() ?: arrayOf())
        localVideoFiles.sortBy { it.lastModified() }

        if (localVideoFiles.isNotEmpty()) {
            for (localFile in localVideoFiles) {
                val fileName = localFile.name
                val mimeType = MimetypeIconUtil.getBestMimeTypeByFilename(fileName)
                val isVideo = mimeType.startsWith("video/")
                if (isVideo) {
                    // TODO: Upload localFile
                    Timber.d("Upload ${localFile.name}")
                }
            }
        }
    }

    companion object {
        const val CAMERA_UPLOADS_WORKER = "CAMERA_UPLOADS_WORKER"
        const val repeatInterval: Long = 15L
        val repeatIntervalTimeUnit: TimeUnit = TimeUnit.MINUTES
    }
}
