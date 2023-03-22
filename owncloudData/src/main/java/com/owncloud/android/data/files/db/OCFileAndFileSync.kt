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

import androidx.room.Embedded
import androidx.room.Relation
import com.owncloud.android.data.spaces.db.SpacesEntity

data class OCFileAndFileSync(
    @Embedded val file: OCFileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "fileId"
    )
    val fileSync: OCFileSyncEntity?,
    @Relation(
        parentColumn = "spaceId",
        entityColumn = "space_id"
    )
    val space: SpacesEntity? = null,
)
