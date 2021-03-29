package com.owncloud.android.ui.preview

import android.accounts.Account
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
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

    private val _downloads = MediatorLiveData<MutableList<Pair<OCFile, WorkInfo>>>()
    val downloads: LiveData<MutableList<Pair<OCFile, WorkInfo>>> = _downloads

    fun startListeningToDownloadsFromAccount(account: Account) {
        _downloads.addSource(
            Transformations.map(transferManager.getLiveDataForDownloadingFromAccount(account = account)) { listOfDownloads ->
                listOfDownloads.asReversed().distinctBy { it.tags }
            }) { listOfWorkInfo ->
            viewModelScope.launch(coroutinesDispatcherProvider.io) {
                val finalList = mutableListOf<Pair<OCFile, WorkInfo>>()
                listOfWorkInfo.forEach { workInfo ->
                    val id: Long = workInfo.tags.first { it.toLongOrNull() != null }.toLong()
                    val useCaseResult = getFileByIdUseCase.execute(GetFileByIdUseCase.Params(fileId = id))
                    val file = useCaseResult.getDataOrNull()
                    if (file != null) {
                        finalList.add(Pair(file, workInfo))
                    }
                }
                _downloads.postValue(finalList)
            }
        }
    }
}
