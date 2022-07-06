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

package com.owncloud.android.data.transfers.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.TRANSFERS_TABLE_NAME

@Entity(
    tableName = TRANSFERS_TABLE_NAME
)
data class OCTransferEntity(
    val localPath: String,
    val remotePath: String,
    val accountName: String,
    val fileSize: Long,
    val status: Int,
    val localBehaviour: Int,
    val forceOverwrite: Boolean,
    val transferEndTimestamp: Long? = null,
    val lastResult: Int? = null,
    val createdBy: Int,
    val transferId: String? = null
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
