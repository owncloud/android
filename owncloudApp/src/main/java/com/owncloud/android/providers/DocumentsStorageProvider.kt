/**
 * ownCloud Android client application
 *
 * @author Bartosz Przybylski
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Shashvat Kedia
 * Copyright (C) 2015  Bartosz Przybylski
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

package com.owncloud.android.providers

import android.accounts.Account
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.graphics.Point
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.preference.PreferenceManager
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.files.services.FileDownloader
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.files.services.TransferRequester
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.operations.CopyFileOperation
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.MoveFileOperation
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.providers.cursors.FileCursor
import com.owncloud.android.providers.cursors.RootCursor
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.NotificationUtils
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.HashMap
import java.util.Vector

class DocumentsStorageProvider : DocumentsProvider() {
    /**
     * If a directory requires to sync, it will write the id of the directory into this variable.
     * After the sync function gets triggered again over the same directory, it will see that a sync got already
     * triggered, and does not need to be triggered again. This way a endless loop is prevented.
     */
    private var requestedFolderIdForSync: Long = -1
    private var syncRequired = true
    private var currentStorageManager: FileDataStorageManager? = null
    private lateinit var fileToUpload: OCFile

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor? {
        Timber.d("Trying to open $documentId in mode $mode")

        // If documentId == NONEXISTENT_DOCUMENT_ID only Upload is needed because file does not exist in our database yet.
        var ocFile: OCFile
        val uploadOnly: Boolean = documentId == NONEXISTENT_DOCUMENT_ID

        var accessMode: Int = ParcelFileDescriptor.parseMode(mode)
        val isWrite: Boolean = mode.contains("w")

        if (!uploadOnly) {
            val docId = documentId.toLong()
            updateCurrentStorageManagerIfNeeded(docId)

            ocFile = getFileByIdOrException(docId)

            if (!ocFile.isDown()) {
                val intent = Intent(context, FileDownloader::class.java).apply {
                    putExtra(FileDownloader.KEY_ACCOUNT, getAccountFromFileId(docId))
                    putExtra(FileDownloader.KEY_FILE, ocFile)
                }

                context?.startService(intent)

                do {
                    if (!waitOrGetCancelled(signal)) {
                        return null
                    }
                    ocFile = getFileByIdOrException(docId)

                } while (!ocFile.isDown())
            }
        } else {
            ocFile = fileToUpload
            accessMode = accessMode or ParcelFileDescriptor.MODE_CREATE
        }

        val fileToOpen = File(ocFile.storagePath)

        if (!isWrite) return ParcelFileDescriptor.open(fileToOpen, accessMode)

        val handler = Handler(context?.mainLooper)
        // Attach a close listener if the document is opened in write mode.
        try {
            return ParcelFileDescriptor.open(fileToOpen, accessMode, handler) {
                // Update the file with the cloud server. The client is done writing.
                Timber.d("A file with id $documentId has been closed! Time to synchronize it with server.")
                // If only needs to upload that file
                if (uploadOnly) {
                    ocFile.length = fileToOpen.length()
                    TransferRequester().run {
                        uploadNewFile(
                            context,
                            currentStorageManager?.account,
                            ocFile.storagePath,
                            ocFile.remotePath,
                            FileUploader.LOCAL_BEHAVIOUR_COPY,
                            ocFile.mimeType,
                            false,
                            UploadFileOperation.CREATED_BY_USER
                        )
                    }
                } else {
                    Thread {
                        SynchronizeFileOperation(
                            ocFile,
                            null,
                            getAccountFromFileId(ocFile.id!!),
                            false,
                            context,
                            false
                        ).apply {
                            val result = execute(currentStorageManager, context)
                            if (result.code == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                                context?.let {
                                    NotificationUtils.notifyConflict(
                                        ocFile,
                                        getAccountFromFileId(ocFile.id!!),
                                        it
                                    )
                                }
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
        sortOrder: String?
    ): Cursor {
        val folderId = parentDocumentId.toLong()
        updateCurrentStorageManagerIfNeeded(folderId)

        val resultCursor = FileCursor(projection)

        // Create result cursor before syncing folder again, in order to enable faster loading
        currentStorageManager?.getFolderContent(currentStorageManager?.getFileById(folderId))
            ?.forEach { file -> resultCursor.addFile(file) }

        //Create notification listener
        val notifyUri: Uri = toNotifyUri(toUri(parentDocumentId))
        resultCursor.setNotificationUri(context?.contentResolver, notifyUri)

        /**
         * This will start syncing the current folder. User will only see this after updating his view with a
         * pull down, or by accessing the folder again.
         */
        if (requestedFolderIdForSync != folderId && syncRequired && currentStorageManager != null) {
            // register for sync
            syncDirectoryWithServer(parentDocumentId)
            requestedFolderIdForSync = folderId
            resultCursor.setMoreToSync(true)
        } else {
            requestedFolderIdForSync = -1
        }

        syncRequired = true
        return resultCursor

    }

    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        Timber.d("Query Document: $documentId")
        if (documentId == NONEXISTENT_DOCUMENT_ID) return FileCursor(projection).apply {
            addFile(fileToUpload)
        }

        val docId = documentId.toLong()
        updateCurrentStorageManagerIfNeeded(docId)

        return FileCursor(projection).apply {
            getFileById(docId)?.let { addFile(it) }
        }
    }

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<String>?): Cursor {
        val result = RootCursor(projection)
        val contextApp = context ?: return result
        // If OwnCloud is protected with passcode or pattern, return empty cursor.
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val passCodeState = preferences.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
        val patternState = preferences.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)
        if (passCodeState || patternState) {
            return result.apply { addProtectedRoot(contextApp, passCodeState) }
        }

        initiateStorageMap()

        for (account in AccountUtils.getAccounts(contextApp)) {
            result.addRoot(account, contextApp)
        }
        return result
    }

    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point?,
        signal: CancellationSignal?
    ): AssetFileDescriptor {

        val docId = documentId.toLong()
        updateCurrentStorageManagerIfNeeded(docId)

        val file = getFileById(docId)

        val realFile = File(file?.storagePath)

        return AssetFileDescriptor(
            ParcelFileDescriptor.open(realFile, ParcelFileDescriptor.MODE_READ_ONLY),
            0,
            AssetFileDescriptor.UNKNOWN_LENGTH
        )
    }

    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<String>?
    ): Cursor {
        updateCurrentStorageManagerIfNeeded(rootId)

        val result = FileCursor(projection)

        val root = getFileByPath(OCFile.ROOT_PATH) ?: return result

        for (f in findFiles(root, query)) {
            result.addFile(f)
        }

        return result
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String
    ): String {
        Timber.d("Create Document ParentID $parentDocumentId Type $mimeType DisplayName $displayName")
        val parentDocId = parentDocumentId.toLong()
        updateCurrentStorageManagerIfNeeded(parentDocId)

        val parentDocument = getFileByIdOrException(parentDocId)

        return if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
            createFolder(parentDocument, displayName)
        } else {
            createFile(parentDocument, mimeType, displayName)
        }
    }

    override fun renameDocument(documentId: String, displayName: String): String? {
        Timber.d("Trying to rename $documentId to $displayName")
        val docId = documentId.toLong()

        updateCurrentStorageManagerIfNeeded(docId)

        val file = getFileByIdOrException(docId)

        RenameFileOperation(file.remotePath, displayName).apply {
            execute(currentStorageManager, context).also {
                checkOperationResult(
                    it,
                    file.parentId.toString()
                )
            }
        }

        return null
    }

    override fun deleteDocument(documentId: String) {
        Timber.d("Trying to delete $documentId")
        val docId = documentId.toLong()

        updateCurrentStorageManagerIfNeeded(docId)

        val file = getFileByIdOrException(docId)

        RemoveFileOperation(file.remotePath, false, true).apply {
            execute(currentStorageManager, context).also {
                checkOperationResult(
                    it,
                    file.parentId.toString()
                )
            }
        }
    }

    override fun copyDocument(sourceDocumentId: String, targetParentDocumentId: String): String {
        Timber.d("Trying to copy $sourceDocumentId to $targetParentDocumentId")

        val sourceDocId = sourceDocumentId.toLong()
        updateCurrentStorageManagerIfNeeded(sourceDocId)

        val sourceFile = getFileByIdOrException(sourceDocId)

        val targetParentDocId = targetParentDocumentId.toLong()
        val targetParentFile = getFileByIdOrException(targetParentDocId)

        CopyFileOperation(
            sourceFile.remotePath,
            targetParentFile.remotePath
        ).apply {
            execute(currentStorageManager, context).also { result ->
                syncRequired = false
                checkOperationResult(result, targetParentFile.id.toString())
                //Returns the document id of the document copied at the target destination
                var newPath = targetParentFile.remotePath + sourceFile.name
                if (sourceFile.isFolder) {
                    newPath += File.separator
                }
                val newFile = getFileByPathOrException(newPath)
                return newFile.id.toString()
            }
        }
    }

    override fun moveDocument(
        sourceDocumentId: String, sourceParentDocumentId: String, targetParentDocumentId: String
    ): String {
        Timber.d("Trying to move $sourceDocumentId to $targetParentDocumentId")

        val sourceDocId = sourceDocumentId.toLong()
        updateCurrentStorageManagerIfNeeded(sourceDocId)

        val sourceFile = getFileByIdOrException(sourceDocId)

        val targetParentDocId = targetParentDocumentId.toLong()
        val targetParentFile = getFileByIdOrException(targetParentDocId)

        MoveFileOperation(
            sourceFile.remotePath,
            targetParentFile.remotePath
        ).apply {
            execute(currentStorageManager, context).also { result ->
                syncRequired = false
                checkOperationResult(result, targetParentFile.id.toString())
                //Returns the document id of the document moved to the target destination
                var newPath = targetParentFile.remotePath + sourceFile.name
                if (sourceFile.isFolder) newPath += File.separator
                val newFile = getFileByPathOrException(newPath)
                return newFile.id.toString()
            }
        }
    }

    private fun checkOperationResult(result: RemoteOperationResult<Any>, folderToNotify: String) {
        if (!result.isSuccess) {
            if (result.code != RemoteOperationResult.ResultCode.WRONG_CONNECTION) notifyChangeInFolder(
                folderToNotify
            )
            throw FileNotFoundException("Remote Operation failed")
        }
        syncRequired = false
        notifyChangeInFolder(folderToNotify)
    }

    private fun createFolder(parentDocument: OCFile, displayName: String): String {
        val newPath = parentDocument.remotePath + displayName + File.separator

        if (!FileUtils.isValidName(displayName)) {
            throw UnsupportedOperationException("Folder $displayName contains at least one invalid character")
        }
        Timber.d("Trying to create folder with path $newPath")

        CreateFolderOperation(newPath, false).apply {
            execute(currentStorageManager, context).also { result ->
                checkOperationResult(result, parentDocument.id.toString())
                val newFolder = getFileByPathOrException(newPath)
                return newFolder.id.toString()
            }
        }
    }

    private fun createFile(parentDocument: OCFile, mimeType: String, displayName: String): String {
        // We just need to return a Document ID, so we'll return an empty one. File does not exist in our db yet.
        // File will be created at [openDocument] method.
        val tempDir =
            File(FileStorageUtils.getTemporalPath(getAccountFromFileId(parentDocument.id!!)?.name))
        val newFile = File(tempDir, displayName)
        // FIXME: 13/10/2020 : New_arch: Migration
//        fileToUpload = OCFile(parentDocument.remotePath + displayName).apply {
//            mimeType = mimeType
//            parentId = parentDocument.id
//            storagePath = newFile.path
//        }

        return NONEXISTENT_DOCUMENT_ID
    }

    private fun updateCurrentStorageManagerIfNeeded(docId: Long) {
        if (rootIdToStorageManager.isEmpty()) {
            initiateStorageMap()
        }
        if (currentStorageManager == null || (
                    rootIdToStorageManager.containsKey(docId) &&
                            currentStorageManager !== rootIdToStorageManager[docId])
        ) {
            currentStorageManager = rootIdToStorageManager[docId]
        }
    }

    private fun updateCurrentStorageManagerIfNeeded(rootId: String) {
        if (rootIdToStorageManager.isEmpty()) {
            return
        }
        for (data in rootIdToStorageManager.values) {
            if (data.account.name == rootId) {
                currentStorageManager = data
            }
        }
    }

    private fun initiateStorageMap() {
        for (account in AccountUtils.getAccounts(context)) {
            val storageManager = FileDataStorageManager(MainApp.appContext, account, MainApp.appContext.contentResolver)
            val rootDir = storageManager.getFileByPath(OCFile.ROOT_PATH)
            rootIdToStorageManager[rootDir!!.id!!] = storageManager
        }
    }

    private fun syncDirectoryWithServer(parentDocumentId: String) {
        Timber.d("Trying to sync $parentDocumentId with server")
        val folderId = parentDocumentId.toLong()

        getFileByIdOrException(folderId)

        val refreshFolderOperation = RefreshFolderOperation(
            getFileById(folderId),
            false,
            getAccountFromFileId(folderId),
            context
        ).apply { syncVersionAndProfileEnabled(false) }

        val thread = Thread {
            refreshFolderOperation.execute(getStoreManagerFromFileId(folderId), context)
            notifyChangeInFolder(parentDocumentId)
        }
        thread.start()
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

        val folderContent = currentStorageManager?.getFolderContent(root) ?: return result
        folderContent.forEach {
            if (it.name!!.contains(query)) {
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
        context?.resources?.getString(R.string.document_provider_authority),
        uri.toString()
    )

    private fun toUri(documentId: String): Uri = Uri.parse(documentId)

    private fun getFileByIdOrException(id: Long): OCFile =
        getFileById(id) ?: throw FileNotFoundException("File $id not found")

    private fun getFileById(id: Long): OCFile? = getStoreManagerFromFileId(id)?.getFileById(id)

    private fun getAccountFromFileId(id: Long): Account? = getStoreManagerFromFileId(id)?.account

    private fun getStoreManagerFromFileId(id: Long): FileDataStorageManager? {
        // If file is found in current storage manager, return it
        currentStorageManager?.getFileById(id)?.let { return currentStorageManager }

        //  Else, look for it in other ones
        var fileFromOtherStorageManager: OCFile?
        var otherStorageManager: FileDataStorageManager? = null
        for (key in rootIdToStorageManager.keys) {
            otherStorageManager = rootIdToStorageManager[key]
            // Skip current storage manager, already checked
            if (otherStorageManager == currentStorageManager) continue
            fileFromOtherStorageManager = otherStorageManager?.getFileById(id)
            if (fileFromOtherStorageManager != null) {
                Timber.d("File with id $id found in storage manager: $otherStorageManager")
                break
            }
        }
        return otherStorageManager
    }

    private fun getFileByPathOrException(path: String): OCFile =
        getFileByPath(path) ?: throw FileNotFoundException("File $path not found")

    private fun getFileByPath(path: String): OCFile? = currentStorageManager?.getFileByPath(path)

    companion object {
        private var rootIdToStorageManager: MutableMap<Long, FileDataStorageManager> = HashMap()
        const val NONEXISTENT_DOCUMENT_ID = "-1"
    }
}
