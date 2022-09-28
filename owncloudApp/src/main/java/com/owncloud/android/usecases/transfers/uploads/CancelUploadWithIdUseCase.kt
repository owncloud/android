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
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.transfers.TransferRepository
import timber.log.Timber

class CancelUploadWithIdUseCase(
    private val workManager: WorkManager,
    private val transferRepository: TransferRepository,
) : BaseUseCase<Unit, CancelUploadWithIdUseCase.Params>() {

    override fun run(params: Params) {
        workManager.cancelAllWorkByTag(params.uploadId.toString())

        transferRepository.removeTransferById(params.uploadId)

        // TODO: Delete cache files of those transfers which were cached (check if they are located in the temp directory)

        Timber.i("Upload with id ${params.uploadId} has been cancelled.")
    }

    data class Params(
        val uploadId: Long,
    )
}
