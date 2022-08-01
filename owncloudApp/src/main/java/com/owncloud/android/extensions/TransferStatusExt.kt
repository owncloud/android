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

package com.owncloud.android.extensions

import androidx.annotation.StringRes
import com.owncloud.android.R
import com.owncloud.android.domain.transfers.model.TransferStatus

@StringRes
fun TransferStatus.toStringRes(): Int {
    return when (this) {
        TransferStatus.TRANSFER_IN_PROGRESS -> R.string.uploads_view_group_current_uploads
        TransferStatus.TRANSFER_FAILED -> R.string.uploads_view_group_failed_uploads
        TransferStatus.TRANSFER_SUCCEEDED -> R.string.uploads_view_group_finished_uploads
        TransferStatus.TRANSFER_QUEUED -> R.string.uploads_view_group_queued_uploads
    }
}
