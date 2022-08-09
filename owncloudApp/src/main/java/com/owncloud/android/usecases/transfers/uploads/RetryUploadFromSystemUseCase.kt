/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2021 ownCloud GmbH.
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

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.domain.transfers.TransferRepository

class RetryUploadFromSystemUseCase(
    private val uploadFileFromSystemUseCase: UploadFileFromSystemUseCase,
    private val transferRepository: TransferRepository,
) : BaseUseCase<Unit, RetryUploadFromSystemUseCase.Params>() {

    override fun run(params: Params) {
        val uploadToRetry = transferRepository.getTransferById(params.uploadIdInStorageManager)

        uploadToRetry ?: return

        transferRepository.updateTransferStatusToEnqueuedById(params.uploadIdInStorageManager)

        uploadFileFromSystemUseCase.execute(
            UploadFileFromSystemUseCase.Params(
                accountName = uploadToRetry.accountName,
                localPath = uploadToRetry.localPath,
                lastModifiedInSeconds = (uploadToRetry.transferEndTimestamp?.div(1000)).toString(),
                behavior = UploadBehavior.fromLegacyLocalBehavior(uploadToRetry.localBehaviour).name,
                uploadPath = uploadToRetry.remotePath,
                uploadIdInStorageManager = params.uploadIdInStorageManager
            )
        )
    }

    data class Params(
        val uploadIdInStorageManager: Long,
    )
}
