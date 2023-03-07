/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Abel García de Prada
 *
 * Copyright (C) 2012  Bartek Przybylski
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel

import android.accounts.Account
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentUseCase
import com.owncloud.android.domain.files.usecases.GetFolderImagesUseCase
import com.owncloud.android.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FileDataStorageManager(
    val account: Account,
) : KoinComponent {

    fun getFileByPath(remotePath: String): OCFile? = getFileByPathAndAccount(remotePath, account.name)

    private fun getFileByPathAndAccount(remotePath: String, accountName: String): OCFile? = runBlocking(CoroutinesDispatcherProvider().io) {
        val getFileByRemotePathUseCase: GetFileByRemotePathUseCase by inject()

        val result = withContext(CoroutineScope(CoroutinesDispatcherProvider().io).coroutineContext) {
            getFileByRemotePathUseCase.execute(GetFileByRemotePathUseCase.Params(accountName, remotePath))
        }.getDataOrNull()
        result
    }

    // TODO: New_arch: Remove this and call usecase inside FilesViewModel
    fun getFileById(id: Long): OCFile? = runBlocking(CoroutinesDispatcherProvider().io) {
        val getFileByIdUseCase: GetFileByIdUseCase by inject()

        val result = withContext(CoroutineScope(CoroutinesDispatcherProvider().io).coroutineContext) {
            getFileByIdUseCase.execute(GetFileByIdUseCase.Params(id))
        }.getDataOrNull()
        result
    }

    fun fileExists(path: String): Boolean = getFileByPath(path) != null

    fun getFolderContent(f: OCFile?): List<OCFile> {
        return if (f != null && f.isFolder && f.id != -1L) {
            // TODO: Remove !!
            getFolderContent(f.id!!)
        } else {
            listOf()
        }
    }

    // TODO: New_arch: Remove this and call usecase inside FilesViewModel
    fun getFolderImages(folder: OCFile?): List<OCFile> = runBlocking(CoroutinesDispatcherProvider().io) {
        val getFolderImagesUseCase: GetFolderImagesUseCase by inject()

        val result = withContext(CoroutineScope(CoroutinesDispatcherProvider().io).coroutineContext) {
            // TODO: Remove !!
            getFolderImagesUseCase.execute(GetFolderImagesUseCase.Params(folderId = folder!!.id!!))
        }.getDataOrNull()
        result ?: listOf()
    }

    // TODO: New_arch: Remove this and call usecase inside FilesViewModel
    private fun getFolderContent(parentId: Long): List<OCFile> = runBlocking(CoroutinesDispatcherProvider().io) {
        val getFolderContentUseCase: GetFolderContentUseCase by inject()

        val result = withContext(CoroutineScope(CoroutinesDispatcherProvider().io).coroutineContext) {
            getFolderContentUseCase.execute(GetFolderContentUseCase.Params(parentId))
        }.getDataOrNull()
        result ?: listOf()
    }

    fun getCapability(accountName: String): OCCapability? = runBlocking(CoroutinesDispatcherProvider().io) {
        val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase by inject()

        val capability = withContext(CoroutineScope(CoroutinesDispatcherProvider().io).coroutineContext) {
            getStoredCapabilitiesUseCase.execute(GetStoredCapabilitiesUseCase.Params(accountName))
        }
        capability
    }

    companion object {
        const val ROOT_PARENT_ID = 0
    }
}
