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
package com.owncloud.android.usecases.transfers.downloads

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.extensions.PENDING_WORK_STATUS
import com.owncloud.android.extensions.buildWorkQuery
import com.owncloud.android.extensions.getTagsForDownload

/**
 * Get a LiveData with the last downloading work info.
 */
class GetLiveDataForDownloadingFileUseCase(
    private val workManager: WorkManager
) : BaseUseCase<LiveData<WorkInfo?>, GetLiveDataForDownloadingFileUseCase.Params>() {

    override fun run(params: Params): LiveData<WorkInfo?> {
        val tagsToFilter = getTagsForDownload(params.file, params.accountName)
        val workQuery = buildWorkQuery(
            tags = tagsToFilter,
            states = PENDING_WORK_STATUS
        )

        return workManager.getWorkInfosLiveData(workQuery).map { listOfDownloads ->
            listOfDownloads.firstOrNull { it.tags.containsAll(tagsToFilter) }
        }
    }

    data class Params(
        val accountName: String,
        val file: OCFile
    )
}
