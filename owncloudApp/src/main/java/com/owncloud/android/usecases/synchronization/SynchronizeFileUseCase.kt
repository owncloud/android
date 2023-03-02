/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 *
 * Copyright (C) 2023 ownCloud GmbH.
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
import com.owncloud.android.domain.files.usecases.SaveConflictUseCase
import com.owncloud.android.usecases.transfers.downloads.DownloadFileUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFileInConflictUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.util.UUID

class SynchronizeFileUseCase(
    private val downloadFileUseCase: DownloadFileUseCase,
    private val uploadFileInConflictUseCase: UploadFileInConflictUseCase,
    private val saveConflictUseCase: SaveConflictUseCase,
    private val fileRepository: FileRepository,
) : BaseUseCaseWithResult<SynchronizeFileUseCase.SyncType, SynchronizeFileUseCase.Params>() {

    override fun run(params: Params): SyncType {
        val fileToSynchronize = params.fileToSynchronize
        val accountName: String = fileToSynchronize.owner

        CoroutineScope(Dispatchers.IO).run {
            // 1. Perform a propfind to check if the file still exists in remote
            val serverFile = try {
                fileRepository.readFile(
                    remotePath = fileToSynchronize.remotePath,
                    accountName = fileToSynchronize.owner,
                    spaceId = fileToSynchronize.spaceId
                )
            } catch (exception: FileNotFoundException) {
                // 1.1 File does not exist anymore in remote
                val localFile = fileToSynchronize.id?.let { fileRepository.getFileById(it) }
                // If it still exists locally, but file has different path, another operation could have been done simultaneously
                // Do not remove the file in that case. It may be synced later
                if (localFile != null && (localFile.remotePath == fileToSynchronize.remotePath && localFile.spaceId == fileToSynchronize.spaceId)) {
                    fileRepository.deleteFiles(listOf(fileToSynchronize), false)
                }
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
                Timber.i("File ${fileToSynchronize.fileName} has changed locally and remotely. We got a conflict with etag: ${serverFile.etag}")
                if (fileToSynchronize.etagInConflict == null) {
                    saveConflictUseCase.execute(
                        SaveConflictUseCase.Params(
                            fileId = fileToSynchronize.id!!,
                            eTagInConflict = serverFile.etag!!
                        )
                    )
                }
                return SyncType.ConflictDetected(serverFile.etag!!)
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
                spaceId = ocFile.spaceId,
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
