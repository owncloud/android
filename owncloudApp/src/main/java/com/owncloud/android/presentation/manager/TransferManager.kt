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
import androidx.work.WorkInfo
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

    private fun isDownloadAlreadyEnqueued(account: Account, file: OCFile): Boolean {
        val downloadWorkersForFile = getWorkInfoFromTags(
            TRANSFER_TAG_DOWNLOAD,
            file.id.toString(),
            account.name
        )

        // Check if this download is in progress.
        var isEnqueued = false
        downloadWorkersForFile.forEach {
            if (!it.state.isFinished) {
                // Let's cancel a work if it has several retries and enqueue it again
                if (it.runAttemptCount > MAXIMUM_NUMBER_OF_RETRIES) {
                    getWorkManager().cancelWorkById(it.id)
                } else {
                    isEnqueued = true
                }
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

    companion object {
        private const val MAXIMUM_NUMBER_OF_RETRIES = 3
    }
}
