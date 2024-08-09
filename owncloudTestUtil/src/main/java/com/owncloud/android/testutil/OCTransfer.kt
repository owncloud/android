/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.testutil

import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferStatus
import com.owncloud.android.domain.transfers.model.UploadEnqueuedBy

val OC_TRANSFER = OCTransfer(
    id = 0L,
    localPath = "/local/path",
    remotePath = "/remote/path",
    accountName = OC_ACCOUNT_NAME,
    fileSize = 1024L,
    status = TransferStatus.TRANSFER_IN_PROGRESS,
    localBehaviour = UploadBehavior.MOVE,
    forceOverwrite = true,
    createdBy = UploadEnqueuedBy.ENQUEUED_BY_USER,
    sourcePath = "/source/path",
)
