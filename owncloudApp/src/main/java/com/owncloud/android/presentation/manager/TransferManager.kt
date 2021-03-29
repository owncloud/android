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
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State.BLOCKED
import androidx.work.WorkInfo.State.CANCELLED
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.WorkInfo.State.RUNNING
import androidx.work.WorkInfo.State.SUCCEEDED
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.extensions.transformToObserveJustLastWork
import com.owncloud.android.providers.impl.TransferProviderImpl
import timber.log.Timber
import java.util.UUID

class TransferManager(
    private val context: Context
) {

    private val transferProvider = TransferProviderImpl(context)

    /**
     * Enqueue a new download and return its uuid.
     * You can check and observe its progress using
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

    fun getLiveDataForDownloadingFile(account: Account, file: OCFile): LiveData<MutableList<WorkInfo>> {
        val workQuery = WorkQuery.Builder
            .fromTags(listOf(TRANSFER_TAG_DOWNLOAD, file.id.toString(), account.name))
            .addStates(listOf(ENQUEUED, RUNNING, BLOCKED))
            .build()

        return getWorkManager().getWorkInfosLiveData(workQuery)
    }

    fun getLiveDataForDownloadingFromAccount(account: Account): LiveData<MutableList<WorkInfo>> {
        val workQuery = WorkQuery.Builder
            .fromTags(listOf(TRANSFER_TAG_DOWNLOAD, account.name))
            .addStates(listOf(SUCCEEDED, CANCELLED, BLOCKED))
            .build()

        return getWorkManager().getWorkInfosLiveData(workQuery)
    }

    private fun isDownloadAlreadyEnqueued(account: Account, file: OCFile): Boolean {
        val workQuery = WorkQuery.Builder
            .fromTags(listOf(TRANSFER_TAG_DOWNLOAD, file.id.toString(), account.name))
            .addStates(listOf(ENQUEUED, RUNNING, BLOCKED))
            .build()

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

    private fun getWorkInfoFromTags(vararg tags: String): List<WorkInfo> {
        val workers = getWorkManager()
            .getWorkInfos(
                WorkQuery.Builder.fromTags(tags.toList()).build()
            ).get()
        return workers.filter { it.tags.containsAll(tags.toList()) }
    }

    fun cancelWorkersForAccount(account: Account) {
        getWorkManager().cancelAllWorkByTag(account.name)
    }

    fun isDownloadPending(account: Account, file: OCFile) =
        getWorkInfoFromTags(TRANSFER_TAG_DOWNLOAD, file.id.toString(), account.name).any { !it.state.isFinished }

    fun cancelDownloadForFile(file: OCFile) {
        val workersToCancel = getWorkInfoFromTags(TRANSFER_TAG_DOWNLOAD, file.id.toString(), file.owner)
        workersToCancel.forEach {
            getWorkManager().cancelWorkById(it.id)
        }
    }

    companion object {
        private const val MAXIMUM_NUMBER_OF_RETRIES = 3
    }
}
