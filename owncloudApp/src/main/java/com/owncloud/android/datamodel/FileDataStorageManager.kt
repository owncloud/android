/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Abel García de Prada
 *
 * Copyright (C) 2012  Bartek Przybylski
 * Copyright (C) 2019 ownCloud GmbH.
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
import android.content.ContentProviderClient
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.OperationApplicationException
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.FileUriExposedException
import android.os.RemoteException
import android.provider.MediaStore

import androidx.core.content.FileProvider
import androidx.core.util.Pair
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.data.sharing.shares.db.OCShareEntity
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.domain.capabilities.model.CapabilityBooleanType
import com.owncloud.android.domain.capabilities.model.OCCapability
import com.owncloud.android.domain.sharing.shares.model.ShareType
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.status.RemoteCapability
import com.owncloud.android.utils.FileStorageUtils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayList
import java.util.Collections
import java.util.HashSet
import java.util.Vector

class FileDataStorageManager {

    var contentResolver: ContentResolver? = null
        private set
    var contentProviderClient: ContentProviderClient? = null
        private set
    var account: Account? = null
    private var mContext: Context? = null

    /**
     * Get a collection with all the files set by the user as available offline, from all the accounts
     * in the device, putting away the folders
     *
     *
     * This is the only method working with a NULL account in [.mAccount]. Not something to do often.
     *
     * @return List with all the files set by the user as available offline.
     */
    // query for any favorite file in any OC account
    val availableOfflineFilesFromEveryAccount: List<Pair<OCFile, String>>
        get() {
            val result = ArrayList<Pair<OCFile, String>>()

            var cursorOnKeptInSync: Cursor? = null
            try {
                cursorOnKeptInSync = contentResolver!!.query(
                    ProviderTableMeta.CONTENT_URI, null,
                    ProviderTableMeta.FILE_KEEP_IN_SYNC + " = ? OR " +
                            ProviderTableMeta.FILE_KEEP_IN_SYNC + " = ?",
                    arrayOf(
                        OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE.value.toString(),
                        OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT.value.toString()
                    ), null
                )

                if (cursorOnKeptInSync != null && cursorOnKeptInSync.moveToFirst()) {
                    var file: OCFile?
                    var accountName: String
                    do {
                        file = createFileInstance(cursorOnKeptInSync)
                        accountName = cursorOnKeptInSync.getString(
                            cursorOnKeptInSync.getColumnIndex(ProviderTableMeta.FILE_ACCOUNT_OWNER)
                        )
                        if (!file!!.isFolder && AccountUtils.exists(accountName, mContext)) {
                            result.add(Pair(file, accountName))
                        }
                    } while (cursorOnKeptInSync.moveToNext())
                }

            } catch (e: Exception) {
                Log_OC.e(TAG, "Exception retrieving all the available offline files", e)

            } finally {
                cursorOnKeptInSync?.close()
            }

            return result
        }

    /**
     * Get a collection with all the files set by the user as available offline, from current account
     * putting away files whose parent is also available offline
     *
     * @return List with all the files set by current user as available offline.
     */
    // query for available offline files in current account and whose parent is not.
    val availableOfflineFilesFromCurrentAccount: Vector<OCFile>
        get() {
            val result = Vector<OCFile>()

            var cursorOnKeptInSync: Cursor? = null
            try {
                cursorOnKeptInSync = contentResolver!!.query(
                    ProviderTableMeta.CONTENT_URI, null,
                    "(" + ProviderTableMeta.FILE_KEEP_IN_SYNC + " = ? AND NOT " +
                            ProviderTableMeta.FILE_KEEP_IN_SYNC + " = ? ) AND " +
                            ProviderTableMeta.FILE_ACCOUNT_OWNER + " = ? ",
                    arrayOf(
                        OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE.value.toString(),
                        OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT.value.toString(),
                        account!!.name
                    ), null
                )

                if (cursorOnKeptInSync != null && cursorOnKeptInSync.moveToFirst()) {
                    var file: OCFile?
                    do {
                        file = createFileInstance(cursorOnKeptInSync)
                        result.add(file)
                    } while (cursorOnKeptInSync.moveToNext())
                }

            } catch (e: Exception) {
                Log_OC.e(TAG, "Exception retrieving all the available offline files", e)

            } finally {
                cursorOnKeptInSync?.close()
            }

            Collections.sort(result)
            return result
        }

    constructor(activity: Context, account: Account, cr: ContentResolver) {
        contentProviderClient = null
        contentResolver = cr
        this.account = account
        mContext = activity
    }

    constructor(activity: Context, account: Account, cp: ContentProviderClient) {
        contentProviderClient = cp
        contentResolver = null
        this.account = account
        mContext = activity
    }

    fun getFileByPath(path: String): OCFile? {
        val c = getFileCursorForValue(ProviderTableMeta.FILE_PATH, path)
        var file: OCFile? = null
        if (c != null) {
            if (c.moveToFirst()) {
                file = createFileInstance(c)
            }
            c.close()
        }
        return if (file == null && OCFile.ROOT_PATH == path) {
            createRootDir() // root should always exist
        } else file
    }

    fun getFileById(id: Long): OCFile? {
        val c = getFileCursorForValue(ProviderTableMeta._ID, id.toString())
        var file: OCFile? = null
        if (c != null) {
            if (c.moveToFirst()) {
                file = createFileInstance(c)
            }
            c.close()
        }
        return file
    }

    /**
     * This will return a OCFile by its given FileId here refered as the remoteId.
     * Its the fileId ownCloud Core uses to identify a file even if its name has changed.
     *
     *
     * An Explenation about how to use ETags an those FileIds can be found here:
     * [](https://github.com/owncloud/client/wiki/Etags-and-file-ids)
     *
     * @param remoteID
     * @return
     */
    fun getFileByRemoteId(remoteID: String): OCFile? {
        val c = getFileCursorForValue(ProviderTableMeta.FILE_REMOTE_ID, remoteID)
        var file: OCFile? = null
        if (c != null) {
            if (c.moveToFirst()) {
                file = createFileInstance(c)
            }
            c.close()
        }
        return file
    }

    fun getFileByLocalPath(path: String): OCFile? {
        val c = getFileCursorForValue(ProviderTableMeta.FILE_STORAGE_PATH, path)
        var file: OCFile? = null
        if (c != null) {
            if (c.moveToFirst()) {
                file = createFileInstance(c)
            }
            c.close()
        }
        return file
    }

    fun fileExists(id: Long): Boolean {
        return fileExists(ProviderTableMeta._ID, id.toString())
    }

    fun fileExists(path: String): Boolean {
        return fileExists(ProviderTableMeta.FILE_PATH, path)
    }

    fun getFolderContent(f: OCFile?, onlyAvailableOffline: Boolean): Vector<OCFile> {
        return if (f != null && f.isFolder && f.fileId != -1L) {
            getFolderContent(f.fileId, onlyAvailableOffline)
        } else {
            Vector()
        }
    }

    fun getFolderImages(folder: OCFile?): Vector<OCFile> {
        val ret = Vector<OCFile>()
        if (folder != null) {
            // TODO better implementation, filtering in the access to database instead of here
            val tmp = getFolderContent(folder, false)
            var current: OCFile
            for (i in tmp.indices) {
                current = tmp[i]
                if (current.isImage) {
                    ret.add(current)
                }
            }
        }
        return ret
    }

    fun saveFile(file: OCFile): Boolean {
        var overriden = false
        val cv = ContentValues()
        cv.put(ProviderTableMeta.FILE_MODIFIED, file.modificationTimestamp)
        cv.put(
            ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
            file.modificationTimestampAtLastSyncForData
        )
        cv.put(ProviderTableMeta.FILE_CREATION, file.creationTimestamp)
        cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, file.fileLength)
        cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, file.mimetype)
        cv.put(ProviderTableMeta.FILE_NAME, file.fileName)
        cv.put(ProviderTableMeta.FILE_PARENT, file.parentId)
        cv.put(ProviderTableMeta.FILE_PATH, file.remotePath)
        if (!file.isFolder) {
            cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.storagePath)
        }
        cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, account!!.name)
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, file.lastSyncDateForProperties)
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, file.lastSyncDateForData)
        cv.put(ProviderTableMeta.FILE_ETAG, file.etag)
        cv.put(ProviderTableMeta.FILE_TREE_ETAG, file.treeEtag)
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, if (file.isSharedViaLink) 1 else 0)
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, if (file.isSharedWithSharee) 1 else 0)
        cv.put(ProviderTableMeta.FILE_PERMISSIONS, file.permissions)
        cv.put(ProviderTableMeta.FILE_REMOTE_ID, file.remoteId)
        cv.put(ProviderTableMeta.FILE_UPDATE_THUMBNAIL, file.needsUpdateThumbnail())
        cv.put(ProviderTableMeta.FILE_IS_DOWNLOADING, file.isDownloading)
        cv.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, file.etagInConflict)
        cv.put(ProviderTableMeta.FILE_PRIVATE_LINK, file.privateLink)

        val sameRemotePath = fileExists(file.remotePath)
        if (sameRemotePath || fileExists(file.fileId)) {  // for renamed files; no more delete and create

            val oldFile: OCFile?
            if (sameRemotePath) {
                oldFile = getFileByPath(file.remotePath)
                file.fileId = oldFile!!.fileId
            } else {
                oldFile = getFileById(file.fileId)
            }

            overriden = true
            if (contentResolver != null) {
                contentResolver!!.update(
                    ProviderTableMeta.CONTENT_URI, cv,
                    ProviderTableMeta._ID + "=?",
                    arrayOf(file.fileId.toString())
                )
            } else {
                try {
                    contentProviderClient!!.update(
                        ProviderTableMeta.CONTENT_URI,
                        cv, ProviderTableMeta._ID + "=?",
                        arrayOf(file.fileId.toString())
                    )
                } catch (e: RemoteException) {
                    Log_OC.e(
                        TAG,
                        "Fail to insert insert file to database " + e.message
                    )
                }

            }

        } else {
            // new file
            setInitialAvailableOfflineStatus(file, cv)

            var result_uri: Uri? = null
            if (contentResolver != null) {
                result_uri = contentResolver!!.insert(
                    ProviderTableMeta.CONTENT_URI_FILE, cv
                )
            } else {
                try {
                    result_uri = contentProviderClient!!.insert(
                        ProviderTableMeta.CONTENT_URI_FILE, cv
                    )
                } catch (e: RemoteException) {
                    Log_OC.e(
                        TAG,
                        "Fail to insert insert file to database " + e.message
                    )
                }

            }
            if (result_uri != null) {
                val new_id = java.lang.Long.parseLong(result_uri.pathSegments[1])
                file.fileId = new_id
            }
        }

        return overriden
    }

    /**
     * Inserts or updates the list of files contained in a given folder.
     *
     *
     * CALLER IS THE RESPONSIBLE FOR GRANTING RIGHT UPDATE OF INFORMATION, NOT THIS METHOD.
     * HERE ONLY DATA CONSISTENCY SHOULD BE GRANTED
     *
     * @param folder
     * @param updatedFiles
     * @param filesToRemove
     */
    fun saveFolder(
        folder: OCFile, updatedFiles: Collection<OCFile>, filesToRemove: Collection<OCFile>
    ) {

        Log_OC.d(
            TAG, "Saving folder " + folder.remotePath + " with " + updatedFiles.size
                    + " children and " + filesToRemove.size + " files to remove"
        )

        val operations = ArrayList<ContentProviderOperation>(updatedFiles.size)

        // prepare operations to insert or update files to save in the given folder
        for (file in updatedFiles) {
            val cv = ContentValues()
            cv.put(ProviderTableMeta.FILE_MODIFIED, file.modificationTimestamp)
            cv.put(
                ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
                file.modificationTimestampAtLastSyncForData
            )
            cv.put(ProviderTableMeta.FILE_CREATION, file.creationTimestamp)
            cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, file.fileLength)
            cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, file.mimetype)
            cv.put(ProviderTableMeta.FILE_NAME, file.fileName)
            cv.put(ProviderTableMeta.FILE_PARENT, folder.fileId)
            cv.put(ProviderTableMeta.FILE_PATH, file.remotePath)
            if (!file.isFolder) {
                cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.storagePath)
            }
            cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, account!!.name)
            cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, file.lastSyncDateForProperties)
            cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, file.lastSyncDateForData)
            cv.put(ProviderTableMeta.FILE_ETAG, file.etag)
            cv.put(ProviderTableMeta.FILE_TREE_ETAG, file.treeEtag)
            cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, if (file.isSharedViaLink) 1 else 0)
            cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, if (file.isSharedWithSharee) 1 else 0)
            cv.put(ProviderTableMeta.FILE_PERMISSIONS, file.permissions)
            cv.put(ProviderTableMeta.FILE_REMOTE_ID, file.remoteId)
            cv.put(ProviderTableMeta.FILE_UPDATE_THUMBNAIL, file.needsUpdateThumbnail())
            cv.put(ProviderTableMeta.FILE_IS_DOWNLOADING, file.isDownloading)
            cv.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, file.etagInConflict)
            cv.put(ProviderTableMeta.FILE_PRIVATE_LINK, file.privateLink)

            val existsByPath = fileExists(file.remotePath)
            if (existsByPath || fileExists(file.fileId)) {
                // updating an existing file
                operations.add(
                    ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).withValues(cv).withSelection(
                        ProviderTableMeta._ID + "=?",
                        arrayOf(file.fileId.toString())
                    )
                        .build()
                )

            } else {
                // adding a new file
                setInitialAvailableOfflineStatus(file, cv)
                operations.add(ContentProviderOperation.newInsert(ProviderTableMeta.CONTENT_URI).withValues(cv).build())
            }
        }

        // prepare operations to remove files in the given folder
        val where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?" + " AND " +
                ProviderTableMeta.FILE_PATH + "=?"
        var whereArgs: Array<String>? = null
        for (file in filesToRemove) {
            if (file.parentId == folder.fileId) {
                whereArgs = arrayOf(account!!.name, file.remotePath)
                if (file.isFolder) {
                    operations.add(
                        ContentProviderOperation.newDelete(
                            ContentUris.withAppendedId(
                                ProviderTableMeta.CONTENT_URI_DIR, file.fileId
                            )
                        ).withSelection(where, whereArgs).build()
                    )

                    val localFolder = File(FileStorageUtils.getDefaultSavePathFor(account!!.name, file))
                    if (localFolder.exists()) {
                        removeLocalFolder(localFolder)
                    }
                } else {
                    operations.add(
                        ContentProviderOperation.newDelete(
                            ContentUris.withAppendedId(
                                ProviderTableMeta.CONTENT_URI_FILE, file.fileId
                            )
                        ).withSelection(where, whereArgs).build()
                    )

                    if (file.isDown) {
                        val path = file.storagePath
                        File(path).delete()
                        triggerMediaScan(path) // notify MediaScanner about removed file
                    }
                }
            }
        }

        // update metadata of folder
        val cv = ContentValues()
        cv.put(ProviderTableMeta.FILE_MODIFIED, folder.modificationTimestamp)
        cv.put(
            ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
            folder.modificationTimestampAtLastSyncForData
        )
        cv.put(ProviderTableMeta.FILE_CREATION, folder.creationTimestamp)
        cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, folder.fileLength)
        cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, folder.mimetype)
        cv.put(ProviderTableMeta.FILE_NAME, folder.fileName)
        cv.put(ProviderTableMeta.FILE_PARENT, folder.parentId)
        cv.put(ProviderTableMeta.FILE_PATH, folder.remotePath)
        cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, account!!.name)
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, folder.lastSyncDateForProperties)
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, folder.lastSyncDateForData)
        cv.put(ProviderTableMeta.FILE_ETAG, folder.etag)
        cv.put(ProviderTableMeta.FILE_TREE_ETAG, folder.treeEtag)
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, if (folder.isSharedViaLink) 1 else 0)
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, if (folder.isSharedWithSharee) 1 else 0)
        cv.put(ProviderTableMeta.FILE_PERMISSIONS, folder.permissions)
        cv.put(ProviderTableMeta.FILE_REMOTE_ID, folder.remoteId)
        cv.put(ProviderTableMeta.FILE_PRIVATE_LINK, folder.privateLink)

        operations.add(
            ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).withValues(cv).withSelection(
                ProviderTableMeta._ID + "=?",
                arrayOf(folder.fileId.toString())
            )
                .build()
        )

        // apply operations in batch
        var results: Array<ContentProviderResult>? = null
        Log_OC.d(TAG, "Sending " + operations.size + " operations to FileContentProvider")
        try {
            if (contentResolver != null) {
                results = contentResolver!!.applyBatch(MainApp.authority, operations)

            } else {
                results = contentProviderClient!!.applyBatch(operations)
            }

        } catch (e: OperationApplicationException) {
            Log_OC.e(TAG, "Exception in batch of operations " + e.message)

        } catch (e: RemoteException) {
            Log_OC.e(TAG, "Exception in batch of operations  " + e.message)
        }

        // update new id in file objects for insertions
        if (results != null) {
            var newId: Long
            val filesIt = updatedFiles.iterator()
            var file: OCFile? = null
            for (i in results.indices) {
                if (filesIt.hasNext()) {
                    file = filesIt.next()
                } else {
                    file = null
                }
                if (results[i].uri != null) {
                    newId = java.lang.Long.parseLong(results[i].uri.pathSegments[1])
                    //updatedFiles.get(i).setFileId(newId);
                    if (file != null) {
                        file.fileId = newId
                    }
                }
            }
        }

    }

    /**
     * Adds the appropriate initial value for ProviderTableMeta.FILE_KEEP_IN_SYNC to
     * passed [ContentValues] instance.
     *
     * @param file [OCFile] which av-offline property will be set.
     * @param cv   [ContentValues] instance where the property is added.
     */
    private fun setInitialAvailableOfflineStatus(file: OCFile, cv: ContentValues) {
        // set appropriate av-off folder depending on ancestor
        val inFolderAvailableOffline = isAnyAncestorAvailableOfflineFolder(file)
        if (inFolderAvailableOffline) {
            cv.put(
                ProviderTableMeta.FILE_KEEP_IN_SYNC,
                OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT.value
            )
        } else {
            cv.put(
                ProviderTableMeta.FILE_KEEP_IN_SYNC,
                OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE.value
            )
        }
    }

    /**
     * Updates available-offline status of OCFile received as a parameter, with its current value.
     *
     *
     * Saves the new value property for the given file in persistent storage.
     *
     *
     * If the file is a folder, updates the value of all its known descendants accordingly.
     *
     * @param file File which available-offline status will be updated.
     * @return 'true' if value was updated, 'false' otherwise.
     */
    fun saveLocalAvailableOfflineStatus(file: OCFile): Boolean {
        if (!fileExists(file.fileId)) {
            return false
        }

        val newStatus = file.availableOfflineStatus
        require(OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT != newStatus) { "Forbidden value, AVAILABLE_OFFLINE_PARENT is calculated, cannot be set" }

        val cv = ContentValues()
        cv.put(ProviderTableMeta.FILE_KEEP_IN_SYNC, file.availableOfflineStatus.value)

        var updatedCount: Int
        if (contentResolver != null) {
            updatedCount = contentResolver!!.update(
                ProviderTableMeta.CONTENT_URI,
                cv,
                ProviderTableMeta._ID + "=?",
                arrayOf(file.fileId.toString())
            )

            // Update descendants
            if (file.isFolder && updatedCount > 0) {
                val descendantsCv = ContentValues()
                if (newStatus == OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE) {
                    // all descendant files MUST be av-off due to inheritance, not due to previous value
                    descendantsCv.put(
                        ProviderTableMeta.FILE_KEEP_IN_SYNC,
                        OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT.value
                    )
                } else {
                    // all descendant files MUST be not-available offline
                    descendantsCv.put(
                        ProviderTableMeta.FILE_KEEP_IN_SYNC,
                        OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE.value
                    )
                }
                val selectDescendants = selectionForAllDescendantsOf(file)
                updatedCount += contentResolver!!.update(
                    ProviderTableMeta.CONTENT_URI,
                    descendantsCv,
                    selectDescendants.first,
                    selectDescendants.second
                )
            }

        } else {
            try {
                updatedCount = contentProviderClient!!.update(
                    ProviderTableMeta.CONTENT_URI,
                    cv,
                    ProviderTableMeta._ID + "=?",
                    arrayOf(file.fileId.toString())
                )

                // If file is a folder, all children files that were available offline must be unset
                if (file.isFolder && updatedCount > 0) {
                    val descendantsCv = ContentValues()
                    descendantsCv.put(
                        ProviderTableMeta.FILE_KEEP_IN_SYNC,
                        OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE.value
                    )
                    val selectDescendants = selectionForAllDescendantsOf(file)
                    updatedCount += contentProviderClient!!.update(
                        ProviderTableMeta.CONTENT_URI,
                        descendantsCv,
                        selectDescendants.first,
                        selectDescendants.second
                    )
                }

            } catch (e: RemoteException) {
                Log_OC.e(TAG, "Fail updating available offline status", e)
                return false
            }

        }

        return updatedCount > 0
    }

    fun removeFile(file: OCFile?, removeDBData: Boolean, removeLocalCopy: Boolean): Boolean {
        var success = true
        if (file != null) {
            if (file.isFolder) {
                success = removeFolder(file, removeDBData, removeLocalCopy)

            } else {
                if (removeDBData) {
                    val file_uri = ContentUris.withAppendedId(
                        ProviderTableMeta.CONTENT_URI_FILE,
                        file.fileId
                    )
                    val where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?" + " AND " +
                            ProviderTableMeta.FILE_PATH + "=?"
                    val whereArgs = arrayOf(account!!.name, file.remotePath)
                    var deleted = 0
                    if (contentProviderClient != null) {
                        try {
                            deleted = contentProviderClient!!.delete(file_uri, where, whereArgs)
                        } catch (e: RemoteException) {
                            e.printStackTrace()
                        }

                    } else {
                        deleted = contentResolver!!.delete(file_uri, where, whereArgs)
                    }
                    success = success and (deleted > 0)
                }
                val localPath = file.storagePath
                if (removeLocalCopy && file.isDown && localPath != null && success) {
                    success = File(localPath).delete()
                    if (success) {
                        deleteFileInMediaScan(localPath)
                    }
                    if (!removeDBData && success) {
                        // maybe unnecessary, but should be checked TODO remove if unnecessary
                        file.storagePath = null
                        saveFile(file)
                        saveConflict(file, null)
                    }
                }
            }
        } else {
            success = false
        }
        return success
    }

    fun removeFolder(folder: OCFile?, removeDBData: Boolean, removeLocalContent: Boolean): Boolean {
        var success = true
        if (folder != null && folder.isFolder) {
            if (removeDBData && folder.fileId != -1L) {
                success = removeFolderInDb(folder)
            }
            if (removeLocalContent && success) {
                success = removeLocalFolder(folder)
            }
        }
        return success
    }

    private fun removeFolderInDb(folder: OCFile): Boolean {
        val folder_uri =
            Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_DIR, "" + folder.fileId)   // URI for recursive deletion
        val where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?" + " AND " +
                ProviderTableMeta.FILE_PATH + "=?"
        val whereArgs = arrayOf(account!!.name, folder.remotePath)
        var deleted = 0
        if (contentProviderClient != null) {
            try {
                deleted = contentProviderClient!!.delete(folder_uri, where, whereArgs)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        } else {
            deleted = contentResolver!!.delete(folder_uri, where, whereArgs)
        }
        return deleted > 0
    }

    private fun removeLocalFolder(folder: OCFile): Boolean {
        var success = true
        val localFolderPath = FileStorageUtils.getDefaultSavePathFor(account!!.name, folder)
        val localFolder = File(localFolderPath)
        if (localFolder.exists()) {
            // stage 1: remove the local files already registered in the files database
            val files = getFolderContent(folder.fileId, false)
            if (files != null) {
                for (file in files) {
                    if (file.isFolder) {
                        success = success and removeLocalFolder(file)
                    } else {
                        if (file.isDown) {
                            val localFile = File(file.storagePath)
                            success = success and localFile.delete()
                            if (success) {
                                // notify MediaScanner about removed file
                                deleteFileInMediaScan(file.storagePath)
                                file.storagePath = null
                                saveFile(file)
                            }
                        }
                    }
                }
            }

            // stage 2: remove the folder itself and any local file inside out of sync;
            //          for instance, after clearing the app cache or reinstalling
            success = success and removeLocalFolder(localFolder)
        }
        return success
    }

    private fun removeLocalFolder(localFolder: File): Boolean {
        var success = true
        val localFiles = localFolder.listFiles()
        if (localFiles != null) {
            for (localFile in localFiles) {
                if (localFile.isDirectory) {
                    success = success and removeLocalFolder(localFile)
                } else {
                    val path = localFile.absolutePath
                    success = success and localFile.delete()
                }
            }
        }
        success = success and localFolder.delete()
        return success
    }

    /**
     * Updates database and file system for a file or folder that was moved to a different location.
     *
     * TODO explore better (faster) implementations
     * TODO throw exceptions up !
     */
    fun moveLocalFile(file: OCFile?, targetPath: String, targetParentPath: String) {

        if (file != null && file.fileExists() && OCFile.ROOT_PATH != file.fileName) {

            val targetParent = getFileByPath(targetParentPath)
                ?: throw IllegalStateException(
                    "Parent folder of the target path does not exist!!"
                )

            /// 1. get all the descendants of the moved element in a single QUERY
            var c: Cursor? = null
            if (contentProviderClient != null) {
                try {
                    c = contentProviderClient!!.query(
                        ProviderTableMeta.CONTENT_URI, null,
                        ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                                ProviderTableMeta.FILE_PATH + " LIKE ? ",
                        arrayOf(account!!.name, file.remotePath + "%"),
                        ProviderTableMeta.FILE_PATH + " ASC "
                    )
                } catch (e: RemoteException) {
                    Log_OC.e(TAG, e.message)
                }

            } else {
                c = contentResolver!!.query(
                    ProviderTableMeta.CONTENT_URI, null,
                    ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                            ProviderTableMeta.FILE_PATH + " LIKE ? ",
                    arrayOf(account!!.name, file.remotePath + "%"),
                    ProviderTableMeta.FILE_PATH + " ASC "
                )
            }

            val originalPathsToTriggerMediaScan = ArrayList<String>()
            val newPathsToTriggerMediaScan = ArrayList<String>()
            val defaultSavePath = FileStorageUtils.getSavePath(account!!.name)

            /// 2. prepare a batch of update operations to change all the descendants
            if (c != null) {
                val operations = ArrayList<ContentProviderOperation>(c.count)
                if (c.moveToFirst()) {
                    val lengthOfOldPath = file.remotePath.length
                    val lengthOfOldStoragePath = defaultSavePath.length + lengthOfOldPath
                    do {
                        val cv = ContentValues() // keep construction in the loop
                        val child = createFileInstance(c)
                        cv.put(
                            ProviderTableMeta.FILE_PATH,
                            targetPath + child!!.remotePath.substring(lengthOfOldPath)
                        )
                        if (child.storagePath != null && child.storagePath.startsWith(defaultSavePath)) {
                            // update link to downloaded content - but local move is not done here!
                            val targetLocalPath = defaultSavePath + targetPath +
                                    child.storagePath.substring(lengthOfOldStoragePath)

                            cv.put(ProviderTableMeta.FILE_STORAGE_PATH, targetLocalPath)

                            originalPathsToTriggerMediaScan.add(child.storagePath)
                            newPathsToTriggerMediaScan.add(targetLocalPath)

                        }
                        if (targetParent.availableOfflineStatus != OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE) {
                            // moving to an available offline subfolder
                            cv.put(
                                ProviderTableMeta.FILE_KEEP_IN_SYNC,
                                OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT.value
                            )

                        } else {
                            // moving to a not available offline subfolder - with care
                            if (file.availableOfflineStatus == OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT) {
                                cv.put(
                                    ProviderTableMeta.FILE_KEEP_IN_SYNC,
                                    OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE.value
                                )
                            }
                        }

                        if (child.remotePath == file.remotePath) {
                            cv.put(
                                ProviderTableMeta.FILE_PARENT,
                                targetParent.fileId
                            )
                        }
                        operations.add(
                            ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).withValues(cv).withSelection(
                                ProviderTableMeta._ID + "=?",
                                arrayOf(child.fileId.toString())
                            )
                                .build()
                        )

                    } while (c.moveToNext())
                }
                c.close()

                /// 3. apply updates in batch
                try {
                    if (contentResolver != null) {
                        contentResolver!!.applyBatch(MainApp.authority, operations)

                    } else {
                        contentProviderClient!!.applyBatch(operations)
                    }

                } catch (e: Exception) {
                    Log_OC.e(
                        TAG, "Fail to update " + file.fileId + " and descendants in database",
                        e
                    )
                }

            }

            /// 4. move in local file system
            val originalLocalPath = FileStorageUtils.getDefaultSavePathFor(account!!.name, file)
            val targetLocalPath = defaultSavePath + targetPath
            val localFile = File(originalLocalPath)
            var renamed = false
            if (localFile.exists()) {
                val targetFile = File(targetLocalPath)
                val targetFolder = targetFile.parentFile
                if (!targetFolder.exists()) {
                    targetFolder.mkdirs()
                }
                renamed = localFile.renameTo(targetFile)
            }

            if (renamed) {
                var it = originalPathsToTriggerMediaScan.iterator()
                while (it.hasNext()) {
                    // Notify MediaScanner about removed file
                    deleteFileInMediaScan(it.next())
                }
                it = newPathsToTriggerMediaScan.iterator()
                while (it.hasNext()) {
                    // Notify MediaScanner about new file/folder
                    triggerMediaScan(it.next())
                }
            }
        }
    }

    fun copyLocalFile(originalFile: OCFile?, targetPath: String, targetFileRemoteId: String) {
        if (originalFile != null && originalFile.fileExists() && OCFile.ROOT_PATH != originalFile.fileName) {
            // 1. Copy in database
            val ocTargetFile = OCFile(targetPath)
            val parentId = getFileByPath(FileStorageUtils.getParentPath(targetPath))!!.fileId
            ocTargetFile.parentId = parentId
            ocTargetFile.remoteId = targetFileRemoteId
            ocTargetFile.fileLength = originalFile.fileLength
            ocTargetFile.mimetype = originalFile.mimetype
            ocTargetFile.modificationTimestamp = System.currentTimeMillis()
            saveFile(ocTargetFile)

            // 2. Copy in local file system
            var copied = false
            val localPath = FileStorageUtils.getDefaultSavePathFor(account!!.name, originalFile)
            val localFile = File(localPath)
            val defaultSavePath = FileStorageUtils.getSavePath(account!!.name)
            if (localFile.exists()) {
                val targetFile = File(defaultSavePath + targetPath)
                val targetFolder = targetFile.parentFile
                if (!targetFolder.exists()) {
                    targetFolder.mkdirs()
                }
                copied = copyFile(localFile, targetFile)
            }

            Log_OC.d(TAG, "Local file COPIED : $copied")
        }
    }

    private fun copyFile(src: File, target: File): Boolean {
        var ret = true

        var input: InputStream? = null
        var out: OutputStream? = null

        try {
            input = FileInputStream(src)
            out = FileOutputStream(target)
            val buf = ByteArray(1024)
            var len: Int
            do {
                len = input.read()
                out.write(buf, 0, len)
            } while (len > 0)
        } catch (ex: IOException) {
            ret = false
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace(System.err)
                }

            }
            if (out != null) {
                try {
                    out.close()
                } catch (e: IOException) {
                    e.printStackTrace(System.err)
                }

            }
        }

        return ret
    }

    private fun getFolderContent(parentId: Long, onlyAvailableOffline: Boolean): Vector<OCFile> {
        val ret = Vector<OCFile>()

        val req_uri = Uri.withAppendedPath(
            ProviderTableMeta.CONTENT_URI_DIR,
            parentId.toString()
        )
        var c: Cursor? = null

        val selection: String
        val selectionArgs: Array<String>

        if (!onlyAvailableOffline) {
            selection = ProviderTableMeta.FILE_PARENT + "=?"
            selectionArgs = arrayOf(parentId.toString())
        } else {
            selection = ProviderTableMeta.FILE_PARENT + "=? AND (" + ProviderTableMeta.FILE_KEEP_IN_SYNC +
                    " = ? OR " + ProviderTableMeta.FILE_KEEP_IN_SYNC + "=? )"
            selectionArgs = arrayOf(
                parentId.toString(),
                OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE.value.toString(),
                OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE_PARENT.value.toString()
            )
        }

        if (contentProviderClient != null) {
            try {
                c = contentProviderClient!!.query(req_uri, null, selection, selectionArgs, null)
            } catch (e: RemoteException) {
                Log_OC.e(TAG, e.message)
                return ret
            }

        } else {
            c = contentResolver!!.query(req_uri, null, selection, selectionArgs, null)
        }

        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    val child = createFileInstance(c)
                    ret.add(child)
                } while (c.moveToNext())
            }
            c.close()
        }

        Collections.sort(ret)

        return ret
    }

    /**
     * Checks if it is favorite or it is inside a favorite folder
     *
     * @param file [OCFile] which ancestors will be searched.
     * @return true/false
     */
    private fun isAnyAncestorAvailableOfflineFolder(file: OCFile): Boolean {
        return getAvailableOfflineAncestorOf(file) != null
    }

    /**
     * Returns ancestor folder with available offline status AVAILABLE_OFFLINE.
     *
     * @param file [OCFile] which ancestors will be searched.
     * @return Ancestor folder with available offline status AVAILABLE_OFFLINE, or null if
     * does not exist.
     */
    fun getAvailableOfflineAncestorOf(file: OCFile): OCFile? {
        var avOffAncestor: OCFile? = null
        val parent = getFileById(file.parentId)
        if (parent != null && parent.isFolder) {  // file is null for the parent of the root folder
            if (parent.availableOfflineStatus == OCFile.AvailableOfflineStatus.AVAILABLE_OFFLINE) {
                avOffAncestor = parent
            } else if (parent.fileName != OCFile.ROOT_PATH) {
                avOffAncestor = getAvailableOfflineAncestorOf(parent)
            }
        }
        return avOffAncestor
    }

    private fun createRootDir(): OCFile {
        val file = OCFile(OCFile.ROOT_PATH)
        file.mimetype = "DIR"
        file.parentId = FileDataStorageManager.ROOT_PARENT_ID.toLong()
        saveFile(file)
        return file
    }

    private fun fileExists(cmp_key: String, value: String): Boolean {
        val c: Cursor?
        if (contentResolver != null) {
            c = contentResolver!!
                .query(
                    ProviderTableMeta.CONTENT_URI, null,
                    cmp_key + "=? AND "
                            + ProviderTableMeta.FILE_ACCOUNT_OWNER
                            + "=?",
                    arrayOf(value, account!!.name), null
                )
        } else {
            try {
                c = contentProviderClient!!.query(
                    ProviderTableMeta.CONTENT_URI, null,
                    cmp_key + "=? AND "
                            + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                    arrayOf(value, account!!.name), null
                )
            } catch (e: RemoteException) {
                Log_OC.e(
                    TAG,
                    "Couldn't determine file existance, assuming non existance: " + e.message
                )
                return false
            }

        }
        var retval = false
        if (c != null) {
            retval = c.moveToFirst()
            c.close()
        }
        return retval
    }

    private fun getFileCursorForValue(key: String, value: String): Cursor? {
        var c: Cursor? = null
        if (contentResolver != null) {
            c = contentResolver!!
                .query(
                    ProviderTableMeta.CONTENT_URI, null,
                    key + "=? AND "
                            + ProviderTableMeta.FILE_ACCOUNT_OWNER
                            + "=?",
                    arrayOf(value, account!!.name), null
                )
        } else {
            try {
                c = contentProviderClient!!.query(
                    ProviderTableMeta.CONTENT_URI, null,
                    key + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER
                            + "=?", arrayOf(value, account!!.name), null
                )
            } catch (e: RemoteException) {
                Log_OC.e(TAG, "Could not get file details: " + e.message)
                c = null
            }

        }
        return c
    }

    private fun createFileInstance(c: Cursor?): OCFile? {
        var file: OCFile? = null
        if (c != null) {
            file = OCFile(
                c.getString(
                    c
                        .getColumnIndex(ProviderTableMeta.FILE_PATH)
                )
            )
            file.fileId = c.getLong(c.getColumnIndex(ProviderTableMeta._ID))
            file.parentId = c.getLong(
                c
                    .getColumnIndex(ProviderTableMeta.FILE_PARENT)
            )
            file.mimetype = c.getString(
                c
                    .getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE)
            )
            if (!file.isFolder) {
                file.storagePath = c.getString(
                    c
                        .getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH)
                )
                if (file.storagePath == null) {
                    // try to find existing file and bind it with current account;
                    // with the current update of SynchronizeFolderOperation, this won't be
                    // necessary anymore after a full synchronization of the account
                    val f = File(FileStorageUtils.getDefaultSavePathFor(account!!.name, file))
                    if (f.exists()) {
                        file.storagePath = f.absolutePath
                        file.lastSyncDateForData = f.lastModified()
                    }
                }
            }
            file.fileLength = c.getLong(
                c
                    .getColumnIndex(ProviderTableMeta.FILE_CONTENT_LENGTH)
            )
            file.creationTimestamp = c.getLong(
                c
                    .getColumnIndex(ProviderTableMeta.FILE_CREATION)
            )
            file.modificationTimestamp = c.getLong(
                c
                    .getColumnIndex(ProviderTableMeta.FILE_MODIFIED)
            )
            file.modificationTimestampAtLastSyncForData = c.getLong(
                c
                    .getColumnIndex(ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA)
            )
            file.lastSyncDateForProperties = c.getLong(
                c
                    .getColumnIndex(ProviderTableMeta.FILE_LAST_SYNC_DATE)
            )
            file.lastSyncDateForData = c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA))
            file.availableOfflineStatus = OCFile.AvailableOfflineStatus.fromValue(
                c.getInt(c.getColumnIndex(ProviderTableMeta.FILE_KEEP_IN_SYNC))
            )
            file.etag = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_ETAG))
            file.treeEtag = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_TREE_ETAG))
            file.isSharedViaLink = c.getInt(
                c.getColumnIndex(ProviderTableMeta.FILE_SHARED_VIA_LINK)
            ) == 1
            file.isSharedWithSharee = c.getInt(
                c.getColumnIndex(ProviderTableMeta.FILE_SHARED_WITH_SHAREE)
            ) == 1
            file.permissions = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PERMISSIONS))
            file.remoteId = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_REMOTE_ID))
            file.setNeedsUpdateThumbnail(
                c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_UPDATE_THUMBNAIL)
                ) == 1
            )
            file.isDownloading = c.getInt(
                c.getColumnIndex(ProviderTableMeta.FILE_IS_DOWNLOADING)
            ) == 1
            file.etagInConflict = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_ETAG_IN_CONFLICT))
            file.privateLink = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PRIVATE_LINK))

        }
        return file
    }

    // Methods for Shares
    fun saveShare(remoteShare: RemoteShare): Boolean {
        var overriden = false
        val cv = ContentValues()
        cv.put(ProviderTableMeta.OCSHARES_FILE_SOURCE, remoteShare.fileSource)
        cv.put(ProviderTableMeta.OCSHARES_ITEM_SOURCE, remoteShare.itemSource)
        cv.put(ProviderTableMeta.OCSHARES_SHARE_TYPE, remoteShare.shareType!!.value)
        cv.put(ProviderTableMeta.OCSHARES_SHARE_WITH, remoteShare.shareWith)
        cv.put(ProviderTableMeta.OCSHARES_PATH, remoteShare.path)
        cv.put(ProviderTableMeta.OCSHARES_PERMISSIONS, remoteShare.permissions)
        cv.put(ProviderTableMeta.OCSHARES_SHARED_DATE, remoteShare.sharedDate)
        cv.put(ProviderTableMeta.OCSHARES_EXPIRATION_DATE, remoteShare.expirationDate)
        cv.put(ProviderTableMeta.OCSHARES_TOKEN, remoteShare.token)
        cv.put(
            ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME,
            remoteShare.sharedWithDisplayName
        )
        cv.put(
            ProviderTableMeta.OCSHARES_SHARE_WITH_ADDITIONAL_INFO,
            remoteShare.sharedWithAdditionalInfo
        )
        cv.put(ProviderTableMeta.OCSHARES_IS_DIRECTORY, if (remoteShare.isFolder) 1 else 0)
        cv.put(ProviderTableMeta.OCSHARES_USER_ID, remoteShare.userId)
        cv.put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, remoteShare.id)
        cv.put(ProviderTableMeta.OCSHARES_NAME, remoteShare.name)
        cv.put(ProviderTableMeta.OCSHARES_URL, remoteShare.shareLink)
        cv.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, account!!.name)

        if (shareExistsForRemoteId(remoteShare.id)) {// for renamed files; no more delete and create
            overriden = true
            if (contentResolver != null) {
                contentResolver!!.update(
                    ProviderTableMeta.CONTENT_URI_SHARE, cv,
                    ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + "=?",
                    arrayOf(remoteShare.id.toString())
                )
            } else {
                try {
                    contentProviderClient!!.update(
                        ProviderTableMeta.CONTENT_URI_SHARE,
                        cv, ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + "=?",
                        arrayOf(remoteShare.id.toString())
                    )
                } catch (e: RemoteException) {
                    Log_OC.e(
                        TAG,
                        "Fail to insert insert file to database " + e.message
                    )
                }

            }
        } else {
            var result_uri: Uri? = null
            if (contentResolver != null) {
                result_uri = contentResolver!!.insert(
                    ProviderTableMeta.CONTENT_URI_SHARE, cv
                )
            } else {
                try {
                    result_uri = contentProviderClient!!.insert(
                        ProviderTableMeta.CONTENT_URI_SHARE, cv
                    )
                } catch (e: RemoteException) {
                    Log_OC.e(
                        TAG,
                        "Fail to insert insert file to database " + e.message
                    )
                }

            }
        }

        return overriden
    }

    /**
     * Retrieves an stored [OCShareEntity] given its id.
     *
     * @param id Identifier.
     * @return Stored [OCShareEntity] given its id.
     */
    fun getShareById(id: Long): OCShareEntity? {
        var share: OCShareEntity? = null
        val c = getShareCursorForValue(
            ProviderTableMeta._ID,
            id.toString()
        )
        if (c != null) {
            if (c.moveToFirst()) {
                share = createShareInstance(c)
            }
            c.close()
        }
        return share
    }

    /**
     * Retrieves an stored [OCShareEntity] given its id.
     *
     * @param id Identifier of the share in OC server.
     * @return Stored [OCShareEntity] given its remote id.
     */
    fun getShareByRemoteId(id: Long): OCShareEntity? {
        var share: OCShareEntity? = null
        val c = getShareCursorForValue(
            ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED,
            id.toString()
        )
        if (c != null) {
            if (c.moveToFirst()) {
                share = createShareInstance(c)
            }
            c.close()
        }
        return share
    }

    /**
     * Checks the existance of an stored [RemoteShare] matching the given remote id (not to be confused with
     * the local id) in the current account.
     *
     * @param remoteId Remote of the share in the server.
     * @return 'True' if a matching [RemoteShare] is stored in the current account.
     */
    private fun shareExistsForRemoteId(remoteId: Long): Boolean {
        return shareExistsForValue(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, remoteId.toString())
    }

    /**
     * Checks the existance of an stored [RemoteShare] in the current account
     * matching a given column and a value for that column
     *
     * @param key   Name of the column to match.
     * @param value Value of the column to match.
     * @return 'True' if a matching [RemoteShare] is stored in the current account.
     */
    private fun shareExistsForValue(key: String, value: String): Boolean {
        val c = getShareCursorForValue(key, value)
        var retval = false
        if (c != null) {
            retval = c.moveToFirst()
            c.close()
        }
        return retval
    }

    /**
     * Gets a [Cursor] for an stored [RemoteShare] in the current account
     * matching a given column and a value for that column
     *
     * @param key   Name of the column to match.
     * @param value Value of the column to match.
     * @return 'True' if a matching [RemoteShare] is stored in the current account.
     */
    private fun getShareCursorForValue(key: String, value: String): Cursor? {
        var c: Cursor?
        if (contentResolver != null) {
            c = contentResolver!!
                .query(
                    ProviderTableMeta.CONTENT_URI_SHARE, null,
                    key + "=? AND "
                            + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?",
                    arrayOf(value, account!!.name), null
                )
        } else {
            try {
                c = contentProviderClient!!.query(
                    ProviderTableMeta.CONTENT_URI_SHARE, null,
                    key + "=? AND "
                            + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?",
                    arrayOf(value, account!!.name), null
                )
            } catch (e: RemoteException) {
                Log_OC.w(
                    TAG,
                    "Could not get details, assuming share does not exist: " + e.message
                )
                c = null
            }

        }
        return c
    }

    private fun createShareInstance(c: Cursor?): OCShareEntity? {
        var share: OCShareEntity? = null
        if (c != null) {
            share = OCShareEntity(
                c.getLong(c.getColumnIndex(ProviderTableMeta.OCSHARES_FILE_SOURCE)),
                c.getLong(c.getColumnIndex(ProviderTableMeta.OCSHARES_ITEM_SOURCE)),
                c.getInt(c.getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_TYPE)),
                c.getString(c.getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_WITH)),
                c.getString(c.getColumnIndex(ProviderTableMeta.OCSHARES_PATH)),
                c.getInt(c.getColumnIndex(ProviderTableMeta.OCSHARES_PERMISSIONS)),
                c.getLong(c.getColumnIndex(ProviderTableMeta.OCSHARES_SHARED_DATE)),
                c.getLong(c.getColumnIndex(ProviderTableMeta.OCSHARES_EXPIRATION_DATE)),
                c.getString(c.getColumnIndex(ProviderTableMeta.OCSHARES_TOKEN)),
                c.getString(c.getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME)),
                c.getString(c.getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_WITH_ADDITIONAL_INFO)),
                c.getInt(c.getColumnIndex(ProviderTableMeta.OCSHARES_IS_DIRECTORY)) == 1,
                c.getLong(c.getColumnIndex(ProviderTableMeta.OCSHARES_USER_ID)),
                c.getLong(c.getColumnIndex(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED)),
                c.getString(c.getColumnIndex(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER)),
                c.getString(c.getColumnIndex(ProviderTableMeta.OCSHARES_NAME)),
                c.getString(c.getColumnIndex(ProviderTableMeta.OCSHARES_URL))
            )
        }
        return share
    }

    private fun resetShareFlagsInAllFiles() {
        val cv = ContentValues()
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, false)
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, false)
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, "")
        val where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?"
        val whereArgs = arrayOf(account!!.name)

        if (contentResolver != null) {
            contentResolver!!.update(ProviderTableMeta.CONTENT_URI, cv, where, whereArgs)

        } else {
            try {
                contentProviderClient!!.update(
                    ProviderTableMeta.CONTENT_URI, cv, where,
                    whereArgs
                )
            } catch (e: RemoteException) {
                Log_OC.e(TAG, "Exception in resetShareFlagsInAllFiles" + e.message)
            }

        }
    }

    private fun resetShareFlagsInFolder(folder: OCFile) {
        val cv = ContentValues()
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, false)
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, false)
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, "")
        val where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                ProviderTableMeta.FILE_PARENT + "=?"
        val whereArgs = arrayOf(account!!.name, folder.fileId.toString())

        if (contentResolver != null) {
            contentResolver!!.update(ProviderTableMeta.CONTENT_URI, cv, where, whereArgs)

        } else {
            try {
                contentProviderClient!!.update(
                    ProviderTableMeta.CONTENT_URI, cv, where,
                    whereArgs
                )
            } catch (e: RemoteException) {
                Log_OC.e(TAG, "Exception in resetShareFlagsInFolder " + e.message)
            }

        }
    }

    private fun resetShareFlagInAFile(filePath: String) {
        val cv = ContentValues()
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, false)
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, false)
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, "")
        val where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                ProviderTableMeta.FILE_PATH + "=?"
        val whereArgs = arrayOf(account!!.name, filePath)

        if (contentResolver != null) {
            contentResolver!!.update(ProviderTableMeta.CONTENT_URI, cv, where, whereArgs)

        } else {
            try {
                contentProviderClient!!.update(
                    ProviderTableMeta.CONTENT_URI, cv, where,
                    whereArgs
                )
            } catch (e: RemoteException) {
                Log_OC.e(TAG, "Exception in resetShareFlagsInFolder " + e.message)
            }

        }
    }

    private fun cleanShares() {
        val where = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?"
        val whereArgs = arrayOf(account!!.name)

        if (contentResolver != null) {
            contentResolver!!.delete(ProviderTableMeta.CONTENT_URI_SHARE, where, whereArgs)

        } else {
            try {
                contentProviderClient!!.delete(
                    ProviderTableMeta.CONTENT_URI_SHARE, where,
                    whereArgs
                )
            } catch (e: RemoteException) {
                Log_OC.e(TAG, "Exception in cleanShares" + e.message)
            }

        }
    }

    fun removeShare(share: OCShareEntity) {
        val share_uri = ProviderTableMeta.CONTENT_URI_SHARE
        val where = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?" + " AND " +
                ProviderTableMeta._ID + "=?"
        val whereArgs = arrayOf(account!!.name, java.lang.Long.toString(share.id.toLong()))
        if (contentProviderClient != null) {
            try {
                contentProviderClient!!.delete(share_uri, where, whereArgs)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        } else {
            contentResolver!!.delete(share_uri, where, whereArgs)
        }
    }

    /**
     * Prepare operations to insert or update files to save in the given folder
     *
     * @param shares     List of shares to insert
     * @param operations List of operations
     * @return
     */
    private fun prepareInsertShares(
        shares: List<RemoteShare>?, operations: ArrayList<ContentProviderOperation>
    ): ArrayList<ContentProviderOperation> {

        if (shares != null) {
            // prepare operations to insert or update files to save in the given folder
            for (remoteShare in shares) {
                val cv = ContentValues()
                cv.put(ProviderTableMeta.OCSHARES_FILE_SOURCE, remoteShare.fileSource)
                cv.put(ProviderTableMeta.OCSHARES_ITEM_SOURCE, remoteShare.itemSource)
                cv.put(ProviderTableMeta.OCSHARES_SHARE_TYPE, remoteShare.shareType!!.value)
                cv.put(ProviderTableMeta.OCSHARES_SHARE_WITH, remoteShare.shareWith)
                cv.put(ProviderTableMeta.OCSHARES_PATH, remoteShare.path)
                cv.put(ProviderTableMeta.OCSHARES_PERMISSIONS, remoteShare.permissions)
                cv.put(ProviderTableMeta.OCSHARES_SHARED_DATE, remoteShare.sharedDate)
                cv.put(ProviderTableMeta.OCSHARES_EXPIRATION_DATE, remoteShare.expirationDate)
                cv.put(ProviderTableMeta.OCSHARES_TOKEN, remoteShare.token)
                cv.put(
                    ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME,
                    remoteShare.sharedWithDisplayName
                )
                cv.put(
                    ProviderTableMeta.OCSHARES_SHARE_WITH_ADDITIONAL_INFO,
                    remoteShare.sharedWithAdditionalInfo
                )
                cv.put(ProviderTableMeta.OCSHARES_IS_DIRECTORY, if (remoteShare.isFolder) 1 else 0)
                cv.put(ProviderTableMeta.OCSHARES_USER_ID, remoteShare.userId)
                cv.put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, remoteShare.id)
                cv.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, account!!.name)
                cv.put(ProviderTableMeta.OCSHARES_NAME, remoteShare.name)
                cv.put(ProviderTableMeta.OCSHARES_URL, remoteShare.shareLink)

                // adding a new share resource
                operations.add(
                    ContentProviderOperation.newInsert(ProviderTableMeta.CONTENT_URI_SHARE).withValues(cv).build()
                )
            }
        }
        return operations
    }

    private fun prepareRemoveSharesInFolder(
        folder: OCFile?, preparedOperations: ArrayList<ContentProviderOperation>
    ): ArrayList<ContentProviderOperation> {
        if (folder != null) {
            val where = (ProviderTableMeta.OCSHARES_PATH + "=?" + " AND "
                    + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?")
            val whereArgs = arrayOf("", account!!.name)

            val files = getFolderContent(folder, false)

            for (file in files) {
                whereArgs[0] = file.remotePath
                preparedOperations.add(
                    ContentProviderOperation.newDelete(ProviderTableMeta.CONTENT_URI_SHARE).withSelection(
                        where,
                        whereArgs
                    ).build()
                )
            }
        }
        return preparedOperations

    }

    private fun prepareRemoveSharesInFile(
        filePath: String, preparedOperations: ArrayList<ContentProviderOperation>
    ): ArrayList<ContentProviderOperation> {

        val where = (ProviderTableMeta.OCSHARES_PATH + "=?" + " AND "
                + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?")
        val whereArgs = arrayOf(filePath, account!!.name)

        preparedOperations.add(
            ContentProviderOperation.newDelete(ProviderTableMeta.CONTENT_URI_SHARE).withSelection(
                where,
                whereArgs
            ).build()
        )

        return preparedOperations

    }

    fun getPrivateSharesForAFile(filePath: String, accountName: String): ArrayList<OCShareEntity> {
        // Condition
        val where = (ProviderTableMeta.OCSHARES_PATH + "=?" + " AND "
                + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?" + "AND"
                + " (" + ProviderTableMeta.OCSHARES_SHARE_TYPE + "=? OR "
                + ProviderTableMeta.OCSHARES_SHARE_TYPE + "=? OR "
                + ProviderTableMeta.OCSHARES_SHARE_TYPE + "=? ) ")
        val whereArgs = arrayOf(
            filePath,
            accountName,
            Integer.toString(ShareType.USER.value),
            Integer.toString(ShareType.GROUP.value),
            Integer.toString(ShareType.FEDERATED.value)
        )

        var c: Cursor?
        if (contentResolver != null) {
            c = contentResolver!!.query(
                ProviderTableMeta.CONTENT_URI_SHARE, null, where, whereArgs, null
            )
        } else {
            try {
                c = contentProviderClient!!.query(
                    ProviderTableMeta.CONTENT_URI_SHARE, null, where, whereArgs, null
                )

            } catch (e: RemoteException) {
                Log_OC.e(TAG, "Could not get list of shares with: " + e.message)
                c = null
            }

        }
        val privateShares = ArrayList<OCShareEntity>()
        var privateShare: OCShareEntity?
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    privateShare = createShareInstance(c)
                    privateShares.add(privateShare!!)
                } while (c.moveToNext())
            }
            c.close()
        }

        return privateShares
    }

    fun getPublicSharesForAFile(filePath: String, accountName: String): ArrayList<OCShareEntity> {
        // Condition
        val where = (ProviderTableMeta.OCSHARES_PATH + "=?" + " AND "
                + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?" + "AND "
                + ProviderTableMeta.OCSHARES_SHARE_TYPE + "=? ")
        val whereArgs = arrayOf(filePath, accountName, Integer.toString(ShareType.PUBLIC_LINK.value))

        var c: Cursor?
        if (contentResolver != null) {
            c = contentResolver!!.query(
                ProviderTableMeta.CONTENT_URI_SHARE, null, where, whereArgs, null
            )
        } else {
            try {
                c = contentProviderClient!!.query(
                    ProviderTableMeta.CONTENT_URI_SHARE, null, where, whereArgs, null
                )

            } catch (e: RemoteException) {
                Log_OC.e(TAG, "Could not get list of shares with: " + e.message)
                c = null
            }

        }
        val publicShares = ArrayList<OCShareEntity>()
        var publicShare: OCShareEntity?
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    publicShare = createShareInstance(c)
                    publicShares.add(publicShare!!)
                    // }
                } while (c.moveToNext())
            }
            c.close()
        }

        return publicShares
    }

    fun triggerMediaScan(path: String?) {
        if (path != null) {
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(File(path))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    MainApp.appContext!!.sendBroadcast(intent)
                } catch (fileUriExposedException: FileUriExposedException) {
                    val newIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    newIntent.data = FileProvider.getUriForFile(
                        mContext!!.applicationContext,
                        mContext!!.resources.getString(R.string.file_provider_authority),
                        File(path)
                    )
                    MainApp.appContext!!.sendBroadcast(newIntent)
                }

            } else {
                MainApp.appContext!!.sendBroadcast(intent)
            }

            MainApp.appContext!!.sendBroadcast(intent)
        }
    }

    fun deleteFileInMediaScan(path: String) {

        val mimetypeString = FileStorageUtils.getMimeTypeFromName(path)
        val contentResolver = contentResolver

        if (contentResolver != null) {
            if (mimetypeString.startsWith("image/")) {
                // Images
                contentResolver.delete(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Media.DATA + "=?", arrayOf(path)
                )
            } else if (mimetypeString.startsWith("audio/")) {
                // Audio
                contentResolver.delete(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Audio.Media.DATA + "=?", arrayOf(path)
                )
            } else if (mimetypeString.startsWith("video/")) {
                // Video
                contentResolver.delete(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.DATA + "=?", arrayOf(path)
                )
            }
        } else {
            val contentProviderClient = contentProviderClient
            try {
                if (mimetypeString.startsWith("image/")) {
                    // Images
                    contentProviderClient!!.delete(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Images.Media.DATA + "=?", arrayOf(path)
                    )
                } else if (mimetypeString.startsWith("audio/")) {
                    // Audio
                    contentProviderClient!!.delete(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Audio.Media.DATA + "=?", arrayOf(path)
                    )
                } else if (mimetypeString.startsWith("video/")) {
                    // Video
                    contentProviderClient!!.delete(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Video.Media.DATA + "=?", arrayOf(path)
                    )
                }
            } catch (e: RemoteException) {
                Log_OC.e(TAG, "Exception deleting media file in MediaStore " + e.message)
            }

        }

    }

    fun saveConflict(file: OCFile, etagInConflict: String?) {
        var etagInConflict = etagInConflict
        if (!file.isDown) {
            etagInConflict = null
        }
        val cv = ContentValues()
        cv.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, etagInConflict)
        var updated = 0
        if (contentResolver != null) {
            updated = contentResolver!!.update(
                ProviderTableMeta.CONTENT_URI_FILE,
                cv,
                ProviderTableMeta._ID + "=?",
                arrayOf(file.fileId.toString())
            )
        } else {
            try {
                updated = contentProviderClient!!.update(
                    ProviderTableMeta.CONTENT_URI_FILE,
                    cv,
                    ProviderTableMeta._ID + "=?",
                    arrayOf(file.fileId.toString())
                )
            } catch (e: RemoteException) {
                Log_OC.e(TAG, "Failed saving conflict in database " + e.message)
            }

        }

        Log_OC.d(TAG, "Number of files updated with CONFLICT: $updated")

        if (updated > 0) {
            if (etagInConflict != null) {
                /// set conflict in all ancestor folders

                var parentId = file.parentId
                val ancestorIds = HashSet<String>()
                while (parentId != FileDataStorageManager.ROOT_PARENT_ID.toLong()) {
                    ancestorIds.add(java.lang.Long.toString(parentId))
                    parentId = getFileById(parentId)!!.parentId
                }

                if (ancestorIds.size > 0) {
                    val whereBuffer = StringBuffer()
                    whereBuffer.append(ProviderTableMeta._ID).append(" IN (")
                    for (i in 0 until ancestorIds.size - 1) {
                        whereBuffer.append("?,")
                    }
                    whereBuffer.append("?")
                    whereBuffer.append(")")

                    if (contentResolver != null) {
                        updated = contentResolver!!.update(
                            ProviderTableMeta.CONTENT_URI_FILE,
                            cv,
                            whereBuffer.toString(),
                            ancestorIds.toTypedArray()
                        )
                    } else {
                        try {
                            updated = contentProviderClient!!.update(
                                ProviderTableMeta.CONTENT_URI_FILE,
                                cv,
                                whereBuffer.toString(),
                                ancestorIds.toTypedArray()
                            )
                        } catch (e: RemoteException) {
                            Log_OC.e(TAG, "Failed saving conflict in database " + e.message)
                        }

                    }
                } // else file is ROOT folder, no parent to set in conflict

            } else {
                /// update conflict in ancestor folders
                // (not directly unset; maybe there are more conflicts below them)
                var parentPath = file.remotePath
                if (parentPath.endsWith(OCFile.PATH_SEPARATOR)) {
                    parentPath = parentPath.substring(0, parentPath.length - 1)
                }
                parentPath = parentPath.substring(0, parentPath.lastIndexOf(OCFile.PATH_SEPARATOR) + 1)

                Log_OC.d(TAG, "checking parents to remove conflict; STARTING with $parentPath")
                while (parentPath.length > 0) {

                    val whereForDescencentsInConflict = ProviderTableMeta.FILE_ETAG_IN_CONFLICT + " IS NOT NULL AND " +
                            ProviderTableMeta.FILE_CONTENT_TYPE + " != 'DIR' AND " +
                            ProviderTableMeta.FILE_ACCOUNT_OWNER + " = ? AND " +
                            ProviderTableMeta.FILE_PATH + " LIKE ?"
                    var descendantsInConflict: Cursor? = null
                    if (contentResolver != null) {
                        descendantsInConflict = contentResolver!!.query(
                            ProviderTableMeta.CONTENT_URI_FILE,
                            arrayOf(ProviderTableMeta._ID),
                            whereForDescencentsInConflict,
                            arrayOf(account!!.name, "$parentPath%"), null
                        )
                    } else {
                        try {
                            descendantsInConflict = contentProviderClient!!.query(
                                ProviderTableMeta.CONTENT_URI_FILE,
                                arrayOf(ProviderTableMeta._ID),
                                whereForDescencentsInConflict,
                                arrayOf(account!!.name, "$parentPath%"), null
                            )
                        } catch (e: RemoteException) {
                            Log_OC.e(TAG, "Failed querying for descendants in conflict " + e.message)
                        }

                    }
                    if (descendantsInConflict == null || descendantsInConflict.count == 0) {
                        Log_OC.d(TAG, "NO MORE conflicts in $parentPath")
                        if (contentResolver != null) {
                            updated = contentResolver!!.update(
                                ProviderTableMeta.CONTENT_URI_FILE,
                                cv,
                                ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                                        ProviderTableMeta.FILE_PATH + "=?",
                                arrayOf(account!!.name, parentPath)
                            )
                        } else {
                            try {
                                updated = contentProviderClient!!.update(
                                    ProviderTableMeta.CONTENT_URI_FILE,
                                    cv,
                                    ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                                            ProviderTableMeta.FILE_PATH + "=?", arrayOf(account!!.name, parentPath)
                                )
                            } catch (e: RemoteException) {
                                Log_OC.e(TAG, "Failed saving conflict in database " + e.message)
                            }

                        }

                    } else {
                        Log_OC.d(TAG, "STILL " + descendantsInConflict.count + " in " + parentPath)
                    }

                    descendantsInConflict?.close()

                    parentPath = parentPath.substring(0, parentPath.length - 1)  // trim last /
                    parentPath = parentPath.substring(0, parentPath.lastIndexOf(OCFile.PATH_SEPARATOR) + 1)
                    Log_OC.d(TAG, "checking parents to remove conflict; NEXT $parentPath")
                }
            }
        }

    }

    fun saveCapabilities(capability: RemoteCapability): RemoteCapability {

        // Prepare capabilities data
        val cv = ContentValues()
        cv.put(ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME, account!!.name)
        cv.put(ProviderTableMeta.CAPABILITIES_VERSION_MAYOR, capability.versionMayor)
        cv.put(ProviderTableMeta.CAPABILITIES_VERSION_MINOR, capability.versionMinor)
        cv.put(ProviderTableMeta.CAPABILITIES_VERSION_MICRO, capability.versionMicro)
        cv.put(ProviderTableMeta.CAPABILITIES_VERSION_STRING, capability.versionString)
        cv.put(ProviderTableMeta.CAPABILITIES_VERSION_EDITION, capability.versionEdition)
        cv.put(ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL, capability.corePollinterval)
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED, capability.filesSharingApiEnabled.value)
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED,
            capability.filesSharingPublicEnabled.value
        )
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED,
            capability.filesSharingPublicPasswordEnforced.value
        )
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY,
            capability.filesSharingPublicPasswordEnforcedReadOnly.value
        )
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE,
            capability.filesSharingPublicPasswordEnforcedReadWrite.value
        )
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY,
            capability.filesSharingPublicPasswordEnforcedUploadOnly.value
        )
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED,
            capability.filesSharingPublicExpireDateEnabled.value
        )
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS,
            capability.filesSharingPublicExpireDateDays
        )
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED,
            capability.filesSharingPublicExpireDateEnforced.value
        )
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL,
            capability.filesSharingPublicSendMail.value
        )
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD,
            capability.filesSharingPublicUpload.value
        )
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE,
            capability.filesSharingPublicMultiple.value
        )
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY,
            capability.filesSharingPublicSupportsUploadOnly.value
        )
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL,
            capability.filesSharingUserSendMail.value
        )
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_RESHARING, capability.filesSharingResharing.value)
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING,
            capability.filesSharingFederationOutgoing.value
        )
        cv.put(
            ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING,
            capability.filesSharingFederationIncoming.value
        )
        cv.put(ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING, capability.filesBigFileChunking.value)
        cv.put(ProviderTableMeta.CAPABILITIES_FILES_UNDELETE, capability.filesUndelete.value)
        cv.put(ProviderTableMeta.CAPABILITIES_FILES_VERSIONING, capability.filesVersioning.value)

        if (capabilityExists(account!!.name)) {
            if (contentResolver != null) {
                contentResolver!!.update(
                    ProviderTableMeta.CONTENT_URI_CAPABILITIES, cv,
                    ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + "=?",
                    arrayOf(account!!.name)
                )
            } else {
                try {
                    contentProviderClient!!.update(
                        ProviderTableMeta.CONTENT_URI_CAPABILITIES,
                        cv, ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + "=?",
                        arrayOf(account!!.name)
                    )
                } catch (e: RemoteException) {
                    Log_OC.e(
                        TAG,
                        "Fail to insert insert file to database " + e.message
                    )
                }

            }
        } else {
            var result_uri: Uri? = null
            if (contentResolver != null) {
                result_uri = contentResolver!!.insert(
                    ProviderTableMeta.CONTENT_URI_CAPABILITIES, cv
                )
            } else {
                try {
                    result_uri = contentProviderClient!!.insert(
                        ProviderTableMeta.CONTENT_URI_CAPABILITIES, cv
                    )
                } catch (e: RemoteException) {
                    Log_OC.e(
                        TAG,
                        "Fail to insert insert capability to database " + e.message
                    )
                }

            }
            if (result_uri != null) {
                val new_id = java.lang.Long.parseLong(result_uri.pathSegments[1])
                capability.accountName = account!!.name
            }
        }

        return capability
    }

    private fun capabilityExists(accountName: String): Boolean {
        val c = getCapabilityCursorForAccount(accountName)
        var exists = false
        if (c != null) {
            exists = c.moveToFirst()
            c.close()
        }
        return exists
    }

    private fun getCapabilityCursorForAccount(accountName: String): Cursor? {
        var c: Cursor? = null
        if (contentResolver != null) {
            c = contentResolver!!
                .query(
                    ProviderTableMeta.CONTENT_URI_CAPABILITIES, null,
                    ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + "=? ",
                    arrayOf(accountName), null
                )
        } else {
            try {
                c = contentProviderClient!!.query(
                    ProviderTableMeta.CONTENT_URI_CAPABILITIES, null,
                    ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + "=? ",
                    arrayOf(accountName), null
                )
            } catch (e: RemoteException) {
                Log_OC.e(
                    TAG,
                    "Couldn't determine capability existance, assuming non existance: " + e.message
                )
            }

        }
        return c
    }

    fun getCapability(accountName: String): OCCapability? {
        val capability: OCCapability? = null
        val c = getCapabilityCursorForAccount(accountName)

        // default value with all UNKNOWN
        if (c != null) {
            if (c.moveToFirst()) {
                createCapabilityInstance(c)
            }
            c.close()
        }
        return capability
    }

    private fun createCapabilityInstance(c: Cursor?): OCCapability? {
        var capability: OCCapability? = null
        if (c != null) {
            capability = OCCapability(
                accountName = c.getString(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME)),
                versionMayor = c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_VERSION_MAYOR)),
                versionMinor = c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_VERSION_MINOR)),
                versionMicro = c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_VERSION_MICRO)),
                versionString = c.getString(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_VERSION_STRING)),
                versionEdition = c.getString(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_VERSION_EDITION)),
                corePollInterval = c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL)),
                filesSharingApiEnabled = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED))
                )!!,
                filesSharingPublicEnabled = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED))
                )!!,
                filesSharingPublicPasswordEnforced = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED))
                )!!,
                filesSharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY))
                )!!,
                filesSharingPublicPasswordEnforcedReadWrite = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE))
                )!!,
                filesSharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY))
                )!!,
                filesSharingPublicExpireDateEnabled = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED))
                )!!,
                filesSharingPublicExpireDateDays = c.getInt(
                    c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS)
                ),
                filesSharingPublicExpireDateEnforced = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED))
                )!!,
                filesSharingPublicSendMail = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL))
                )!!,
                filesSharingPublicUpload = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD))
                )!!,
                filesSharingPublicMultiple = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE))
                )!!,
                filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY))
                )!!,
                filesSharingUserSendMail = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL))
                )!!,
                filesSharingResharing = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_RESHARING))
                )!!,
                filesSharingFederationOutgoing = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING))
                )!!,
                filesSharingFederationIncoming = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING))
                )!!,
                filesBigFileChunking = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING))
                )!!,
                filesUndelete = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_FILES_UNDELETE))
                )!!,
                filesVersioning = CapabilityBooleanType.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.CAPABILITIES_FILES_VERSIONING))
                )!!
            )
        }
        return capability
    }

    private fun selectionForAllDescendantsOf(file: OCFile): Pair<String, Array<String>> {
        val selection = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                ProviderTableMeta.FILE_PATH + " LIKE ? "
        val selectionArgs = arrayOf(
            account!!.name, file.remotePath + "_%"     // one or more characters after remote path
        )
        return Pair(
            selection,
            selectionArgs
        )
    }

    companion object {
        val ROOT_PARENT_ID = 0
        private val TAG = FileDataStorageManager::class.java.simpleName
    }
}
