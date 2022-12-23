/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
package com.owncloud.android.data.spaces.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface SpacesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplaceSpaces(listOfSpacesEntities: List<SpacesEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplaceSpecials(listOfSpecialEntities: List<SpaceSpecialEntity>): List<Long>

}
