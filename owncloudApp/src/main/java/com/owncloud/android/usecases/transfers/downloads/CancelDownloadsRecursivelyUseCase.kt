/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.usecases.transfers.downloads

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFolderContentUseCase
import com.owncloud.android.extensions.getWorkInfoByTags
import com.owncloud.android.workers.DownloadFileWorker
import timber.log.Timber

/**
 * Cancel every pending download for a file or folder and all its content.
 * Note that cancellation is a best-effort policy and work that is
 * already executing may continue to run.
 */
class CancelDownloadsRecursivelyUseCase(
    private val workManager: WorkManager,
    private val getFolderContentUseCase: GetFolderContentUseCase,
) : BaseUseCase<Unit, CancelDownloadsRecursivelyUseCase.Params>() {

    private lateinit var downloadsWorkInfos: List<WorkInfo>

    override fun run(params: Params) {
        downloadsWorkInfos = workManager.getWorkInfoByTags(
            listOf(
                params.accountName,
                DownloadFileWorker::class.java.name
            )
        )

        val files = params.files
        files.forEach { file ->
            cancelRecursively(file)
        }
    }

    private fun cancelRecursively(file: OCFile) {
        if (file.isFolder) {
            val result = getFolderContentUseCase(GetFolderContentUseCase.Params(file.id!!))
            val files = result.getDataOrNull()
            files?.forEach { fileInFolder ->
                cancelRecursively(fileInFolder)
            }
        } else {
            val workersToCancel = downloadsWorkInfos.filter { it.tags.contains(file.id.toString()) }

            workersToCancel.forEach {
                workManager.cancelWorkById(it.id)
                Timber.i("Download with id ${file.id} has been cancelled.")
            }
        }
    }

    data class Params(
        val files: List<OCFile>,
        val accountName: String
    )
}
