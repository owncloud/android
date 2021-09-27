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

package com.owncloud.android.presentation.viewmodels.logging

import androidx.lifecycle.ViewModel
import com.owncloud.android.MainApp
import com.owncloud.android.data.storage.LocalStorageProvider
import java.io.File

class LogListViewModel : ViewModel() {

    private fun getLogsDirectory(): File {
        val localStorageProvider = LocalStorageProvider.LegacyStorageProvider(MainApp.dataFolder)
        val logsPath = localStorageProvider.getLogsPath()
        return File(logsPath)
    }

    private fun getLogsFiles(logsFolder: File): List<File> {
        return logsFolder.listFiles()?.toList() ?: listOf()
    }

    fun getData(): List<File> = getLogsFiles(getLogsDirectory())

}
