/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.presentation.files.operations

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.appregistry.usecases.CreateFileWithAppProviderUseCase
import com.owncloud.android.domain.availableoffline.usecases.SetFilesAsAvailableOfflineUseCase
import com.owncloud.android.domain.availableoffline.usecases.UnsetFilesAsAvailableOfflineUseCase
import com.owncloud.android.domain.exceptions.NoNetworkConnectionException
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.CopyFileUseCase
import com.owncloud.android.domain.files.usecases.CreateFolderAsyncUseCase
import com.owncloud.android.domain.files.usecases.IsAnyFileAvailableLocallyAndNotAvailableOfflineUseCase
import com.owncloud.android.domain.files.usecases.ManageDeepLinkUseCase
import com.owncloud.android.domain.files.usecases.MoveFileUseCase
import com.owncloud.android.domain.files.usecases.RemoveFileUseCase
import com.owncloud.android.domain.files.usecases.RenameFileUseCase
import com.owncloud.android.domain.files.usecases.SetLastUsageFileUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.providers.ContextProvider
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.ui.dialog.FileAlreadyExistsDialog
import com.owncloud.android.usecases.synchronization.SynchronizeFileUseCase
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.URI

class FileOperationsViewModel(
    private val createFolderAsyncUseCase: CreateFolderAsyncUseCase,
    private val copyFileUseCase: CopyFileUseCase,
    private val moveFileUseCase: MoveFileUseCase,
    private val removeFileUseCase: RemoveFileUseCase,
    private val renameFileUseCase: RenameFileUseCase,
    private val synchronizeFileUseCase: SynchronizeFileUseCase,
    private val synchronizeFolderUseCase: SynchronizeFolderUseCase,
    private val createFileWithAppProviderUseCase: CreateFileWithAppProviderUseCase,
    private val setFilesAsAvailableOfflineUseCase: SetFilesAsAvailableOfflineUseCase,
    private val unsetFilesAsAvailableOfflineUseCase: UnsetFilesAsAvailableOfflineUseCase,
    private val manageDeepLinkUseCase: ManageDeepLinkUseCase,
    private val setLastUsageFileUseCase: SetLastUsageFileUseCase,
    private val isAnyFileAvailableLocallyAndNotAvailableOfflineUseCase: IsAnyFileAvailableLocallyAndNotAvailableOfflineUseCase,
    private val contextProvider: ContextProvider,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
) : ViewModel() {

    private val _createFolder = MediatorLiveData<Event<UIResult<Unit>>>()
    val createFolder: LiveData<Event<UIResult<Unit>>> = _createFolder

    private val _copyFileLiveData = MediatorLiveData<Event<UIResult<List<OCFile>>>>()
    val copyFileLiveData: LiveData<Event<UIResult<List<OCFile>>>> = _copyFileLiveData

    private val _moveFileLiveData = MediatorLiveData<Event<UIResult<List<OCFile>>>>()
    val moveFileLiveData: LiveData<Event<UIResult<List<OCFile>>>> = _moveFileLiveData

    private val _removeFileLiveData = MediatorLiveData<Event<UIResult<List<OCFile>>>>()
    val removeFileLiveData: LiveData<Event<UIResult<List<OCFile>>>> = _removeFileLiveData

    private val _renameFileLiveData = MediatorLiveData<Event<UIResult<OCFile>>>()
    val renameFileLiveData: LiveData<Event<UIResult<OCFile>>> = _renameFileLiveData

    private val _syncFileLiveData = MediatorLiveData<Event<UIResult<SynchronizeFileUseCase.SyncType>>>()
    val syncFileLiveData: LiveData<Event<UIResult<SynchronizeFileUseCase.SyncType>>> = _syncFileLiveData

    private val _syncFolderLiveData = MediatorLiveData<Event<UIResult<Unit>>>()
    val syncFolderLiveData: LiveData<Event<UIResult<Unit>>> = _syncFolderLiveData

    private val _refreshFolderLiveData = MediatorLiveData<Event<UIResult<Unit>>>()
    val refreshFolderLiveData: LiveData<Event<UIResult<Unit>>> = _refreshFolderLiveData

    private val _createFileWithAppProviderFlow = MutableStateFlow<Event<UIResult<String>>?>(null)
    val createFileWithAppProviderFlow: StateFlow<Event<UIResult<String>>?> = _createFileWithAppProviderFlow

    private val _deepLinkFlow = MutableStateFlow<Event<UIResult<OCFile?>>?>(null)
    val deepLinkFlow: StateFlow<Event<UIResult<OCFile?>>?> = _deepLinkFlow

    private val _checkIfFileIsLocalAndNotAvailableOfflineSharedFlow = MutableSharedFlow<UIResult<Boolean>>()
    val checkIfFileIsLocalAndNotAvailableOfflineSharedFlow: SharedFlow<UIResult<Boolean>> = _checkIfFileIsLocalAndNotAvailableOfflineSharedFlow

    val openDialogs = mutableListOf<FileAlreadyExistsDialog>()

    // Used to save the last operation folder
    private var lastTargetFolder: OCFile? = null

    fun performOperation(fileOperation: FileOperation) {
        when (fileOperation) {
            is FileOperation.MoveOperation -> moveOperation(fileOperation)
            is FileOperation.RemoveOperation -> removeOperation(fileOperation)
            is FileOperation.RenameOperation -> renameOperation(fileOperation)
            is FileOperation.CopyOperation -> copyOperation(fileOperation)
            is FileOperation.SynchronizeFileOperation -> syncFileOperation(fileOperation)
            is FileOperation.CreateFolder -> createFolderOperation(fileOperation)
            is FileOperation.SetFilesAsAvailableOffline -> setFileAsAvailableOffline(fileOperation)
            is FileOperation.UnsetFilesAsAvailableOffline -> unsetFileAsAvailableOffline(fileOperation)
            is FileOperation.SynchronizeFolderOperation -> syncFolderOperation(fileOperation)
            is FileOperation.RefreshFolderOperation -> refreshFolderOperation(fileOperation)
            is FileOperation.CreateFileWithAppProviderOperation -> createFileWithAppProvider(fileOperation)
        }
    }

    fun showRemoveDialog(filesToRemove: List<OCFile>) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            showLoading = true,
            sharedFlow = _checkIfFileIsLocalAndNotAvailableOfflineSharedFlow,
            useCase = isAnyFileAvailableLocallyAndNotAvailableOfflineUseCase,
            useCaseParams = IsAnyFileAvailableLocallyAndNotAvailableOfflineUseCase.Params(filesToRemove),
            requiresConnection = false
        )
    }

    fun setLastUsageFile(file: OCFile) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            setLastUsageFileUseCase(
                SetLastUsageFileUseCase.Params(
                    fileId = file.id!!,
                    lastUsage = System.currentTimeMillis(),
                    isAvailableLocally = file.isAvailableLocally,
                    isFolder = file.isFolder,
                )
            )
        }
    }

    fun handleDeepLink(uri: Uri, accountName: String) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            showLoading = true,
            flow = _deepLinkFlow,
            useCase = manageDeepLinkUseCase,
            useCaseParams = ManageDeepLinkUseCase.Params(URI(uri.toString()), accountName),
        )
    }

    private fun createFolderOperation(fileOperation: FileOperation.CreateFolder) {
        runOperation(
            liveData = _createFolder,
            useCase = createFolderAsyncUseCase,
            useCaseParams = CreateFolderAsyncUseCase.Params(fileOperation.folderName, fileOperation.parentFile),
            postValue = Unit
        )
    }

    private fun copyOperation(fileOperation: FileOperation.CopyOperation) {
        val targetFolder = if (fileOperation.targetFolder != null) {
            lastTargetFolder = fileOperation.targetFolder
            fileOperation.targetFolder
        } else {
            lastTargetFolder
        }
        targetFolder?.let { folder ->
            runUseCaseWithResult(
                coroutineDispatcher = coroutinesDispatcherProvider.io,
                liveData = _copyFileLiveData,
                useCase = copyFileUseCase,
                useCaseParams = CopyFileUseCase.Params(
                    listOfFilesToCopy = fileOperation.listOfFilesToCopy,
                    targetFolder = folder,
                    replace = fileOperation.replace,
                    isUserLogged = fileOperation.isUserLogged,
                ),
                showLoading = true,
            )
        }
    }

    private fun moveOperation(fileOperation: FileOperation.MoveOperation) {
        val targetFolder = if (fileOperation.targetFolder != null) {
            lastTargetFolder = fileOperation.targetFolder
            fileOperation.targetFolder
        } else {
            lastTargetFolder
        }
        targetFolder?.let { folder ->
            runUseCaseWithResult(
                coroutineDispatcher = coroutinesDispatcherProvider.io,
                liveData = _moveFileLiveData,
                useCase = moveFileUseCase,
                useCaseParams = MoveFileUseCase.Params(
                    listOfFilesToMove = fileOperation.listOfFilesToMove,
                    targetFolder = folder,
                    replace = fileOperation.replace,
                    isUserLogged = fileOperation.isUserLogged,
                ),
                showLoading = true,
            )
        }
    }

    private fun removeOperation(fileOperation: FileOperation.RemoveOperation) {
        runOperation(
            liveData = _removeFileLiveData,
            useCase = removeFileUseCase,
            useCaseParams = RemoveFileUseCase.Params(fileOperation.listOfFilesToRemove, fileOperation.removeOnlyLocalCopy),
            postValue = fileOperation.listOfFilesToRemove,
            requiresConnection = !fileOperation.removeOnlyLocalCopy,
        )
    }

    private fun renameOperation(fileOperation: FileOperation.RenameOperation) {
        runOperation(
            liveData = _renameFileLiveData,
            useCase = renameFileUseCase,
            useCaseParams = RenameFileUseCase.Params(fileOperation.ocFileToRename, fileOperation.newName),
            postValue = fileOperation.ocFileToRename
        )
    }

    private fun syncFileOperation(fileOperation: FileOperation.SynchronizeFileOperation) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            requiresConnection = true,
            liveData = _syncFileLiveData,
            useCase = synchronizeFileUseCase,
            useCaseParams = SynchronizeFileUseCase.Params(fileOperation.fileToSync)
        )
    }

    private fun syncFolderOperation(fileOperation: FileOperation.SynchronizeFolderOperation) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            liveData = _syncFolderLiveData,
            useCase = synchronizeFolderUseCase,
            showLoading = false,
            useCaseParams = SynchronizeFolderUseCase.Params(
                remotePath = fileOperation.folderToSync.remotePath,
                accountName = fileOperation.folderToSync.owner,
                spaceId = fileOperation.folderToSync.spaceId,
                syncMode = SynchronizeFolderUseCase.SyncFolderMode.SYNC_FOLDER_RECURSIVELY,
                isActionSetFolderAvailableOfflineOrSynchronize = fileOperation.isActionSetFolderAvailableOfflineOrSynchronize,
            )
        )
    }

    private fun refreshFolderOperation(fileOperation: FileOperation.RefreshFolderOperation) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            liveData = _refreshFolderLiveData,
            useCase = synchronizeFolderUseCase,
            showLoading = true,
            useCaseParams = SynchronizeFolderUseCase.Params(
                remotePath = fileOperation.folderToRefresh.remotePath,
                accountName = fileOperation.folderToRefresh.owner,
                spaceId = fileOperation.folderToRefresh.spaceId,
                syncMode = if (fileOperation.shouldSyncContents) SynchronizeFolderUseCase.SyncFolderMode.SYNC_CONTENTS else SynchronizeFolderUseCase.SyncFolderMode.REFRESH_FOLDER
            )
        )
    }

    private fun createFileWithAppProvider(fileOperation: FileOperation.CreateFileWithAppProviderOperation) {
        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            flow = _createFileWithAppProviderFlow,
            useCase = createFileWithAppProviderUseCase,
            useCaseParams = CreateFileWithAppProviderUseCase.Params(
                accountName = fileOperation.accountName,
                parentContainerId = fileOperation.parentContainerId,
                filename = fileOperation.filename,
            ),
            showLoading = false,
            requiresConnection = true,
        )
    }

    private fun setFileAsAvailableOffline(fileOperation: FileOperation.SetFilesAsAvailableOffline) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            setFilesAsAvailableOfflineUseCase(SetFilesAsAvailableOfflineUseCase.Params(fileOperation.filesToUpdate))
        }
    }

    private fun unsetFileAsAvailableOffline(fileOperation: FileOperation.UnsetFilesAsAvailableOffline) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            unsetFilesAsAvailableOfflineUseCase(UnsetFilesAsAvailableOfflineUseCase.Params(fileOperation.filesToUpdate))
        }
    }

    private fun <Type, Params, PostResult> runOperation(
        liveData: MediatorLiveData<Event<UIResult<PostResult>>>,
        useCase: BaseUseCaseWithResult<Type, Params>,
        useCaseParams: Params,
        postValue: PostResult? = null,
        requiresConnection: Boolean = true,
    ) {
        viewModelScope.launch(coroutinesDispatcherProvider.io) {
            liveData.postValue(Event(UIResult.Loading()))

            if (!contextProvider.isConnected() && requiresConnection) {
                liveData.postValue(Event(UIResult.Error(error = NoNetworkConnectionException())))
                Timber.w("${useCase.javaClass.simpleName} will not be executed due to lack of network connection")
                return@launch
            }

            val useCaseResult = useCase(useCaseParams).also {
                Timber.d("Use case executed: ${useCase.javaClass.simpleName} with result: $it")
            }

            when (useCaseResult) {
                is UseCaseResult.Success -> {
                    liveData.postValue(Event(UIResult.Success(postValue)))
                }

                is UseCaseResult.Error -> {
                    liveData.postValue(Event(UIResult.Error(error = useCaseResult.throwable)))
                }
            }
        }
    }
}
