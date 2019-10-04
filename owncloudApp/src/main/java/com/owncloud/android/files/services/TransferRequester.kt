/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 *
 *
 * Copyright (C) 2019 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.files.services

import android.accounts.Account
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.OCUpload
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.db.UploadResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_PICTURE
import com.owncloud.android.operations.UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_VIDEO
import com.owncloud.android.utils.ConnectivityUtils
import com.owncloud.android.utils.Extras
import com.owncloud.android.utils.PowerUtils
import java.net.SocketTimeoutException

/**
 * Facade to start operations in transfer services without the verbosity of Android Intents.
 */

/**
 * Facade class providing methods to ease requesting commands to transfer services [FileUploader] and
 * [FileDownloader].
 *
 * Protects client objects from the verbosity of [android.content.Intent]s.
 *
 * TODO add methods for [FileDownloader], right now it's just about uploads
 */

class TransferRequester {

    /**
     * Call to upload several new files
     */
    fun uploadNewFiles(
        context: Context,
        account: Account,
        localPaths: Array<String>,
        remotePaths: Array<String?>,
        mimeTypes: Array<String>?,
        behaviour: Int?,
        createRemoteFolder: Boolean?,
        createdBy: Int
    ) {
        val intent = Intent(context, FileUploader::class.java)

        intent.putExtra(FileUploader.KEY_ACCOUNT, account)
        intent.putExtra(FileUploader.KEY_LOCAL_FILE, localPaths)
        intent.putExtra(FileUploader.KEY_REMOTE_FILE, remotePaths)
        intent.putExtra(FileUploader.KEY_MIME_TYPE, mimeTypes)
        intent.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, behaviour)
        intent.putExtra(FileUploader.KEY_CREATE_REMOTE_FOLDER, createRemoteFolder)
        intent.putExtra(FileUploader.KEY_CREATED_BY, createdBy)

        if ((createdBy == CREATED_AS_CAMERA_UPLOAD_PICTURE || createdBy == CREATED_AS_CAMERA_UPLOAD_VIDEO) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Since in Android O the apps in background are not allowed to start background
            // services and camera uploads feature may try to do it, this is the way to proceed
            Log_OC.d(
                TAG,
                "Start to upload some files from foreground/background, " + "startForeground() will be called soon"
            )
            context.startForegroundService(intent)
        } else {
            Log_OC.d(TAG, "Start to upload some files from foreground")
            context.startService(intent)
        }
    }

    /**
     * Call to upload a new single file
     */
    fun uploadNewFile(
        context: Context,
        account: Account,
        localPath: String,
        remotePath: String,
        behaviour: Int,
        mimeType: String,
        createRemoteFile: Boolean,
        createdBy: Int
    ) {

        uploadNewFiles(
            context,
            account,
            arrayOf(localPath),
            arrayOf(remotePath),
            arrayOf(mimeType),
            behaviour,
            createRemoteFile,
            createdBy
        )
    }

    /**
     * Call to update multiple files already uploaded
     */
    private fun uploadsUpdate(
        context: Context, account: Account, existingFiles: Array<OCFile>, behaviour: Int?,
        forceOverwrite: Boolean?, requestedFromAvOfflineJobService: Boolean
    ) {
        val intent = Intent(context, FileUploader::class.java)

        intent.putExtra(FileUploader.KEY_ACCOUNT, account)
        intent.putExtra(FileUploader.KEY_FILE, existingFiles)
        intent.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, behaviour)
        intent.putExtra(FileUploader.KEY_FORCE_OVERWRITE, forceOverwrite)

        // Since in Android O and above the apps in background are not allowed to start background
        // services and available offline feature may try to do it, this is the way to proceed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && requestedFromAvOfflineJobService) {
            intent.putExtra(FileUploader.KEY_IS_AVAILABLE_OFFLINE_FILE, true)
            Log_OC.d(
                TAG,
                "Start to upload some already uploaded files from foreground/background, " + "startForeground() will be called soon"
            )
            context.startForegroundService(intent)
        } else {
            Log_OC.d(TAG, "Start to upload some already uploaded files from foreground")
            context.startService(intent)
        }
    }

    /**
     * Call to update a dingle file already uploaded
     */
    fun uploadUpdate(
        context: Context, account: Account, existingFile: OCFile, behaviour: Int?,
        forceOverwrite: Boolean?, requestedFromAvOfflineJobService: Boolean
    ) {

        uploadsUpdate(
            context, account, arrayOf(existingFile), behaviour, forceOverwrite,
            requestedFromAvOfflineJobService
        )
    }

    /**
     * Call to retry upload identified by remotePath
     */
    fun retry(context: Context?, upload: OCUpload?, requestedFromWifiBackEvent: Boolean) {
        if (upload != null && context != null) {
            val account = AccountUtils.getOwnCloudAccountByName(
                context,
                upload.accountName
            )
            retry(context, account, upload, requestedFromWifiBackEvent)

        } else {
            throw IllegalArgumentException("Null parameter!")
        }
    }

    /**
     * Retry a subset of all the stored failed uploads.
     *
     * @param context           Caller [Context]
     * @param account           If not null, only failed uploads to this OC account will be retried; otherwise,
     * uploads of all accounts will be retried.
     * @param uploadResult      If not null, only failed uploads with the result specified will be retried;
     * otherwise, failed uploads due to any result will be retried.
     * @param requestedFromWifiBackEvent  true if the retry was requested because wifi connection was back,
     * false otherwise
     */
    fun retryFailedUploads(
        context: Context, account: Account?, uploadResult: UploadResult?,
        requestedFromWifiBackEvent: Boolean
    ) {
        val uploadsStorageManager = UploadsStorageManager(context.contentResolver)
        val failedUploads = uploadsStorageManager.failedUploads
        var currentAccountFailed: Account? = null
        var resultMatch: Boolean
        var accountMatch: Boolean
        failedUploads.forEach {
            accountMatch = account == null || account.name == it.accountName
            resultMatch = uploadResult == null || uploadResult == it.lastResult
            if (accountMatch && resultMatch) {
                if (currentAccountFailed == null || currentAccountFailed!!.name != it.accountName) {
                    currentAccountFailed = it.getAccount(context)
                }
                retry(context, currentAccountFailed, it, requestedFromWifiBackEvent)
            }
        }
    }

    /**
     * Private implementation of retry.
     *
     * @param context           Caller [Context]
     * @param account           OC account where the upload will be retried.
     * @param upload            Persisted upload to retry.
     * @param requestedFromWifiBackEvent true if the retry was requested because wifi connection was back,
     * false otherwise
     */
    private fun retry(
        context: Context,
        account: Account?,
        upload: OCUpload?,
        requestedFromWifiBackEvent: Boolean
    ) {
        if (upload != null) {
            val intent = Intent(context, FileUploader::class.java)
            intent.putExtra(FileUploader.KEY_RETRY, true)
            intent.putExtra(FileUploader.KEY_ACCOUNT, account)
            intent.putExtra(FileUploader.KEY_RETRY_UPLOAD, upload)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && (upload.createdBy == CREATED_AS_CAMERA_UPLOAD_PICTURE || upload.createdBy == CREATED_AS_CAMERA_UPLOAD_VIDEO ||
                        requestedFromWifiBackEvent)
            ) {
                // Since in Android O the apps in background are not allowed to start background
                // services and camera uploads feature may try to do it, this is the way to proceed
                if (requestedFromWifiBackEvent) {
                    intent.putExtra(FileUploader.KEY_REQUESTED_FROM_WIFI_BACK_EVENT, true)
                }
                Log_OC.d(
                    TAG,
                    "Retry some uploads from foreground/background, " + "startForeground() will be called soon"
                )
                context.startForegroundService(intent)
            } else {
                Log_OC.d(TAG, "Retry some uploads from foreground")
                context.startService(intent)
            }
        }
    }

    /**
     * Return 'true' when conditions for a scheduled retry are met.
     *
     * @param context       Caller [Context]
     * @return              'true' when conditions for a scheduled retry are met, 'false' otherwise.
     */
    fun shouldScheduleRetry(context: Context, exception: Exception): Boolean {
        return !ConnectivityUtils.isNetworkActive(context) ||
                PowerUtils.isDeviceIdle(context) ||
                exception is SocketTimeoutException // TODO check if exception is the same in HTTP server
    }

    /**
     * Schedule a future retry of an upload, to be done when a connection via an unmetered network (free Wifi)
     * is available.
     *
     * @param context           Caller [Context].
     * @param jobId             Identifier to set to the retry job.
     * @param accountName       Local name of the OC account where the upload will be retried.
     * @param remotePath        Full path of the file to upload, relative to root of the OC account.
     */
    fun scheduleUpload(
        context: Context,
        jobId: Int,
        accountName: String,
        remotePath: String
    ) {
        val scheduled = scheduleTransfer(
            context,
            RetryUploadJobService::class.java,
            jobId,
            accountName,
            remotePath
        )

        if (scheduled) {
            Log_OC.d(
                TAG,
                String.format(
                    "Scheduled upload retry for %1s in %2s",
                    remotePath,
                    accountName
                )
            )
        }
    }

    /**
     * Schedule a future retry of a download, to be done when a connection via an unmetered network (free Wifi)
     * is available.
     *
     * @param context           Caller [Context].
     * @param jobId             Identifier to set to the retry job.
     * @param accountName       Local name of the OC account where the download will be retried.
     * @param remotePath        Full path of the file to download, relative to root of the OC account.
     */
    fun scheduleDownload(
        context: Context,
        jobId: Int,
        accountName: String,
        remotePath: String
    ) {
        val scheduled = scheduleTransfer(
            context,
            RetryDownloadJobService::class.java,
            jobId,
            accountName,
            remotePath
        )

        if (scheduled) {
            Log_OC.d(
                TAG,
                String.format(
                    "Scheduled download retry for %1s in %2s",
                    remotePath,
                    accountName
                )
            )
        }
    }

    /**
     * Schedule a future transfer of an upload, to be done when a connection via an unmetered network (free Wifi)
     * is available.
     *
     * @param context                   Caller [Context].
     * @param scheduledRetryService     Class of the appropriate retry service, either to retry downloads
     * or to retry uploads.
     * @param jobId                     Identifier to set to the retry job.
     * @param accountName               Local name of the OC account where the upload will be retried.
     * @param remotePath                Full path of the file to upload, relative to root of the OC account.
     */
    private fun scheduleTransfer(
        context: Context,
        scheduledRetryService: Class<*>,
        jobId: Int,
        accountName: String,
        remotePath: String
    ): Boolean {

        // JobShceduler requires Android >= 5.0 ; do not remove this protection while minSdkVersion is lower
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false
        }

        val serviceComponent = ComponentName(
            context,
            scheduledRetryService
        )

        val builder = JobInfo.Builder(jobId, serviceComponent)

        val networkType = getRequiredNetworkType(context, accountName, remotePath)

        // require network type (Wifi or Wifi and cellular)
        builder.setRequiredNetworkType(networkType)

        // Persist job and prevent it from being deleted after a device restart
        builder.setPersisted(true)

        // Extra data
        val extras = PersistableBundle()
        extras.putString(Extras.EXTRA_REMOTE_PATH, remotePath)
        extras.putString(Extras.EXTRA_ACCOUNT_NAME, accountName)
        builder.setExtras(extras)

        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(builder.build())

        return true
    }

    /**
     * Retrieve the type of network connection required to schedule the last upload for an account
     * @param context
     * @param accountName
     * @param remotePath to upload the file
     * @return 2 if only wifi is required, 1 if any internet connection is required (wifi or cellular)
     */
    private fun getRequiredNetworkType(
        context: Context,
        accountName: String,
        remotePath: String
    ): Int {

        val uploadsStorageManager = UploadsStorageManager(context.contentResolver)

        // Get last upload to be retried
        val ocUpload = uploadsStorageManager.getLastUploadFor(OCFile(remotePath), accountName)

        val mConfig = PreferenceManager.getCameraUploadsConfiguration(context)

        // Wifi by default
        var networkType = JobInfo.NETWORK_TYPE_UNMETERED

        if (ocUpload != null && (ocUpload.createdBy == CREATED_AS_CAMERA_UPLOAD_PICTURE && !mConfig.isWifiOnlyForPictures || ocUpload.createdBy == CREATED_AS_CAMERA_UPLOAD_VIDEO && !mConfig.isWifiOnlyForVideos)) {
            // Wifi or cellular
            networkType = JobInfo.NETWORK_TYPE_ANY
        }

        return networkType
    }

    companion object {

        private val TAG = TransferRequester::class.java.name
    }
}
