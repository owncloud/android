/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
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
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.storage.LocalStorageProvider
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.exceptions.LocalFileNotFoundException
import com.owncloud.android.domain.exceptions.UnauthorizedException
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.transfers.TransferRepository
import com.owncloud.android.domain.transfers.model.TransferResult
import com.owncloud.android.domain.transfers.model.TransferStatus
import com.owncloud.android.extensions.parseError
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.files.CheckPathExistenceRemoteOperation
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.lib.resources.files.UploadFileFromFileSystemOperation
import com.owncloud.android.lib.resources.files.chunks.ChunkedUploadFromFileSystemOperation
import com.owncloud.android.lib.resources.files.services.implementation.OCChunkService
import com.owncloud.android.utils.NotificationUtils
import com.owncloud.android.utils.RemoteFileUtils.Companion.getAvailableRemotePath
import com.owncloud.android.utils.UPLOAD_NOTIFICATION_CHANNEL_ID
import com.owncloud.android.utils.UriUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class UploadFileFromContentUriWorker(
    private val appContext: Context,
    private val workerParameters: WorkerParameters,
) : CoroutineWorker(
    appContext,
    workerParameters
), KoinComponent, OnDatatransferProgressListener {

    private lateinit var account: Account
    private lateinit var contentUri: Uri
    private lateinit var lastModified: String
    private lateinit var behavior: UploadBehavior
    private lateinit var uploadPath: String
    private lateinit var cachePath: String
    private lateinit var mimeType: String
    private var fileSize: Long = 0
    private var uploadIdInStorageManager: Long = -1

    private lateinit var uploadFileOperation: UploadFileFromFileSystemOperation

    private var lastPercent = 0

    private val transferRepository: TransferRepository by inject()

    override suspend fun doWork(): Result {

        if (!areParametersValid()) return Result.failure()

        transferRepository.updateTransferStatusToInProgressById(uploadIdInStorageManager)

        return try {
            checkDocumentFileExists()
            checkPermissionsToReadDocumentAreGranted()
            copyFileToLocalStorage()
            checkParentFolderExistence()
            checkNameCollisionAndGetAnAvailableOneInCase()
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
        val paramContentUri = workerParameters.inputData.getString(KEY_PARAM_CONTENT_URI)
        val paramUploadId = workerParameters.inputData.getLong(KEY_PARAM_UPLOAD_ID, -1)

        account = AccountUtils.getOwnCloudAccountByName(appContext, paramAccountName) ?: return false
        contentUri = paramContentUri?.toUri() ?: return false
        uploadPath = paramUploadPath ?: return false
        behavior = paramBehavior?.let { UploadBehavior.fromString(it) } ?: return false
        lastModified = paramLastModified ?: return false
        uploadIdInStorageManager = paramUploadId

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
            throw LocalFileNotFoundException()
        }
    }

    private fun copyFileToLocalStorage() {
        val localStorageProvider: LocalStorageProvider by inject()
        cachePath = localStorageProvider.getTemporalPath(account.name) + uploadPath + UriUtils.getDisplayNameForUri(contentUri, appContext)
        val cacheFile = File(cachePath)
        val cacheDir = cacheFile.parentFile
        if (cacheDir != null) {
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
        }
        cacheFile.createNewFile()

        val inputStream = appContext.contentResolver.openInputStream(contentUri)
        val outputStream = FileOutputStream(cachePath)
        outputStream.use { fileOut ->
            inputStream?.copyTo(fileOut)
        }
        inputStream?.close()
        outputStream.close()

        mimeType = cacheFile.extension
        fileSize = cacheFile.length()
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

    private fun checkNameCollisionAndGetAnAvailableOneInCase() {
        Timber.d("Checking name collision in server")
        val remotePath = getAvailableRemotePath(getClientForThisUpload(), uploadPath)
        if (remotePath != null && remotePath != uploadPath) {
            uploadPath = remotePath
            Timber.d("Name collision detected, let's rename it to %s", remotePath)
        }
    }

    private fun uploadDocument() {
        val client = getClientForThisUpload()

        val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase by inject()
        val capabilitiesForAccount = getStoredCapabilitiesUseCase.execute(
            GetStoredCapabilitiesUseCase.Params(
                accountName = account.name
            )
        )
        val isChunkingAllowed = capabilitiesForAccount != null && capabilitiesForAccount.isChunkingAllowed()
        Timber.d("Chunking is allowed: %s", isChunkingAllowed)

        if (isChunkingAllowed) {
            uploadChunkedFile(client)
        } else {
            uploadPlainFile(client)
        }
    }

    private fun uploadPlainFile(client: OwnCloudClient) {
        uploadFileOperation = UploadFileFromFileSystemOperation(
            localPath = cachePath,
            remotePath = uploadPath,
            mimeType = mimeType,
            lastModifiedTimestamp = lastModified,
            requiredEtag = null,
        ).also {
            it.addDataTransferProgressListener(this)
        }

        val result = executeRemoteOperation { uploadFileOperation.execute(client) }

        if (result == Unit && behavior == UploadBehavior.MOVE) {
            removeLocalFile()
        }
    }

    private fun uploadChunkedFile(client: OwnCloudClient) {
        // Step 1: Create folder where the chunks will be uploaded.
        val createChunksRemoteFolderOperation = CreateRemoteFolderOperation(
            remotePath = uploadIdInStorageManager.toString(), createFullPath = false, isChunksFolder = true
        )
        executeRemoteOperation { createChunksRemoteFolderOperation.execute(client) }

        // Step 2: Upload file by chunks
        uploadFileOperation = ChunkedUploadFromFileSystemOperation(
            transferId = uploadIdInStorageManager.toString(),
            localPath = cachePath,
            remotePath = uploadPath,
            mimeType = mimeType,
            lastModifiedTimestamp = lastModified,
            requiredEtag = null,
        ).also {
            it.addDataTransferProgressListener(this)
        }

        val result = executeRemoteOperation { uploadFileOperation.execute(client) }

        // Step 3: Move remote file to the final remote destination
        val ocChunkService = OCChunkService(client)
        ocChunkService.moveFile(
            sourceRemotePath = "$uploadIdInStorageManager${OCFile.PATH_SEPARATOR}${FileUtils.FINAL_CHUNKS_FILE}",
            targetRemotePath = uploadPath,
            fileLastModificationTimestamp = lastModified,
            fileLength = fileSize
        )

        // Step 4: Remove local file after uploading
        if (result == Unit && behavior == UploadBehavior.MOVE) {
            removeLocalFile()
        }
    }

    private fun removeLocalFile() {
        val documentFile = DocumentFile.fromSingleUri(appContext, contentUri)
        documentFile?.delete()
    }

    private fun updateUploadsDatabaseWithResult(throwable: Throwable?) {
        transferRepository.updateTransferWhenFinished(
            id = uploadIdInStorageManager,
            status = getUploadStatusForThrowable(throwable),
            transferEndTimestamp = System.currentTimeMillis(),
            lastResult = TransferResult.fromThrowable(throwable)
        )
    }

    private fun getUploadStatusForThrowable(throwable: Throwable?): TransferStatus {
        return if (throwable == null) {
            TransferStatus.TRANSFER_SUCCEEDED
        } else {
            TransferStatus.TRANSFER_FAILED
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

    private fun getClientForThisUpload(): OwnCloudClient = SingleSessionManager.getDefaultSingleton()
        .getClientFor(OwnCloudAccount(AccountUtils.getOwnCloudAccountByName(appContext, account.name), appContext), appContext,
        SingleSessionManager.getConnectionValidator())

    override fun onTransferProgress(
        progressRate: Long,
        totalTransferredSoFar: Long,
        totalToTransfer: Long,
        filePath: String
    ) {
        val percent: Int = (100.0 * totalTransferredSoFar.toDouble() / totalToTransfer.toDouble()).toInt()
        if (percent == lastPercent) return

        // Set current progress. Observers will listen.
        CoroutineScope(Dispatchers.IO).launch {
            val progress = workDataOf(DownloadFileWorker.WORKER_KEY_PROGRESS to percent)
            setProgress(progress)
        }

        lastPercent = percent
    }

    companion object {
        const val KEY_PARAM_ACCOUNT_NAME = "KEY_PARAM_ACCOUNT_NAME"
        const val KEY_PARAM_BEHAVIOR = "KEY_PARAM_BEHAVIOR"
        const val KEY_PARAM_CONTENT_URI = "KEY_PARAM_CONTENT_URI"
        const val KEY_PARAM_LAST_MODIFIED = "KEY_PARAM_LAST_MODIFIED"
        const val KEY_PARAM_UPLOAD_PATH = "KEY_PARAM_UPLOAD_PATH"
        const val KEY_PARAM_UPLOAD_ID = "KEY_PARAM_UPLOAD_ID"
    }
}
