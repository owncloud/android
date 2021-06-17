package com.owncloud.android.workers

import android.accounts.Account
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.domain.camerauploads.model.FolderBackUpConfiguration
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.webdav.PutMethod
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.files.CheckPathExistenceRemoteOperation
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation
import com.owncloud.android.utils.RemoteFileUtils.Companion.getAvailableRemotePath
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import org.koin.core.KoinComponent
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URL

class UploadFileFromContentUriWorker(
    private val appContext: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
), KoinComponent {

    lateinit var account: Account
    lateinit var contentUri: Uri
    lateinit var lastModified: String
    lateinit var behavior: FolderBackUpConfiguration.Behavior
    lateinit var uploadPath: String

    override suspend fun doWork(): Result {

        if (!areParametersValid()) return Result.failure()

        return try {
            // 1- Check permissions to read are granted
            checkPermissionsToReadDocumentAreGranted()
            // 2- Check file exists
            checkDocumentFileExists()
            // 3- Check the existence of the parent folder for the file to upload
            checkParentFolderExistence()
            // 4- Check collision automatic rename of file to upload in case of name collision in server
            checkNameCollisionAndGetAnAvailableOneInCase()
            // 5- Perform the upload
            uploadDocument()
            // 6a- If upload succeeds, update database.
            // 6b- If upload fails, save error
            Result.success()
        } catch (throwable: Throwable) {
            Timber.e(throwable)
            if (throwable is NoConnectionWithServerException) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun areParametersValid(): Boolean {
        val paramAccountName = workerParameters.inputData.getString(KEY_PARAM_ACCOUNT_NAME)
        val paramUploadPath = workerParameters.inputData.getString(KEY_PARAM_UPLOAD_PATH)
        val paramLastModified = workerParameters.inputData.getString(KEY_PARAM_LAST_MODIFIED)
        val paramBehavior = workerParameters.inputData.getString(KEY_PARAM_BEHAVIOR)
        val paramContentUri = workerParameters.inputData.getString(KEY_PARAM_CONTENT_URI)

        account = AccountUtils.getOwnCloudAccountByName(appContext, paramAccountName) ?: return false
        contentUri = paramContentUri?.toUri() ?: return false
        uploadPath = paramUploadPath ?: return false
        behavior = paramBehavior?.let { FolderBackUpConfiguration.Behavior.fromString(it) } ?: return false
        lastModified = paramLastModified ?: return false

        return true
    }

    private fun checkPermissionsToReadDocumentAreGranted() {
        val documentFile = DocumentFile.fromSingleUri(appContext, contentUri)
        if (documentFile?.canRead() != true) {
            // Permissions not granted. Throw an exception to ask for them.
            throw Throwable("Cannot read the file")
        }
    }

    private fun checkDocumentFileExists() {
        val documentFile = DocumentFile.fromSingleUri(appContext, contentUri)
        if (documentFile?.exists() != true && documentFile?.isFile != true) {
            // File does not exists anymore. Throw an exception to tell the user
            throw FileNotFoundException()
        }
    }

    private fun checkParentFolderExistence() {
        var pathToGrant: String = File(uploadPath).parent ?: ""
        pathToGrant = if (pathToGrant.endsWith(File.separator)) pathToGrant else pathToGrant + File.separator

        val checkPathExistenceOperation = CheckPathExistenceRemoteOperation(pathToGrant, false)
        val checkPathExistenceResult = checkPathExistenceOperation.execute(getClientForThisUpload())
        if (checkPathExistenceResult.code == ResultCode.FILE_NOT_FOUND) {
            val createRemoteFolderOperation = CreateRemoteFolderOperation(pathToGrant, true)
            val createRemoteFolderResult = createRemoteFolderOperation.execute(getClientForThisUpload())
            if (!createRemoteFolderResult.isSuccess) {
                throw Throwable("Parent folder was not created")
            }
        }
    }

    private fun checkNameCollisionAndGetAnAvailableOneInCase() {
        Timber.d("Checking name collision in server")
        val remotePath = getAvailableRemotePath(getClientForThisUpload(), uploadPath)
        if (remotePath != null && remotePath != uploadPath) {
            uploadPath = remotePath
            Timber.d("Name collision detected, let's rename it to %s", remotePath)
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

    fun uploadDocument() {
        val requestBody = ContentUriRequestBody(appContext.contentResolver, contentUri)

        val client = getClientForThisUpload()

        val putMethod = PutMethod(URL(client.userFilesWebDavUri.toString() + WebdavUtils.encodePath(uploadPath)), requestBody).apply {
            setRetryOnConnectionFailure(false)
            addRequestHeader(HttpConstants.OC_TOTAL_LENGTH_HEADER, requestBody.contentLength().toString())
            addRequestHeader(HttpConstants.OC_X_OC_MTIME_HEADER, lastModified)
        }

        val result = client.executeHttpMethod(putMethod)

        if (!isSuccess(result)) {
            throw Throwable(putMethod.statusMessage)
        } else {
            if (behavior == FolderBackUpConfiguration.Behavior.MOVE) {
                removeLocalFile()
            }
        }
    }

    private fun removeLocalFile() {
        val documentFile = DocumentFile.fromSingleUri(appContext, contentUri)
        documentFile?.delete()
    }

    fun getClientForThisUpload(): OwnCloudClient = SingleSessionManager.getDefaultSingleton()
        .getClientFor(OwnCloudAccount(AccountUtils.getOwnCloudAccountByName(appContext, account.name), appContext), appContext)

    fun isSuccess(status: Int): Boolean {
        return status == HttpConstants.HTTP_OK || status == HttpConstants.HTTP_CREATED || status == HttpConstants.HTTP_NO_CONTENT
    }

    companion object {
        const val TRANSFER_TAG_CAMERA_UPLOAD = "TRANSFER_TAG_CAMERA_UPLOAD"

        const val KEY_PARAM_ACCOUNT_NAME = "KEY_PARAM_ACCOUNT_NAME"
        const val KEY_PARAM_BEHAVIOR = "KEY_PARAM_BEHAVIOR"
        const val KEY_PARAM_CONTENT_URI = "KEY_PARAM_CONTENT_URI"
        const val KEY_PARAM_LAST_MODIFIED = "KEY_PARAM_LAST_MODIFIED"
        const val KEY_PARAM_UPLOAD_PATH = "KEY_PARAM_UPLOAD_PATH"
    }
}
