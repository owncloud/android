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

package com.owncloud.android.domain.files.usecases

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo

class SortFilesWithSyncInfoUseCase : BaseUseCase<List<OCFileWithSyncInfo>, SortFilesWithSyncInfoUseCase.Params>() {

    override fun run(params: Params): List<OCFileWithSyncInfo> {
        return when (params.sortType) {
            SortType.SORT_BY_NAME -> sortByName(params.listOfFiles, params.ascending)
            SortType.SORT_BY_SIZE -> sortBySize(params.listOfFiles, params.ascending)
            SortType.SORT_BY_DATE -> sortByDate(params.listOfFiles, params.ascending)
        }
    }

    private fun sortByName(listOfFiles: List<OCFileWithSyncInfo>, ascending: Boolean): List<OCFileWithSyncInfo> {
        val newListOfFiles =
            if (ascending) listOfFiles.sortedBy { it.file.fileName.lowercase() }
            else listOfFiles.sortedByDescending { it.file.fileName.lowercase() }

        // Show first the folders when sorting by name
        return newListOfFiles.sortedByDescending { it.file.isFolder }
    }

    private fun sortBySize(listOfFiles: List<OCFileWithSyncInfo>, ascending: Boolean): List<OCFileWithSyncInfo> {
        return if (ascending) listOfFiles.sortedBy { it.file.length }
        else listOfFiles.sortedByDescending { it.file.length }
    }

    private fun sortByDate(listOfFiles: List<OCFileWithSyncInfo>, ascending: Boolean): List<OCFileWithSyncInfo> {
        return if (ascending) listOfFiles.sortedBy { it.file.modificationTimestamp }
        else listOfFiles.sortedByDescending { it.file.modificationTimestamp }
    }

    data class Params(
        val listOfFiles: List<OCFileWithSyncInfo>,
        val sortType: SortType,
        val ascending: Boolean,
    )
}
