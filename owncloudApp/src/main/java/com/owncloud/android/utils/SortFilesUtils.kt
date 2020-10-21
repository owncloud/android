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
package com.owncloud.android.utils

import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.presentation.ui.files.SortType
import java.util.Vector

class SortFilesUtils {
    fun sortFiles(
        listOfFiles: Vector<OCFile>,
        sortTypeValue: Int,
        ascending: Boolean,
    ): Vector<OCFile> {
        return when (SortType.fromPreference(sortTypeValue)) {
            SortType.SORT_TYPE_BY_NAME -> sortByName(listOfFiles, ascending)
            SortType.SORT_TYPE_BY_SIZE -> sortBySize(listOfFiles, ascending)
            SortType.SORT_TYPE_BY_DATE -> sortByDate(listOfFiles, ascending)
        }
    }

    private fun sortByName(listOfFiles: Vector<OCFile>, ascending: Boolean): Vector<OCFile> {
        val newListOfFiles =
            if (ascending) listOfFiles.sortedBy { it.fileName.lowercase() }
            else listOfFiles.sortedByDescending { it.fileName.lowercase() }

        // Show first the folders when sorting by name
        return newListOfFiles.sortedByDescending { it.isFolder }.toVector()
    }

    private fun sortBySize(listOfFiles: Vector<OCFile>, ascending: Boolean): Vector<OCFile> {
        return if (ascending) listOfFiles.sortedBy { it.length }.toVector()
        else listOfFiles.sortedByDescending { it.length }.toVector()
    }

    private fun sortByDate(listOfFiles: Vector<OCFile>, ascending: Boolean): Vector<OCFile> {
        return if (ascending) listOfFiles.sortedBy { it.modificationTimestamp }.toVector()
        else listOfFiles.sortedByDescending { it.modificationTimestamp }.toVector()
    }

    private fun List<OCFile>.toVector(): Vector<OCFile> {
        return Vector(this)
    }
}
