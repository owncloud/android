/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * <p>
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

package com.owncloud.android.files.services;

import android.accounts.Account;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;

import androidx.documentfile.provider.DocumentFile;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCUpload;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.usecases.RetryFailedUploadsUseCase;
import com.owncloud.android.usecases.RetryUploadFromContentUriUseCase;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.utils.ConnectivityUtils;
import com.owncloud.android.utils.Extras;
import com.owncloud.android.utils.PowerUtils;
import kotlin.Unit;
import timber.log.Timber;

import java.net.SocketTimeoutException;

import static com.owncloud.android.operations.UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_PICTURE;
import static com.owncloud.android.operations.UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_VIDEO;

/*
 * Facade to start operations in transfer services without the verbosity of Android Intents.
 */

/**
 * Facade class providing methods to ease requesting commands to transfer services {@link FileUploader}.
 * <p>
 * Protects client objects from the verbosity of {@link android.content.Intent}s.
 * <p>
 */

public class TransferRequester {
    /**
     * Call to upload several new files
     */
    public void uploadNewFiles(
            Context context,
            Account account,
            String[] localPaths,
            String[] remotePaths,
            String[] mimeTypes,
            Integer behaviour,
            int createdBy
    ) {
        Intent intent = new Intent(context, FileUploader.class);

        intent.putExtra(FileUploader.KEY_ACCOUNT, account);
        intent.putExtra(FileUploader.KEY_LOCAL_FILE, localPaths);
        intent.putExtra(FileUploader.KEY_REMOTE_FILE, remotePaths);
        intent.putExtra(FileUploader.KEY_MIME_TYPE, mimeTypes);
        intent.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, behaviour);
        intent.putExtra(FileUploader.KEY_CREATED_BY, createdBy);

        if ((createdBy == CREATED_AS_CAMERA_UPLOAD_PICTURE || createdBy == CREATED_AS_CAMERA_UPLOAD_VIDEO) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Since in Android O the apps running in background are not allowed to start background services. The
            // camera uploads feature may try to do this. A way to solve this is to run the camera upload feature in
            // the foreground.
            Timber.d("Start to upload some files from foreground/background, startForeground() will be called soon");
            context.startForegroundService(intent);
        } else {
            Timber.d("Start to upload some files from foreground");
            context.startService(intent);
        }
    }

    /**
     * Call to upload a new single file
     */
    public void uploadNewFile(Context context, Account account, String localPath, String remotePath, int
            behaviour, String mimeType, int createdBy) {

        uploadNewFiles(
                context,
                account,
                new String[]{localPath},
                new String[]{remotePath},
                new String[]{mimeType},
                behaviour,
                createdBy
        );
    }

    /**
     * Call to update multiple files already uploaded
     */
    private void uploadsUpdate(Context context, Account account, OCFile[] existingFiles, Integer behaviour,
                               Boolean forceOverwrite, boolean requestedFromAvOfflineJobService) {
        Intent intent = new Intent(context, FileUploader.class);

        intent.putExtra(FileUploader.KEY_ACCOUNT, account);
        intent.putExtra(FileUploader.KEY_FILE, existingFiles);
        intent.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, behaviour);
        intent.putExtra(FileUploader.KEY_FORCE_OVERWRITE, forceOverwrite);

        // Since in Android O the apps running in background are not allowed to start background services. The
        // available offline feature may try to do this. A way to solve this is to run the available offline feature in
        // the foreground.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && requestedFromAvOfflineJobService) {
            intent.putExtra(FileUploader.KEY_IS_AVAILABLE_OFFLINE_FILE, true);
            Timber.d("Start to upload some already uploaded files from foreground/background, startForeground() will be called soon");
            context.startForegroundService(intent);
        } else {
            Timber.d("Start to upload some already uploaded files from foreground");
            context.startService(intent);
        }
    }

    /**
     * Call to update a dingle file already uploaded
     */
    public void uploadUpdate(Context context, Account account, OCFile existingFile, Integer behaviour,
                             Boolean forceOverwrite, boolean requestedFromAvOfflineJobService) {

        uploadsUpdate(context, account, new OCFile[]{existingFile}, behaviour, forceOverwrite,
                requestedFromAvOfflineJobService);
    }
}
