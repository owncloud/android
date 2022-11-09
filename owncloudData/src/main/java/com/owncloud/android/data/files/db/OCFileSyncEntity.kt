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

package com.owncloud.android.data.files.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.owncloud.android.data.ProviderMeta.ProviderTableMeta.FILES_SYNC_TABLE_NAME
import java.util.UUID

@Entity(
    tableName = FILES_SYNC_TABLE_NAME,
    foreignKeys = [ForeignKey(
        entity = OCFileEntity::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("fileId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class OCFileSyncEntity(
    @PrimaryKey val fileId: Long,
    val uploadWorkerUuid: UUID?,
    val downloadWorkerUuid: UUID?,
    val isSynchronizing: Boolean
)
