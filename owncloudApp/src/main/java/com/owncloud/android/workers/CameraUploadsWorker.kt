package com.owncloud.android.workers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.datamodel.CameraUploadsSyncStorageManager
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.domain.camerauploads.usecases.GetCameraUploadsConfigurationUseCase
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.webdav.PutMethod
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.utils.MimetypeIconUtil
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URL
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

        val cameraPictureSourceUri: Uri = pictureUploadsConfiguration.sourcePath.toUri()
        val localPicturesDocumentFiles: MutableList<DocumentFile> = mutableListOf()
        val documentTree = DocumentFile.fromTreeUri(applicationContext, cameraPictureSourceUri)
        localPicturesDocumentFiles.addAll(documentTree?.listFiles() ?: arrayOf())
        localPicturesDocumentFiles.sortBy { it.lastModified() }
        if (localPicturesDocumentFiles.isNotEmpty()) {
            for (documentFile in localPicturesDocumentFiles) {
                val fileName = documentFile.name
                val mimeType = MimetypeIconUtil.getBestMimeTypeByFilename(fileName)
                val isImage = mimeType.startsWith("image/")

                val cameraUploadSync = CameraUploadsSyncStorageManager(appContext.contentResolver).getCameraUploadSync(
                    null,
                    null,
                    null
                )

                if (isImage) {
                    if (documentFile.lastModified() <= cameraUploadSync.picturesLastSync) {
                        Timber.i("Image ${documentFile.name} created before period to check, ignoring... Last Modified: ${documentFile.lastModified()} Last sync: ${cameraUploadSync.picturesLastSync}")
                    } else {
                        Timber.d("Upload document file ${documentFile.name}")
                        uploadDocument(
                            documentFile.uri,
                            appContext,
                            pictureUploadsConfiguration.uploadPath.plus(documentFile.name),
                            (documentFile.lastModified()/1000L).toString()
                        )
                    }
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

    class ContentUriRequestBody(
        private val contentResolver: ContentResolver,
        private val contentUri: Uri
    ) : RequestBody() {

        override fun contentType(): MediaType? {
            val contentType = contentResolver.getType(contentUri) ?: return null
            return contentType.toMediaTypeOrNull()
        }

        override fun writeTo(sink: BufferedSink) {
            val inputStream = contentResolver.openInputStream(contentUri)
                ?: throw IOException("Couldn't open content URI for reading: $contentUri")

            inputStream.source().use { source ->
                sink.writeAll(source)
            }
        }
    }

    fun uploadDocument(contentUri: Uri, context: Context, remotePath: String, lastModified: String) {
        val requestBody = ContentUriRequestBody(context.contentResolver, contentUri)

        val client = SingleSessionManager.getDefaultSingleton()
            .getClientFor(OwnCloudAccount(AccountUtils.getCurrentOwnCloudAccount(context), context), context)

        val putMethod = PutMethod(URL(client.userFilesWebDavUri.toString() + WebdavUtils.encodePath(remotePath)), requestBody)

        putMethod.setRetryOnConnectionFailure(false)

        putMethod.addRequestHeader(HttpConstants.OC_TOTAL_LENGTH_HEADER, requestBody.contentLength().toString())
        putMethod.addRequestHeader(HttpConstants.OC_X_OC_MTIME_HEADER, lastModified)

        val status: Int = client.executeHttpMethod(putMethod)
    }

    companion object {
        const val CAMERA_UPLOADS_WORKER = "CAMERA_UPLOADS_WORKER"
        const val repeatInterval: Long = 15L
        val repeatIntervalTimeUnit: TimeUnit = TimeUnit.MINUTES
    }
}
