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
package com.owncloud.android.presentation.manager

import android.accounts.Account
import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.workDataOf
import com.google.common.util.concurrent.ListenableFuture
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.workers.DownloadFileWorker
import timber.log.Timber
import java.util.UUID

class TransferManager(
    private val context: Context
) {

    /**
     * Enqueue a new download and return its uuid.
     * You can check and observe its progress using
     * @see WorkManager.getWorkInfoById and
     * @see WorkManager.getWorkInfoByIdLiveData
     */
    fun downloadFile(account: Account, file: OCFile): UUID? {
        if (file.id == null) return null

        val downloadWorkersForFile = getWorkInfoFromTags(
            TRANSFER_TAG_DOWNLOAD,
            file.id.toString()
        ).get()

        // Check if this download is in progress.
        var isEnqueued = false
        downloadWorkersForFile.forEach {
            Timber.d(it.toString())
            if (!it.state.isFinished) {
                isEnqueued = true
            }
        }

        if (isEnqueued) {
            Timber.i("Download of ${file.fileName} has not finished yet. Do not enqueue it again.")
            return null
        }

        val inputData = workDataOf(
            DownloadFileWorker.KEY_PARAM_ACCOUNT to account.name,
            DownloadFileWorker.KEY_PARAM_FILE_ID to file.id
        )

        val downloadFileWork = OneTimeWorkRequestBuilder<DownloadFileWorker>()
            .setInputData(inputData)
            .addTag(file.id.toString())
            .addTag(TRANSFER_TAG_DOWNLOAD)
            .build()

        getWorkManager().enqueue(downloadFileWork)
        Timber.i("Download of ${file.fileName} has been enqueued.")

        return downloadFileWork.id
    }

    private fun getWorkInfoFromTags(vararg tags: String): ListenableFuture<MutableList<WorkInfo>> {
        return getWorkManager()
            .getWorkInfos(
                WorkQuery.Builder.fromTags(tags.toList()).build()
            )
    }

    private fun getWorkManager() = WorkManager.getInstance(context)
}
