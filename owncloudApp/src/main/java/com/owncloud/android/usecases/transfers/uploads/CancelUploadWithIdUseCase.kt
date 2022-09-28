/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.usecases.transfers.uploads

import androidx.work.WorkManager
import com.owncloud.android.data.storage.LocalStorageProvider
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.transfers.TransferRepository
import com.owncloud.android.domain.transfers.model.OCTransfer
import timber.log.Timber
import java.io.File

class CancelUploadWithIdUseCase(
    private val workManager: WorkManager,
    private val transferRepository: TransferRepository,
    private val localStorageProvider: LocalStorageProvider,
) : BaseUseCase<Unit, CancelUploadWithIdUseCase.Params>() {

    override fun run(params: Params) {
        workManager.cancelAllWorkByTag(params.uploadId.toString())

        val transfer = transferRepository.getTransferById(params.uploadId)
        deleteCacheIfNeeded(transfer!!)

        transferRepository.removeTransferById(params.uploadId)

        Timber.i("Upload with id ${params.uploadId} has been cancelled.")
    }

    private fun deleteCacheIfNeeded(transfer: OCTransfer) {
        val cacheDir = localStorageProvider.getTemporalPath(transfer.accountName)
        if (transfer.localPath.startsWith(cacheDir)) {
            val cacheFile = File(transfer.localPath)
            cacheFile.delete()
        }
    }

    data class Params(
        val uploadId: Long,
    )
}
