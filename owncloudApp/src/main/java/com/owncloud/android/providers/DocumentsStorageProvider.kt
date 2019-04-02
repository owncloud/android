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
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation
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
    private lateinit var documentsProviderAuthority: String

    override fun openDocument(documentId: String, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor? {
        val docId = documentId.toLong()
        updateCurrentStorageManagerIfNeeded(docId)

        var file = currentStorageManager!!.getFileById(docId)

        if (!file!!.isDown) {

            val i = Intent(context, FileDownloader::class.java).apply {
                putExtra(FileDownloader.KEY_ACCOUNT, currentStorageManager!!.account)
                putExtra(FileDownloader.KEY_FILE, file)
            }

            context!!.startService(i)

            do {
                if (!waitOrGetCancelled(signal)) {
                    return null
                }
                file = currentStorageManager!!.getFileById(docId)

            } while (!file!!.isDown)
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

        val browsedDir = currentStorageManager!!.getFileById(folderId)

        // Create result cursor before syncing folder again, in order to enable faster loading
        for (file in currentStorageManager!!.getFolderContent(browsedDir,false)) {
            resultCursor.addFile(file)
        }

        val notifyUri: Uri = toNotifyUri(toUri(parentDocumentId))
        resultCursor.setNotificationUri(context!!.contentResolver, notifyUri)

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
        Log_OC.d(TAG, "Query Document:$documentId")
        val docId = documentId.toLong()
        updateCurrentStorageManagerIfNeeded(docId)

        val result = FileCursor(projection)
        result.addFile(currentStorageManager!!.getFileById(docId))

        return result
    }

    override fun onCreate(): Boolean {
        documentsProviderAuthority = context!!.resources.getString(R.string.document_provider_authority)
        return true
    }

    override fun queryRoots(projection: Array<String>?): Cursor {
        initiateStorageMap()
        val result = RootCursor(projection)

        for (account in AccountUtils.getAccounts(context)) {
            result.addRoot(account, context!!)
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

        val file = currentStorageManager!!.getFileById(docId)

        val realFile = File(file!!.storagePath)

        return AssetFileDescriptor(
            ParcelFileDescriptor.open(realFile, ParcelFileDescriptor.MODE_READ_ONLY),
            0,
            AssetFileDescriptor.UNKNOWN_LENGTH
        )
    }

    override fun querySearchDocuments(rootId: String, query: String, projection: Array<String>): Cursor {
        updateCurrentStorageManagerIfNeeded(rootId)

        val root = currentStorageManager!!.getFileByPath(OCFile.ROOT_PATH)
        val result = FileCursor(projection)

        for (f in findFiles(root, query)) {
            result.addFile(f)
        }

        return result
    }

    @TargetApi(21)
    override fun renameDocument(documentId: String, displayName: String): String? {
        val docId = documentId.toLong()

        updateCurrentStorageManagerIfNeeded(docId)

        val file = currentStorageManager?.getFileById(docId) ?: throw FileNotFoundException("File $docId not found")
        Log_OC.d(TAG, "Trying to rename ${file.fileName} to $displayName")

        val renameFileOperation = RenameFileOperation(file.remotePath, displayName)
        val result = renameFileOperation.execute(currentStorageManager, context)

        if (!result.isSuccess) {
            context?.contentResolver?.notifyChange(toNotifyUri(toUri(file.parentId.toString())), null)
            throw java.lang.UnsupportedOperationException("Rename failed")
        } else {
            syncRequired = false
            context?.contentResolver?.notifyChange(toNotifyUri(toUri(file.parentId.toString())), null)
        }

        return null
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
        val contentResolver = context!!.contentResolver

        for (account in AccountUtils.getAccounts(context)) {
            val storageManager = FileDataStorageManager(context, account, contentResolver)
            val rootDir = storageManager.getFileByPath(OCFile.ROOT_PATH)
            rootIdToStorageManager[rootDir!!.fileId] = storageManager
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
        )
        refreshFolderOperation.syncVersionAndProfileEnabled(false)

        val contentResolver = context!!.contentResolver

        val thread = Thread {
            refreshFolderOperation.execute(currentStorageManager, context)
            contentResolver.notifyChange(toNotifyUri(toUri(parentDocumentId)), null)
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
        for (f in currentStorageManager!!.getFolderContent(root,false)) {
            if (f.isFolder) {
                result.addAll(findFiles(f, query))
            } else {
                if (f.fileName.contains(query)) {
                    result.add(f)
                }
            }
        }
        return result
    }

    private fun toNotifyUri(uri: Uri): Uri {
        return DocumentsContract.buildDocumentUri(
            documentsProviderAuthority, uri.toString()
        )
    }

    private fun toUri(documentId: String): Uri {
        return Uri.parse(documentId)
    }

    companion object {
        private val TAG = DocumentsStorageProvider::class.java.toString()
        private var rootIdToStorageManager: MutableMap<Long, FileDataStorageManager> = HashMap()
    }
}