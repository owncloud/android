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

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.owncloud.android.domain.availableoffline.usecases.GetFilesAvailableOfflineFromEveryAccountUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.usecases.synchronization.SynchronizeFileUseCase
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class AvailableOfflinePeriodicWorker(
    val appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
), KoinComponent {

    private val getFilesAvailableOfflineFromEveryAccountUseCase: GetFilesAvailableOfflineFromEveryAccountUseCase by inject()
    private val synchronizeFileUseCase: SynchronizeFileUseCase by inject()
    private val synchronizeFolderUseCase: SynchronizeFolderUseCase by inject()

    override suspend fun doWork(): Result {

        return try {
            val availableOfflineFiles = getFilesAvailableOfflineFromEveryAccountUseCase(Unit)
            Timber.i("Available offline files that needs to be synced: ${availableOfflineFiles.size}")

            syncAvailableOfflineFiles(availableOfflineFiles)

            Result.success()
        } catch (exception: Exception) {
            Result.failure()
        }
    }

    private fun syncAvailableOfflineFiles(availableOfflineFiles: List<OCFile>) {
        availableOfflineFiles.forEach {
            if (it.isFolder) {
                synchronizeFolderUseCase(
                    SynchronizeFolderUseCase.Params(
                        remotePath = it.remotePath,
                        accountName = it.owner,
                        spaceId = it.spaceId,
                        syncMode = SynchronizeFolderUseCase.SyncFolderMode.SYNC_FOLDER_RECURSIVELY
                    )
                )
            } else {
                synchronizeFileUseCase(SynchronizeFileUseCase.Params(it))
            }
        }
    }

    companion object {
        const val AVAILABLE_OFFLINE_PERIODIC_WORKER = "AVAILABLE_OFFLINE_PERIODIC_WORKER"
        const val repeatInterval: Long = 15L
        val repeatIntervalTimeUnit: TimeUnit = TimeUnit.MINUTES
    }
}
