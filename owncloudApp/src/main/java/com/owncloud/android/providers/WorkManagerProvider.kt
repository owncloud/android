/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.owncloud.android.workers.CameraUploadsWorker

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
}
