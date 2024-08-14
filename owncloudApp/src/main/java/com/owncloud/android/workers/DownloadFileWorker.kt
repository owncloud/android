/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.workers

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import at.bitfire.dav4jvm.exception.UnauthorizedException
import com.owncloud.android.R
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.data.providers.LocalStorageProvider
import com.owncloud.android.domain.exceptions.CancelledException
import com.owncloud.android.domain.exceptions.LocalStorageNotMovedException
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.CleanConflictUseCase
import com.owncloud.android.domain.files.usecases.CleanWorkersUUIDUseCase
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.files.usecases.GetWebDavUrlForSpaceUseCase
import com.owncloud.android.domain.files.usecases.SaveDownloadWorkerUUIDUseCase
import com.owncloud.android.domain.files.usecases.SaveFileOrFolderUseCase
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.resources.files.DownloadRemoteFileOperation
import com.owncloud.android.presentation.authentication.ACTION_UPDATE_EXPIRED_TOKEN
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.presentation.authentication.EXTRA_ACCOUNT
import com.owncloud.android.presentation.authentication.EXTRA_ACTION
import com.owncloud.android.presentation.authentication.LoginActivity
import com.owncloud.android.presentation.transfers.TransferOperation.Download
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.ui.preview.PreviewImageFragment.Companion.canBePreviewed
import com.owncloud.android.utils.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import com.owncloud.android.utils.DOWNLOAD_NOTIFICATION_ID_DEFAULT
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.NOTIFICATION_TIMEOUT_STANDARD
import com.owncloud.android.utils.NotificationUtils.createBasicNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File

class DownloadFileWorker(
    private val appContext: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters,
), KoinComponent, OnDatatransferProgressListener {

    private val getFileByIdUseCase: GetFileByIdUseCase by inject()
    private val saveFileOrFolderUseCase: SaveFileOrFolderUseCase by inject()
    private val getWebdavUrlForSpaceUseCase: GetWebDavUrlForSpaceUseCase by inject()
    private val cleanConflictUseCase: CleanConflictUseCase by inject()
    private val saveDownloadWorkerUuidUseCase: SaveDownloadWorkerUUIDUseCase by inject()
    private val cleanWorkersUuidUseCase: CleanWorkersUUIDUseCase by inject()
    private val localStorageProvider: LocalStorageProvider by inject()

    lateinit var account: Account
    lateinit var ocFile: OCFile

    private lateinit var downloadRemoteFileOperation: DownloadRemoteFileOperation
    private var lastPercent = 0

    /**
     * Temporal path for this file to be downloaded.
     */
    private val temporalFilePath
        get() = temporalFolderPath + ocFile.remotePath

    /**
     * Temporal path where every file of this account will be downloaded.
     */
    private val temporalFolderPath
        get() = FileStorageUtils.getTemporalPath(account.name, ocFile.spaceId)

    /**
     * Final path where this file should be stored.
     *
     * In case this file was previously downloaded, override it. Otherwise,
     * @see LocalStorageProvider.getDefaultSavePathFor
     */
    private val finalLocationForFile: String
        get() = ocFile.storagePath.takeUnless { it.isNullOrBlank() }
            ?: localStorageProvider.getDefaultSavePathFor(accountName = account.name, remotePath = ocFile.remotePath, spaceId = ocFile.spaceId)

    override suspend fun doWork(): Result {
        if (!areParametersValid()) return Result.failure()

        return try {
            downloadFileToTemporalFile()
            moveTemporalFileToFinalLocation()
            updateDatabaseWithLatestInfoForThisFile()
            notifyDownloadResult(null)
        } catch (throwable: Throwable) {
            Timber.e(throwable)
            notifyDownloadResult(throwable)
        }
    }

    /**
     * Verify that the parameters are valid.
     *
     * This verification includes (at the moment):
     * - Check whether account exists
     * - Check whether file exists in the database
     * - Check that the file is not a folder
     */
    private fun areParametersValid(): Boolean {
        val accountName = workerParameters.inputData.getString(KEY_PARAM_ACCOUNT)
        val fileId = workerParameters.inputData.getLong(KEY_PARAM_FILE_ID, -1)

        account = AccountUtils.getOwnCloudAccountByName(appContext, accountName) ?: return false
        ocFile = getFileByIdUseCase(GetFileByIdUseCase.Params(fileId)).getDataOrNull() ?: return false

        return !ocFile.isFolder
    }

    /**
     * Download the file or throw an exception if something goes wrong.
     * We will initialize a listener to update the notification according to the download progress.
     *
     * File will be downloaded to a temporalFolder in the RemoteOperation.
     * @see temporalFolderPath for the temporal location
     */
    private fun downloadFileToTemporalFile() {
        saveDownloadWorkerUuidUseCase(
            SaveDownloadWorkerUUIDUseCase.Params(
                fileId = workerParameters.inputData.getLong(KEY_PARAM_FILE_ID, -1),
                workerUuid = id
            )
        )

        val spaceWebDavUrl =
            getWebdavUrlForSpaceUseCase(GetWebDavUrlForSpaceUseCase.Params(accountName = account.name, spaceId = ocFile.spaceId))

        downloadRemoteFileOperation = DownloadRemoteFileOperation(
            ocFile.remotePath,
            temporalFolderPath,
            spaceWebDavUrl,
        ).also {
            it.addDatatransferProgressListener(this)
        }
        val client = getClientForThisDownload()

        // It will throw an exception if something goes wrong.
        executeRemoteOperation {
            downloadRemoteFileOperation.execute(client)
        }
    }

    /**
     * Move the temporal file to the final location.
     * @see temporalFilePath for the temporal location
     * @see finalLocationForFile for the final one
     */
    private fun moveTemporalFileToFinalLocation() {
        val temporalLocation = File(temporalFilePath)

        if (FileStorageUtils.getUsableSpace() < temporalLocation.length()) {
            Timber.w("Not enough space to copy %s", temporalLocation.absolutePath)
        }

        val finalLocation = File(finalLocationForFile)
        finalLocation.parentFile?.mkdirs()
        val movedToTheFinalLocation = temporalLocation.renameTo(finalLocation)

        if (!movedToTheFinalLocation) {
            throw LocalStorageNotMovedException()
        }
    }

    /**
     * Update the database with latest details about this file.
     *
     * We will ask for thumbnails after a download
     * We will update info about the file (modification timestamp and etag)
     * We will update info about local storage (where it was stored and its size)
     */
    private fun updateDatabaseWithLatestInfoForThisFile() {
        val currentTime = System.currentTimeMillis()
        ocFile.apply {
            needsToUpdateThumbnail = true
            modificationTimestamp = downloadRemoteFileOperation.modificationTimestamp
            etag = downloadRemoteFileOperation.etag
            storagePath = finalLocationForFile
            length = (File(finalLocationForFile).length())
            lastSyncDateForData = currentTime
            modifiedAtLastSyncForData = downloadRemoteFileOperation.modificationTimestamp
            lastUsage = currentTime
        }
        saveFileOrFolderUseCase(SaveFileOrFolderUseCase.Params(ocFile))
        cleanConflictUseCase(
            CleanConflictUseCase.Params(
                fileId = ocFile.id!!
            )
        )

        // To be done. Probably we will move it out from here.
        //mStorageManager.triggerMediaScan(file.getStoragePath())
    }

    /**
     * Notify download result and then return Worker Result.
     */
    private fun notifyDownloadResult(
        throwable: Throwable?
    ): Result {
        cleanWorkersUuidUseCase(
            CleanWorkersUUIDUseCase.Params(
                fileId = workerParameters.inputData.getLong(KEY_PARAM_FILE_ID, -1)
            )
        )
        if (throwable !is CancelledException) {

            var tickerId = if (throwable == null) {
                R.string.downloader_download_succeeded_ticker
            } else {
                R.string.downloader_download_failed_ticker
            }

            var pendingIntent: PendingIntent? = null
            if (throwable is UnauthorizedException) {
                tickerId = R.string.downloader_download_failed_credentials_error
                pendingIntent = composePendingIntentToRefreshCredentials()
            }

            val contextText = ErrorMessageAdapter.getMessageFromTransfer(
                transferOperation = Download(finalLocationForFile),
                throwable = throwable,
                resources = appContext.resources
            )

            var timeOut: Long? = null

            // Remove success notification after timeout
            if (throwable == null) {
                timeOut = NOTIFICATION_TIMEOUT_STANDARD
            }

            createBasicNotification(
                context = appContext,
                contentTitle = appContext.getString(tickerId),
                notificationChannelId = DOWNLOAD_NOTIFICATION_CHANNEL_ID,
                notificationId = DOWNLOAD_NOTIFICATION_ID_DEFAULT,
                intent = pendingIntent,
                contentText = contextText,
                timeOut = timeOut
            )
        }

        return if (throwable == null) {
            Result.success()
        } else {
            if (throwable is NoConnectionWithServerException) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun composePendingIntentToRefreshCredentials(): PendingIntent {
        val updateCredentialsIntent =
            Intent(appContext, LoginActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT, account.name)
                putExtra(EXTRA_ACTION, ACTION_UPDATE_EXPIRED_TOKEN)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_FROM_BACKGROUND)
            }

        return PendingIntent.getActivity(
            appContext,
            System.currentTimeMillis().toInt(),
            updateCredentialsIntent,
            PendingIntent.FLAG_ONE_SHOT
        )
    }

    private fun composePendingIntentToPreviewFile(): PendingIntent {
        /// includes a pending intent in the notification showing the details view of the file
        val showDetailsIntent: Intent =
            if (canBePreviewed(ocFile)) {
                Intent(appContext, PreviewImageActivity::class.java)
            } else {
                Intent(appContext, FileDisplayActivity::class.java)
            }.apply {
                putExtra(FileActivity.EXTRA_FILE, ocFile)
                putExtra(FileActivity.EXTRA_ACCOUNT, account)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        return PendingIntent.getActivity(
            appContext,
            System.currentTimeMillis().toInt(),
            showDetailsIntent,
            0
        )
    }

    private fun getClientForThisDownload(): OwnCloudClient = SingleSessionManager.getDefaultSingleton()
        .getClientFor(OwnCloudAccount(AccountUtils.getOwnCloudAccountByName(appContext, account.name), appContext), appContext)

    override fun onTransferProgress(
        progressRate: Long,
        totalTransferredSoFar: Long,
        totalToTransfer: Long,
        filePath: String
    ) {
        if (this.isStopped) {
            Timber.w("Cancelling remote operation. The worker is stopped by user or system")
            downloadRemoteFileOperation.cancel()
            downloadRemoteFileOperation.removeDatatransferProgressListener(this)
        }

        val percent: Int = if (totalToTransfer == -1L) -1 else (100.0 * totalTransferredSoFar.toDouble() / totalToTransfer.toDouble()).toInt()
        if (percent == lastPercent) return

        // Set current progress. Observers will listen.
        CoroutineScope(Dispatchers.IO).launch {
            val progress = workDataOf(WORKER_KEY_PROGRESS to percent)
            setProgress(progress)
        }

        lastPercent = percent
    }

    companion object {
        const val KEY_PARAM_ACCOUNT = "KEY_PARAM_ACCOUNT"
        const val KEY_PARAM_FILE_ID = "KEY_PARAM_FILE_ID"
        const val WORKER_KEY_PROGRESS = "KEY_PROGRESS"
    }
}
