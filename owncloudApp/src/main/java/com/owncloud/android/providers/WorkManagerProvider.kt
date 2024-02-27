/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
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

package com.owncloud.android.providers

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.owncloud.android.extensions.getRunningWorkInfosLiveData
import com.owncloud.android.workers.AccountDiscoveryWorker
import com.owncloud.android.workers.AvailableOfflinePeriodicWorker
import com.owncloud.android.workers.AvailableOfflinePeriodicWorker.Companion.AVAILABLE_OFFLINE_PERIODIC_WORKER
import com.owncloud.android.workers.CameraUploadsWorker
import com.owncloud.android.workers.OldLogsCollectorWorker
import com.owncloud.android.workers.RemoveLocallyFilesWithLastUsageOlderThanGivenTimeWorker
import com.owncloud.android.workers.UploadFileFromContentUriWorker
import com.owncloud.android.workers.UploadFileFromFileSystemWorker

class WorkManagerProvider(
    val context: Context
) {
    fun enqueueCameraUploadsWorker() {
        val cameraUploadsWorker = PeriodicWorkRequestBuilder<CameraUploadsWorker>(
            repeatInterval = CameraUploadsWorker.repeatInterval,
            repeatIntervalTimeUnit = CameraUploadsWorker.repeatIntervalTimeUnit
        ).addTag(CameraUploadsWorker.CAMERA_UPLOADS_WORKER)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(CameraUploadsWorker.CAMERA_UPLOADS_WORKER, ExistingPeriodicWorkPolicy.KEEP, cameraUploadsWorker)
    }

    fun enqueueOldLogsCollectorWorker() {
        val constraintsRequired = Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()

        val oldLogsCollectorWorker = PeriodicWorkRequestBuilder<OldLogsCollectorWorker>(
            repeatInterval = OldLogsCollectorWorker.repeatInterval,
            repeatIntervalTimeUnit = OldLogsCollectorWorker.repeatIntervalTimeUnit
        )
            .addTag(OldLogsCollectorWorker.OLD_LOGS_COLLECTOR_WORKER)
            .setConstraints(constraintsRequired)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(OldLogsCollectorWorker.OLD_LOGS_COLLECTOR_WORKER, ExistingPeriodicWorkPolicy.UPDATE, oldLogsCollectorWorker)
    }

    fun enqueueAvailableOfflinePeriodicWorker() {
        val constraintsRequired = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val availableOfflinePeriodicWorker = PeriodicWorkRequestBuilder<AvailableOfflinePeriodicWorker>(
            repeatInterval = AvailableOfflinePeriodicWorker.repeatInterval,
            repeatIntervalTimeUnit = AvailableOfflinePeriodicWorker.repeatIntervalTimeUnit
        )
            .addTag(AVAILABLE_OFFLINE_PERIODIC_WORKER)
            .setConstraints(constraintsRequired)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(AVAILABLE_OFFLINE_PERIODIC_WORKER, ExistingPeriodicWorkPolicy.KEEP, availableOfflinePeriodicWorker)
    }

    fun enqueueAccountDiscovery(accountName: String) {
        val constraintsRequired = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val inputData = workDataOf(
            AccountDiscoveryWorker.KEY_PARAM_DISCOVERY_ACCOUNT to accountName,
        )

        val accountDiscoveryWorker = OneTimeWorkRequestBuilder<AccountDiscoveryWorker>()
            .setInputData(inputData)
            .addTag(accountName)
            .setConstraints(constraintsRequired)
            .build()

        WorkManager.getInstance(context).enqueue(accountDiscoveryWorker)
    }

    fun enqueueRemoveLocallyFilesWithLastUsageOlderThanGivenTimeWorker() {
        val constraintsRequired = Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()

        val removeLocallyFilesWithLastUsageOlderThanGivenTimeWorker =
            PeriodicWorkRequestBuilder<RemoveLocallyFilesWithLastUsageOlderThanGivenTimeWorker>(
                repeatInterval = RemoveLocallyFilesWithLastUsageOlderThanGivenTimeWorker.repeatInterval,
                repeatIntervalTimeUnit = RemoveLocallyFilesWithLastUsageOlderThanGivenTimeWorker.repeatIntervalTimeUnit
            )
                .addTag(RemoveLocallyFilesWithLastUsageOlderThanGivenTimeWorker.DELETE_FILES_OLDER_GIVEN_TIME_WORKER)
                .setConstraints(constraintsRequired)
                .build()

        WorkManager.getInstance(context).enqueue(removeLocallyFilesWithLastUsageOlderThanGivenTimeWorker)
    }

    fun getRunningUploadsWorkInfosLiveData(): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context).getRunningWorkInfosLiveData(
            listOf(
                UploadFileFromContentUriWorker::class.java.name,
                UploadFileFromFileSystemWorker::class.java.name
            )
        )
    }

    fun cancelAllWorkByTag(tag: String) = WorkManager.getInstance(context).cancelAllWorkByTag(tag)

}
