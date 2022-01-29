/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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
import com.owncloud.android.data.storage.LocalStorageProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit

class OldLogsCollectorWorker(
    val appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
), KoinComponent {

    override suspend fun doWork(): Result {
        val logsDirectory = getLogsDirectory()
        val logsFiles = getLogsFiles(logsDirectory)

        return try {
            removeOldLogs(logsFiles)
            Result.success()
        } catch (ioException: IOException) {
            Result.failure()
        } catch (securityException: SecurityException) {
            Result.failure()
        }
    }

    private fun getLogsDirectory(): File {
        val localStorageProvider: LocalStorageProvider by inject()
        val logsPath = localStorageProvider.getLogsPath()
        return File(logsPath)
    }

    private fun getLogsFiles(logsFolder: File): List<File> {
        return logsFolder.listFiles()?.toList() ?: listOf()
    }

    private fun removeOldLogs(logFiles: List<File>) {
        logFiles.forEach { log ->
            if (log.lastModified() < getLastTimestampAllowed()) {
                Timber.i("Removing log: ${log.name}")
                log.delete()
            }
        }
    }

    private fun getLastTimestampAllowed(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -numberOfDaysToKeepLogs)

        return calendar.timeInMillis
    }

    companion object {
        const val OLD_LOGS_COLLECTOR_WORKER = "OLD_LOGS_COLLECTOR_WORKER"
        const val repeatInterval: Long = 7L
        val repeatIntervalTimeUnit: TimeUnit = TimeUnit.DAYS
        private const val numberOfDaysToKeepLogs = 7
    }
}
