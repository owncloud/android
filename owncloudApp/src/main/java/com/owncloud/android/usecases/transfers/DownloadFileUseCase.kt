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
package com.owncloud.android.usecases.transfers

import android.accounts.Account
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.extensions.PENDING_WORK_STATUS
import com.owncloud.android.extensions.buildWorkQuery
import com.owncloud.android.extensions.getTagsForDownload
import com.owncloud.android.presentation.manager.MAXIMUM_NUMBER_OF_RETRIES
import com.owncloud.android.presentation.manager.TRANSFER_TAG_DOWNLOAD
import com.owncloud.android.workers.DownloadFileWorker
import timber.log.Timber
import java.util.UUID

/**
 * We will use [WorkManager] to perform downloads.
 * We will send the file Id and the owner account as parameters. We cannot send complex objects as parameters.
 * The worker will retrieve the file from database and the account from the account manager to make it work.
 * The worker will have 3 tags. FileId, Account and the operation TAG.
 * This is helpful if we want to cancel or observe enqueued workers when an account, of a file is removed.
 * In that case, we will cancel every worker with that TAG
 *
 * @return UUID - UUID for the enqueued worker. It is important if we want to observe its process.
 */
class DownloadFileUseCase(
    private val workManager: WorkManager
) : BaseUseCase<UUID?, DownloadFileUseCase.Params>() {

    override fun run(params: Params): UUID? {
        val ocFile = params.file
        val account = params.account

        if (ocFile.id == null) return null

        if (isDownloadAlreadyEnqueued(account, ocFile)) {
            return null
        }

        return enqueueNewDownload(ocFile, account.name)
    }

    private fun isDownloadAlreadyEnqueued(account: Account, file: OCFile): Boolean {
        val tagsToFilter = getTagsForDownload(file, account)
        val workQuery = buildWorkQuery(
            tags = tagsToFilter,
            states = PENDING_WORK_STATUS,
        )

        val downloadWorkersForFile =
            workManager.getWorkInfos(workQuery).get().filter { it.tags.containsAll(tagsToFilter) }

        var isEnqueued = false
        downloadWorkersForFile.forEach {
            // Let's cancel a work if it has several retries and enqueue it again
            if (it.runAttemptCount > MAXIMUM_NUMBER_OF_RETRIES) {
                workManager.cancelWorkById(it.id)
            } else {
                isEnqueued = true
            }
        }

        if (isEnqueued) {
            Timber.i("Download of ${file.fileName} has not finished yet. Do not enqueue it again.")
        }

        return isEnqueued
    }

    private fun enqueueNewDownload(ocFile: OCFile, accountName: String): UUID {
        val inputData = workDataOf(
            DownloadFileWorker.KEY_PARAM_ACCOUNT to accountName,
            DownloadFileWorker.KEY_PARAM_FILE_ID to ocFile.id
        )

        val downloadFileWork = OneTimeWorkRequestBuilder<DownloadFileWorker>()
            .setInputData(inputData)
            .addTag(ocFile.id.toString())
            .addTag(accountName)
            .addTag(TRANSFER_TAG_DOWNLOAD)
            .build()

        workManager.enqueue(downloadFileWork)
        Timber.i("Download of ${ocFile.fileName} has been enqueued.")

        return downloadFileWork.id
    }

    data class Params(
        val account: Account,
        val file: OCFile
    )
}
