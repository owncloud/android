/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2011  Bartek Przybylski
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

package com.owncloud.android.providers

import android.accounts.AccountManager
import android.content.*
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.owncloud.android.AppExecutors
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.capabilities.datasource.OCLocalCapabilitiesDataSource
import com.owncloud.android.capabilities.db.OCCapability
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OwncloudDatabase
import com.owncloud.android.db.ProviderMeta
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.datasource.OCLocalSharesDataSource
import com.owncloud.android.shares.db.OCShare
import com.owncloud.android.utils.FileStorageUtils
import java.io.File
import java.io.FileNotFoundException
import java.util.*

/**
 * The ContentProvider for the ownCloud App.
 */
class FileContentProvider(val appExecutors: AppExecutors = AppExecutors()) : ContentProvider() {

    private lateinit var dbHelper: DataBaseHelper

    private lateinit var uriMatcher: UriMatcher

    override fun delete(uri: Uri, where: String?, whereArgs: Array<String>?): Int {
        //Log_OC.d(TAG, "Deleting " + uri + " at provider " + this);
        val count: Int
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            count = delete(db, uri, where, whereArgs)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        context?.contentResolver?.notifyChange(
            uri,
            null
        )
        return count
    }

    private fun delete(db: SQLiteDatabase, uri: Uri, where: String?, whereArgs: Array<String>?): Int {
        var count = 0
        when (uriMatcher.match(uri)) {
            SINGLE_FILE -> {
                val c = query(uri, null, where, whereArgs, null)
                var remoteId = ""
                if (c.moveToFirst()) {
                    remoteId = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_REMOTE_ID))
                    c.close()
                }
                Log_OC.d(TAG, "Removing FILE $remoteId")

                count = db.delete(
                    ProviderTableMeta.FILE_TABLE_NAME,
                    ProviderTableMeta._ID
                            + "="
                            + uri.pathSegments[1]
                            + if (!TextUtils.isEmpty(where))
                        " AND (" + where
                                + ")"
                    else
                        "", whereArgs
                )
            }
            DIRECTORY -> {
                // deletion of folder is recursive
                val children = query(uri, null, null, null, null)
                if (children.moveToFirst()) {
                    var childId: Long
                    var isDir: Boolean
                    while (!children.isAfterLast) {
                        childId = children.getLong(children.getColumnIndex(ProviderTableMeta._ID))
                        isDir = "DIR" == children.getString(
                            children.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE)
                        )
                        count += if (isDir) {
                            delete(
                                db,
                                ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_DIR, childId), null, null
                            )
                        } else {
                            delete(
                                db,
                                ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE, childId), null, null
                            )
                        }
                        children.moveToNext()
                    }
                    children.close()
                }
                count += db.delete(
                    ProviderTableMeta.FILE_TABLE_NAME,
                    ProviderTableMeta._ID
                            + "="
                            + uri.pathSegments[1]
                            + if (!TextUtils.isEmpty(where))
                        " AND (" + where
                                + ")"
                    else
                        "", whereArgs
                )
            }
            ROOT_DIRECTORY ->
                //Log_OC.d(TAG, "Removing ROOT!");
                count = db.delete(ProviderTableMeta.FILE_TABLE_NAME, where, whereArgs)
            SHARES -> count = db.delete(ProviderTableMeta.OCSHARES_TABLE_NAME, where, whereArgs)
            CAPABILITIES -> count = db.delete(ProviderTableMeta.CAPABILITIES_TABLE_NAME, where, whereArgs)
            UPLOADS -> count = db.delete(ProviderTableMeta.UPLOADS_TABLE_NAME, where, whereArgs)
            CAMERA_UPLOADS_SYNC -> count = db.delete(ProviderTableMeta.CAMERA_UPLOADS_SYNC_TABLE_NAME, where, whereArgs)
            QUOTAS -> count = db.delete(ProviderTableMeta.USER_QUOTAS_TABLE_NAME, where, whereArgs)
            else -> throw IllegalArgumentException("Unknown uri: $uri")
        }
        return count
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            ROOT_DIRECTORY -> ProviderTableMeta.CONTENT_TYPE
            SINGLE_FILE -> ProviderTableMeta.CONTENT_TYPE_ITEM
            else -> throw IllegalArgumentException("Unknown Uri id.$uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues): Uri? {
        val newUri: Uri?
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            newUri = insert(db, uri, values)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        context?.contentResolver?.notifyChange(newUri!!, null)
        return newUri
    }

    private fun insert(db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        when (uriMatcher.match(uri)) {
            ROOT_DIRECTORY, SINGLE_FILE -> {
                val remotePath = values.getAsString(ProviderTableMeta.FILE_PATH)
                val accountName = values.getAsString(ProviderTableMeta.FILE_ACCOUNT_OWNER)
                val projection =
                    arrayOf(ProviderTableMeta._ID, ProviderTableMeta.FILE_PATH, ProviderTableMeta.FILE_ACCOUNT_OWNER)
                val where = "${ProviderTableMeta.FILE_PATH}=? AND ${ProviderTableMeta.FILE_ACCOUNT_OWNER}=?"
                val whereArgs = arrayOf(remotePath, accountName)
                val doubleCheck = query(uri, projection, where, whereArgs, null)
                // ugly patch; serious refactorization is needed to reduce work in
                // FileDataStorageManager and bring it to FileContentProvider
                if (!doubleCheck.moveToFirst()) {
                    doubleCheck.close()
                    val fileId = db.insert(ProviderTableMeta.FILE_TABLE_NAME, null, values)
                    if (fileId <= 0) throw SQLException("ERROR $uri")
                    return ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE, fileId)
                } else {
                    // file is already inserted; race condition, let's avoid a duplicated entry
                    val insertedFileUri = ContentUris.withAppendedId(
                        ProviderTableMeta.CONTENT_URI_FILE,
                        doubleCheck.getLong(doubleCheck.getColumnIndex(ProviderTableMeta._ID))
                    )
                    doubleCheck.close()
                    return insertedFileUri
                }
            }
            SHARES -> {
                val shareId = OwncloudDatabase.getDatabase(context).shareDao().insert(
                    listOf(OCShare.fromContentValues(values))
                )[0]

                if (shareId <= 0) throw SQLException("ERROR $uri")
                return ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_SHARE, shareId)
            }

            CAPABILITIES -> {
                val capabilityId = db.insert(ProviderTableMeta.CAPABILITIES_TABLE_NAME, null, values)

                if (capabilityId <= 0) throw SQLException("ERROR $uri")
                return ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_CAPABILITIES, capabilityId)
            }

            UPLOADS -> {
                val uploadId = db.insert(ProviderTableMeta.UPLOADS_TABLE_NAME, null, values)

                if (uploadId <= 0) throw SQLException("ERROR $uri")
                trimSuccessfulUploads(db)
                return ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_UPLOADS, uploadId)
            }

            CAMERA_UPLOADS_SYNC -> {
                val cameraUploadId = db.insert(
                    ProviderTableMeta.CAMERA_UPLOADS_SYNC_TABLE_NAME, null,
                    values
                )
                if (cameraUploadId <= 0) throw SQLException("ERROR $uri")

                return ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_CAMERA_UPLOADS_SYNC, cameraUploadId)
            }
            QUOTAS -> {
                val quotaId = db.insert(
                    ProviderTableMeta.USER_QUOTAS_TABLE_NAME, null,
                    values
                )

                if (quotaId <= 0) throw SQLException("ERROR $uri")
                return ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_QUOTAS, quotaId)
            }
            else -> throw IllegalArgumentException("Unknown uri id: $uri")
        }

    }

    private fun updateFilesTableAccordingToShareInsertion(
        db: SQLiteDatabase, newShare: ContentValues
    ) {
        val fileValues = ContentValues()
        val newShareType = newShare.getAsInteger(ProviderTableMeta.OCSHARES_SHARE_TYPE)!!
        if (newShareType == ShareType.PUBLIC_LINK.value) {
            fileValues.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, TRUE)
        } else if (newShareType == ShareType.USER.value ||
            newShareType == ShareType.GROUP.value ||
            newShareType == ShareType.FEDERATED.value
        ) {
            fileValues.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, TRUE)
        }

        val where = "${ProviderTableMeta.FILE_PATH}=? AND ${ProviderTableMeta.FILE_ACCOUNT_OWNER}=?"
        val whereArgs = arrayOf(
            newShare.getAsString(ProviderTableMeta.OCSHARES_PATH),
            newShare.getAsString(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER)
        )
        db.update(ProviderTableMeta.FILE_TABLE_NAME, fileValues, where, whereArgs)
    }

    override fun onCreate(): Boolean {
        dbHelper = DataBaseHelper(context)

        val authority = context?.resources?.getString(R.string.authority)
        uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        uriMatcher.addURI(authority, null, ROOT_DIRECTORY)
        uriMatcher.addURI(authority, "file/", SINGLE_FILE)
        uriMatcher.addURI(authority, "file/#", SINGLE_FILE)
        uriMatcher.addURI(authority, "dir/", DIRECTORY)
        uriMatcher.addURI(authority, "dir/#", DIRECTORY)
        uriMatcher.addURI(authority, "shares/", SHARES)
        uriMatcher.addURI(authority, "shares/#", SHARES)
        uriMatcher.addURI(authority, "capabilities/", CAPABILITIES)
        uriMatcher.addURI(authority, "capabilities/#", CAPABILITIES)
        uriMatcher.addURI(authority, "uploads/", UPLOADS)
        uriMatcher.addURI(authority, "uploads/#", UPLOADS)
        uriMatcher.addURI(authority, "cameraUploadsSync/", CAMERA_UPLOADS_SYNC)
        uriMatcher.addURI(authority, "cameraUploadsSync/#", CAMERA_UPLOADS_SYNC)
        uriMatcher.addURI(authority, "quotas/", QUOTAS)
        uriMatcher.addURI(authority, "quotas/#", QUOTAS)

        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {
        if (selection != null && selectionArgs == null) {
            throw IllegalArgumentException("Selection not allowed, use parameterized queries")
        }

        val db: SQLiteDatabase = dbHelper.writableDatabase

        val sqlQuery = SQLiteQueryBuilder()

        sqlQuery.tables = ProviderTableMeta.FILE_TABLE_NAME

        // To use full SQL queries within Room
        val newDb: SupportSQLiteDatabase = OwncloudDatabase.getDatabase(context).openHelper.writableDatabase

        when (uriMatcher.match(uri)) {
            ROOT_DIRECTORY -> sqlQuery.setProjectionMap(fileProjectionMap)
            DIRECTORY -> {
                val folderId = uri.pathSegments[1]
                sqlQuery.appendWhere(
                    ProviderTableMeta.FILE_PARENT + "="
                            + folderId
                )
                sqlQuery.setProjectionMap(fileProjectionMap)
            }
            SINGLE_FILE -> {
                if (uri.pathSegments.size > 1) {
                    sqlQuery.appendWhere(
                        ProviderTableMeta._ID + "="
                                + uri.pathSegments[1]
                    )
                }
                sqlQuery.setProjectionMap(fileProjectionMap)
            }
            SHARES -> {
                val supportSqlQuery = SupportSQLiteQueryBuilder
                    .builder(ProviderTableMeta.OCSHARES_TABLE_NAME)
                    .columns(projection)
                    .selection(selection, selectionArgs)
                    .orderBy(
                        if (TextUtils.isEmpty(sortOrder)) {
                            sortOrder
                        } else {
                            ProviderTableMeta.OCSHARES_DEFAULT_SORT_ORDER
                        }
                    ).create()

                return newDb.query(supportSqlQuery)
            }
            CAPABILITIES -> {
                sqlQuery.tables = ProviderTableMeta.CAPABILITIES_TABLE_NAME
                if (uri.pathSegments.size > 1) {
                    sqlQuery.appendWhereEscapeString(
                        ProviderTableMeta._ID + "="
                                + uri.pathSegments[1]
                    )
                }
                sqlQuery.setProjectionMap(capabilityProjectionMap)
            }
            UPLOADS -> {
                sqlQuery.tables = ProviderTableMeta.UPLOADS_TABLE_NAME
                if (uri.pathSegments.size > 1) {
                    sqlQuery.appendWhere(
                        ProviderTableMeta._ID + "="
                                + uri.pathSegments[1]
                    )
                }
                sqlQuery.setProjectionMap(uploadProjectionMap)
            }
            CAMERA_UPLOADS_SYNC -> {
                sqlQuery.tables = ProviderTableMeta.CAMERA_UPLOADS_SYNC_TABLE_NAME
                if (uri.pathSegments.size > 1) {
                    sqlQuery.appendWhere(
                        ProviderTableMeta._ID + "="
                                + uri.pathSegments[1]
                    )
                }
            }
            QUOTAS -> {
                sqlQuery.tables = ProviderTableMeta.USER_QUOTAS_TABLE_NAME
                if (uri.pathSegments.size > 1) {
                    sqlQuery.appendWhere(
                        ProviderTableMeta._ID + "="
                                + uri.pathSegments[1]
                    )
                }
            }
            else -> throw IllegalArgumentException("Unknown uri id: $uri")
        }

        val order: String? = if (TextUtils.isEmpty(sortOrder)) {
            when (uriMatcher.match(uri)) {
                SHARES -> ProviderTableMeta.OCSHARES_DEFAULT_SORT_ORDER
                CAPABILITIES -> ProviderTableMeta.CAPABILITIES_DEFAULT_SORT_ORDER
                UPLOADS -> ProviderTableMeta.UPLOADS_DEFAULT_SORT_ORDER
                CAMERA_UPLOADS_SYNC -> ProviderTableMeta.CAMERA_UPLOADS_SYNC_DEFAULT_SORT_ORDER
                else // Files
                -> ProviderTableMeta.FILE_DEFAULT_SORT_ORDER
            }
        } else {
            sortOrder
        }

        // DB case_sensitive
        db.execSQL("PRAGMA case_sensitive_like = true")
        val c = sqlQuery.query(db, projection, selection, selectionArgs, null, null, order)
        c.setNotificationUri(context?.contentResolver, uri)
        return c
    }

    override fun update(uri: Uri, values: ContentValues, selection: String?, selectionArgs: Array<String>?): Int {
        val count: Int
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            count = update(db, uri, values, selection, selectionArgs)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        context?.contentResolver?.notifyChange(uri, null)
        return count
    }

    private fun update(
        db: SQLiteDatabase,
        uri: Uri,
        values: ContentValues,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        when (uriMatcher.match(uri)) {
            DIRECTORY -> return 0 //updateFolderSize(db, selectionArgs[0]);
            SHARES -> {
                return OwncloudDatabase.getDatabase(context!!).shareDao().update(OCShare.fromContentValues(values))
                    .toInt()
            }
            CAPABILITIES -> return db.update(
                ProviderTableMeta.CAPABILITIES_TABLE_NAME, values, selection, selectionArgs
            )
            UPLOADS -> {
                val ret = db.update(
                    ProviderTableMeta.UPLOADS_TABLE_NAME, values, selection, selectionArgs
                )
                trimSuccessfulUploads(db)
                return ret
            }
            CAMERA_UPLOADS_SYNC -> return db.update(
                ProviderTableMeta.CAMERA_UPLOADS_SYNC_TABLE_NAME, values, selection,
                selectionArgs
            )
            QUOTAS -> return db.update(ProviderTableMeta.USER_QUOTAS_TABLE_NAME, values, selection, selectionArgs)
            else -> return db.update(
                ProviderTableMeta.FILE_TABLE_NAME, values, selection, selectionArgs
            )
        }
    }

    @Throws(OperationApplicationException::class)
    override fun applyBatch(operations: ArrayList<ContentProviderOperation>): Array<ContentProviderResult?> {
        Log_OC.d(
            "FileContentProvider", "applying batch in provider " + this +
                    " (temporary: " + isTemporary + ")"
        )
        val results = arrayOfNulls<ContentProviderResult>(operations.size)
        var i = 0

        val db = dbHelper.writableDatabase
        db.beginTransaction()  // it's supposed that transactions can be nested
        try {
            for (operation in operations) {
                results[i] = operation.apply(this, results, i)
                i++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        Log_OC.d("FileContentProvider", "applied batch in provider $this")
        return results
    }

    private inner class DataBaseHelper internal constructor(context: Context?) :
        SQLiteOpenHelper(context, ProviderMeta.DB_NAME, null, ProviderMeta.DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            // files table
            Log_OC.i("SQL", "Entering in onCreate")
            createFilesTable(db)

            // Create capabilities table
            createCapabilitiesTable(db)

            // Create uploads table
            createUploadsTable(db)

            // Create user avatar table
            createUserAvatarsTable(db)

            // Create user quota table
            createUserQuotaTable(db)

            // Create camera upload sync table
            createCameraUploadsSyncTable(db)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log_OC.i("SQL", "Entering in onUpgrade")
            var upgraded = false
            if (oldVersion == 1 && newVersion >= 2) {
                Log_OC.i("SQL", "Entering in the #1 ADD in onUpgrade")
                db.execSQL(
                    "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_KEEP_IN_SYNC + " INTEGER " +
                            " DEFAULT 0"
                )
                upgraded = true
            }
            if (oldVersion < 3 && newVersion >= 3) {
                Log_OC.i("SQL", "Entering in the #2 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA +
                                " INTEGER " + " DEFAULT 0"
                    )

                    // assume there are not local changes pending to upload
                    db.execSQL(
                        "UPDATE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " SET " + ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA + " = "
                                + System.currentTimeMillis() +
                                " WHERE " + ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }
            if (oldVersion < 4 && newVersion >= 4) {
                Log_OC.i("SQL", "Entering in the #3 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA +
                                " INTEGER " + " DEFAULT 0"
                    )

                    db.execSQL(
                        "UPDATE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " SET " + ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA + " = " +
                                ProviderTableMeta.FILE_MODIFIED +
                                " WHERE " + ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 5 && newVersion >= 5) {
                Log_OC.i("SQL", "Entering in the #4 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.FILE_ETAG + " TEXT " +
                                " DEFAULT NULL"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 6 && newVersion >= 6) {
                Log_OC.i("SQL", "Entering in the #5 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.FILE_SHARED_VIA_LINK + " INTEGER " +
                                " DEFAULT 0"
                    )

                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.FILE_PUBLIC_LINK + " TEXT " +
                                " DEFAULT NULL"
                    )

                    // Create table ocshares
                    createOCSharesTable(db)
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 7 && newVersion >= 7) {
                Log_OC.i("SQL", "Entering in the #7 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.FILE_PERMISSIONS + " TEXT " +
                                " DEFAULT NULL"
                    )

                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.FILE_REMOTE_ID + " TEXT " +
                                " DEFAULT NULL"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 8 && newVersion >= 8) {
                Log_OC.i("SQL", "Entering in the #8 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.FILE_UPDATE_THUMBNAIL + " INTEGER " +
                                " DEFAULT 0"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 9 && newVersion >= 9) {
                Log_OC.i("SQL", "Entering in the #9 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.FILE_IS_DOWNLOADING + " INTEGER " +
                                " DEFAULT 0"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 10 && newVersion >= 10) {
                Log_OC.i("SQL", "Entering in the #10 ADD in onUpgrade")
                updateAccountName(db)
                upgraded = true
            }

            if (oldVersion < 11 && newVersion >= 11) {
                Log_OC.i("SQL", "Entering in the #11 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.FILE_ETAG_IN_CONFLICT + " TEXT " +
                                " DEFAULT NULL"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 12 && newVersion >= 12) {
                Log_OC.i("SQL", "Entering in the #12 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.FILE_SHARED_WITH_SHAREE + " INTEGER " +
                                " DEFAULT 0"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 13 && newVersion >= 13) {
                Log_OC.i("SQL", "Entering in the #13 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    // Create capabilities table
                    createCapabilitiesTable(db)
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 14 && newVersion >= 14) {
                Log_OC.i("SQL", "Entering in the #14 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    // drop old instant_upload table
                    db.execSQL("DROP TABLE IF EXISTS " + "instant_upload" + ";")
                    // Create uploads table
                    createUploadsTable(db)
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 15 && newVersion >= 15) {
                Log_OC.i("SQL", "Entering in the #15 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    // Create user profiles table
                    createUserAvatarsTable(db)
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 16 && newVersion >= 16) {
                Log_OC.i("SQL", "Entering in the #16 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.FILE_TREE_ETAG + " TEXT " +
                                " DEFAULT NULL"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }
            if (oldVersion < 17 && newVersion >= 17) {
                Log_OC.i("SQL", "Entering in the #17 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.OCSHARES_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.OCSHARES_NAME + " TEXT " +
                                " DEFAULT NULL"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                    // SQLite does not allow to drop a columns; ftm, we'll not recreate
                    // the files table without the column FILE_PUBLIC_LINK, just forget about
                } finally {
                    db.endTransaction()
                }
            }
            if (oldVersion < 18 && newVersion >= 18) {
                Log_OC.i("SQL", "Entering in the #18 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.OCSHARES_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.OCSHARES_URL + " TEXT " +
                                " DEFAULT NULL"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }
            if (oldVersion < 19 && newVersion >= 19) {

                Log_OC.i("SQL", "Entering in the #19 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE
                                + " INTEGER " + " DEFAULT -1"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }
            if (oldVersion < 20 && newVersion >= 20) {

                Log_OC.i("SQL", "Entering in the #20 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY + " INTEGER " +
                                " DEFAULT -1"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 21 && newVersion >= 21) {
                Log_OC.i("SQL", "Entering in the #21 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.FILE_PRIVATE_LINK + " TEXT " +
                                " DEFAULT NULL"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 22 && newVersion >= 22) {
                Log_OC.i("SQL", "Entering in the #22 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    createCameraUploadsSyncTable(db)
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 23 && newVersion >= 23) {
                Log_OC.i("SQL", "Entering in the #23 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    createUserQuotaTable(db)
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 24 && newVersion >= 24) {
                Log_OC.i("SQL", "Entering in the #24 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.UPLOADS_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.UPLOADS_TRANSFER_ID + " TEXT " +
                                " DEFAULT NULL"
                    )
                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 25 && newVersion >= 25) {
                Log_OC.i("SQL", "Entering in the #25 ADD in onUpgrade")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                " ADD COLUMN " +
                                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY +
                                " INTEGER DEFAULT NULL"
                    )

                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                " ADD COLUMN " +
                                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE +
                                " INTEGER DEFAULT NULL"
                    )

                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                " ADD COLUMN " +
                                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY +
                                " INTEGER DEFAULT NULL"
                    )

                    db.execSQL(
                        "ALTER TABLE " + ProviderTableMeta.OCSHARES_TABLE_NAME +
                                " ADD COLUMN " + ProviderTableMeta.OCSHARES_SHARE_WITH_ADDITIONAL_INFO + " TEXT " +
                                " DEFAULT NULL"
                    )

                    db.setTransactionSuccessful()
                    upgraded = true
                } finally {
                    db.endTransaction()
                }
            }

            if (oldVersion < 26 && newVersion >= 26) {
                Log_OC.i("SQL", "Entering in #26 to migrate shares from SQLite to Room")
                val cursor = db.query(
                    ProviderTableMeta.OCSHARES_TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )

                if (cursor.moveToFirst()) {
                    val shares = mutableListOf<OCShare>()

                    do {
                        shares.add(OCShare.fromCursor(cursor))
                    } while (cursor.moveToNext())

                    // Insert share list to the new shares table in new database
                    appExecutors.diskIO().execute {
                        OCLocalSharesDataSource().insert(shares)
                    }

                    // Drop old shares table from old database
                    db.execSQL("DROP TABLE IF EXISTS " + ProviderTableMeta.OCSHARES_TABLE_NAME + ";")
                }
            }

            if (oldVersion < 27 && newVersion >= 27) {
                Log_OC.i("SQL", "Entering in #27 to migrate capabilities from SQLite to Room")
                val cursor = db.query(
                    ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )

                if (cursor.moveToFirst()) {
                    // Insert capability to the new capabilities table in new database
                    appExecutors.diskIO().execute {
                        OCLocalCapabilitiesDataSource().insert(
                            listOf(OCCapability.fromCursor(cursor))
                        )
                    }
                }
            }

            if (!upgraded) {
                Log_OC.i(
                    "SQL", "OUT of the ADD in onUpgrade; oldVersion == " + oldVersion +
                            ", newVersion == " + newVersion
                )
            }
        }
    }

    private fun createFilesTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE " + ProviderTableMeta.FILE_TABLE_NAME + "("
                    + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                    + ProviderTableMeta.FILE_NAME + " TEXT, "
                    + ProviderTableMeta.FILE_PATH + " TEXT, "
                    + ProviderTableMeta.FILE_PARENT + " INTEGER, "
                    + ProviderTableMeta.FILE_CREATION + " INTEGER, "
                    + ProviderTableMeta.FILE_MODIFIED + " INTEGER, "
                    + ProviderTableMeta.FILE_CONTENT_TYPE + " TEXT, "
                    + ProviderTableMeta.FILE_CONTENT_LENGTH + " INTEGER, "
                    + ProviderTableMeta.FILE_STORAGE_PATH + " TEXT, "
                    + ProviderTableMeta.FILE_ACCOUNT_OWNER + " TEXT, "
                    + ProviderTableMeta.FILE_LAST_SYNC_DATE + " INTEGER, "
                    + ProviderTableMeta.FILE_KEEP_IN_SYNC + " INTEGER, "
                    + ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA + " INTEGER, "
                    + ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA + " INTEGER, "
                    + ProviderTableMeta.FILE_ETAG + " TEXT, "
                    + ProviderTableMeta.FILE_TREE_ETAG + " TEXT, "
                    + ProviderTableMeta.FILE_SHARED_VIA_LINK + " INTEGER, "
                    + ProviderTableMeta.FILE_PUBLIC_LINK + " TEXT, "
                    + ProviderTableMeta.FILE_PERMISSIONS + " TEXT null,"
                    + ProviderTableMeta.FILE_REMOTE_ID + " TEXT null,"
                    + ProviderTableMeta.FILE_UPDATE_THUMBNAIL + " INTEGER," //boolean

                    + ProviderTableMeta.FILE_IS_DOWNLOADING + " INTEGER," //boolean

                    + ProviderTableMeta.FILE_ETAG_IN_CONFLICT + " TEXT,"
                    + ProviderTableMeta.FILE_SHARED_WITH_SHAREE + " INTEGER,"
                    + ProviderTableMeta.FILE_PRIVATE_LINK + " TEXT );"
        )
    }

    private fun createOCSharesTable(db: SQLiteDatabase) {
        // Create ocshares table
        db.execSQL(
            "CREATE TABLE " + ProviderTableMeta.OCSHARES_TABLE_NAME + "("
                    + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                    + ProviderTableMeta.OCSHARES_FILE_SOURCE + " INTEGER, "
                    + ProviderTableMeta.OCSHARES_ITEM_SOURCE + " INTEGER, "
                    + ProviderTableMeta.OCSHARES_SHARE_TYPE + " INTEGER, "
                    + ProviderTableMeta.OCSHARES_SHARE_WITH + " TEXT, "
                    + ProviderTableMeta.OCSHARES_PATH + " TEXT, "
                    + ProviderTableMeta.OCSHARES_PERMISSIONS + " INTEGER, "
                    + ProviderTableMeta.OCSHARES_SHARED_DATE + " INTEGER, "
                    + ProviderTableMeta.OCSHARES_EXPIRATION_DATE + " INTEGER, "
                    + ProviderTableMeta.OCSHARES_TOKEN + " TEXT, "
                    + ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME + " TEXT, "
                    + ProviderTableMeta.OCSHARES_IS_DIRECTORY + " INTEGER, "  // boolean
                    + ProviderTableMeta.OCSHARES_USER_ID + " INTEGER, "
                    + ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + " INTEGER,"
                    + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " TEXT, "
                    + ProviderTableMeta.OCSHARES_URL + " TEXT, "
                    + ProviderTableMeta.OCSHARES_NAME + " TEXT );"
        )
    }

    private fun createCapabilitiesTable(db: SQLiteDatabase) {
        // Create capabilities table
        db.execSQL(
            "CREATE TABLE " + ProviderTableMeta.CAPABILITIES_TABLE_NAME + "("
                    + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                    + ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + " TEXT, "
                    + ProviderTableMeta.CAPABILITIES_VERSION_MAYOR + " INTEGER, "
                    + ProviderTableMeta.CAPABILITIES_VERSION_MINOR + " INTEGER, "
                    + ProviderTableMeta.CAPABILITIES_VERSION_MICRO + " INTEGER, "
                    + ProviderTableMeta.CAPABILITIES_VERSION_STRING + " TEXT, "
                    + ProviderTableMeta.CAPABILITIES_VERSION_EDITION + " TEXT, "
                    + ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL + " INTEGER, "
                    + ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED + " INTEGER, " // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED + " INTEGER, "  // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED + " INTEGER, "    // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY + " INTEGER, "   // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE + " INTEGER, "  // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY + " INTEGER, " // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED + " INTEGER, "  // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS + " INTEGER, "
                    + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED + " INTEGER, " // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL + " INTEGER, "    // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD + " INTEGER, "       // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE + " INTEGER, "     // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY + " INTEGER, "     // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL + " INTEGER, "      // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_RESHARING + " INTEGER, "           // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING + " INTEGER, "     // boolean
                    + ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING + " INTEGER, "     // boolean
                    + ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING + " INTEGER, "   // boolean
                    + ProviderTableMeta.CAPABILITIES_FILES_UNDELETE + " INTEGER, "  // boolean
                    + ProviderTableMeta.CAPABILITIES_FILES_VERSIONING + " INTEGER );"
        )   // boolean
    }

    private fun createUploadsTable(db: SQLiteDatabase) {
        // Create uploads table
        db.execSQL(
            "CREATE TABLE " + ProviderTableMeta.UPLOADS_TABLE_NAME + "("
                    + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                    + ProviderTableMeta.UPLOADS_LOCAL_PATH + " TEXT, "
                    + ProviderTableMeta.UPLOADS_REMOTE_PATH + " TEXT, "
                    + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + " TEXT, "
                    + ProviderTableMeta.UPLOADS_FILE_SIZE + " LONG, "
                    + ProviderTableMeta.UPLOADS_STATUS + " INTEGER, "               // UploadStatus

                    + ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR + " INTEGER, "      // Upload LocalBehaviour

                    + ProviderTableMeta.UPLOADS_UPLOAD_TIME + " INTEGER, "
                    + ProviderTableMeta.UPLOADS_FORCE_OVERWRITE + " INTEGER, "  // boolean

                    + ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER + " INTEGER, "  // boolean

                    + ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP + " INTEGER, "
                    + ProviderTableMeta.UPLOADS_LAST_RESULT + " INTEGER, "     // Upload LastResult

                    + ProviderTableMeta.UPLOADS_CREATED_BY + " INTEGER, "     // Upload createdBy

                    + ProviderTableMeta.UPLOADS_TRANSFER_ID + " TEXT );"    // Upload chunkedUploadId
        )
    }

    private fun createUserAvatarsTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE " + ProviderTableMeta.USER_AVATARS__TABLE_NAME + "("
                    + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                    + ProviderTableMeta.USER_AVATARS__ACCOUNT_NAME + " TEXT, "
                    + ProviderTableMeta.USER_AVATARS__CACHE_KEY + " TEXT, "
                    + ProviderTableMeta.USER_AVATARS__MIME_TYPE + " TEXT, "
                    + ProviderTableMeta.USER_AVATARS__ETAG + " TEXT );"
        )
    }

    private fun createUserQuotaTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE " + ProviderTableMeta.USER_QUOTAS_TABLE_NAME + "("
                    + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                    + ProviderTableMeta.USER_QUOTAS__ACCOUNT_NAME + " TEXT, "
                    + ProviderTableMeta.USER_QUOTAS__FREE + " LONG, "
                    + ProviderTableMeta.USER_QUOTAS__RELATIVE + " LONG, "
                    + ProviderTableMeta.USER_QUOTAS__TOTAL + " LONG, "
                    + ProviderTableMeta.USER_QUOTAS__USED + " LONG );"
        )
    }

    private fun createCameraUploadsSyncTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE " + ProviderTableMeta.CAMERA_UPLOADS_SYNC_TABLE_NAME + "("
                    + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                    + ProviderTableMeta.PICTURES_LAST_SYNC_TIMESTAMP + " INTEGER,"
                    + ProviderTableMeta.VIDEOS_LAST_SYNC_TIMESTAMP + " INTEGER);"
        )
    }

    /**
     * Version 10 of database does not modify its scheme. It coincides with the upgrade of the ownCloud account names
     * structure to include in it the path to the server instance. Updating the account names and path to local files
     * in the files table is a must to keep the existing account working and the database clean.
     *
     * See [com.owncloud.android.authentication.AccountUtils.updateAccountVersion]
     *
     * @param db Database where table of files is included.
     */
    private fun updateAccountName(db: SQLiteDatabase) {
        Log_OC.d("SQL", "THREAD:  " + Thread.currentThread().name)
        val ama = AccountManager.get(context)
        try {
            // get accounts from AccountManager ;  we can't be sure if accounts in it are updated or not although
            // we know the update was previously done in {link @FileActivity#onCreate} because the changes through
            // AccountManager are not synchronous
            val accounts = AccountManager.get(context).getAccountsByType(
                MainApp.getAccountType()
            )
            var serverUrl: String
            var username: String?
            var oldAccountName: String
            var newAccountName: String
            for (account in accounts) {
                // build both old and new account name
                serverUrl = ama.getUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL)
                username = AccountUtils.getUsernameForAccount(account)
                oldAccountName = AccountUtils.buildAccountNameOld(Uri.parse(serverUrl), username)
                newAccountName = AccountUtils.buildAccountName(Uri.parse(serverUrl), username)

                // update values in database
                db.beginTransaction()
                try {
                    val cv = ContentValues()
                    cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, newAccountName)
                    val num = db.update(
                        ProviderTableMeta.FILE_TABLE_NAME,
                        cv,
                        ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                        arrayOf(oldAccountName)
                    )

                    Log_OC.d(
                        "SQL", "Updated account in database: old name == " + oldAccountName +
                                ", new name == " + newAccountName + " (" + num + " rows updated )"
                    )

                    // update path for downloaded files
                    updateDownloadedFiles(db, newAccountName, oldAccountName)

                    db.setTransactionSuccessful()

                } catch (e: SQLException) {
                    Log_OC.e(TAG, "SQL Exception upgrading account names or paths in database", e)
                } finally {
                    db.endTransaction()
                }
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Exception upgrading account names or paths in database", e)
        }

    }

    /**
     * Rename the local ownCloud folder of one account to match the a rename of the account itself. Updates the
     * table of files in database so that the paths to the local files keep being the same.
     *
     * @param db             Database where table of files is included.
     * @param newAccountName New name for the target OC account.
     * @param oldAccountName Old name of the target OC account.
     */
    private fun updateDownloadedFiles(
        db: SQLiteDatabase, newAccountName: String,
        oldAccountName: String
    ) {

        val whereClause = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL"

        val c = db.query(
            ProviderTableMeta.FILE_TABLE_NAME, null,
            whereClause,
            arrayOf(newAccountName), null, null, null
        )

        c.use {
            if (it.moveToFirst()) {
                // create storage path
                val oldAccountPath = FileStorageUtils.getSavePath(oldAccountName)
                val newAccountPath = FileStorageUtils.getSavePath(newAccountName)

                // move files
                val oldAccountFolder = File(oldAccountPath)
                val newAccountFolder = File(newAccountPath)
                oldAccountFolder.renameTo(newAccountFolder)

                // update database
                do {
                    // Update database
                    val oldPath = it.getString(
                        it.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH)
                    )
                    val file = OCFile(
                        it.getString(it.getColumnIndex(ProviderTableMeta.FILE_PATH))
                    )
                    val newPath = FileStorageUtils.getDefaultSavePathFor(newAccountName, file)

                    val cv = ContentValues()
                    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, newPath)
                    db.update(
                        ProviderTableMeta.FILE_TABLE_NAME,
                        cv,
                        ProviderTableMeta.FILE_STORAGE_PATH + "=?",
                        arrayOf(oldPath)
                    )

                    Log_OC.v(
                        "SQL", "Updated path of downloaded file: old file name == " + oldPath +
                                ", new file name == " + newPath
                    )

                } while (it.moveToNext())
            }
        }
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String, signal: CancellationSignal?): ParcelFileDescriptor? {
        return super.openFile(uri, mode, signal)
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return super.openFile(uri, mode)
    }

    /**
     * Grants that total count of successful uploads stored is not greater than MAX_SUCCESSFUL_UPLOADS.
     *
     * Removes older uploads if needed.
     */
    private fun trimSuccessfulUploads(db: SQLiteDatabase) {
        var c: Cursor? = null
        try {
            c = db.rawQuery(
                "delete from " + ProviderTableMeta.UPLOADS_TABLE_NAME +
                        " where " + ProviderTableMeta.UPLOADS_STATUS + " == "
                        + UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED.value +
                        " and " + ProviderTableMeta._ID +
                        " not in (select " + ProviderTableMeta._ID +
                        " from " + ProviderTableMeta.UPLOADS_TABLE_NAME +
                        " where " + ProviderTableMeta.UPLOADS_STATUS + " == "
                        + UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED.value +
                        " order by " + ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP +
                        " desc limit " + MAX_SUCCESSFUL_UPLOADS +
                        ")", null
            )
            c!!.moveToFirst() // do something with the cursor, or deletion doesn't happen; true story

        } catch (e: Exception) {
            Log_OC.e(
                TAG,
                "Something wrong trimming successful uploads, database could grow more than expected",
                e
            )

        } finally {
            c?.close()
        }
    }

    companion object {

        private const val SINGLE_FILE = 1
        private const val DIRECTORY = 2
        private const val ROOT_DIRECTORY = 3
        private const val SHARES = 4
        private const val CAPABILITIES = 5
        private const val UPLOADS = 6
        private const val CAMERA_UPLOADS_SYNC = 7
        private const val QUOTAS = 8

        private val TAG = FileContentProvider::class.java.simpleName

        private const val MAX_SUCCESSFUL_UPLOADS = "30"

        private val fileProjectionMap = HashMap<String, String>()

        private const val TRUE = 1
        private const val FALSE = 0

        init {

            fileProjectionMap[ProviderTableMeta._ID] = ProviderTableMeta._ID
            fileProjectionMap[ProviderTableMeta.FILE_PARENT] = ProviderTableMeta.FILE_PARENT
            fileProjectionMap[ProviderTableMeta.FILE_NAME] = ProviderTableMeta.FILE_NAME
            fileProjectionMap[ProviderTableMeta.FILE_CREATION] = ProviderTableMeta.FILE_CREATION
            fileProjectionMap[ProviderTableMeta.FILE_MODIFIED] = ProviderTableMeta.FILE_MODIFIED
            fileProjectionMap[ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA] =
                ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA
            fileProjectionMap[ProviderTableMeta.FILE_CONTENT_LENGTH] = ProviderTableMeta.FILE_CONTENT_LENGTH
            fileProjectionMap[ProviderTableMeta.FILE_CONTENT_TYPE] = ProviderTableMeta.FILE_CONTENT_TYPE
            fileProjectionMap[ProviderTableMeta.FILE_STORAGE_PATH] = ProviderTableMeta.FILE_STORAGE_PATH
            fileProjectionMap[ProviderTableMeta.FILE_PATH] = ProviderTableMeta.FILE_PATH
            fileProjectionMap[ProviderTableMeta.FILE_ACCOUNT_OWNER] = ProviderTableMeta.FILE_ACCOUNT_OWNER
            fileProjectionMap[ProviderTableMeta.FILE_LAST_SYNC_DATE] = ProviderTableMeta.FILE_LAST_SYNC_DATE
            fileProjectionMap[ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA] =
                ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA
            fileProjectionMap[ProviderTableMeta.FILE_KEEP_IN_SYNC] = ProviderTableMeta.FILE_KEEP_IN_SYNC
            fileProjectionMap[ProviderTableMeta.FILE_ETAG] = ProviderTableMeta.FILE_ETAG
            fileProjectionMap[ProviderTableMeta.FILE_TREE_ETAG] = ProviderTableMeta.FILE_TREE_ETAG
            fileProjectionMap[ProviderTableMeta.FILE_SHARED_VIA_LINK] = ProviderTableMeta.FILE_SHARED_VIA_LINK
            fileProjectionMap[ProviderTableMeta.FILE_SHARED_WITH_SHAREE] = ProviderTableMeta.FILE_SHARED_WITH_SHAREE
            fileProjectionMap[ProviderTableMeta.FILE_PERMISSIONS] = ProviderTableMeta.FILE_PERMISSIONS
            fileProjectionMap[ProviderTableMeta.FILE_REMOTE_ID] = ProviderTableMeta.FILE_REMOTE_ID
            fileProjectionMap[ProviderTableMeta.FILE_UPDATE_THUMBNAIL] = ProviderTableMeta.FILE_UPDATE_THUMBNAIL
            fileProjectionMap[ProviderTableMeta.FILE_IS_DOWNLOADING] = ProviderTableMeta.FILE_IS_DOWNLOADING
            fileProjectionMap[ProviderTableMeta.FILE_ETAG_IN_CONFLICT] = ProviderTableMeta.FILE_ETAG_IN_CONFLICT
            fileProjectionMap[ProviderTableMeta.FILE_PRIVATE_LINK] = ProviderTableMeta.FILE_PRIVATE_LINK
        }

        private val shareProjectionMap = HashMap<String, String>()

        init {

            shareProjectionMap[ProviderTableMeta._ID] = ProviderTableMeta._ID
            shareProjectionMap[ProviderTableMeta.OCSHARES_FILE_SOURCE] = ProviderTableMeta.OCSHARES_FILE_SOURCE
            shareProjectionMap[ProviderTableMeta.OCSHARES_ITEM_SOURCE] = ProviderTableMeta.OCSHARES_ITEM_SOURCE
            shareProjectionMap[ProviderTableMeta.OCSHARES_SHARE_TYPE] = ProviderTableMeta.OCSHARES_SHARE_TYPE
            shareProjectionMap[ProviderTableMeta.OCSHARES_SHARE_WITH] = ProviderTableMeta.OCSHARES_SHARE_WITH
            shareProjectionMap[ProviderTableMeta.OCSHARES_PATH] = ProviderTableMeta.OCSHARES_PATH
            shareProjectionMap[ProviderTableMeta.OCSHARES_PERMISSIONS] = ProviderTableMeta.OCSHARES_PERMISSIONS
            shareProjectionMap[ProviderTableMeta.OCSHARES_SHARED_DATE] = ProviderTableMeta.OCSHARES_SHARED_DATE
            shareProjectionMap[ProviderTableMeta.OCSHARES_EXPIRATION_DATE] = ProviderTableMeta.OCSHARES_EXPIRATION_DATE
            shareProjectionMap[ProviderTableMeta.OCSHARES_TOKEN] = ProviderTableMeta.OCSHARES_TOKEN
            shareProjectionMap[ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME] =
                ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME
            shareProjectionMap[ProviderTableMeta.OCSHARES_SHARE_WITH_ADDITIONAL_INFO] =
                ProviderTableMeta.OCSHARES_SHARE_WITH_ADDITIONAL_INFO
            shareProjectionMap[ProviderTableMeta.OCSHARES_IS_DIRECTORY] = ProviderTableMeta.OCSHARES_IS_DIRECTORY
            shareProjectionMap[ProviderTableMeta.OCSHARES_USER_ID] = ProviderTableMeta.OCSHARES_USER_ID
            shareProjectionMap[ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED] =
                ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED
            shareProjectionMap[ProviderTableMeta.OCSHARES_ACCOUNT_OWNER] = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER
            shareProjectionMap[ProviderTableMeta.OCSHARES_NAME] = ProviderTableMeta.OCSHARES_NAME
            shareProjectionMap[ProviderTableMeta.OCSHARES_URL] = ProviderTableMeta.OCSHARES_URL
        }

        private val capabilityProjectionMap = HashMap<String, String>()

        init {

            capabilityProjectionMap[ProviderTableMeta._ID] = ProviderTableMeta._ID
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME] =
                ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_VERSION_MAYOR] =
                ProviderTableMeta.CAPABILITIES_VERSION_MAYOR
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_VERSION_MINOR] =
                ProviderTableMeta.CAPABILITIES_VERSION_MINOR
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_VERSION_MICRO] =
                ProviderTableMeta.CAPABILITIES_VERSION_MICRO
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_VERSION_STRING] =
                ProviderTableMeta.CAPABILITIES_VERSION_STRING
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_VERSION_EDITION] =
                ProviderTableMeta.CAPABILITIES_VERSION_EDITION
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL] =
                ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED] =
                ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED] =
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED] =
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY] =
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE] =
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY] =
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED] =
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS] =
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED] =
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL] =
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD] =
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE] =
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY] =
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL] =
                ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_RESHARING] =
                ProviderTableMeta.CAPABILITIES_SHARING_RESHARING
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING] =
                ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING] =
                ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING] =
                ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_FILES_UNDELETE] =
                ProviderTableMeta.CAPABILITIES_FILES_UNDELETE
            capabilityProjectionMap[ProviderTableMeta.CAPABILITIES_FILES_VERSIONING] =
                ProviderTableMeta.CAPABILITIES_FILES_VERSIONING
        }

        private val uploadProjectionMap = HashMap<String, String>()

        init {

            uploadProjectionMap[ProviderTableMeta._ID] = ProviderTableMeta._ID
            uploadProjectionMap[ProviderTableMeta.UPLOADS_LOCAL_PATH] = ProviderTableMeta.UPLOADS_LOCAL_PATH
            uploadProjectionMap[ProviderTableMeta.UPLOADS_REMOTE_PATH] = ProviderTableMeta.UPLOADS_REMOTE_PATH
            uploadProjectionMap[ProviderTableMeta.UPLOADS_ACCOUNT_NAME] = ProviderTableMeta.UPLOADS_ACCOUNT_NAME
            uploadProjectionMap[ProviderTableMeta.UPLOADS_FILE_SIZE] = ProviderTableMeta.UPLOADS_FILE_SIZE
            uploadProjectionMap[ProviderTableMeta.UPLOADS_STATUS] = ProviderTableMeta.UPLOADS_STATUS
            uploadProjectionMap[ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR] = ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR
            uploadProjectionMap[ProviderTableMeta.UPLOADS_UPLOAD_TIME] = ProviderTableMeta.UPLOADS_UPLOAD_TIME
            uploadProjectionMap[ProviderTableMeta.UPLOADS_FORCE_OVERWRITE] = ProviderTableMeta.UPLOADS_FORCE_OVERWRITE
            uploadProjectionMap[ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER] =
                ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER
            uploadProjectionMap[ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP] =
                ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP
            uploadProjectionMap[ProviderTableMeta.UPLOADS_LAST_RESULT] = ProviderTableMeta.UPLOADS_LAST_RESULT
            uploadProjectionMap[ProviderTableMeta.UPLOADS_CREATED_BY] = ProviderTableMeta.UPLOADS_CREATED_BY
            uploadProjectionMap[ProviderTableMeta.UPLOADS_TRANSFER_ID] = ProviderTableMeta.UPLOADS_TRANSFER_ID
        }
    }
}
