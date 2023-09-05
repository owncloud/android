/*
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * Copyright (C) 2021 ownCloud GmbH.
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
 *
 */

package com.owncloud.android.presentation.logging

import androidx.lifecycle.ViewModel
import com.owncloud.android.data.providers.LocalStorageProvider
import java.io.File

class LogListViewModel(
    private val localStorageProvider: LocalStorageProvider
) : ViewModel() {

    private fun getLogsDirectory(): File {
        val logsPath = localStorageProvider.getLogsPath()
        return File(logsPath)
    }

    fun getLogsFiles(): List<File> {
        return getLogsDirectory().listFiles()?.toList()?.sortedBy { it.name } ?: listOf()
    }
}
