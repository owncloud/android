/*
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.presentation.viewmodels.files

import android.accounts.Account
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.presentation.manager.TransferManager

class FileDetailsViewModel(
    private val transferManager: TransferManager,
) : ViewModel() {

    private val _downloads = MediatorLiveData<WorkInfo?>()
    val downloads: LiveData<WorkInfo?> = _downloads

    fun startListeningToDownloadsFromAccountAndFile(account: Account, file: OCFile) {
        _downloads.addSource(transferManager.getLiveDataForDownloadingFile(account, file)) { workInfo ->
            _downloads.postValue(workInfo)
        }
    }

    fun cancelCurrentDownload(file: OCFile) {
        transferManager.cancelDownloadForFile(file)
    }
}
