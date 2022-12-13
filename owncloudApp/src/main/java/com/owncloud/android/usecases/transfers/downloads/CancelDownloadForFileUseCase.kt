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

package com.owncloud.android.usecases.transfers.downloads

import androidx.work.WorkManager
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.extensions.getWorkInfoByTags
import com.owncloud.android.workers.DownloadFileWorker
import timber.log.Timber

/**
 * Cancel every pending download for file. Note that cancellation is a best-effort
 * policy and work that is already executing may continue to run.
 */
class CancelDownloadForFileUseCase(
    private val workManager: WorkManager
) : BaseUseCase<Unit, CancelDownloadForFileUseCase.Params>() {

    override fun run(params: Params) {
        val file = params.file

        val workersToCancel = workManager.getWorkInfoByTags(
            listOf(
                file.id.toString(),
                file.owner,
                DownloadFileWorker::class.java.name,
            )
        )

        workersToCancel.forEach {
            workManager.cancelWorkById(it.id)
            Timber.i("Download with id ${file.id} has been cancelled.")
        }
    }

    data class Params(
        val file: OCFile
    )
}
