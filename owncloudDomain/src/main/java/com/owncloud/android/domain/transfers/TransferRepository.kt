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

package com.owncloud.android.domain.transfers

import android.database.Cursor
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.transfers.model.OCTransfer

interface TransferRepository {
    fun storeTransfer(transfer: OCTransfer): Long
    fun updateTransfer(transfer: OCTransfer)
    fun removeTransfer(transfer: OCTransfer)
    fun removeAllTransfersFromAccount(accountName: String)
    fun getTransfers(selection: String, selectionArgs: Array<String>, sortOrder: String): Array<OCTransfer>
    fun getAllTransfers(): Array<OCTransfer>
    fun getLastTransferFor(file: OCFile, accountName: String): OCTransfer
    fun createTransferFromCursor(cursor: Cursor): OCTransfer
    fun getCurrentAndPendingTransfers(): Array<OCTransfer>
    fun getFailedTransfers(): Array<OCTransfer>
    fun getFinishedTransfers(): Array<OCTransfer>
    fun getFailedButNotDelayedByWifiTransfers(): Array<OCTransfer>
    fun clearFailedButNotDelayedByWifiTransfers()
    fun clearSuccessfulTransfers()
}
