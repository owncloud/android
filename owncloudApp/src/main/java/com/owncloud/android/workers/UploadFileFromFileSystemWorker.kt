/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2022 ownCloud GmbH.
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

import android.accounts.Account
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.datamodel.OCUpload
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.UploadResult
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.domain.exceptions.ConflictException
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.exceptions.ForbiddenException
import com.owncloud.android.domain.exceptions.LocalFileNotFoundException
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.domain.exceptions.QuotaExceededException
import com.owncloud.android.domain.exceptions.SSLRecoverablePeerUnverifiedException
import com.owncloud.android.domain.exceptions.ServiceUnavailableException
import com.owncloud.android.domain.exceptions.SpecificUnsupportedMediaTypeException
import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.extensions.parseError
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.files.CheckPathExistenceRemoteOperation
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation
import com.owncloud.android.lib.resources.files.UploadFileFromFileSystemOperation
import com.owncloud.android.utils.NotificationUtils
import com.owncloud.android.utils.RemoteFileUtils.Companion.getAvailableRemotePath
import com.owncloud.android.utils.UPLOAD_NOTIFICATION_CHANNEL_ID
import org.koin.core.KoinComponent
import timber.log.Timber
import java.io.File

class UploadFileFromFileSystemWorker(
    private val appContext: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
), KoinComponent {

    private lateinit var account: Account
    private lateinit var fileSystemPath: String
    private lateinit var lastModified: String
    private lateinit var behavior: UploadBehavior
    private lateinit var uploadPath: String
    private lateinit var mimetype: String
    private var uploadIdInStorageManager: Long = -1
    private lateinit var ocUpload: OCUpload

    override suspend fun doWork(): Result {

        if (!areParametersValid()) return Result.failure()

        return try {
            checkPermissionsToReadDocumentAreGranted()
            checkParentFolderExistence()
            checkNameCollisionOrGetAnAvailableOneInCase()
            uploadDocument()
            updateUploadsDatabaseWithResult(null)
            Result.success()
        } catch (throwable: Throwable) {
            Timber.e(throwable)
            showNotification(throwable)
            updateUploadsDatabaseWithResult(throwable)
            Result.failure()
        }
    }

    private fun areParametersValid(): Boolean {
        val paramAccountName = workerParameters.inputData.getString(KEY_PARAM_ACCOUNT_NAME)
        val paramUploadPath = workerParameters.inputData.getString(KEY_PARAM_UPLOAD_PATH)
        val paramLastModified = workerParameters.inputData.getString(KEY_PARAM_LAST_MODIFIED)
        val paramBehavior = workerParameters.inputData.getString(KEY_PARAM_BEHAVIOR)
        val paramFileSystemUri = workerParameters.inputData.getString(KEY_PARAM_CONTENT_URI)
        val paramUploadId = workerParameters.inputData.getLong(KEY_PARAM_UPLOAD_ID, -1)

        account = AccountUtils.getOwnCloudAccountByName(appContext, paramAccountName) ?: return false
        fileSystemPath = paramFileSystemUri.takeUnless { it.isNullOrBlank() } ?: return false
        uploadPath = paramUploadPath ?: return false
        behavior = paramBehavior?.let { UploadBehavior.valueOf(it) } ?: return false
        lastModified = paramLastModified ?: return false
        uploadIdInStorageManager = paramUploadId.takeUnless { it == -1L } ?: return false
        ocUpload = retrieveUploadInfoFromDatabase() ?: return false

        return true
    }

    private fun retrieveUploadInfoFromDatabase(): OCUpload? {
        val uploadStorageManager = UploadsStorageManager(appContext.contentResolver)

        val storedUploads = uploadStorageManager.allStoredUploads

        return storedUploads.find { uploadIdInStorageManager == it.uploadId }.also {
            if (it != null) {
                Timber.d("Upload with id ($uploadIdInStorageManager) has been found in database.")
                Timber.d("Upload info: $it")
            } else {
                Timber.w("Upload with id ($uploadIdInStorageManager) has not been found in database.")
                Timber.w("$uploadPath won't be uploaded")
            }
        }
    }

    private fun checkPermissionsToReadDocumentAreGranted() {
        val fileInFileSystem = File(fileSystemPath)
        if (!fileInFileSystem.exists() || !fileInFileSystem.isFile || !fileInFileSystem.canRead()) {
            // Permissions not granted. Throw an exception to ask for them.
            throw LocalFileNotFoundException()
        }
        mimetype = fileInFileSystem.extension
    }

    private fun checkParentFolderExistence() {
        var pathToGrant: String = File(uploadPath).parent ?: ""
        pathToGrant = if (pathToGrant.endsWith(File.separator)) pathToGrant else pathToGrant + File.separator

        val checkPathExistenceOperation = CheckPathExistenceRemoteOperation(pathToGrant, false)
        val checkPathExistenceResult = checkPathExistenceOperation.execute(getClientForThisUpload())
        if (checkPathExistenceResult.code == ResultCode.FILE_NOT_FOUND) {
            val createRemoteFolderOperation = CreateRemoteFolderOperation(pathToGrant, true)
            createRemoteFolderOperation.execute(getClientForThisUpload())
        }
    }

    private fun checkNameCollisionOrGetAnAvailableOneInCase() {
        if (ocUpload.isForceOverwrite) {
            Timber.d("Upload will override current server file")
            // FIXME: Retrieve somehow the EtagInConflict from the OCFile.
            //   We need it to override the file in server.
            return
        }

        Timber.d("Checking name collision in server")
        val remotePath = getAvailableRemotePath(getClientForThisUpload(), uploadPath)
        if (remotePath != null && remotePath != uploadPath) {
            uploadPath = remotePath
            Timber.d("Name collision detected, let's rename it to $remotePath")
        }
    }

    private fun uploadDocument() {
        val client = getClientForThisUpload()

        // FIXME: Retrieve somehow the EtagInConflict from the OCFile.
        //   We need it to override the file in server.
        val uploadFileFromFileSystemOperation = UploadFileFromFileSystemOperation(
            localPath = fileSystemPath,
            remotePath = uploadPath,
            mimeType = mimetype,
            lastModifiedTimestamp = lastModified,
            requiredEtag = ""
        )

        val result = executeRemoteOperation { uploadFileFromFileSystemOperation.execute(client) }

        if (result == Unit && behavior == UploadBehavior.MOVE) {
            removeLocalFile()
        }
    }

    private fun removeLocalFile() {
        val fileDeleted = File(fileSystemPath).delete()
        Timber.d("File with path: $fileSystemPath has been removed: $fileDeleted after uploading.")
    }

    private fun updateUploadsDatabaseWithResult(throwable: Throwable?) {
        val uploadStorageManager = UploadsStorageManager(appContext.contentResolver)

        val ocUpload = OCUpload(fileSystemPath, uploadPath, account.name).apply {
            uploadStatus = getUploadStatusForThrowable(throwable)
            uploadEndTimestamp = System.currentTimeMillis()
            lastResult = getUploadResultFromThrowable(throwable)
            uploadId = uploadIdInStorageManager
        }

        uploadStorageManager.updateUpload(ocUpload)
    }

    private fun getUploadStatusForThrowable(throwable: Throwable?): UploadsStorageManager.UploadStatus {
        return if (throwable == null) {
            UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED
        } else {
            UploadsStorageManager.UploadStatus.UPLOAD_FAILED
        }
    }

    private fun showNotification(throwable: Throwable) {
        // check credentials error
        val needsToUpdateCredentials = throwable is UnauthorizedException

        val tickerId =
            if (needsToUpdateCredentials) R.string.uploader_upload_failed_credentials_error else R.string.uploader_upload_failed_ticker

        val pendingIntent = if (needsToUpdateCredentials) {
            NotificationUtils.composePendingIntentToRefreshCredentials(appContext, account)
        } else {
            NotificationUtils.composePendingIntentToUploadList(appContext)
        }

        NotificationUtils.createBasicNotification(
            context = appContext,
            contentTitle = appContext.getString(tickerId),
            contentText = throwable.parseError("", appContext.resources, true).toString(),
            notificationChannelId = UPLOAD_NOTIFICATION_CHANNEL_ID,
            notificationId = 12,
            intent = pendingIntent,
            onGoing = false,
            timeOut = null
        )
    }

    private fun getUploadResultFromThrowable(throwable: Throwable?): UploadResult {
        if (throwable == null) return UploadResult.UPLOADED

        return when (throwable) {
            is LocalFileNotFoundException -> UploadResult.FOLDER_ERROR
            is NoConnectionWithServerException -> UploadResult.NETWORK_CONNECTION
            is UnauthorizedException -> UploadResult.CREDENTIAL_ERROR
            is FileNotFoundException -> UploadResult.FILE_NOT_FOUND
            is ConflictException -> UploadResult.CONFLICT_ERROR
            is ForbiddenException -> UploadResult.PRIVILEGES_ERROR
            is ServiceUnavailableException -> UploadResult.SERVICE_UNAVAILABLE
            is QuotaExceededException -> UploadResult.QUOTA_EXCEEDED
            is SpecificUnsupportedMediaTypeException -> UploadResult.SPECIFIC_UNSUPPORTED_MEDIA_TYPE
            is SSLRecoverablePeerUnverifiedException -> UploadResult.SSL_RECOVERABLE_PEER_UNVERIFIED
            else -> UploadResult.UNKNOWN
        }
    }

    private fun getClientForThisUpload(): OwnCloudClient = SingleSessionManager.getDefaultSingleton()
        .getClientFor(OwnCloudAccount(AccountUtils.getOwnCloudAccountByName(appContext, account.name), appContext), appContext)

    companion object {
        const val KEY_PARAM_ACCOUNT_NAME = "KEY_PARAM_ACCOUNT_NAME"
        const val KEY_PARAM_BEHAVIOR = "KEY_PARAM_BEHAVIOR"
        const val KEY_PARAM_CONTENT_URI = "KEY_PARAM_CONTENT_URI"
        const val KEY_PARAM_LAST_MODIFIED = "KEY_PARAM_LAST_MODIFIED"
        const val KEY_PARAM_UPLOAD_PATH = "KEY_PARAM_UPLOAD_PATH"
        const val KEY_PARAM_UPLOAD_ID = "KEY_PARAM_UPLOAD_ID"
    }
}
