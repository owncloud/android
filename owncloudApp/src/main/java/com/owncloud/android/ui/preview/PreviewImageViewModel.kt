/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
 */
package com.owncloud.android.ui.preview

import android.accounts.Account
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.presentation.manager.TransferManager
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch

class PreviewImageViewModel(
    private val transferManager: TransferManager,
    private val getFileByIdUseCase: GetFileByIdUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _downloads = MediatorLiveData<List<Pair<OCFile, WorkInfo>>>()
    val downloads: LiveData<List<Pair<OCFile, WorkInfo>>> = _downloads

    fun startListeningToDownloadsFromAccount(account: Account) {
        _downloads.addSource(transferManager.getLiveDataForFinishedDownloadsFromAccount(account)) { listOfWorkInfo ->
            viewModelScope.launch(coroutinesDispatcherProvider.io) {
                val finalList = getListOfPairs(listOfWorkInfo)
                _downloads.postValue(finalList)
            }
        }
    }

    /**
     * It receives a list of WorkInfo, and it returns a list of Pair(OCFile, WorkInfo)
     * This way, each OCFile is linked to its latest work info.
     */
    private fun getListOfPairs(
        listOfWorkInfo: List<WorkInfo>
    ): List<Pair<OCFile, WorkInfo>> {
        val finalList = mutableListOf<Pair<OCFile, WorkInfo>>()

        listOfWorkInfo.forEach { workInfo ->
            val id: Long = workInfo.tags.first { it.toLongOrNull() != null }.toLong()
            val useCaseResult = getFileByIdUseCase.execute(GetFileByIdUseCase.Params(fileId = id))
            val file = useCaseResult.getDataOrNull()
            if (file != null) {
                finalList.add(Pair(file, workInfo))
            }
        }
        return finalList
    }
}
