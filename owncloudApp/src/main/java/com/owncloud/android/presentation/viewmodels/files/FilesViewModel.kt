/*
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.CreateFolderAsyncUseCase
import com.owncloud.android.domain.files.usecases.RefreshFolderFromServerAsyncUseCase
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.ViewModelExt.runUseCaseWithResult
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.providers.CoroutinesDispatcherProvider

class FilesViewModel(
    private val createFolderAsyncUseCase: CreateFolderAsyncUseCase,
    private val refreshFolderFromServerAsyncUseCase: RefreshFolderFromServerAsyncUseCase,
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val _createFolder = MediatorLiveData<Event<UIResult<Unit>>>()
    val createFolder: LiveData<Event<UIResult<Unit>>> = _createFolder

    fun createFolder(
        parentFile: OCFile,
        folderName: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutineDispatcherProvider.io,
        liveData = _createFolder,
        useCase = createFolderAsyncUseCase,
        useCaseParams = CreateFolderAsyncUseCase.Params(
            parentFile = parentFile,
            folderName = folderName
        )
    )

    fun refreshFolder(
        remotePath: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutineDispatcherProvider.io,
        liveData = _createFolder,
        useCase = refreshFolderFromServerAsyncUseCase,
        useCaseParams = RefreshFolderFromServerAsyncUseCase.Params(
            remotePath = remotePath
        )
    )
}
