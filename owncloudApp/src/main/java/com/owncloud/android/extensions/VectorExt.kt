/**
 * ownCloud Android client application
 *
 * @author John Kalimeris
 * Copyright (C) 2020 ownCloud GmbH.
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

import com.owncloud.android.domain.files.model.OCFile
import java.util.ArrayList
import java.util.Locale
import java.util.Vector

fun Vector<OCFile>.filterByQuery(query: String): List<OCFile> {
    val query = query.toLowerCase(Locale.ROOT)

    val filteredList: MutableList<OCFile> = ArrayList()

    for (fileToAdd in this) {
        val nameOfTheFileToAdd: String = fileToAdd.name?.toLowerCase(Locale.ROOT).orEmpty()
        if (nameOfTheFileToAdd.contains(query)) {
            filteredList.add(fileToAdd)
        }
    }

    // Remove not matching files from this filelist
    for (i in this.indices.reversed()) {
        if (!filteredList.contains(this[i])) {
            removeAt(i)
        }
    }

    // Add matching files to this filelist
    for (i in filteredList.indices) {
        if (!contains(filteredList[i])) {
            add(i, filteredList[i])
        }
    }

    return this
}