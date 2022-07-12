/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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
package com.owncloud.android.usecases.synchronization

import android.accounts.Account
import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.files.FileRepository

/**
 * Synchronize a folder.
 *
 * Params:
 * - syncJustAlreadyDownloadedFiles: When true, just the already downloaded files will be synced.
 *                                   It is useful to check if there were local changes that needs to sync with server,
 *                                   but we don't want to download every single file that it is not downloaded yet.
 * - syncFoldersRecursively:         When true, folders will be synced recursively.
 *                                   Use carefully..
 *
 *
 * Normal refresh: syncJustAlreadyDownloadedFiles = true, syncFoldersRecursively = false
 * Discover full account: syncJustAlreadyDownloadedFiles = true, syncFoldersRecursively = true
 * Full sync recursive: syncJustAlreadyDownloadedFiles = false, syncFoldersRecursively = true
 * Sync files in folder: syncJustAlreadyDownloadedFiles = false, syncFoldersRecursively = false
 */
class SynchronizeFolderUseCase(
    private val synchronizeFileUseCase: SynchronizeFileUseCase,
    private val fileRepository: FileRepository,
) : BaseUseCaseWithResult<Unit, SynchronizeFolderUseCase.Params>() {

    override fun run(params: Params) {
        val remotePath = params.remotePath
        val account = params.account

        val folderContent = fileRepository.refreshFolder(remotePath)

        folderContent.forEach { ocFile ->
            if (ocFile.isFolder) {
                if (params.syncFoldersRecursively) {
                    SynchronizeFolderUseCase(synchronizeFileUseCase, fileRepository).execute(
                        Params(
                            remotePath = ocFile.remotePath,
                            account = account,
                            syncJustAlreadyDownloadedFiles = params.syncJustAlreadyDownloadedFiles,
                            syncFoldersRecursively = params.syncFoldersRecursively
                        )
                    )
                }
            } else {
                if (ocFile.isAvailableLocally || !params.syncJustAlreadyDownloadedFiles) {
                    synchronizeFileUseCase.execute(
                        SynchronizeFileUseCase.Params(
                            fileToSynchronize = ocFile,
                            account = account
                        )
                    )
                }
            }
        }
    }

    data class Params(
        val remotePath: String,
        val account: Account,
        val syncJustAlreadyDownloadedFiles: Boolean = true,
        val syncFoldersRecursively: Boolean = false,
    )
}
