/**
 * ownCloud Android client application
 *
 * Copyright (C) 2022 ownCloud GmbH.
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
package com.owncloud.android.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase

class ReceiveExternalFilesViewModel(
    private val synchronizeFolderUseCase: SynchronizeFolderUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _syncFolderLiveData = MediatorLiveData<Event<UIResult<Unit>>>()
    val syncFolderLiveData: LiveData<Event<UIResult<Unit>>> = _syncFolderLiveData

    fun refreshFolderUseCase(
        folderToSync: OCFile,
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        showLoading = true,
        liveData = _syncFolderLiveData,
        useCase = synchronizeFolderUseCase,
        useCaseParams = SynchronizeFolderUseCase.Params(
            accountName = folderToSync.owner,
            remotePath = folderToSync.remotePath,
            syncMode = SynchronizeFolderUseCase.SyncFolderMode.REFRESH_FOLDER
        )
    )
}
