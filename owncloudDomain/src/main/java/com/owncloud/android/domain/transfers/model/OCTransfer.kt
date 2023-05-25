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

package com.owncloud.android.domain.transfers.model

import android.os.Parcelable
import com.owncloud.android.domain.camerauploads.model.UploadBehavior
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class OCTransfer(
    var id: Long? = null,
    val localPath: String,
    val remotePath: String,
    val accountName: String,
    val fileSize: Long,
    var status: TransferStatus,
    val localBehaviour: UploadBehavior,
    val forceOverwrite: Boolean,
    val transferEndTimestamp: Long? = null,
    val lastResult: TransferResult? = null,
    val createdBy: UploadEnqueuedBy,
    val transferId: String? = null,
    val spaceId: String? = null,
) : Parcelable {
    init {
        if (!remotePath.startsWith(File.separator)) throw IllegalArgumentException("Remote path must be an absolute path in the local file system")
        if (accountName.isEmpty()) throw IllegalArgumentException("Invalid account name")
    }
}
