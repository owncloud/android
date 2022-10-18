/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

import android.accounts.Account
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.usecases.transfers.TRANSFER_TAG_DOWNLOAD

val PENDING_WORK_STATUS = listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED)
val FINISHED_WORK_STATUS = listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED, WorkInfo.State.CANCELLED)

/**
 * Get a list of WorkInfo that matches EVERY tag.
 */
fun WorkManager.getWorkInfoByTags(tags: List<String>): List<WorkInfo> =
    this.getWorkInfos(buildWorkQuery(tags = tags)).get().filter { it.tags.containsAll(tags) }

/**
 * Get a list of WorkInfo of running workers that matches EVERY tag.
 */
fun WorkManager.getRunningWorkInfosByTags(tags: List<String>): List<WorkInfo> {
    return getWorkInfos(buildWorkQuery(tags = tags, states = listOf(WorkInfo.State.RUNNING))).get().filter { it.tags.containsAll(tags) }
}

/**
 * Get a list of WorkInfo of running workers as LiveData that matches at least one of the tags.
 */
fun WorkManager.getRunningWorkInfosLiveData(tags: List<String>): LiveData<List<WorkInfo>> {
    return getWorkInfosLiveData(buildWorkQuery(tags = tags, states = listOf(WorkInfo.State.RUNNING)))
}

/**
 * Check if a download is pending. It could be enqueued, downloading or blocked.
 * @param account - Owner of the file
 * @param file - File to check whether it is pending.
 *
 * @return true if the download is pending.
 */
fun WorkManager.isDownloadPending(account: Account, file: OCFile): Boolean =
    this.getWorkInfoByTags(getTagsForDownload(file, account.name)).any { !it.state.isFinished }

/**
 * Check if an upload is pending. It could be enqueued, uploading or blocked.
 * @param account - Owner of the file
 * @param file - File to check whether it is pending.
 *
 * @return true if the download is pending.
 */
fun WorkManager.isUploadPending(account: Account, file: OCFile): Boolean = false
// TODO: Uploads work different from downloads. So that, this one will be different.

fun getTagsForDownload(file: OCFile, accountName: String) =
    listOf(TRANSFER_TAG_DOWNLOAD, file.id.toString(), accountName)

/**
 * Take care with WorkQueries. It will return workers that match at least ONE of the tags.
 * If we perform a query with tags {"account@server", "2"}, [WorkManager.getWorkInfos] will return workers that
 * contains at least ONE of the tags, but not both of them. If we want workers that match every tag,
 * @see getWorkInfoByTags
 */
fun buildWorkQuery(
    tags: List<String>,
    states: List<WorkInfo.State> = listOf(),
): WorkQuery = WorkQuery.Builder
    .fromTags(tags)
    .addStates(states)
    .build()
