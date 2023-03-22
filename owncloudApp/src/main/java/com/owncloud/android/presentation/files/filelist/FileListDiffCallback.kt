/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
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

package com.owncloud.android.presentation.files.filelist

import androidx.recyclerview.widget.DiffUtil
import com.owncloud.android.domain.files.model.FileListOption
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import com.owncloud.android.domain.files.model.OCFooterFile

class FileListDiffCallback(
    private val oldList: List<Any>,
    private val newList: List<Any>,
    private val oldFileListOption: FileListOption,
    private val newFileListOption: FileListOption,
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        if (oldItem is Unit && newItem is Unit) {
            return true
        }

        if (oldItem is Boolean && newItem is Boolean) {
            return true
        }

        if (oldItem is OCFileWithSyncInfo && newItem is OCFileWithSyncInfo) {
            return oldItem.file.id == newItem.file.id
        }

        if (oldItem is OCFooterFile && newItem is OCFooterFile) {
            return oldItem.text == newItem.text
        }

        return false
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition] && oldFileListOption == newFileListOption
}
