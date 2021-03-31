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
package com.owncloud.android.presentation.manager

import android.accounts.Account
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State.BLOCKED
import androidx.work.WorkInfo.State.CANCELLED
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.WorkInfo.State.RUNNING
import androidx.work.WorkInfo.State.SUCCEEDED
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.providers.impl.TransferProviderImpl
import timber.log.Timber
import java.util.UUID

class TransferManager(
    private val context: Context
) {

    private val transferProvider = TransferProviderImpl(context)

    /**
     * Enqueue a new download and return its uuid.
     * We can check and observe its progress using
     * @see WorkManager.getWorkInfoById and
     * @see WorkManager.getWorkInfoByIdLiveData
     */
    fun downloadFile(account: Account, file: OCFile): UUID? {
        if (file.id == null) return null

        if (isDownloadAlreadyEnqueued(account, file)) {
            return null
        }

        return transferProvider.downloadFile(account, file)
    }

    /**
     * Get a LiveData with the last downloading work info.
     */
    fun getLiveDataForDownloadingFile(account: Account, file: OCFile): LiveData<WorkInfo?> {
        val tagsToFilter = listOf(TRANSFER_TAG_DOWNLOAD, file.id.toString(), account.name)
        val workQuery = buildWorkQuery(
            tags = tagsToFilter,
            states = listOf(ENQUEUED, RUNNING, BLOCKED)
        )

        return Transformations.map(getWorkManager().getWorkInfosLiveData(workQuery)) { listOfDownloads ->
            listOfDownloads
                .asReversed()
                .distinctBy { it.tags }
                .firstOrNull { it.tags.containsAll(tagsToFilter) }
        }
    }

    fun getWorkInfoByIdLiveData(uuid: UUID): LiveData<WorkInfo?> = getWorkManager().getWorkInfoByIdLiveData(uuid)

    /**
     * Get a LiveData with the lasts downloads from an account
     */
    fun getLiveDataForFinishedDownloadsFromAccount(account: Account): LiveData<List<WorkInfo>> {
        val tagsToFilter = listOf(TRANSFER_TAG_DOWNLOAD, account.name)
        val workQuery = buildWorkQuery(
            tags = tagsToFilter,
            states = listOf(SUCCEEDED, CANCELLED, BLOCKED)
        )

        return Transformations.map(getWorkManager().getWorkInfosLiveData(workQuery)) { listOfDownloads ->
            listOfDownloads
                .asReversed()
                .distinctBy { it.tags }
                .filter { it.tags.containsAll(tagsToFilter) }
        }
    }

    private fun isDownloadAlreadyEnqueued(account: Account, file: OCFile): Boolean {
        val workQuery = buildWorkQuery(
            tags = listOf(TRANSFER_TAG_DOWNLOAD, file.id.toString(), account.name),
            states = listOf(ENQUEUED, RUNNING, BLOCKED),
        )

        val downloadWorkersForFile = getWorkManager().getWorkInfos(workQuery).get()

        var isEnqueued = false
        downloadWorkersForFile.forEach {
            // Let's cancel a work if it has several retries and enqueue it again
            if (it.runAttemptCount > MAXIMUM_NUMBER_OF_RETRIES) {
                getWorkManager().cancelWorkById(it.id)
            } else {
                isEnqueued = true
            }
        }

        if (isEnqueued) {
            Timber.i("Download of ${file.fileName} has not finished yet. Do not enqueue it again.")
        }

        return isEnqueued
    }

    private fun getWorkManager() = WorkManager.getInstance(context)

    /**
     * Get a list of WorkInfo that matches EVERY tag.
     */
    private fun getWorkInfoFromTags(vararg tags: String): List<WorkInfo> {
        val workers = getWorkManager()
            .getWorkInfos(
                buildWorkQuery(tags = tags.toList())
            ).get()
        return workers.filter { it.tags.containsAll(tags.toList()) }
    }

    /**
     * Take care with WorkQueries. It will return workers that match at least ONE of the tags.
     * If we perform a query with tags {"account@server", "2"}, [WorkManager.getWorkInfos] will return workers that
     * contains at least ONE of the tags, but not both of them. If we want workers that match every tag,
     * @see getWorkInfoFromTags
     */
    private fun buildWorkQuery(
        tags: List<String>,
        states: List<WorkInfo.State> = listOf(),
    ): WorkQuery = WorkQuery.Builder
        .fromTags(tags)
        .addStates(states)
        .build()

    /**
     * Check if a download is pending. It could be enqueued, downloading or blocked.
     * @param account - Owner of the file
     * @param file - File to check whether it is pending.
     *
     * @return true if the download is pending.
     */
    fun isDownloadPending(account: Account, file: OCFile): Boolean =
        getWorkInfoFromTags(TRANSFER_TAG_DOWNLOAD, file.id.toString(), account.name).any { !it.state.isFinished }

    /**
     * Cancel every pending download for an account. Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run.
     *
     * @param account
     */
    fun cancelDownloadForAccount(account: Account) {
        val workersToCancel = getWorkInfoFromTags(TRANSFER_TAG_DOWNLOAD, account.name)
        workersToCancel.forEach {
            getWorkManager().cancelWorkById(it.id)
        }
    }

    /**
     * Cancel every pending download for file. Note that cancellation is a best-effort
     * policy and work that is already executing may continue to run.
     *
     * @param file to cancel
     */
    fun cancelDownloadForFile(file: OCFile) {
        val workersToCancel = getWorkInfoFromTags(TRANSFER_TAG_DOWNLOAD, file.id.toString(), file.owner)
        workersToCancel.forEach {
            getWorkManager().cancelWorkById(it.id)
        }
    }

    companion object {
        private const val MAXIMUM_NUMBER_OF_RETRIES = 3

        // Temporary solution. Probably we won't need it.
        const val DOWNLOAD_ADDED_MESSAGE = "DOWNLOAD_ADDED"
        const val DOWNLOAD_FINISH_MESSAGE = "DOWNLOAD_FINISH"
    }
}
