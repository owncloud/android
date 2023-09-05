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
import com.owncloud.android.data.providers.LocalStorageProvider
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.transfers.TransferRepository
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.extensions.getWorkInfoByTags
import com.owncloud.android.workers.UploadFileFromContentUriWorker
import com.owncloud.android.workers.UploadFileFromFileSystemWorker
import timber.log.Timber

class CancelUploadUseCase(
    private val workManager: WorkManager,
    private val transferRepository: TransferRepository,
    private val localStorageProvider: LocalStorageProvider,
) : BaseUseCase<Unit, CancelUploadUseCase.Params>() {

    override fun run(params: Params) {
        val upload = params.upload

        val workersFromContentUriToCancel = workManager.getWorkInfoByTags(
            listOf(
                upload.id.toString(),
                upload.accountName,
                UploadFileFromContentUriWorker::class.java.name
            )
        )

        val workersFromFileSystemToCancel = workManager.getWorkInfoByTags(
            listOf(
                upload.id.toString(),
                upload.accountName,
                UploadFileFromFileSystemWorker::class.java.name
            )
        )

        val workersToCancel = workersFromContentUriToCancel + workersFromFileSystemToCancel

        workersToCancel.forEach {
            workManager.cancelWorkById(it.id)
            Timber.i("Upload with id ${upload.id} has been cancelled.")
        }

        localStorageProvider.deleteCacheIfNeeded(upload)

        transferRepository.deleteTransferById(upload.id!!)
    }

    data class Params(
        val upload: OCTransfer,
    )
}
