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
package com.owncloud.android.extensions

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import com.owncloud.android.workers.DownloadFileWorker.Companion.WORKER_KEY_PROGRESS

fun LiveData<WorkInfo?>.observeWorkerTillItFinishes(
    owner: LifecycleOwner,
    onWorkEnqueued: () -> Unit = {},
    onWorkRunning: (progress: Int) -> Unit,
    onWorkSucceeded: () -> Unit,
    onWorkFailed: () -> Unit,
    onWorkBlocked: () -> Unit = {},
    onWorkCancelled: () -> Unit = {},
    removeObserverAfterNull: Boolean = true,
) {
    observe(owner, object : Observer<WorkInfo?> {
        override fun onChanged(value: WorkInfo?) {
            if (value == null) {
                if (removeObserverAfterNull) {
                    removeObserver(this)
                }
                return
            }

            if (value.state.isFinished) {
                removeObserver(this)
            }
            when (value.state) {
                WorkInfo.State.ENQUEUED -> onWorkEnqueued()
                WorkInfo.State.RUNNING -> onWorkRunning(value.progress.getInt(WORKER_KEY_PROGRESS, -1))
                WorkInfo.State.SUCCEEDED -> onWorkSucceeded()
                WorkInfo.State.FAILED -> onWorkFailed()
                WorkInfo.State.BLOCKED -> onWorkBlocked()
                WorkInfo.State.CANCELLED -> onWorkCancelled()
            }
        }
    })
}
