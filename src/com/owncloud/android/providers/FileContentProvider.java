/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2018 ownCloud GmbH.
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

package com.owncloud.android.providers;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The ContentProvider for the ownCloud App.
 */
public class FileContentProvider extends ContentProvider {

    private DataBaseHelper mDbHelper;

    private static final int SINGLE_FILE = 1;
    private static final int DIRECTORY = 2;
    private static final int ROOT_DIRECTORY = 3;
    private static final int SHARES = 4;
    private static final int CAPABILITIES = 5;
    private static final int UPLOADS = 6;
    private static final int CAMERA_UPLOADS_SYNC = 7;
    private static final int QUOTAS = 8;

    private static final String TAG = FileContentProvider.class.getSimpleName();

    private static final String MAX_SUCCESSFUL_UPLOADS = "30";

    private UriMatcher mUriMatcher;

    private static HashMap<String, String> mFileProjectionMap = new HashMap<>();

    static {

        mFileProjectionMap.put(ProviderTableMeta._ID, ProviderTableMeta._ID);
        mFileProjectionMap.put(ProviderTableMeta.FILE_PARENT, ProviderTableMeta.FILE_PARENT);
        mFileProjectionMap.put(ProviderTableMeta.FILE_NAME, ProviderTableMeta.FILE_NAME);
        mFileProjectionMap.put(ProviderTableMeta.FILE_CREATION, ProviderTableMeta.FILE_CREATION);
        mFileProjectionMap.put(ProviderTableMeta.FILE_MODIFIED, ProviderTableMeta.FILE_MODIFIED);
        mFileProjectionMap.put(ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
                ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA);
        mFileProjectionMap.put(ProviderTableMeta.FILE_CONTENT_LENGTH, ProviderTableMeta.FILE_CONTENT_LENGTH);
        mFileProjectionMap.put(ProviderTableMeta.FILE_CONTENT_TYPE, ProviderTableMeta.FILE_CONTENT_TYPE);
        mFileProjectionMap.put(ProviderTableMeta.FILE_STORAGE_PATH, ProviderTableMeta.FILE_STORAGE_PATH);
        mFileProjectionMap.put(ProviderTableMeta.FILE_PATH, ProviderTableMeta.FILE_PATH);
        mFileProjectionMap.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, ProviderTableMeta.FILE_ACCOUNT_OWNER);
        mFileProjectionMap.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, ProviderTableMeta.FILE_LAST_SYNC_DATE);
        mFileProjectionMap.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA,
                ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA);
        mFileProjectionMap.put(ProviderTableMeta.FILE_KEEP_IN_SYNC, ProviderTableMeta.FILE_KEEP_IN_SYNC);
        mFileProjectionMap.put(ProviderTableMeta.FILE_ETAG, ProviderTableMeta.FILE_ETAG);
        mFileProjectionMap.put(ProviderTableMeta.FILE_TREE_ETAG, ProviderTableMeta.FILE_TREE_ETAG);
        mFileProjectionMap.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, ProviderTableMeta.FILE_SHARED_VIA_LINK);
        mFileProjectionMap.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, ProviderTableMeta.FILE_SHARED_WITH_SHAREE);
        mFileProjectionMap.put(ProviderTableMeta.FILE_PERMISSIONS, ProviderTableMeta.FILE_PERMISSIONS);
        mFileProjectionMap.put(ProviderTableMeta.FILE_REMOTE_ID, ProviderTableMeta.FILE_REMOTE_ID);
        mFileProjectionMap.put(ProviderTableMeta.FILE_UPDATE_THUMBNAIL, ProviderTableMeta.FILE_UPDATE_THUMBNAIL);
        mFileProjectionMap.put(ProviderTableMeta.FILE_IS_DOWNLOADING, ProviderTableMeta.FILE_IS_DOWNLOADING);
        mFileProjectionMap.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, ProviderTableMeta.FILE_ETAG_IN_CONFLICT);
        mFileProjectionMap.put(ProviderTableMeta.FILE_PRIVATE_LINK, ProviderTableMeta.FILE_PRIVATE_LINK);
    }

    private static HashMap<String, String> mShareProjectionMap = new HashMap<>();

    static {

        mShareProjectionMap.put(ProviderTableMeta._ID, ProviderTableMeta._ID);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_FILE_SOURCE, ProviderTableMeta.OCSHARES_FILE_SOURCE);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_ITEM_SOURCE, ProviderTableMeta.OCSHARES_ITEM_SOURCE);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_SHARE_TYPE, ProviderTableMeta.OCSHARES_SHARE_TYPE);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_SHARE_WITH, ProviderTableMeta.OCSHARES_SHARE_WITH);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_PATH, ProviderTableMeta.OCSHARES_PATH);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_PERMISSIONS, ProviderTableMeta.OCSHARES_PERMISSIONS);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_SHARED_DATE, ProviderTableMeta.OCSHARES_SHARED_DATE);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_EXPIRATION_DATE, ProviderTableMeta.OCSHARES_EXPIRATION_DATE);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_TOKEN, ProviderTableMeta.OCSHARES_TOKEN);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME,
                ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_IS_DIRECTORY,
                ProviderTableMeta.OCSHARES_IS_DIRECTORY);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_USER_ID, ProviderTableMeta.OCSHARES_USER_ID);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, ProviderTableMeta.OCSHARES_ACCOUNT_OWNER);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_NAME, ProviderTableMeta.OCSHARES_NAME);
        mShareProjectionMap.put(ProviderTableMeta.OCSHARES_URL, ProviderTableMeta.OCSHARES_URL);
    }

    private static HashMap<String, String> mCapabilityProjectionMap = new HashMap<>();

    static {

        mCapabilityProjectionMap.put(ProviderTableMeta._ID, ProviderTableMeta._ID);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME,
                ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_VERSION_MAYOR,
                ProviderTableMeta.CAPABILITIES_VERSION_MAYOR);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_VERSION_MINOR,
                ProviderTableMeta.CAPABILITIES_VERSION_MINOR);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_VERSION_MICRO,
                ProviderTableMeta.CAPABILITIES_VERSION_MICRO);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_VERSION_STRING,
                ProviderTableMeta.CAPABILITIES_VERSION_STRING);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_VERSION_EDITION,
                ProviderTableMeta.CAPABILITIES_VERSION_EDITION);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL,
                ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED,
                ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED,
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED,
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED,
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS,
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED,
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL,
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD,
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE,
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY,
                ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL,
                ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_RESHARING,
                ProviderTableMeta.CAPABILITIES_SHARING_RESHARING);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING,
                ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING,
                ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING,
                ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_FILES_UNDELETE,
                ProviderTableMeta.CAPABILITIES_FILES_UNDELETE);
        mCapabilityProjectionMap.put(ProviderTableMeta.CAPABILITIES_FILES_VERSIONING,
                ProviderTableMeta.CAPABILITIES_FILES_VERSIONING);
    }

    private static HashMap<String, String> mUploadProjectionMap = new HashMap<>();

    static {

        mUploadProjectionMap.put(ProviderTableMeta._ID, ProviderTableMeta._ID);
        mUploadProjectionMap.put(ProviderTableMeta.UPLOADS_LOCAL_PATH, ProviderTableMeta.UPLOADS_LOCAL_PATH);
        mUploadProjectionMap.put(ProviderTableMeta.UPLOADS_REMOTE_PATH, ProviderTableMeta.UPLOADS_REMOTE_PATH);
        mUploadProjectionMap.put(ProviderTableMeta.UPLOADS_ACCOUNT_NAME, ProviderTableMeta.UPLOADS_ACCOUNT_NAME);
        mUploadProjectionMap.put(ProviderTableMeta.UPLOADS_FILE_SIZE, ProviderTableMeta.UPLOADS_FILE_SIZE);
        mUploadProjectionMap.put(ProviderTableMeta.UPLOADS_STATUS, ProviderTableMeta.UPLOADS_STATUS);
        mUploadProjectionMap.put(ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR,
                ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR);
        mUploadProjectionMap.put(ProviderTableMeta.UPLOADS_UPLOAD_TIME, ProviderTableMeta.UPLOADS_UPLOAD_TIME);
        mUploadProjectionMap.put(ProviderTableMeta.UPLOADS_FORCE_OVERWRITE, ProviderTableMeta.UPLOADS_FORCE_OVERWRITE);
        mUploadProjectionMap.put(ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER,
                ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER);
        mUploadProjectionMap.put(ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP,
                ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP);
        mUploadProjectionMap.put(ProviderTableMeta.UPLOADS_LAST_RESULT, ProviderTableMeta.UPLOADS_LAST_RESULT);
        mUploadProjectionMap.put(ProviderTableMeta.UPLOADS_CREATED_BY, ProviderTableMeta.UPLOADS_CREATED_BY);
        mUploadProjectionMap.put(ProviderTableMeta.UPLOADS_TRANSFER_ID,
                ProviderTableMeta.UPLOADS_TRANSFER_ID);
    }

    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        //Log_OC.d(TAG, "Deleting " + uri + " at provider " + this);
        int count = 0;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            count = delete(db, uri, where, whereArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private int delete(SQLiteDatabase db, Uri uri, String where, String[] whereArgs) {
        int count = 0;
        switch (mUriMatcher.match(uri)) {
            case SINGLE_FILE:
                Cursor c = query(db, uri, null, where, whereArgs, null);
                String remoteId = "";
                if (c != null && c.moveToFirst()) {
                    remoteId = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_REMOTE_ID));
                    //ThumbnailsCacheManager.removeFileFromCache(remoteId);
                    c.close();
                }
                Log_OC.d(TAG, "Removing FILE " + remoteId);

                count = db.delete(ProviderTableMeta.FILE_TABLE_NAME,
                        ProviderTableMeta._ID
                                + "="
                                + uri.getPathSegments().get(1)
                                + (!TextUtils.isEmpty(where) ? " AND (" + where
                                + ")" : ""), whereArgs);
                break;
            case DIRECTORY:
                // deletion of folder is recursive

                Cursor children = query(uri, null, null, null, null);
                if (children != null && children.moveToFirst()) {
                    long childId;
                    boolean isDir;
                    while (!children.isAfterLast()) {
                        childId = children.getLong(children.getColumnIndex(ProviderTableMeta._ID));
                        isDir = "DIR".equals(children.getString(
                                children.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE)
                        ));
                        if (isDir) {
                            count += delete(
                                    db,
                                    ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_DIR, childId),
                                    null,
                                    null
                            );
                        } else {
                            count += delete(
                                    db,
                                    ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE, childId),
                                    null,
                                    null
                            );
                        }
                        children.moveToNext();
                    }
                    children.close();
                }
                count += db.delete(ProviderTableMeta.FILE_TABLE_NAME,
                        ProviderTableMeta._ID
                                + "="
                                + uri.getPathSegments().get(1)
                                + (!TextUtils.isEmpty(where) ? " AND (" + where
                                + ")" : ""), whereArgs);

                break;
            case ROOT_DIRECTORY:
                //Log_OC.d(TAG, "Removing ROOT!");
                count = db.delete(ProviderTableMeta.FILE_TABLE_NAME, where, whereArgs);
                break;
            case SHARES:
                count = db.delete(ProviderTableMeta.OCSHARES_TABLE_NAME, where, whereArgs);
                break;
            case CAPABILITIES:
                count = db.delete(ProviderTableMeta.CAPABILITIES_TABLE_NAME, where, whereArgs);
                break;
            case UPLOADS:
                count = db.delete(ProviderTableMeta.UPLOADS_TABLE_NAME, where, whereArgs);
                break;
            case CAMERA_UPLOADS_SYNC:
                count = db.delete(ProviderTableMeta.CAMERA_UPLOADS_SYNC_TABLE_NAME, where, whereArgs);
                break;
            case QUOTAS:
                count = db.delete(ProviderTableMeta.USER_QUOTAS_TABLE_NAME, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown uri: " + uri.toString());
        }
        return count;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY:
                return ProviderTableMeta.CONTENT_TYPE;
            case SINGLE_FILE:
                return ProviderTableMeta.CONTENT_TYPE_ITEM;
            default:
                throw new IllegalArgumentException("Unknown Uri id."
                        + uri.toString());
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Uri newUri = null;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            newUri = insert(db, uri, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(newUri, null);
        return newUri;
    }

    private Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY:
            case SINGLE_FILE:
                String remotePath = values.getAsString(ProviderTableMeta.FILE_PATH);
                String accountName = values.getAsString(ProviderTableMeta.FILE_ACCOUNT_OWNER);
                String[] projection = new String[]{
                        ProviderTableMeta._ID, ProviderTableMeta.FILE_PATH,
                        ProviderTableMeta.FILE_ACCOUNT_OWNER
                };
                String where = ProviderTableMeta.FILE_PATH + "=? AND " +
                        ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?";
                String[] whereArgs = new String[]{remotePath, accountName};
                Cursor doubleCheck = query(db, uri, projection, where, whereArgs, null);
                // ugly patch; serious refactorization is needed to reduce work in
                // FileDataStorageManager and bring it to FileContentProvider
                if (doubleCheck == null || !doubleCheck.moveToFirst()) {
                    if (doubleCheck != null) {
                        doubleCheck.close();
                    }
                    long rowId = db.insert(ProviderTableMeta.FILE_TABLE_NAME, null, values);
                    if (rowId > 0) {
                        return ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE, rowId);
                    } else {
                        throw new SQLException("ERROR " + uri);
                    }
                } else {
                    // file is already inserted; race condition, let's avoid a duplicated entry
                    Uri insertedFileUri = ContentUris.withAppendedId(
                            ProviderTableMeta.CONTENT_URI_FILE,
                            doubleCheck.getLong(doubleCheck.getColumnIndex(ProviderTableMeta._ID))
                    );
                    doubleCheck.close();

                    return insertedFileUri;
                }

            case SHARES:
                Uri insertedShareUri;
                long rowId = db.insert(ProviderTableMeta.OCSHARES_TABLE_NAME, null, values);
                if (rowId > 0) {
                    insertedShareUri =
                            ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_SHARE, rowId);
                } else {
                    throw new SQLException("ERROR " + uri);

                }
                updateFilesTableAccordingToShareInsertion(db, values);
                return insertedShareUri;

            case CAPABILITIES:
                Uri insertedCapUri;
                long id = db.insert(ProviderTableMeta.CAPABILITIES_TABLE_NAME, null, values);
                if (id > 0) {
                    insertedCapUri =
                            ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_CAPABILITIES, id);
                } else {
                    throw new SQLException("ERROR " + uri);

                }
                return insertedCapUri;

            case UPLOADS:
                Uri insertedUploadUri;
                long uploadId = db.insert(ProviderTableMeta.UPLOADS_TABLE_NAME, null, values);
                if (uploadId > 0) {
                    insertedUploadUri =
                            ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_UPLOADS, uploadId);
                    trimSuccessfulUploads(db);
                } else {
                    throw new SQLException("ERROR " + uri);

                }
                return insertedUploadUri;

            case CAMERA_UPLOADS_SYNC:
                Uri insertedCameraUploadUri;
                long cameraUploadId = db.insert(ProviderTableMeta.CAMERA_UPLOADS_SYNC_TABLE_NAME, null,
                        values);
                if (cameraUploadId > 0) {
                    insertedCameraUploadUri =
                            ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_CAMERA_UPLOADS_SYNC,
                                    cameraUploadId);
                } else {
                    throw new SQLException("ERROR " + uri);
                }
                return insertedCameraUploadUri;
            case QUOTAS:
                Uri insertedQuotaUri;
                long quotaId = db.insert(ProviderTableMeta.USER_QUOTAS_TABLE_NAME, null,
                        values);
                if (quotaId > 0) {
                    insertedQuotaUri =
                            ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_QUOTAS,
                                    quotaId);
                } else {
                    throw new SQLException("ERROR " + uri);
                }
                return insertedQuotaUri;
            default:
                throw new IllegalArgumentException("Unknown uri id: " + uri);
        }

    }

    private void updateFilesTableAccordingToShareInsertion(
            SQLiteDatabase db, ContentValues newShare
    ) {
        ContentValues fileValues = new ContentValues();
        int newShareType = newShare.getAsInteger(ProviderTableMeta.OCSHARES_SHARE_TYPE);
        if (newShareType == ShareType.PUBLIC_LINK.getValue()) {
            fileValues.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, 1);
        } else if (
                newShareType == ShareType.USER.getValue() ||
                        newShareType == ShareType.GROUP.getValue() ||
                        newShareType == ShareType.FEDERATED.getValue()) {
            fileValues.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, 1);
        }

        String where = ProviderTableMeta.FILE_PATH + "=? AND " +
                ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?";
        String[] whereArgs = new String[]{
                newShare.getAsString(ProviderTableMeta.OCSHARES_PATH),
                newShare.getAsString(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER)
        };
        db.update(ProviderTableMeta.FILE_TABLE_NAME, fileValues, where, whereArgs);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new DataBaseHelper(getContext());

        String authority = getContext().getResources().getString(R.string.authority);
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(authority, null, ROOT_DIRECTORY);
        mUriMatcher.addURI(authority, "file/", SINGLE_FILE);
        mUriMatcher.addURI(authority, "file/#", SINGLE_FILE);
        mUriMatcher.addURI(authority, "dir/", DIRECTORY);
        mUriMatcher.addURI(authority, "dir/#", DIRECTORY);
        mUriMatcher.addURI(authority, "shares/", SHARES);
        mUriMatcher.addURI(authority, "shares/#", SHARES);
        mUriMatcher.addURI(authority, "capabilities/", CAPABILITIES);
        mUriMatcher.addURI(authority, "capabilities/#", CAPABILITIES);
        mUriMatcher.addURI(authority, "uploads/", UPLOADS);
        mUriMatcher.addURI(authority, "uploads/#", UPLOADS);
        mUriMatcher.addURI(authority, "cameraUploadsSync/", CAMERA_UPLOADS_SYNC);
        mUriMatcher.addURI(authority, "cameraUploadsSync/#", CAMERA_UPLOADS_SYNC);
        mUriMatcher.addURI(authority, "quotas/", QUOTAS);
        mUriMatcher.addURI(authority, "quotas/#", QUOTAS);

        return true;
    }


    @Override
    public Cursor query(
            @NonNull Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {

        Cursor result = null;
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            result = query(db, uri, projection, selection, selectionArgs, sortOrder);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return result;
    }

    private Cursor query(
            SQLiteDatabase db,
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {

        if (selection != null && selectionArgs == null) {
            throw new IllegalArgumentException("Selection not allowed, use parameterized queries");
        }

        SQLiteQueryBuilder sqlQuery = new SQLiteQueryBuilder();

        sqlQuery.setTables(ProviderTableMeta.FILE_TABLE_NAME);

        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY:
                sqlQuery.setProjectionMap(mFileProjectionMap);
                break;
            case DIRECTORY:
                String folderId = uri.getPathSegments().get(1);
                sqlQuery.appendWhere(ProviderTableMeta.FILE_PARENT + "="
                        + folderId);
                sqlQuery.setProjectionMap(mFileProjectionMap);
                break;
            case SINGLE_FILE:
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhere(ProviderTableMeta._ID + "="
                            + uri.getPathSegments().get(1));
                }
                sqlQuery.setProjectionMap(mFileProjectionMap);
                break;
            case SHARES:
                sqlQuery.setTables(ProviderTableMeta.OCSHARES_TABLE_NAME);
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhere(ProviderTableMeta._ID + "="
                            + uri.getPathSegments().get(1));
                }
                sqlQuery.setProjectionMap(mShareProjectionMap);
                break;
            case CAPABILITIES:
                sqlQuery.setTables(ProviderTableMeta.CAPABILITIES_TABLE_NAME);
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhereEscapeString(ProviderTableMeta._ID + "="
                            + uri.getPathSegments().get(1));
                }
                sqlQuery.setProjectionMap(mCapabilityProjectionMap);
                break;
            case UPLOADS:
                sqlQuery.setTables(ProviderTableMeta.UPLOADS_TABLE_NAME);
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhere(ProviderTableMeta._ID + "="
                            + uri.getPathSegments().get(1));
                }
                sqlQuery.setProjectionMap(mUploadProjectionMap);
                break;
            case CAMERA_UPLOADS_SYNC:
                sqlQuery.setTables(ProviderTableMeta.CAMERA_UPLOADS_SYNC_TABLE_NAME);
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhere(ProviderTableMeta._ID + "="
                            + uri.getPathSegments().get(1));
                }
                break;
            case QUOTAS:
                sqlQuery.setTables(ProviderTableMeta.USER_QUOTAS_TABLE_NAME);
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhere(ProviderTableMeta._ID + "="
                            + uri.getPathSegments().get(1));
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown uri id: " + uri);
        }

        String order;
        if (TextUtils.isEmpty(sortOrder)) {
            switch (mUriMatcher.match(uri)) {
                case SHARES:
                    order = ProviderTableMeta.OCSHARES_DEFAULT_SORT_ORDER;
                    break;
                case CAPABILITIES:
                    order = ProviderTableMeta.CAPABILITIES_DEFAULT_SORT_ORDER;
                    break;
                case UPLOADS:
                    order = ProviderTableMeta.UPLOADS_DEFAULT_SORT_ORDER;
                    break;
                case CAMERA_UPLOADS_SYNC:
                    order = ProviderTableMeta.CAMERA_UPLOADS_SYNC_DEFAULT_SORT_ORDER;
                    break;
                default: // Files
                    order = ProviderTableMeta.FILE_DEFAULT_SORT_ORDER;
                    break;
            }
        } else {
            order = sortOrder;
        }

        // DB case_sensitive
        db.execSQL("PRAGMA case_sensitive_like = true");
        Cursor c = sqlQuery.query(db, projection, selection, selectionArgs, null, null, order);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int count = 0;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            count = update(db, uri, values, selection, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private int update(
            SQLiteDatabase db,
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs
    ) {
        switch (mUriMatcher.match(uri)) {
            case DIRECTORY:
                return 0; //updateFolderSize(db, selectionArgs[0]);
            case SHARES:
                return db.update(
                        ProviderTableMeta.OCSHARES_TABLE_NAME, values, selection, selectionArgs
                );
            case CAPABILITIES:
                return db.update(
                        ProviderTableMeta.CAPABILITIES_TABLE_NAME, values, selection, selectionArgs
                );
            case UPLOADS:
                int ret = db.update(
                        ProviderTableMeta.UPLOADS_TABLE_NAME, values, selection, selectionArgs
                );
                trimSuccessfulUploads(db);
                return ret;
            case CAMERA_UPLOADS_SYNC:
                return db.update(
                        ProviderTableMeta.CAMERA_UPLOADS_SYNC_TABLE_NAME, values, selection,
                        selectionArgs);
            case QUOTAS:
                return db.update(ProviderTableMeta.USER_QUOTAS_TABLE_NAME, values, selection, selectionArgs);
            default:
                return db.update(
                        ProviderTableMeta.FILE_TABLE_NAME, values, selection, selectionArgs
                );
        }
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        Log_OC.d("FileContentProvider", "applying batch in provider " + this +
                " (temporary: " + isTemporary() + ")");
        ContentProviderResult[] results = new ContentProviderResult[operations.size()];
        int i = 0;

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();  // it's supposed that transactions can be nested
        try {
            for (ContentProviderOperation operation : operations) {
                results[i] = operation.apply(this, results, i);
                i++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        Log_OC.d("FileContentProvider", "applied batch in provider " + this);
        return results;
    }


    private class DataBaseHelper extends SQLiteOpenHelper {

        DataBaseHelper(Context context) {
            super(context, ProviderMeta.DB_NAME, null, ProviderMeta.DB_VERSION);

        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // files table
            Log_OC.i("SQL", "Entering in onCreate");
            createFilesTable(db);

            // Create ocshares table
            createOCSharesTable(db);

            // Create capabilities table
            createCapabilitiesTable(db);

            // Create uploads table
            createUploadsTable(db);

            // Create user avatar table
            createUserAvatarsTable(db);

            // Create user quota table
            createUserQuotaTable(db);

            // Create camera upload sync table
            createCameraUploadsSyncTable(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log_OC.i("SQL", "Entering in onUpgrade");
            boolean upgraded = false;
            if (oldVersion == 1 && newVersion >= 2) {
                Log_OC.i("SQL", "Entering in the #1 ADD in onUpgrade");
                db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                        " ADD COLUMN " + ProviderTableMeta.FILE_KEEP_IN_SYNC + " INTEGER " +
                        " DEFAULT 0");
                upgraded = true;
            }
            if (oldVersion < 3 && newVersion >= 3) {
                Log_OC.i("SQL", "Entering in the #2 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA +
                            " INTEGER " + " DEFAULT 0");

                    // assume there are not local changes pending to upload
                    db.execSQL("UPDATE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " SET " + ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA + " = "
                            + System.currentTimeMillis() +
                            " WHERE " + ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }
            if (oldVersion < 4 && newVersion >= 4) {
                Log_OC.i("SQL", "Entering in the #3 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA +
                            " INTEGER " + " DEFAULT 0");

                    db.execSQL("UPDATE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " SET " + ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA + " = " +
                            ProviderTableMeta.FILE_MODIFIED +
                            " WHERE " + ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 5 && newVersion >= 5) {
                Log_OC.i("SQL", "Entering in the #4 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_ETAG + " TEXT " +
                            " DEFAULT NULL");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 6 && newVersion >= 6) {
                Log_OC.i("SQL", "Entering in the #5 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_SHARED_VIA_LINK + " INTEGER " +
                            " DEFAULT 0");

                    db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_PUBLIC_LINK + " TEXT " +
                            " DEFAULT NULL");

                    // Create table ocshares
                    createOCSharesTable(db);
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 7 && newVersion >= 7) {
                Log_OC.i("SQL", "Entering in the #7 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_PERMISSIONS + " TEXT " +
                            " DEFAULT NULL");

                    db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_REMOTE_ID + " TEXT " +
                            " DEFAULT NULL");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 8 && newVersion >= 8) {
                Log_OC.i("SQL", "Entering in the #8 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_UPDATE_THUMBNAIL + " INTEGER " +
                            " DEFAULT 0");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 9 && newVersion >= 9) {
                Log_OC.i("SQL", "Entering in the #9 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_IS_DOWNLOADING + " INTEGER " +
                            " DEFAULT 0");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 10 && newVersion >= 10) {
                Log_OC.i("SQL", "Entering in the #10 ADD in onUpgrade");
                updateAccountName(db);
                upgraded = true;
            }

            if (oldVersion < 11 && newVersion >= 11) {
                Log_OC.i("SQL", "Entering in the #11 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_ETAG_IN_CONFLICT + " TEXT " +
                            " DEFAULT NULL");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 12 && newVersion >= 12) {
                Log_OC.i("SQL", "Entering in the #12 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_SHARED_WITH_SHAREE + " INTEGER " +
                            " DEFAULT 0");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 13 && newVersion >= 13) {
                Log_OC.i("SQL", "Entering in the #13 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    // Create capabilities table
                    createCapabilitiesTable(db);
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 14 && newVersion >= 14) {
                Log_OC.i("SQL", "Entering in the #14 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    // drop old instant_upload table
                    db.execSQL("DROP TABLE IF EXISTS " + "instant_upload" + ";");
                    // Create uploads table
                    createUploadsTable(db);
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 15 && newVersion >= 15) {
                Log_OC.i("SQL", "Entering in the #15 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    // Create user profiles table
                    createUserAvatarsTable(db);
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 16 && newVersion >= 16) {
                Log_OC.i("SQL", "Entering in the #16 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_TREE_ETAG + " TEXT " +
                            " DEFAULT NULL");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }
            if (oldVersion < 17 && newVersion >= 17) {
                Log_OC.i("SQL", "Entering in the #17 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.OCSHARES_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.OCSHARES_NAME + " TEXT " +
                            " DEFAULT NULL");
                    db.setTransactionSuccessful();
                    upgraded = true;
                    // SQLite does not allow to drop a columns; ftm, we'll not recreate
                    // the files table without the column FILE_PUBLIC_LINK, just forget about
                } finally {
                    db.endTransaction();
                }
            }
            if (oldVersion < 18 && newVersion >= 18) {
                Log_OC.i("SQL", "Entering in the #18 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.OCSHARES_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.OCSHARES_URL + " TEXT " +
                            " DEFAULT NULL");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }
            if (oldVersion < 19 && newVersion >= 19) {

                Log_OC.i("SQL", "Entering in the #19 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE
                            + " INTEGER " + " DEFAULT -1");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }
            if (oldVersion < 20 && newVersion >= 20) {

                Log_OC.i("SQL", "Entering in the #20 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.
                            CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY + " INTEGER " +
                            " DEFAULT -1");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 21 && newVersion >= 21) {
                Log_OC.i("SQL", "Entering in the #21 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.FILE_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.FILE_PRIVATE_LINK + " TEXT " +
                            " DEFAULT NULL");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 22 && newVersion >= 22) {
                Log_OC.i("SQL", "Entering in the #22 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    createCameraUploadsSyncTable(db);
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 23 && newVersion >= 23) {
                Log_OC.i("SQL", "Entering in the #23 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    createUserQuotaTable(db);
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 24 && newVersion >= 24) {
                Log_OC.i("SQL", "Entering in the #24 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.UPLOADS_TABLE_NAME +
                            " ADD COLUMN " + ProviderTableMeta.UPLOADS_TRANSFER_ID + " TEXT " +
                            " DEFAULT NULL");
                    db.setTransactionSuccessful();
                    upgraded = true;
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i("SQL", "OUT of the ADD in onUpgrade; oldVersion == " + oldVersion +
                        ", newVersion == " + newVersion);
            }
        }
    }

    private void createFilesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.FILE_TABLE_NAME + "("
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
        );
    }

    private void createOCSharesTable(SQLiteDatabase db) {
        // Create ocshares table
        db.execSQL("CREATE TABLE " + ProviderTableMeta.OCSHARES_TABLE_NAME + "("
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
                + ProviderTableMeta.OCSHARES_NAME + " TEXT );");
    }

    private void createCapabilitiesTable(SQLiteDatabase db) {
        // Create capabilities table
        db.execSQL("CREATE TABLE " + ProviderTableMeta.CAPABILITIES_TABLE_NAME + "("
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
                + ProviderTableMeta.CAPABILITIES_FILES_VERSIONING + " INTEGER );");   // boolean
    }

    private void createUploadsTable(SQLiteDatabase db) {
        // Create uploads table
        db.execSQL("CREATE TABLE " + ProviderTableMeta.UPLOADS_TABLE_NAME + "("
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
        );
    }

    private void createUserAvatarsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.USER_AVATARS__TABLE_NAME + "("
                + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                + ProviderTableMeta.USER_AVATARS__ACCOUNT_NAME + " TEXT, "
                + ProviderTableMeta.USER_AVATARS__CACHE_KEY + " TEXT, "
                + ProviderTableMeta.USER_AVATARS__MIME_TYPE + " TEXT, "
                + ProviderTableMeta.USER_AVATARS__ETAG + " TEXT );"
        );
    }

    private void createUserQuotaTable (SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.USER_QUOTAS_TABLE_NAME + "("
                + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                + ProviderTableMeta.USER_QUOTAS__ACCOUNT_NAME + " TEXT, "
                + ProviderTableMeta.USER_QUOTAS__FREE + " LONG, "
                + ProviderTableMeta.USER_QUOTAS__RELATIVE + " LONG, "
                + ProviderTableMeta.USER_QUOTAS__TOTAL + " LONG, "
                + ProviderTableMeta.USER_QUOTAS__USED + " LONG );"
        );
    }

    private void createCameraUploadsSyncTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.CAMERA_UPLOADS_SYNC_TABLE_NAME + "("
                + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                + ProviderTableMeta.PICTURES_LAST_SYNC_TIMESTAMP + " INTEGER,"
                + ProviderTableMeta.VIDEOS_LAST_SYNC_TIMESTAMP + " INTEGER);"
        );
    }

    /**
     * Version 10 of database does not modify its scheme. It coincides with the upgrade of the ownCloud account names
     * structure to include in it the path to the server instance. Updating the account names and path to local files
     * in the files table is a must to keep the existing account working and the database clean.
     *
     * See {@link com.owncloud.android.authentication.AccountUtils#updateAccountVersion(android.content.Context)}
     *
     * @param db Database where table of files is included.
     */
    private void updateAccountName(SQLiteDatabase db) {
        Log_OC.d("SQL", "THREAD:  " + Thread.currentThread().getName());
        AccountManager ama = AccountManager.get(getContext());
        try {
            // get accounts from AccountManager ;  we can't be sure if accounts in it are updated or not although
            // we know the update was previously done in {link @FileActivity#onCreate} because the changes through
            // AccountManager are not synchronous
            Account[] accounts = AccountManager.get(getContext()).getAccountsByType(
                    MainApp.getAccountType());
            String serverUrl, username, oldAccountName, newAccountName;
            for (Account account : accounts) {
                // build both old and new account name
                serverUrl = ama.getUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL);
                username = AccountUtils.getUsernameForAccount(account);
                oldAccountName = AccountUtils.buildAccountNameOld(Uri.parse(serverUrl), username);
                newAccountName = AccountUtils.buildAccountName(Uri.parse(serverUrl), username);

                // update values in database
                db.beginTransaction();
                try {
                    ContentValues cv = new ContentValues();
                    cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, newAccountName);
                    int num = db.update(ProviderTableMeta.FILE_TABLE_NAME,
                            cv,
                            ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                            new String[]{oldAccountName});

                    Log_OC.d("SQL", "Updated account in database: old name == " + oldAccountName +
                            ", new name == " + newAccountName + " (" + num + " rows updated )");

                    // update path for downloaded files
                    updateDownloadedFiles(db, newAccountName, oldAccountName);

                    db.setTransactionSuccessful();

                } catch (SQLException e) {
                    Log_OC.e(TAG, "SQL Exception upgrading account names or paths in database", e);
                } finally {
                    db.endTransaction();
                }
            }
        } catch (Exception e) {
            Log_OC.e(TAG, "Exception upgrading account names or paths in database", e);
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
    private void updateDownloadedFiles(SQLiteDatabase db, String newAccountName,
                                       String oldAccountName) {

        String whereClause = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL";

        Cursor c = db.query(ProviderTableMeta.FILE_TABLE_NAME,
                null,
                whereClause,
                new String[]{newAccountName},
                null, null, null);

        try {
            if (c.moveToFirst()) {
                // create storage path
                String oldAccountPath = FileStorageUtils.getSavePath(oldAccountName);
                String newAccountPath = FileStorageUtils.getSavePath(newAccountName);

                // move files
                File oldAccountFolder = new File(oldAccountPath);
                File newAccountFolder = new File(newAccountPath);
                oldAccountFolder.renameTo(newAccountFolder);

                // update database
                do {
                    // Update database
                    String oldPath = c.getString(
                            c.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH));
                    OCFile file = new OCFile(
                            c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PATH)));
                    String newPath = FileStorageUtils.getDefaultSavePathFor(newAccountName, file);

                    ContentValues cv = new ContentValues();
                    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, newPath);
                    db.update(ProviderTableMeta.FILE_TABLE_NAME,
                            cv,
                            ProviderTableMeta.FILE_STORAGE_PATH + "=?",
                            new String[]{oldPath});

                    Log_OC.v("SQL", "Updated path of downloaded file: old file name == " + oldPath +
                            ", new file name == " + newPath);

                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }

    }

    /**
     * Grants that total count of successful uploads stored is not greater than MAX_SUCCESSFUL_UPLOADS.
     *
     * Removes older uploads if needed.
     */
    private void trimSuccessfulUploads(SQLiteDatabase db) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "delete from " + ProviderTableMeta.UPLOADS_TABLE_NAME +
                            " where " + ProviderTableMeta.UPLOADS_STATUS + " == "
                            + UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED.getValue() +
                            " and " + ProviderTableMeta._ID +
                            " not in (select " + ProviderTableMeta._ID +
                            " from " + ProviderTableMeta.UPLOADS_TABLE_NAME +
                            " where " + ProviderTableMeta.UPLOADS_STATUS + " == "
                            + UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED.getValue() +
                            " order by " + ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP +
                            " desc limit " + MAX_SUCCESSFUL_UPLOADS +
                            ")",
                    null
            );
            c.moveToFirst(); // do something with the cursor, or deletion doesn't happen; true story

        } catch (Exception e) {
            Log_OC.e(
                    TAG,
                    "Something wrong trimming successful uploads, database could grow more than expected",
                    e
            );

        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
}