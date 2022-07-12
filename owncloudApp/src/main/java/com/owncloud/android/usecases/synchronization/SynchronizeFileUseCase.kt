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

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.files.FileRepository
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.usecases.SaveFileOrFolderUseCase
import com.owncloud.android.usecases.transfers.downloads.DownloadFileUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFileInConflictUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.util.UUID

class SynchronizeFileUseCase(
    private val downloadFileUseCase: DownloadFileUseCase,
    private val uploadFileInConflictUseCase: UploadFileInConflictUseCase,
    private val saveFileUseCase: SaveFileOrFolderUseCase,
    private val fileRepository: FileRepository,
) : BaseUseCaseWithResult<SynchronizeFileUseCase.SyncType, SynchronizeFileUseCase.Params>() {

    override fun run(params: Params): SyncType {
        val fileToSynchronize = params.fileToSynchronize
        val accountName: String = fileToSynchronize.owner

        CoroutineScope(Dispatchers.IO).run {
            // 1. Perform a propfind to check if the file still exists in remote
            val serverFile = try {
                fileRepository.readFile(fileToSynchronize.remotePath)
            } catch (exception: FileNotFoundException) {
                // 1.1 File not exists anymore -> remove file locally (DB and Storage)
                fileRepository.removeFile(listOf(fileToSynchronize), false)
                return SyncType.FileNotFound
            }

            // 2. File not downloaded -> Download it
            if (!fileToSynchronize.isAvailableLocally) {
                Timber.i("File ${fileToSynchronize.fileName} is not downloaded. Let's download it")
                val uuid = requestForDownload(accountName = accountName, ocFile = fileToSynchronize)
                return SyncType.DownloadEnqueued(uuid)
            }

            // 3. Check if file has changed locally
            val changedLocally = fileToSynchronize.localModificationTimestamp > fileToSynchronize.lastSyncDateForData!!
            Timber.i("Local file modification timestamp :${fileToSynchronize.localModificationTimestamp} and last sync date for data :${fileToSynchronize.lastSyncDateForData}")
            Timber.i("So it has changed locally: $changedLocally")

            // 4. Check if file has changed remotely
            val changedRemotely = serverFile.etag != fileToSynchronize.etag
            Timber.i("Local etag :${fileToSynchronize.etag} and remote etag :${serverFile.etag}")
            Timber.i("So it has changed remotely: $changedRemotely")

            if (changedLocally && changedRemotely) {
                // 5.1 File has changed locally and remotely. We got a conflict, save the conflict.
                Timber.i("File ${fileToSynchronize.fileName} has changed locally and remotely. We got a conflict")
                saveFileUseCase.execute(SaveFileOrFolderUseCase.Params(fileToSynchronize.copy(etagInConflict = serverFile.etag)))
                return SyncType.ConflictDetected(serverFile.etag!!)
                // FIXME Conflicts
            } else if (changedRemotely) {
                // 5.2 File has changed ONLY remotely -> download new version
                Timber.i("File ${fileToSynchronize.fileName} has changed remotely. Let's download the new version")
                val uuid = requestForDownload(accountName, fileToSynchronize)
                return SyncType.DownloadEnqueued(uuid)
            } else if (changedLocally) {
                // 5.3 File has change ONLY locally -> upload new version
                Timber.i("File ${fileToSynchronize.fileName} has changed locally. Let's upload the new version")
                val uuid = requestForUpload(accountName, fileToSynchronize, fileToSynchronize.etag!!)
                return SyncType.UploadEnqueued(uuid)
            } else {
                // 5.4 File has not change locally not remotely -> do nothing
                Timber.i("File ${fileToSynchronize.fileName} is already synchronized. Nothing to do here")
                return SyncType.AlreadySynchronized
            }
        }
    }

    private fun requestForDownload(accountName: String, ocFile: OCFile): UUID? {
        return downloadFileUseCase.execute(
            DownloadFileUseCase.Params(
                accountName = accountName,
                file = ocFile
            )
        )
    }

    private fun requestForUpload(accountName: String, ocFile: OCFile, etagInConflict: String): UUID? {
        return uploadFileInConflictUseCase.execute(
            UploadFileInConflictUseCase.Params(
                accountName = accountName,
                localPath = ocFile.storagePath!!,
                uploadFolderPath = ocFile.getParentRemotePath(),
            )
        )
    }

    data class Params(
        val fileToSynchronize: OCFile,
    )

    sealed interface SyncType {
        object FileNotFound : SyncType
        data class ConflictDetected(val etagInConflict: String) : SyncType
        data class DownloadEnqueued(val workerId: UUID?) : SyncType
        data class UploadEnqueued(val workerId: UUID?) : SyncType
        object AlreadySynchronized : SyncType
    }
}
