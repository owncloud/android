/**
 * ownCloud Android client application
 *
 * @author Bartosz Przybylski
 * @author Christian Schabesberger
 * @author David González Verdugo
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

package com.owncloud.android.providers;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.providers.cursors.FileCursor;
import com.owncloud.android.providers.cursors.RootCursor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DocumentsStorageProvider extends DocumentsProvider {

    private static final String TAG = DocumentsStorageProvider.class.toString();

    /**
     * If a directory requires to sync, it will write the id of the directory into this variable.
     * After the sync function gets triggered again over the same directory, it will see that a sync got already
     * triggered, and does not need to be triggered again. This way a endless loop is prevented.
     */
    private long requestedFolderIdForSync = -1;

    private FileDataStorageManager mCurrentStorageManager = null;
    private static Map<Long, FileDataStorageManager> mRootIdToStorageManager;

    @Override
    public Cursor queryRoots(String[] projection) {
        initiateStorageMap();

        final RootCursor result = new RootCursor(projection);

        for (Account account : AccountUtils.getAccounts(getContext())) {
            result.addRoot(account, getContext());
        }

        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) {
        final long docId = Long.parseLong(documentId);
        updateCurrentStorageManagerIfNeeded(docId);

        final FileCursor result = new FileCursor(projection);
        if (result != null) {
            result.addFile(mCurrentStorageManager.getFileById(docId));
        }

        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) {
        final long folderId = Long.parseLong(parentDocumentId);
        updateCurrentStorageManagerIfNeeded(folderId);

        final FileCursor resultCursor = new FileCursor(projection);

        final OCFile browsedDir = mCurrentStorageManager.getFileById(folderId);

        // Create result cursor before syncing folder again, in order to enable faster loading
        for (OCFile file : mCurrentStorageManager.getFolderContent(browsedDir, false)) {
            resultCursor.addFile(file);
        }

        /**
         * This will start syncing the current folder. User will only see this after updating his view with a
         * pull down, or by accessing the folder again.
         */
        if (requestedFolderIdForSync != folderId) {
            // register for sync
            syncDirectoryWithServer(parentDocumentId, resultCursor);
            requestedFolderIdForSync = folderId;
            final Bundle extrasBundle = new Bundle();
            extrasBundle.putBoolean(DocumentsContract.EXTRA_LOADING, true);
            resultCursor.setMoreToSync(true);
        } else {
            requestedFolderIdForSync = -1;
        }

        return resultCursor;
    }

    private void syncDirectoryWithServer(String parentDocumentId, FileCursor cursor) {
        final long folderId = Long.parseLong(parentDocumentId);
        final RefreshFolderOperation refreshFolderOperation = new RefreshFolderOperation(
                mCurrentStorageManager.getFileById(folderId),
                false,
                false,
                mCurrentStorageManager.getAccount(),
                getContext());
        refreshFolderOperation.syncVersionAndProfileEnabled(false);

        final ContentResolver contentResolver = getContext().getContentResolver();
        Log_OC.e(TAG, parentDocumentId);

        final Uri browsedDirIdUri = DocumentsContract.buildChildDocumentsUri(
                getContext().getString(R.string.document_provider_authority),
                parentDocumentId);
        cursor.setNotificationUri(contentResolver,
                browsedDirIdUri);
        Thread thread = new Thread(() -> {
            refreshFolderOperation.execute(mCurrentStorageManager, getContext());
            contentResolver.notifyChange(browsedDirIdUri, null);
        });
        thread.start();
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal cancellationSignal)
            throws FileNotFoundException {
        final long docId = Long.parseLong(documentId);
        updateCurrentStorageManagerIfNeeded(docId);

        OCFile file = mCurrentStorageManager.getFileById(docId);

        if (!file.isDown()) {

            Intent i = new Intent(getContext(), FileDownloader.class);
            i.putExtra(FileDownloader.KEY_ACCOUNT, mCurrentStorageManager.getAccount());
            i.putExtra(FileDownloader.KEY_FILE, file);
            getContext().startService(i);

            do {
                if (!waitOrGetCancelled(cancellationSignal)) {
                    return null;
                }
                file = mCurrentStorageManager.getFileById(docId);

            } while (!file.isDown());
        }

        return ParcelFileDescriptor.open(
                new File(file.getStoragePath()), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint, CancellationSignal signal)
            throws FileNotFoundException {

        final long docId = Long.parseLong(documentId);
        updateCurrentStorageManagerIfNeeded(docId);

        OCFile file = mCurrentStorageManager.getFileById(docId);

        File realFile = new File(file.getStoragePath());

        return new AssetFileDescriptor(
                ParcelFileDescriptor.open(realFile, ParcelFileDescriptor.MODE_READ_ONLY),
                0,
                AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) {
        updateCurrentStorageManagerIfNeeded(rootId);

        OCFile root = mCurrentStorageManager.getFileByPath("/");
        FileCursor result = new FileCursor(projection);

        for (OCFile f : findFiles(root, query)) {
            result.addFile(f);
        }

        return result;
    }

    private void updateCurrentStorageManagerIfNeeded(long docId) {
        if (mRootIdToStorageManager == null) {
            initiateStorageMap();
        }
        if (mCurrentStorageManager == null ||
                (mRootIdToStorageManager.containsKey(docId) &&
                        mCurrentStorageManager != mRootIdToStorageManager.get(docId))) {
            mCurrentStorageManager = mRootIdToStorageManager.get(docId);
        }
    }

    private void updateCurrentStorageManagerIfNeeded(String rootId) {
        for (FileDataStorageManager data : mRootIdToStorageManager.values()) {
            if (data.getAccount().name.equals(rootId)) {
                mCurrentStorageManager = data;
            }
        }
    }

    private void initiateStorageMap() {
        mRootIdToStorageManager = new HashMap<>();

        ContentResolver contentResolver = getContext().getContentResolver();

        for (Account account : AccountUtils.getAccounts(getContext())) {
            final FileDataStorageManager storageManager =
                    new FileDataStorageManager(getContext(), account, contentResolver);
            final OCFile rootDir = storageManager.getFileByPath("/");
            mRootIdToStorageManager.put(rootDir.getFileId(), storageManager);
        }

    }

    private boolean waitOrGetCancelled(CancellationSignal cancellationSignal) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            return false;
        }

        return cancellationSignal == null || !cancellationSignal.isCanceled();
    }

    Vector<OCFile> findFiles(OCFile root, String query) {
        Vector<OCFile> result = new Vector<OCFile>();
        for (OCFile f : mCurrentStorageManager.getFolderContent(root, false)) {
            if (f.isFolder()) {
                result.addAll(findFiles(f, query));
            } else {
                if (f.getFileName().contains(query)) {
                    result.add(f);
                }
            }
        }
        return result;
    }
}
