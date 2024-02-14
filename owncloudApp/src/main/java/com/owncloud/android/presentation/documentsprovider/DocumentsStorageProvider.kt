/**
 * ownCloud Android client application
 *
 * @author Bartosz Przybylski
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Shashvat Kedia
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2015  Bartosz Przybylski
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.presentation.documentsprovider

import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.data.providers.SharedPreferencesProvider
import com.owncloud.android.domain.UseCaseResult
import com.owncloud.android.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.domain.exceptions.validation.FileNameException
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFile.Companion.PATH_SEPARATOR
import com.owncloud.android.domain.files.model.OCFile.Companion.ROOT_PATH
import com.owncloud.android.domain.files.usecases.CopyFileUseCase
import com.owncloud.android.domain.files.usecases.CreateFolderAsyncUseCase
import com.owncloud.android.domain.files.usecases.GetFileByIdUseCase
import com.owncloud.android.domain.files.usecases.GetFileByRemotePathUseCase
import com.owncloud.android.domain.files.usecases.GetFolderContentUseCase
import com.owncloud.android.domain.files.usecases.MoveFileUseCase
import com.owncloud.android.domain.files.usecases.RemoveFileUseCase
import com.owncloud.android.domain.files.usecases.RenameFileUseCase
import com.owncloud.android.domain.spaces.usecases.GetPersonalAndProjectSpacesForAccountUseCase
import com.owncloud.android.domain.spaces.usecases.RefreshSpacesFromServerAsyncUseCase
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.presentation.documentsprovider.cursors.FileCursor
import com.owncloud.android.presentation.documentsprovider.cursors.RootCursor
import com.owncloud.android.presentation.documentsprovider.cursors.SpaceCursor
import com.owncloud.android.presentation.settings.security.SettingsSecurityFragment.Companion.PREFERENCE_LOCK_ACCESS_FROM_DOCUMENT_PROVIDER
import com.owncloud.android.usecases.synchronization.SynchronizeFileUseCase
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase
import com.owncloud.android.usecases.transfers.downloads.DownloadFileUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromSystemUseCase
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Vector

class DocumentsStorageProvider : DocumentsProvider() {
    /**
     * If a directory requires to sync, it will write the id of the directory into this variable.
     * After the sync function gets triggered again over the same directory, it will see that a sync got already
     * triggered, and does not need to be triggered again. This way a endless loop is prevented.
     */
    private var requestedFolderIdForSync: Long = -1
    private var syncRequired = true

    private var spacesSyncRequired = true

    private lateinit var fileToUpload: OCFile

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor? {
        Timber.d("Trying to open $documentId in mode $mode")

        // If documentId == NONEXISTENT_DOCUMENT_ID only Upload is needed because file does not exist in our database yet.
        var ocFile: OCFile
        val uploadOnly: Boolean = documentId == NONEXISTENT_DOCUMENT_ID || documentId == "null"

        var accessMode: Int = ParcelFileDescriptor.parseMode(mode)
        val isWrite: Boolean = mode.contains("w")

        if (!uploadOnly) {
            ocFile = getFileByIdOrException(documentId.toInt())

            if (!ocFile.isAvailableLocally) {
                val downloadFileUseCase: DownloadFileUseCase by inject()

                downloadFileUseCase(DownloadFileUseCase.Params(accountName = ocFile.owner, file = ocFile))

                do {
                    if (!waitOrGetCancelled(signal)) {
                        return null
                    }
                    ocFile = getFileByIdOrException(documentId.toInt())

                } while (!ocFile.isAvailableLocally)
            }
        } else {
            ocFile = fileToUpload
            accessMode = accessMode or ParcelFileDescriptor.MODE_CREATE
        }

        val fileToOpen = File(ocFile.storagePath)

        if (!isWrite) return ParcelFileDescriptor.open(fileToOpen, accessMode)

        val handler = Handler(MainApp.appContext.mainLooper)
        // Attach a close listener if the document is opened in write mode.
        try {
            return ParcelFileDescriptor.open(fileToOpen, accessMode, handler) {
                // Update the file with the cloud server. The client is done writing.
                Timber.d("A file with id $documentId has been closed! Time to synchronize it with server.")
                // If only needs to upload that file
                if (uploadOnly) {
                    ocFile.length = fileToOpen.length()
                    val uploadFilesUseCase: UploadFilesFromSystemUseCase by inject()
                    val uploadFilesUseCaseParams = UploadFilesFromSystemUseCase.Params(
                        accountName = ocFile.owner,
                        listOfLocalPaths = listOf(fileToOpen.path),
                        uploadFolderPath = ocFile.remotePath.substringBeforeLast(PATH_SEPARATOR).plus(PATH_SEPARATOR),
                        spaceId = ocFile.spaceId,
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        uploadFilesUseCase(uploadFilesUseCaseParams)
                    }
                } else {
                    Thread {
                        val synchronizeFileUseCase: SynchronizeFileUseCase by inject()
                        val result = synchronizeFileUseCase(
                            SynchronizeFileUseCase.Params(
                                fileToSynchronize = ocFile,
                            )
                        )
                        Timber.d("Synced ${ocFile.remotePath} from ${ocFile.owner} with result: $result")
                        if (result.getDataOrNull() is SynchronizeFileUseCase.SyncType.ConflictDetected) {
                            context?.let {
                                NotificationUtils.notifyConflict(
                                    fileInConflict = ocFile,
                                    account = AccountUtils.getOwnCloudAccountByName(it, ocFile.owner),
                                    context = it
                                )
                            }
                        }
                    }.start()
                }
            }
        } catch (e: IOException) {
            throw FileNotFoundException("Failed to open document with id $documentId and mode $mode")
        }
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?,
    ): Cursor {
        val resultCursor: MatrixCursor

        val folderId = try {
            parentDocumentId.toLong()
        } catch (numberFormatException: NumberFormatException) {
            null
        }

        // Folder id is null, so at this point we need to list the spaces for the account.
        if (folderId == null) {
            resultCursor = SpaceCursor(projection)

            val getPersonalAndProjectSpacesForAccountUseCase: GetPersonalAndProjectSpacesForAccountUseCase by inject()
            val getFileByRemotePathUseCase: GetFileByRemotePathUseCase by inject()

            getPersonalAndProjectSpacesForAccountUseCase(
                GetPersonalAndProjectSpacesForAccountUseCase.Params(
                    accountName = parentDocumentId,
                )
            ).forEach { space ->
                if (!space.isDisabled) {
                    getFileByRemotePathUseCase(
                        GetFileByRemotePathUseCase.Params(
                            owner = space.accountName,
                            remotePath = ROOT_PATH,
                            spaceId = space.id,
                        )
                    ).getDataOrNull()?.let { rootFolder ->
                        resultCursor.addSpace(space, rootFolder, context)
                    }
                }
            }

            /**
             * This will start syncing the spaces. User will only see this after updating his view with a
             * pull down, or by accessing the spaces folder.
             */
            if (spacesSyncRequired) {
                syncSpacesWithServer(parentDocumentId)
                resultCursor.setMoreToSync(true)
            }

            spacesSyncRequired = true
        } else {
            // Folder id is not null, so this is a regular folder
            resultCursor = FileCursor(projection)

            // Create result cursor before syncing folder again, in order to enable faster loading
            getFolderContent(folderId.toInt()).forEach { file -> resultCursor.addFile(file) }

            /**
             * This will start syncing the current folder. User will only see this after updating his view with a
             * pull down, or by accessing the folder again.
             */
            if (requestedFolderIdForSync != folderId && syncRequired) {
                // register for sync
                syncDirectoryWithServer(parentDocumentId)
                requestedFolderIdForSync = folderId
                resultCursor.setMoreToSync(true)
            } else {
                requestedFolderIdForSync = -1
            }

            syncRequired = true
        }

        // Create notification listener
        val notifyUri: Uri = toNotifyUri(toUri(parentDocumentId))
        resultCursor.setNotificationUri(context?.contentResolver, notifyUri)

        return resultCursor

    }

    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        Timber.d("Query Document: $documentId")
        if (documentId == NONEXISTENT_DOCUMENT_ID) return FileCursor(projection).apply {
            addFile(fileToUpload)
        }

        val fileId = try {
            documentId.toInt()
        } catch (numberFormatException: NumberFormatException) {
            null
        }

        return if (fileId != null) {
            // file id is not null, this is a regular file.
            FileCursor(projection).apply {
                addFile(getFileByIdOrException(fileId))
            }
        } else {
            // file id is null, so at this point this is the root folder for spaces supported account.
            SpaceCursor(projection).apply {
                addRootForSpaces(context = context, accountName = documentId)
            }
        }
    }

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<String>?): Cursor {
        val result = RootCursor(projection)
        val contextApp = context ?: return result
        val accounts = AccountUtils.getAccounts(contextApp)

        // If access from document provider is not allowed, return empty cursor
        val preferences: SharedPreferencesProvider by inject()
        val lockAccessFromDocumentProvider = preferences.getBoolean(PREFERENCE_LOCK_ACCESS_FROM_DOCUMENT_PROVIDER, false)
        if (lockAccessFromDocumentProvider && accounts.isNotEmpty()) {
            return result.apply { addProtectedRoot(contextApp) }
        }

        for (account in accounts) {
            val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase by inject()
            val capabilities = getStoredCapabilitiesUseCase(
                GetStoredCapabilitiesUseCase.Params(
                    accountName = account.name
                )
            )
            val spacesFeatureAllowedForAccount = AccountUtils.isSpacesFeatureAllowedForAccount(contextApp, account, capabilities)

            result.addRoot(account, contextApp, spacesFeatureAllowedForAccount)
        }
        return result
    }

    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point?,
        signal: CancellationSignal?
    ): AssetFileDescriptor {
        // TODO: Show thumbnail for spaces
        val file = getFileByIdOrException(documentId.toInt())

        val realFile = File(file.storagePath)

        return AssetFileDescriptor(
            ParcelFileDescriptor.open(realFile, ParcelFileDescriptor.MODE_READ_ONLY), 0, AssetFileDescriptor.UNKNOWN_LENGTH
        )
    }

    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<String>?
    ): Cursor {
        val result = FileCursor(projection)

        val root = getFileByPathOrException(ROOT_PATH, AccountUtils.getCurrentOwnCloudAccount(context).name)

        for (f in findFiles(root, query)) {
            result.addFile(f)
        }

        return result
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String,
    ): String {
        Timber.d("Create Document ParentID $parentDocumentId Type $mimeType DisplayName $displayName")
        val parentDocument = getFileByIdOrException(parentDocumentId.toInt())

        return if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
            createFolder(parentDocument, displayName)
        } else {
            createFile(parentDocument, mimeType, displayName)
        }
    }

    override fun renameDocument(documentId: String, displayName: String): String? {
        Timber.d("Trying to rename $documentId to $displayName")

        val file = getFileByIdOrException(documentId.toInt())

        val renameFileUseCase: RenameFileUseCase by inject()
        renameFileUseCase(RenameFileUseCase.Params(file, displayName)).also {
            checkUseCaseResult(
                it, file.parentId.toString()
            )
        }

        return null
    }

    override fun deleteDocument(documentId: String) {
        Timber.d("Trying to delete $documentId")
        val file = getFileByIdOrException(documentId.toInt())

        val removeFileUseCase: RemoveFileUseCase by inject()
        removeFileUseCase(RemoveFileUseCase.Params(listOf(file), false)).also {
            checkUseCaseResult(
                it, file.parentId.toString()
            )
        }
    }

    override fun copyDocument(sourceDocumentId: String, targetParentDocumentId: String): String {
        Timber.d("Trying to copy $sourceDocumentId to $targetParentDocumentId")

        val sourceFile = getFileByIdOrException(sourceDocumentId.toInt())
        val targetParentFile = getFileByIdOrException(targetParentDocumentId.toInt())

        val copyFileUseCase: CopyFileUseCase by inject()

        copyFileUseCase(
            CopyFileUseCase.Params(
                listOfFilesToCopy = listOf(sourceFile),
                targetFolder = targetParentFile,
                replace = listOf(false),
                isUserLogged = AccountUtils.getCurrentOwnCloudAccount(context) != null,
            )
        ).also { result ->
            syncRequired = false
            checkUseCaseResult(result, targetParentFile.id.toString())
            // Returns the document id of the document copied at the target destination
            var newPath = targetParentFile.remotePath + sourceFile.fileName
            if (sourceFile.isFolder) newPath += File.separator
            val newFile = getFileByPathOrException(newPath, targetParentFile.owner)
            return newFile.id.toString()
        }
    }

    override fun moveDocument(
        sourceDocumentId: String,
        sourceParentDocumentId: String,
        targetParentDocumentId: String,
    ): String {
        Timber.d("Trying to move $sourceDocumentId to $targetParentDocumentId")

        val sourceFile = getFileByIdOrException(sourceDocumentId.toInt())
        val targetParentFile = getFileByIdOrException(targetParentDocumentId.toInt())

        val moveFileUseCase: MoveFileUseCase by inject()

        moveFileUseCase(
            MoveFileUseCase.Params(
                listOfFilesToMove = listOf(sourceFile),
                targetFolder = targetParentFile,
                replace = listOf(false),
                isUserLogged = AccountUtils.getCurrentOwnCloudAccount(context) != null,
            )
        ).also { result ->
            syncRequired = false
            checkUseCaseResult(result, targetParentFile.id.toString())
            // Returns the document id of the document moved to the target destination
            var newPath = targetParentFile.remotePath + sourceFile.fileName
            if (sourceFile.isFolder) newPath += File.separator
            val newFile = getFileByPathOrException(newPath, targetParentFile.owner)
            return newFile.id.toString()
        }
    }

    private fun checkUseCaseResult(result: UseCaseResult<Any>, folderToNotify: String) {
        if (!result.isSuccess) {
            Timber.e(result.getThrowableOrNull()!!)
            if (result.getThrowableOrNull() is FileNameException) {
                throw UnsupportedOperationException("Operation contains at least one invalid character")
            }
            if (result.getThrowableOrNull() !is NoConnectionWithServerException) {
                notifyChangeInFolder(folderToNotify)
            }
            throw FileNotFoundException("Remote Operation failed")
        }
        syncRequired = false
        notifyChangeInFolder(folderToNotify)
    }

    private fun createFolder(parentDocument: OCFile, displayName: String): String {
        Timber.d("Trying to create a new folder with name $displayName and parent ${parentDocument.remotePath}")

        val createFolderAsyncUseCase: CreateFolderAsyncUseCase by inject()

        createFolderAsyncUseCase(CreateFolderAsyncUseCase.Params(displayName, parentDocument)).run {
            checkUseCaseResult(this, parentDocument.id.toString())
            val newPath = parentDocument.remotePath + displayName + File.separator
            val newFolder = getFileByPathOrException(newPath, parentDocument.owner, parentDocument.spaceId)
            return newFolder.id.toString()
        }
    }

    private fun createFile(
        parentDocument: OCFile,
        mimeType: String,
        displayName: String,
    ): String {
        // We just need to return a Document ID, so we'll return an empty one. File does not exist in our db yet.
        // File will be created at [openDocument] method.
        val tempDir = File(FileStorageUtils.getTemporalPath(parentDocument.owner, parentDocument.spaceId))
        val newFile = File(tempDir, displayName)
        newFile.parentFile?.mkdirs()
        fileToUpload = OCFile(
            remotePath = parentDocument.remotePath + displayName,
            mimeType = mimeType,
            parentId = parentDocument.id,
            owner = parentDocument.owner,
            spaceId = parentDocument.spaceId
        ).apply {
            storagePath = newFile.path
        }

        return NONEXISTENT_DOCUMENT_ID
    }

    private fun syncDirectoryWithServer(parentDocumentId: String) {
        Timber.d("Trying to sync $parentDocumentId with server")
        val folderToSync = getFileByIdOrException(parentDocumentId.toInt())

        val synchronizeFolderUseCase: SynchronizeFolderUseCase by inject()
        val synchronizeFolderUseCaseParams = SynchronizeFolderUseCase.Params(
            remotePath = folderToSync.remotePath,
            accountName = folderToSync.owner,
            spaceId = folderToSync.spaceId,
            syncMode = SynchronizeFolderUseCase.SyncFolderMode.REFRESH_FOLDER,
        )

        CoroutineScope(Dispatchers.IO).launch {
            val useCaseResult = synchronizeFolderUseCase(synchronizeFolderUseCaseParams)
            Timber.d("${folderToSync.remotePath} from ${folderToSync.owner} was synced with server with result: $useCaseResult")

            if (useCaseResult.isSuccess) {
                notifyChangeInFolder(parentDocumentId)
            }
        }
    }

    private fun syncSpacesWithServer(parentDocumentId: String) {
        Timber.d("Trying to sync spaces from account $parentDocumentId with server")

        val refreshSpacesFromServerAsyncUseCase: RefreshSpacesFromServerAsyncUseCase by inject()
        val refreshSpacesFromServerAsyncUseCaseParams = RefreshSpacesFromServerAsyncUseCase.Params(
            accountName = parentDocumentId,
        )

        CoroutineScope(Dispatchers.IO).launch {
            val useCaseResult = refreshSpacesFromServerAsyncUseCase(refreshSpacesFromServerAsyncUseCaseParams)
            Timber.d("Spaces from account were synced with server with result: $useCaseResult")

            if (useCaseResult.isSuccess) {
                notifyChangeInFolder(parentDocumentId)
            }
            spacesSyncRequired = false
        }
    }

    private fun waitOrGetCancelled(cancellationSignal: CancellationSignal?): Boolean {
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            return false
        }

        return cancellationSignal == null || !cancellationSignal.isCanceled
    }

    private fun findFiles(root: OCFile, query: String): Vector<OCFile> {
        val result = Vector<OCFile>()

        val folderContent = getFolderContent(root.id!!.toInt())
        folderContent.forEach {
            if (it.fileName.contains(query)) {
                result.add(it)
                if (it.isFolder) result.addAll(findFiles(it, query))
            }
        }
        return result
    }

    private fun notifyChangeInFolder(folderToNotify: String) {
        context?.contentResolver?.notifyChange(toNotifyUri(toUri(folderToNotify)), null)
    }

    private fun toNotifyUri(uri: Uri): Uri = DocumentsContract.buildDocumentUri(
        context?.resources?.getString(R.string.document_provider_authority), uri.toString()
    )

    private fun toUri(documentId: String): Uri = Uri.parse(documentId)

    private fun getFileByIdOrException(id: Int): OCFile {
        val getFileByIdUseCase: GetFileByIdUseCase by inject()
        val result = getFileByIdUseCase(GetFileByIdUseCase.Params(id.toLong()))
        return result.getDataOrNull() ?: throw FileNotFoundException("File $id not found")
    }

    private fun getFileByPathOrException(remotePath: String, accountName: String, spaceId: String? = null): OCFile {
        val getFileByRemotePathUseCase: GetFileByRemotePathUseCase by inject()
        val result =
            getFileByRemotePathUseCase(GetFileByRemotePathUseCase.Params(owner = accountName, remotePath = remotePath, spaceId = spaceId))
        return result.getDataOrNull() ?: throw FileNotFoundException("File $remotePath not found")
    }

    private fun getFolderContent(id: Int): List<OCFile> {
        val getFolderContentUseCase: GetFolderContentUseCase by inject()
        val result = getFolderContentUseCase(GetFolderContentUseCase.Params(id.toLong()))
        return result.getDataOrNull() ?: throw FileNotFoundException("Folder $id not found")
    }

    companion object {
        const val NONEXISTENT_DOCUMENT_ID = "-1"
    }
}
