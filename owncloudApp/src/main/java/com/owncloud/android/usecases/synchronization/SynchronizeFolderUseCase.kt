/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.extensions.isOneOf
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase.SyncFolderMode.REFRESH_FOLDER_RECURSIVELY
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase.SyncFolderMode.SYNC_CONTENTS
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase.SyncFolderMode.SYNC_FOLDER_RECURSIVELY

class SynchronizeFolderUseCase(
    private val synchronizeFileUseCase: SynchronizeFileUseCase,
    private val fileRepository: FileRepository,
) : BaseUseCaseWithResult<Unit, SynchronizeFolderUseCase.Params>() {

    override fun run(params: Params) {
        val remotePath = params.remotePath
        val accountName = params.accountName

        val folderContent = fileRepository.refreshFolder(
            remotePath = remotePath,
            accountName = accountName,
            spaceId = params.spaceId,
            isActionSetFolderAvailableOfflineOrSynchronize = params.isActionSetFolderAvailableOfflineOrSynchronize,
        )

        folderContent.forEach { ocFile ->
            if (ocFile.isFolder) {
                if (shouldSyncFolder(params.syncMode, ocFile)) {
                    SynchronizeFolderUseCase(synchronizeFileUseCase, fileRepository)(
                        Params(
                            remotePath = ocFile.remotePath,
                            accountName = accountName,
                            spaceId = ocFile.spaceId,
                            syncMode = params.syncMode,
                            isActionSetFolderAvailableOfflineOrSynchronize = params.isActionSetFolderAvailableOfflineOrSynchronize,
                        )
                    )
                }
            } else if (shouldSyncFile(params.syncMode, ocFile)) {
                synchronizeFileUseCase(
                    SynchronizeFileUseCase.Params(
                        fileToSynchronize = ocFile,
                    )
                )
            }
        }
    }

    private fun shouldSyncFolder(syncMode: SyncFolderMode, ocFolder: OCFile) =
        syncMode.isOneOf(REFRESH_FOLDER_RECURSIVELY, SYNC_FOLDER_RECURSIVELY) || syncMode == SYNC_CONTENTS && ocFolder.isAvailableOffline

    private fun shouldSyncFile(syncMode: SyncFolderMode, ocFile: OCFile) =
        syncMode == SYNC_FOLDER_RECURSIVELY || (syncMode == SYNC_CONTENTS && (ocFile.isAvailableLocally || ocFile.isAvailableOffline))

    data class Params(
        val remotePath: String,
        val accountName: String,
        val spaceId: String? = null,
        val syncMode: SyncFolderMode,
        val isActionSetFolderAvailableOfflineOrSynchronize: Boolean = false,
    )

    /**
     * Potential use cases for each SyncFolderMode:
     * - REFRESH_FOLDER: To get the content when picking a folder.
     * - REFRESH_FOLDER_RECURSIVELY: To discover the full account content. Probably worthy to do when adding a new account.
     * - SYNC_CONTENTS: To refresh and also sync the already downloaded content to check if there were changes locally or remotely
     * - SYNC_FOLDER_RECURSIVELY: Full folder synchronization. Probably also triggered in av. offline worker.
     */
    enum class SyncFolderMode {
        REFRESH_FOLDER, REFRESH_FOLDER_RECURSIVELY, SYNC_CONTENTS, SYNC_FOLDER_RECURSIVELY;
    }
}
