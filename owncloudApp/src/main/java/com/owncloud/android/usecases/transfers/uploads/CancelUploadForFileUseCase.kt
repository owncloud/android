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

package com.owncloud.android.usecases.transfers.uploads

import androidx.work.WorkManager
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.transfers.TransferRepository
import com.owncloud.android.extensions.getWorkInfoByTags
import timber.log.Timber

/**
 * Cancel every pending upload for file. Note that cancellation is a best-effort
 * policy and work that is already executing may continue to run.
 */
class CancelUploadForFileUseCase(
    private val workManager: WorkManager,
    private val transferRepository: TransferRepository,
) : BaseUseCase<Unit, CancelUploadForFileUseCase.Params>() {

    override fun run(params: Params) {
        val file = params.file

        // Check if there are pending uploads for this file.
        // FirstOrNull because it should not be 2 uploads with same owner and remote path at the same time
        val uploadForFile = transferRepository.getCurrentAndPendingTransfers().firstOrNull {
            file.owner == it.accountName && file.remotePath == it.remotePath
        }

        if (uploadForFile == null) {
            Timber.w("Didn't found any pending upload to cancel for file ${file.remotePath} and owner ${file.owner}")
            return
        }

        val workersToCancel =
            workManager.getWorkInfoByTags(listOf(uploadForFile.id.toString(), file.owner))

        workersToCancel.forEach {
            // TODO: We need to check if the work is cancelled before finishing.
            //  Otherwise, the database would be updated by the worker.
            //  It should be a way to check within the worker that a cancellation has been triggered.
            //  In that case, we would update the database there.
            workManager.cancelWorkById(it.id)
        }
    }

    data class Params(
        val file: OCFile
    )
}
