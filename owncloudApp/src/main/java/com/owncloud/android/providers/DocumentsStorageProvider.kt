/**
 * ownCloud Android client application
 *
 * @author Bartosz Przybylski
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Abel García de Prada
 * Copyright (C) 2015  Bartosz Przybylski
 * Copyright (C) 2019 ownCloud GmbH.
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

import android.annotation.TargetApi
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.graphics.Point
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.services.FileDownloader
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.providers.cursors.FileCursor
import com.owncloud.android.providers.cursors.RootCursor
import java.io.File
import java.io.FileNotFoundException
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

    override fun openDocument(documentId: String, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor? {
        val docId = documentId.toLong()
        updateCurrentStorageManagerIfNeeded(docId)

        var file = currentStorageManager?.getFileById(docId)
            ?: throw FileNotFoundException("Failed to open document with id $documentId and mode $mode")

        if (!file.isDown) {

            val i = Intent(context, FileDownloader::class.java).apply {
                putExtra(FileDownloader.KEY_ACCOUNT, currentStorageManager!!.account)
                putExtra(FileDownloader.KEY_FILE, file)
            }

            context?.startService(i)

            do {
                if (!waitOrGetCancelled(signal)) {
                    return null
                }
                file = currentStorageManager?.getFileById(docId)
                    ?: throw FileNotFoundException("Failed to open document with id $documentId and mode $mode")

            } while (!file.isDown)
        }

        return ParcelFileDescriptor.open(File(file.storagePath), ParcelFileDescriptor.MODE_READ_ONLY)
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
        currentStorageManager?.getFolderContent(currentStorageManager?.getFileById(folderId), false)
            ?.forEach { file -> resultCursor.addFile(file) }

        //Create notification listener
        val notifyUri: Uri = toNotifyUri(toUri(parentDocumentId))
        resultCursor.setNotificationUri(context?.contentResolver, notifyUri)

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
        return resultCursor

    }

    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        Log_OC.d(TAG, "Query Document: $documentId")
        val docId = documentId.toLong()
        updateCurrentStorageManagerIfNeeded(docId)

        return FileCursor(projection).apply {
            val ocFile: OCFile? = currentStorageManager?.getFileById(docId)
            if (ocFile != null) addFile(ocFile)
        }
    }

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<String>?): Cursor {
        initiateStorageMap()
        val result = RootCursor(projection)
        val contextApp = context ?: return result

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

        val file = currentStorageManager?.getFileById(docId)

        val realFile = File(file?.storagePath)

        return AssetFileDescriptor(
            ParcelFileDescriptor.open(realFile, ParcelFileDescriptor.MODE_READ_ONLY),
            0,
            AssetFileDescriptor.UNKNOWN_LENGTH
        )
    }

    override fun querySearchDocuments(rootId: String, query: String, projection: Array<String>?): Cursor {
        updateCurrentStorageManagerIfNeeded(rootId)

        val result = FileCursor(projection)

        val root = currentStorageManager?.getFileByPath(OCFile.ROOT_PATH) ?: return result

        for (f in findFiles(root, query)) {
            result.addFile(f)
        }

        return result
    }

    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        Log_OC.d(TAG, "Create Document ParentID $parentDocumentId Type $mimeType DisplayName $displayName")
        val parentDocId = parentDocumentId.toLong()
        updateCurrentStorageManagerIfNeeded(parentDocId)

        val parentDocument = currentStorageManager?.getFileById(parentDocId)
            ?: throw FileNotFoundException("Folder $parentDocId not found")

        return if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
            createFolder(parentDocument, displayName)
        } else {
            Log_OC.d(TAG, "Not Supported yet")
            super.createDocument(parentDocumentId, mimeType, displayName)
        }
    }

    @TargetApi(21)
    override fun renameDocument(documentId: String, displayName: String): String? {
        val docId = documentId.toLong()

        updateCurrentStorageManagerIfNeeded(docId)

        val file = currentStorageManager?.getFileById(docId) ?: throw FileNotFoundException("File $docId not found")
        Log_OC.d(TAG, "Trying to rename ${file.fileName} to $displayName")

        RenameFileOperation(file.remotePath, displayName).apply {
            execute(currentStorageManager, context).also { checkOperationResult(it, file.parentId.toString()) }
        }

        return null
    }

    override fun deleteDocument(documentId: String) {
        val docId = documentId.toLong()

        updateCurrentStorageManagerIfNeeded(docId)

        val file = currentStorageManager?.getFileById(docId) ?: throw FileNotFoundException("File $docId not found")
        Log_OC.d(TAG, "Trying to delete ${file.fileName} with id ${file.fileId}")

        RemoveFileOperation(file.remotePath, false).apply {
            execute(currentStorageManager, context).also { checkOperationResult(it, file.parentId.toString()) }
        }
    }

    private fun checkOperationResult(result: RemoteOperationResult<Any>, folderToNotify: String) {
        if (!result.isSuccess) {
            if (result.code != RemoteOperationResult.ResultCode.WRONG_CONNECTION) notifyChangeInFolder(folderToNotify)
            throw FileNotFoundException("Remote Operation failed due to ${result.exception.message}")
        }
        syncRequired = false
        notifyChangeInFolder(folderToNotify)
    }

    private fun createFolder(parentDocument: OCFile, displayName: String): String {
        val newPath = parentDocument.remotePath + displayName + OCFile.PATH_SEPARATOR

        Log_OC.d(TAG, "Trying to create folder with path $newPath")

        CreateFolderOperation(newPath, false).apply {
            execute(currentStorageManager, context).also { result ->
                checkOperationResult(result, parentDocument.fileId.toString())
                val newFolder = currentStorageManager?.getFileByPath(newPath)
                    ?: throw FileNotFoundException("Folder $newPath not found")
                return newFolder.fileId.toString()
            }
        }
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
            val storageManager = FileDataStorageManager(context, account, context?.contentResolver)
            val rootDir = storageManager.getFileByPath(OCFile.ROOT_PATH)
            rootIdToStorageManager[rootDir.fileId] = storageManager
        }
    }

    private fun syncDirectoryWithServer(parentDocumentId: String) {
        val folderId = parentDocumentId.toLong()
        val refreshFolderOperation = RefreshFolderOperation(
            currentStorageManager?.getFileById(folderId),
            false,
            false,
            currentStorageManager?.account,
            context
        ).apply { syncVersionAndProfileEnabled(false) }

        val thread = Thread {
            refreshFolderOperation.execute(currentStorageManager, context)
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

        val folderContent = currentStorageManager?.getFolderContent(root, false) ?: return result

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
        context?.resources?.getString(R.string.document_provider_authority),
        uri.toString()
    )

    private fun toUri(documentId: String): Uri = Uri.parse(documentId)

    companion object {
        private val TAG = DocumentsStorageProvider::class.java.toString()
        private var rootIdToStorageManager: MutableMap<Long, FileDataStorageManager> = HashMap()
    }
}