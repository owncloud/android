/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import at.bitfire.dav4jvm.exception.UnauthorizedException
import com.owncloud.android.R
import com.owncloud.android.data.executeRemoteOperation
import com.owncloud.android.domain.exceptions.CancelledException
import com.owncloud.android.domain.exceptions.LocalStorageNotMovedException
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.files.usecases.SaveFileOrFolderUseCase
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.resources.files.DownloadRemoteFileOperation
import com.owncloud.android.presentation.ui.authentication.ACTION_UPDATE_EXPIRED_TOKEN
import com.owncloud.android.presentation.ui.authentication.EXTRA_ACCOUNT
import com.owncloud.android.presentation.ui.authentication.EXTRA_ACTION
import com.owncloud.android.presentation.ui.authentication.LoginActivity
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter
import com.owncloud.android.ui.errorhandling.TypeOfOperation.TransferDownload
import com.owncloud.android.utils.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.NOTIFICATION_TIMEOUT_STANDARD
import com.owncloud.android.utils.NotificationUtils.createBasicNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.io.File

class DownloadFileWorker(
    private val appContext: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
), KoinComponent, OnDatatransferProgressListener {

    private val client: OwnCloudClient by inject()
    private val getFileByIdUseCase: GetFileByIdUseCase by inject()
    private val saveFileOrFolderUseCase: SaveFileOrFolderUseCase by inject()

    lateinit var accountName: String
    lateinit var downloadRemoteFileOperation: DownloadRemoteFileOperation
    lateinit var ocFile: OCFile

    /**
     * Temporal path for this file to be downloaded.
     */
    private val temporalFilePath
        get() = temporalFolderPath + ocFile.remotePath

    /**
     * Temporal path where every file of this account will be downloaded.
     */
    private val temporalFolderPath
        get() = FileStorageUtils.getTemporalPath(accountName)

    /**
     * Final path where this file should be stored.
     *
     * In case this file was previously downloaded, override it. Otherwise,
     * @see FileStorageUtils.getDefaultSavePathFor
     */
    private val finalLocationForFile: String
        get() = ocFile.storagePath.takeUnless { it.isNullOrBlank() }
            ?: FileStorageUtils.getDefaultSavePathFor(accountName, ocFile)

    override suspend fun doWork(): Result {

        // Params
        accountName = workerParameters.inputData.getString(KEY_PARAM_ACCOUNT) as String
        val fileId = workerParameters.inputData.getLong(KEY_PARAM_FILE_ID, -1)

        val ocFile: OCFile? = getFileByIdUseCase.execute(GetFileByIdUseCase.Params(fileId)).getDataOrNull()

        // If file is not found in the database or file is a folder, return failure.
        if (ocFile == null || ocFile.isFolder) {
            Timber.d("PARAM: $fileId OCFILE : ${ocFile?.remotePath} is folder ${ocFile?.isFolder}")
            return Result.failure()
        } else {
            this.ocFile = ocFile
        }

        return try {
            downloadFileToTemporalFile()
            moveTemporalFileToFinalLocation()
            updateDatabaseWithLatestInfoForThisFile()
            notifyDownloadResult(null)
        } catch (throwable: Throwable) {
            // clean up and log
            Timber.e(throwable)
            notifyDownloadResult(throwable)
        }
    }

    /**
     * Download the file or throw an exception if something goes wrong.
     * We will initialize a listener to update the notification according to the download progress.
     *
     * File will be downloaded to a temporalFolder in the RemoteOperation.
     * @see temporalFolderPath for the temporal location
     */
    private fun downloadFileToTemporalFile() {
        downloadRemoteFileOperation = DownloadRemoteFileOperation(
            ocFile.remotePath,
            temporalFolderPath
        ).also {
            it.addDatatransferProgressListener(this)
        }

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
        ocFile.apply {
            needsToUpdateThumbnail = true
            modificationTimestamp = downloadRemoteFileOperation.modificationTimestamp
            etag = downloadRemoteFileOperation.etag
            storagePath = finalLocationForFile
            length = (File(finalLocationForFile).length())
        }
        saveFileOrFolderUseCase.execute(SaveFileOrFolderUseCase.Params(ocFile))

        //mStorageManager.triggerMediaScan(file.getStoragePath())
        //mStorageManager.saveConflict(file, null)
    }

    /**
     * Notify download result and then return Worker Result.
     */
    private fun notifyDownloadResult(
        throwable: Throwable?
    ): Result {
        if (throwable !is CancelledException) {

            var tickerId = if (throwable == null) {
                R.string.downloader_download_succeeded_ticker
            } else {
                R.string.downloader_download_failed_ticker
            }

            val needsToUpdateCredentials = throwable is UnauthorizedException
            tickerId = if (needsToUpdateCredentials) R.string.downloader_download_failed_credentials_error else tickerId

            val pendingIntent: PendingIntent?
            if (needsToUpdateCredentials) {
                // let the user update credentials with one click
                val updateCredentialsIntent =
                    Intent(appContext, LoginActivity::class.java).apply {
                        putExtra(EXTRA_ACCOUNT, accountName)
                        putExtra(EXTRA_ACTION, ACTION_UPDATE_EXPIRED_TOKEN)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        addFlags(Intent.FLAG_FROM_BACKGROUND)
                    }
                pendingIntent = PendingIntent.getActivity(
                    appContext,
                    System.currentTimeMillis().toInt(),
                    updateCredentialsIntent,
                    PendingIntent.FLAG_ONE_SHOT
                )

            } else {
                // TODO put something smart in showDetailsIntent
                val showDetailsIntent = Intent()
                pendingIntent =
                    PendingIntent.getActivity(
                        appContext,
                        System.currentTimeMillis().toInt(),
                        showDetailsIntent,
                        0
                    )
            }

            val contextText = ErrorMessageAdapter.getMessageFromOperation(
                typeOfOperation = TransferDownload(finalLocationForFile),
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
                notificationId = ocFile.id!!.toInt(),
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

    override fun onTransferProgress(
        progressRate: Long,
        totalTransferredSoFar: Long,
        totalToTransfer: Long,
        filePath: String
    ) {
        val contentTitle = appContext.getString(R.string.downloader_download_in_progress_ticker)

        val percent: Int = (100.0 * totalTransferredSoFar.toDouble() / totalToTransfer.toDouble()).toInt()
        val contentText = String.format(
            appContext.getString(R.string.downloader_download_in_progress_content),
            percent,
            File(this.finalLocationForFile).name
        )

        // Set current progress. Observers will listen.
        CoroutineScope(Dispatchers.IO).launch {
            val progress = workDataOf(KEY_PROGRESS to percent)
            setProgress(progress)
        }

        showNotificationWithProgress(
            maxValue = totalToTransfer.toInt(),
            progress = totalTransferredSoFar.toInt(),
            notificationChannelId = DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            contentText = contentText,
            contentTitle = contentTitle,
            fileId = ocFile.id
        )
    }

    companion object {
        const val KEY_PARAM_ACCOUNT = "KEY_PARAM_ACCOUNT"
        const val KEY_PARAM_FILE_ID = "KEY_PARAM_FILE_ID"
        const val KEY_PROGRESS = "KEY_PROGRESS"
    }
}
