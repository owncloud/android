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

import android.content.Context
import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.transfers.TransferRepository
import com.owncloud.android.extensions.isContentUri
import timber.log.Timber

class RetryFailedUploadsUseCase(
    private val context: Context,
    private val retryUploadFromContentUriUseCase: RetryUploadFromContentUriUseCase,
    private val retryUploadFromSystemUseCase: RetryUploadFromSystemUseCase,
    private val transferRepository: TransferRepository,
) : BaseUseCase<Unit, Unit>() {

    override fun run(params: Unit) {
        val failedUploads = transferRepository.getFailedTransfers()

        if (failedUploads.isEmpty()) {
            Timber.d("There are no failed uploads to retry.")
            return
        }
        failedUploads.forEach { upload ->
            if (upload.isContentUri(context)) {
                retryUploadFromContentUriUseCase(RetryUploadFromContentUriUseCase.Params(upload.id!!))
            } else {
                retryUploadFromSystemUseCase(RetryUploadFromSystemUseCase.Params(upload.id!!))
            }
        }
    }
}
