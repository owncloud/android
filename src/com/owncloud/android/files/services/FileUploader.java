/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.files.services;

import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import com.owncloud.android.authentication.AccountAuthenticator;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.operations.ChunkedUploadFileOperation;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.utils.ConnectivityUtils;
import com.owncloud.android.utils.InstantUploadUtils;
import com.owncloud.android.utils.OwnCloudVersion;

import eu.alefzero.webdav.OnDatatransferProgressListener;
import eu.alefzero.webdav.WebdavEntry;
import eu.alefzero.webdav.WebdavUtils;

import com.owncloud.android.network.OwnCloudClientUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountsException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.webkit.MimeTypeMap;
import android.widget.RemoteViews;

import com.owncloud.android.Log_OC;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.db.DbHandler;
import com.owncloud.android.ui.activity.FailedUploadActivity;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.InstantUploadActivity;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.utils.FileStorageUtils;

import eu.alefzero.webdav.WebdavClient;

public class FileUploader extends Service implements OnDatatransferProgressListener {

    private static final String TAG = FileUploader.class.getSimpleName();

    private static final String MY_PACKAGE = FileUploader.class.getPackage() != null ? FileUploader.class.getPackage().getName() : "com.owncloud.android.files.services";

    /// Intent actions that the service is prepared to receive
    public static final String ACTION_ADD_UPLOAD        = MY_PACKAGE + ".action.ADD_UPLOAD";
    public static final String ACTION_RESUME_UPLOADS    = MY_PACKAGE + ".action.RESUME_UPLOADS";

    /// Intent actions that the service starts
    public static final String ACTION_UPLOAD_ADDED      = MY_PACKAGE + ".action.UPLOAD_ADDED";
    public static final String ACTION_UPLOAD_FINISHED   = MY_PACKAGE + ".action.UPLOAD_FINISHED";

    /// Keys for Intent extras
    public static final String EXTRA_ACCOUNT            = MY_PACKAGE + ".extra.ACCOUNT";
    public static final String EXTRA_FILE               = MY_PACKAGE + ".extra.FILE";
    public static final String EXTRA_LOCAL_PATH         = MY_PACKAGE + ".extra.LOCAL_PATH";
    public static final String EXTRA_REMOTE_PATH        = MY_PACKAGE + ".extra.REMOTE_PATH";
    public static final String EXTRA_MIME_TYPE          = MY_PACKAGE + ".extra.MIME_TYPE";
    public static final String EXTRA_UPLOAD_TYPE        = MY_PACKAGE + ".extra.UPLOAD_TYPE";
    public static final String EXTRA_FORCE_OVERWRITE    = MY_PACKAGE + ".extra.FORCE_OVERWRITE";
    public static final String EXTRA_INSTANT_UPLOAD     = MY_PACKAGE + ".extra.INSTANT_UPLOAD";
    public static final String EXTRA_LOCAL_BEHAVIOUR    = MY_PACKAGE + ".extra.LOCAL_BEHAVIOUR";

    public static final String EXTRA_UPLOAD_RESULT      = MY_PACKAGE + ".extra.UPLOAD_RESULT";
    public static final String EXTRA_OLD_REMOTE_PATH    = MY_PACKAGE + ".extra.OLD_REMOTE_PATH";
    public static final String EXTRA_OLD_LOCAL_PATH     = MY_PACKAGE + ".extra.OLD_LOCAL_PATH";
    public static final String EXTRA_ACCOUNT_NAME       = MY_PACKAGE + ".extra.ACCOUNT_NAME";

    /// Values for extras
    public static final int LOCAL_BEHAVIOUR_COPY = 0;
    public static final int LOCAL_BEHAVIOUR_MOVE = 1;
    public static final int LOCAL_BEHAVIOUR_FORGET = 2;

    public static final int UPLOAD_TYPE_SINGLE_FILE = 0;
    public static final int UPLOAD_TYPE_MULTIPLE_FILES = 1;


    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private IBinder mBinder;
    private WebdavClient mUploadClient = null;
    private Account mLastAccount = null;
    private FileDataStorageManager mStorageManager;

    private ConcurrentMap<String, UploadFileOperation> mPendingUploads = new ConcurrentHashMap<String, UploadFileOperation>();
    private UploadFileOperation mCurrentUpload = null;

    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private int mLastPercent;
    private RemoteViews mDefaultNotificationContentView;

    private boolean mForcedClientRefresh;

    /**
     * Builds a key for mPendingUploads from the account and file to upload
     * 
     * @param account   Account where the file to upload is stored
     * @param file      File to upload
     */
    private String buildRemoteName(Account account, OCFile file) {
        return account.name + file.getRemotePath();
    }

    private String buildRemoteName(Account account, String remotePath) {
        return account.name + remotePath;
    }

    /**
     * Checks if an ownCloud server version should support chunked uploads.
     * 
     * @param version OwnCloud version instance corresponding to an ownCloud
     *            server.
     * @return 'True' if the ownCloud server with version supports chunked
     *         uploads.
     */
    private static boolean chunkedUploadIsSupported(OwnCloudVersion version) {
        return (version != null && version.compareTo(OwnCloudVersion.owncloud_v4_5) >= 0);
    }

    /**
     * Service initialization
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log_OC.i(TAG, "mPendingUploads size:" + mPendingUploads.size());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        HandlerThread thread = new HandlerThread("FileUploaderThread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mBinder = new FileUploaderBinder();
    }


    /**
     * Processes incoming commands send through {@link Context#startService(Intent)} method.
     * 
     * {@inheritDoc}
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        AbstractList<String> requestedUploads = null /*new Vector<String>()*/;

        String action = intent.getAction();
        if (action.equals(ACTION_ADD_UPLOAD)) { 
            requestedUploads = processNewUploadRequest(intent);

        } else if (action.equals(ACTION_RESUME_UPLOADS)) {
            requestedUploads = processResumeRequest(intent);

        } else {
            Log_OC.e(TAG, "Unknown action received as a command");
        }

        if (requestedUploads.size() > 0) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = requestedUploads;
            mServiceHandler.sendMessage(msg);
        }
        Log_OC.i(TAG, "mPendingUploads size:" + mPendingUploads.size());
        return Service.START_NOT_STICKY;
    }


    /**
     * Processes requests to add one or several files to the queue of uploads, creating
     * the {link UploadFileOperation}s needed to upload them.
     * 
     * @param intent        {@link Intent} containing all the parameters of the upload request.
     * @return              A list of pending uploads to send to {@link #mServiceHandler}.
     */
    private AbstractList<String> processNewUploadRequest(Intent intent) {
        AbstractList<String> requestedUploads = new Vector<String>();

        /// check parameters received in the Intent
        if (!intent.hasExtra(EXTRA_ACCOUNT) || !intent.hasExtra(EXTRA_UPLOAD_TYPE)
                || !(intent.hasExtra(EXTRA_LOCAL_PATH) || intent.hasExtra(EXTRA_FILE))) {
            Log_OC.e(TAG, "Not enough information provided in intent");
            return requestedUploads;
        }
        int uploadType = intent.getIntExtra(EXTRA_UPLOAD_TYPE, -1);
        if (uploadType == -1) {
            Log_OC.e(TAG, "Incorrect upload type provided");
            return requestedUploads;
        }

        Account account = intent.getParcelableExtra(EXTRA_ACCOUNT);
        String[] localPaths = null, remotePaths = null, mimeTypes = null;
        OCFile[] files = null;

        if (uploadType == UPLOAD_TYPE_SINGLE_FILE) {

            if (intent.hasExtra(EXTRA_FILE)) {
                files = new OCFile[] { intent.getParcelableExtra(EXTRA_FILE) };
            } else {
                localPaths = new String[] { intent.getStringExtra(EXTRA_LOCAL_PATH) };
                remotePaths = new String[] { intent.getStringExtra(EXTRA_REMOTE_PATH) };
                mimeTypes = new String[] { intent.getStringExtra(EXTRA_MIME_TYPE) };
            }

        } else { // mUploadType == UPLOAD_TYPE_MULTIPLE_FILES

            if (intent.hasExtra(EXTRA_FILE)) {
                files = (OCFile[]) intent.getParcelableArrayExtra(EXTRA_FILE);
            } else {
                localPaths = intent.getStringArrayExtra(EXTRA_LOCAL_PATH);
                remotePaths = intent.getStringArrayExtra(EXTRA_REMOTE_PATH);
                mimeTypes = intent.getStringArrayExtra(EXTRA_MIME_TYPE);
            }
        }
        FileDataStorageManager storageManager = new FileDataStorageManager(account, getContentResolver());

        boolean forceOverwrite = intent.getBooleanExtra(EXTRA_FORCE_OVERWRITE, false);
        boolean isInstant = intent.getBooleanExtra(EXTRA_INSTANT_UPLOAD, false);
        int localAction = intent.getIntExtra(EXTRA_LOCAL_BEHAVIOUR, LOCAL_BEHAVIOUR_COPY);
        boolean fixed = false;
        if (isInstant) {
            fixed = checkAndFixInstantUploadDirectory(storageManager); // MUST be done BEFORE calling obtainNewOCFileToUpload

            // same always temporally the picture to upload
            String file_path="";
            if (files!=  null)
            {
                file_path = files[0].getFileName();
            }
            else
            {
                file_path = localPaths[0];
            }
            
            DbHandler db = new DbHandler(getApplicationContext());
            db.putFileForLater(file_path, account.name, null);
            Log_OC.d(TAG,  "Instant Upload on DB: file--> " + file_path);
            db.close();

        }

        if (intent.hasExtra(EXTRA_FILE) && files == null) {
            Log_OC.e(TAG, "Incorrect array for OCFiles provided in upload intent");
            return requestedUploads;

        } else if (!intent.hasExtra(EXTRA_FILE)) {
            if (localPaths == null) {
                Log_OC.e(TAG, "Incorrect array for local paths provided in upload intent");
                return requestedUploads;
            }
            if (remotePaths == null) {
                Log_OC.e(TAG, "Incorrect array for remote paths provided in upload intent");
                return requestedUploads;
            }
            if (localPaths.length != remotePaths.length) {
                Log_OC.e(TAG, "Different number of remote paths and local paths!");
                return requestedUploads;
            }

            files = new OCFile[localPaths.length];
            for (int i = 0; i < localPaths.length; i++) {
                files[i] = obtainNewOCFileToUpload(remotePaths[i], localPaths[i], ((mimeTypes != null) ? mimeTypes[i]
                        : (String) null), storageManager);
                if (files[i] == null) {
                    // TODO @andomaex add failure Notification
                    return requestedUploads;
                }

                // Insert OCFile on DB
                storageManager.saveFile(files[i]);
            }
        }

        // Get Uploading files - ONLY FROM THE SAME ACCOUNT AS THE UPLOAD REQUESTED ?
        OwnCloudVersion ocv = new OwnCloudVersion(AccountManager.get(this).getUserData(account,
                AccountAuthenticator.KEY_OC_VERSION));
        boolean chunked = FileUploader.chunkedUploadIsSupported(ocv);
        String uploadKey = null;
        UploadFileOperation newUpload = null;

        if (files != null){
            for (int i = 0; i < files.length; i++) {
                uploadKey = buildRemoteName(account, files[i].getRemotePath());
                if (chunked && files[i].getFileLength() > ChunkedUploadFileOperation.CHUNK_SIZE) {
                    newUpload = new ChunkedUploadFileOperation(account, files[i], isInstant, forceOverwrite,
                            localAction);
                } else {
                    newUpload = new UploadFileOperation(account, files[i], isInstant, forceOverwrite, localAction);
                }
                if (fixed && i == 0) {
                    newUpload.setRemoteFolderToBeCreated();
                }

                // Check connectivity conditions
                // isOnline and noInstant 
                // isInstant and conditions for instant uploads are OK
                if(ConnectivityUtils.isOnline(getApplicationContext()) &&
                        (!isInstant ||
                                (!InstantUploadUtils.instantUploadViaWiFiOnly(getApplicationContext()) 
                                        || (InstantUploadUtils.instantUploadViaWiFiOnly(getApplicationContext()) 
                                                == ConnectivityUtils.isConnectedViaWiFi(getApplicationContext()) == true)))) {

                    mPendingUploads.putIfAbsent(uploadKey, newUpload);
                    newUpload.addDatatransferProgressListener(this);
                    newUpload.addDatatransferProgressListener((FileUploaderBinder)mBinder);

                    requestedUploads.add(uploadKey);
                }

                // Update uploading field of the OCFile on Database
                storageManager.updateUploading(files[i].getRemotePath(), true);
                sendBroadcastNewUploader(newUpload);
                Log_OC.d(TAG, "Upload field is TRUE for file " + files[i].getRemotePath());                

            }
        }

        return requestedUploads;
    }


    /**
     * Retrieves all the files waiting for upload in the local database and returns the 
     * {link {@link UploadFileOperation}s needed to uploads them.
     * 
     * @param intent        {@link Intent} containing all the parameters of the upload request.
     * @return              A list of pending uploads to send to {@link #mServiceHandler}.
     */
    private Vector<String> processResumeRequest(Intent intent)
    {
        Account[] accounts = AccountUtils.getOwncloudAccounts(getApplicationContext());
        Log_OC.d(TAG, "offlineUpload - Owncloud Accounts number=" + accounts.length);

        mForcedClientRefresh = true;
        
        FileDataStorageManager storageManager;
        Vector<OCFile> uploadingFiles;
        OwnCloudVersion ocv;
        boolean chunked;
        String uploadKey = null;
        UploadFileOperation newUpload = null;

        boolean forceOverwrite = false; //intent.getBooleanExtra(EXTRA_FORCE_OVERWRITE, false);
        int localAction = LOCAL_BEHAVIOUR_COPY; //intent.getIntExtra(EXTRA_LOCAL_BEHAVIOUR, LOCAL_BEHAVIOUR_COPY);
        boolean fixed = false;

        Vector<String> requestedUploads = new Vector<String>();

        ConcurrentMap<String, UploadFileOperation> mInstantUploads = new ConcurrentHashMap<String, UploadFileOperation>();

        // Add Pending InstantUploads
        Log_OC.d(TAG, "offlineUpload - Adding Pending Instant Uploads: " );
        
        // Instant Uploads from DB
        DbHandler db = new DbHandler(getApplicationContext());
        Cursor c = db.getAwaitingFiles();
        if (c.moveToFirst()) {
            do {
                String account_name = c.getString(c.getColumnIndex("account"));
                String file_path = c.getString(c.getColumnIndex("path"));
                File f = new File(file_path);          

                if (f.exists()) {
                    String remote_path = FileStorageUtils.getInstantUploadFilePath(getApplicationContext(), f.getName());
                    Account account = new Account(account_name, AccountAuthenticator.ACCOUNT_TYPE);

                    storageManager= new FileDataStorageManager(account, getContentResolver());
                    fixed = checkAndFixInstantUploadDirectory(storageManager);     

                    ocv = new OwnCloudVersion(AccountManager.get(this).getUserData(account, AccountAuthenticator.KEY_OC_VERSION));
                    chunked = FileUploader.chunkedUploadIsSupported(ocv);
                    uploadKey = null;
                    newUpload = null;

                    String mimeType = "application/octet-stream";

                    OCFile ocF = obtainNewOCFileToUpload(remote_path, file_path, mimeType, storageManager);

                    uploadKey = buildRemoteName(account, remote_path);
                    Log_OC.d(TAG, "InstantUpload - File name: " + remote_path + " key = " + uploadKey);
                    if (chunked && ocF.getFileLength() > ChunkedUploadFileOperation.CHUNK_SIZE) {
                        newUpload = new ChunkedUploadFileOperation(account, ocF, true, forceOverwrite, localAction);
                    } else {
                        newUpload = new UploadFileOperation(account, ocF, true, forceOverwrite, localAction);
                    }
                    if (fixed) {
                        newUpload.setRemoteFolderToBeCreated();
                    }
                  
                    // Insert OCFile on pending
                    // Check connectivity conditions to includes Instant upload on pending uploads
                    if (ConnectivityUtils.isOnline(getApplicationContext()) &&
                            !InstantUploadUtils.instantUploadViaWiFiOnly(getApplicationContext()) 
                                            || (InstantUploadUtils.instantUploadViaWiFiOnly(getApplicationContext()) 
                                                    == ConnectivityUtils.isConnectedViaWiFi(getApplicationContext()) == true)) {
                        
                        mPendingUploads.putIfAbsent(uploadKey, newUpload);
                        newUpload.addDatatransferProgressListener(this);
                        newUpload.addDatatransferProgressListener((FileUploaderBinder)mBinder);
                        sendBroadcastNewUploader(newUpload);

                        // Update uploading field of the OCFile on Database
                        storageManager.updateUploading(remote_path, true);
                        Log_OC.d(TAG, "Instant -- uploadKey: " + uploadKey + " offlineUpload: Upload field is TRUE for file " + remote_path);

                        requestedUploads.add(uploadKey);
                    }
                    else {
                        mInstantUploads.putIfAbsent(uploadKey, newUpload);
                    }

                } else {
                    Log_OC.w(TAG, "Instant upload file " + f.getAbsolutePath() + " dont exist anymore");
                }
            } while (c.moveToNext());
            
        }
        Log_OC.i(TAG, "offlineUpload - mInstantUploads size:" + mInstantUploads.size());
        c.close();
        db.close();

        for (Account account:accounts){
            Log_OC.d(TAG, "offlineUpload - Account: " + account.name);
            storageManager= new FileDataStorageManager(account, getContentResolver());

            // Get Offline files
            uploadingFiles = storageManager.getUploadingFiles();
            Log_OC.d(TAG, "offlineUpload - files: " + uploadingFiles.size());

            ocv = new OwnCloudVersion(AccountManager.get(this).getUserData(account, AccountAuthenticator.KEY_OC_VERSION));
            chunked = FileUploader.chunkedUploadIsSupported(ocv);
            uploadKey = null;
            newUpload = null;

            // Uploading files (Offline): adding to pending uploads
            for (int i = 0; i < uploadingFiles.size(); i++) {        

                uploadKey = buildRemoteName(account, uploadingFiles.get(i).getRemotePath());
                Log_OC.d(TAG, "OffLineUpload - key = " + uploadKey);
                
                // Check connectivity for uploading
                if (!InstantUploadUtils.instantUploadViaWiFiOnly(getApplicationContext()) || 
                        ConnectivityUtils.isConnectedViaWiFi(getApplicationContext()) || !mInstantUploads.containsKey(uploadKey)) {

                    if (chunked && uploadingFiles.get(i).getFileLength() > ChunkedUploadFileOperation.CHUNK_SIZE) {
                        newUpload = new ChunkedUploadFileOperation(account, uploadingFiles.get(i), false, forceOverwrite,
                                localAction);
                    } else {
                        newUpload = new UploadFileOperation(account, uploadingFiles.get(i), false, forceOverwrite, localAction);
                    }
                    if (fixed && i == 0) {
                        newUpload.setRemoteFolderToBeCreated();
                    }

                    mPendingUploads.putIfAbsent(uploadKey, newUpload);
                    newUpload.addDatatransferProgressListener(this);
                    newUpload.addDatatransferProgressListener((FileUploaderBinder)mBinder);
                    sendBroadcastNewUploader(newUpload);

                    // Update uploading field of the OCFile on Database
                    storageManager.updateUploading(uploadingFiles.get(i).getRemotePath(), true);
                    Log_OC.d(TAG,  "uploadKey: " + uploadKey + " offlineUpload: Upload field is TRUE for file " + uploadingFiles.get(i).getRemotePath());

                    requestedUploads.add(uploadKey);
                }

            }          

            Log_OC.i(TAG, "offlineUpload - Account: " + account.name + " mPendingUploads size:" + mPendingUploads.size());
        }

        return requestedUploads;

    }

    /**
     * Provides a binder object that clients can use to perform operations on
     * the queue of uploads, excepting the addition of new files.
     * 
     * Implemented to perform cancellation, pause and resume of existing
     * uploads.
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    /**
     * Called when ALL the bound clients were onbound.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        ((FileUploaderBinder)mBinder).clearListeners();
        return false;   // not accepting rebinding (default behaviour)
    }


    /**
     * Binder to let client components to perform operations on the queue of
     * uploads.
     * 
     * It provides by itself the available operations.
     */
    public class FileUploaderBinder extends Binder implements OnDatatransferProgressListener {

        /** 
         * Map of listeners that will be reported about progress of uploads from a {@link FileUploaderBinder} instance 
         */
        private Map<String, OnDatatransferProgressListener> mBoundListeners = new HashMap<String, OnDatatransferProgressListener>();

        /**
         * Cancels a pending or current upload of a remote file.
         * 
         * @param account Owncloud account where the remote file will be stored.
         * @param file A file in the queue of pending uploads
         */
        public void cancel(Account account, OCFile file) {
            UploadFileOperation upload = null;
            synchronized (mPendingUploads) {
                upload = mPendingUploads.remove(buildRemoteName(account, file));
            }
            OCFile canceledFile = null;
            if (upload != null) {
                upload.cancel();
                
                canceledFile = upload.getOldFile();
                if (canceledFile == null) {
                    canceledFile = upload.getFile();
                }
            } else {
                canceledFile = file;
            }
            if (canceledFile != null && account != null) {
                FileDataStorageManager fdsm = new FileDataStorageManager(account, getContentResolver()); 
                if (canceledFile.getLastSyncDateForData() == 0) {
                    fdsm.removeFile(canceledFile, false);
                } else {
                    fdsm.updateUploading(canceledFile.getRemotePath(), false);
                }
            }
            
        }



        public void clearListeners() {
            mBoundListeners.clear();
        }




        /**
         * Returns True when the file described by 'file' is being uploaded to
         * the ownCloud account 'account' or waiting for it
         * 
         * If 'file' is a directory, returns 'true' if some of its descendant files is uploading or waiting to upload. 
         * 
         * @param account Owncloud account where the remote file will be stored.
         * @param file A file that could be in the queue of pending uploads
         */
        public boolean isUploading(Account account, OCFile file) {
            if (account == null || file == null)
                return false;

            return file.isUploading();

            //            String targetKey = buildRemoteName(account, file);     
            //            
            //            synchronized (mPendingUploads) {
            //                if (file.isDirectory()) {
            //                    // this can be slow if there are many uploads :(
            //                    Iterator<String> it = mPendingUploads.keySet().iterator();
            //                    boolean found = false;
            //                    while (it.hasNext() && !found) {
            //                        found = it.next().startsWith(targetKey);
            //                    }
            //                    return found;
            //                } else {
            //                    return (mPendingUploads.containsKey(targetKey));
            //                }
            //            }
        }


        /**
         * Adds a listener interested in the progress of the upload for a concrete file.
         * 
         * @param listener      Object to notify about progress of transfer.    
         * @param account       ownCloud account holding the file of interest.
         * @param file          {@link OCfile} of interest for listener. 
         */
        public void addDatatransferProgressListener (OnDatatransferProgressListener listener, Account account, OCFile file) {
            if (account == null || file == null || listener == null) return;
            String targetKey = buildRemoteName(account, file);
            mBoundListeners.put(targetKey, listener);
        }



        /**
         * Removes a listener interested in the progress of the upload for a concrete file.
         * 
         * @param listener      Object to notify about progress of transfer.    
         * @param account       ownCloud account holding the file of interest.
         * @param file          {@link OCfile} of interest for listener. 
         */
        public void removeDatatransferProgressListener (OnDatatransferProgressListener listener, Account account, OCFile file) {
            if (account == null || file == null || listener == null) return;
            String targetKey = buildRemoteName(account, file);
            if (mBoundListeners.get(targetKey) == listener) {
                mBoundListeners.remove(targetKey);
            }
        }


        @Override
        public void onTransferProgress(long progressRate) {
            // old way, should not be in use any more
        }


        @Override
        public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer,
                String fileName) {
            String key = buildRemoteName(mCurrentUpload.getAccount(), mCurrentUpload.getFile());
            OnDatatransferProgressListener boundListener = mBoundListeners.get(key);
            if (boundListener != null) {
                boundListener.onTransferProgress(progressRate, totalTransferredSoFar, totalToTransfer, fileName);
            }
        }

    }

    /**
     * Upload worker. Performs the pending uploads in the order they were
     * requested.
     * 
     * Created with the Looper of a new thread, started in
     * {@link FileUploader#onCreate()}.
     */
    private static class ServiceHandler extends Handler {
        // don't make it a final class, and don't remove the static ; lint will
        // warn about a possible memory leak
        FileUploader mService;

        public ServiceHandler(Looper looper, FileUploader service) {
            super(looper);
            if (service == null)
                throw new IllegalArgumentException("Received invalid NULL in parameter 'service'");
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            @SuppressWarnings("unchecked")
            AbstractList<String> requestedUploads = (AbstractList<String>) msg.obj;
            if (msg.obj != null) {
                Iterator<String> it = requestedUploads.iterator();
                while (it.hasNext()) {
                    mService.uploadFile(it.next());
                }
            }
            mService.stopSelf(msg.arg1);
        }
    }

    /**
     * Core upload method: sends the file(s) to upload
     * 
     * @param uploadKey Key to access the upload to perform, contained in
     *            mPendingUploads
     */
    public void uploadFile(String uploadKey) {

        synchronized (mPendingUploads) {
            mCurrentUpload = mPendingUploads.get(uploadKey);
        }

        if (mCurrentUpload != null) {

            notifyUploadStart(mCurrentUpload);

            RemoteOperationResult uploadResult = null;

            try {
                    /// prepare client object to send requests to the ownCloud server
                    if (mUploadClient == null || !mLastAccount.equals(mCurrentUpload.getAccount()) || mForcedClientRefresh) {
                        mLastAccount = mCurrentUpload.getAccount();
                        mStorageManager = new FileDataStorageManager(mLastAccount, getContentResolver());
                        mUploadClient = OwnCloudClientUtils.createOwnCloudClient(mLastAccount, getApplicationContext());
                        mForcedClientRefresh = false;
                    }
    
                    /// create remote folder for instant uploads
                    if (mCurrentUpload.isRemoteFolderToBeCreated()) {
                        RemoteOperation operation = new CreateFolderOperation(  FileStorageUtils.getInstantUploadFilePath(this, ""), 
                                mStorageManager.getFileByPath(OCFile.PATH_SEPARATOR).getFileId(), // TODO generalize this : INSTANT_UPLOAD_DIR could not be a child of root
                                mStorageManager);
                        operation.execute(mUploadClient);      // ignoring result; fail could just mean that it already exists, but local database is not synchronized; the upload will be tried anyway
                    }
    
    
                    /// perform the upload
                    uploadResult = mCurrentUpload.execute(mUploadClient);

            } catch (AccountsException e) {
                Log_OC.e(TAG, "Error while trying to get authorization for " + mLastAccount.name, e);
                uploadResult = new RemoteOperationResult(e);

            } catch (IOException e) {
                Log_OC.e(TAG, "Error while trying to get authorization for " + mLastAccount.name, e);
                uploadResult = new RemoteOperationResult(e);

            } finally {

                synchronized (mPendingUploads) {
                    mPendingUploads.remove(uploadKey);
                    Log_OC.i(TAG, "Remove CurrentUploadItem from pending upload Item Map.");
                }
            }

            processUploadResult(uploadResult);
            
        }

    }

    /**
     * Make all the necessary actions necessary depending of the result of the last upload performed. 
     *  
     * @param uploadResult          Result of the last upload performed.
     */
    private void processUploadResult(RemoteOperationResult uploadResult) {
    
        if (uploadResult.isSuccess()) {
            saveUploadedFile();
        }
        
        sendFinalBroadcast(mCurrentUpload, uploadResult);
        
        notifyUploadResult(uploadResult, mCurrentUpload);
        
        /// update of instant uploads specific database - TODO remove this database
        if (uploadResult.isCancelled() || uploadResult.isSuccess()) {
            DbHandler db = new DbHandler(this.getBaseContext());
            db.removeIUPendingFile(mCurrentUpload.getFile().getStoragePath());
            db.close();
        } else {
            if (mCurrentUpload.isInstant()) {
                DbHandler db = null;
                try {
                    db = new DbHandler(this.getBaseContext());
                    String message = uploadResult.getLogMessage() + " errorCode: " + uploadResult.getCode();
                    if (uploadResult.getCode() == ResultCode.QUOTA_EXCEEDED) {
                        message = getString(R.string.failed_upload_quota_exceeded_text);
                    }
                    if (db.updateFileState(mCurrentUpload.getOriginalStoragePath(), DbHandler.UPLOAD_STATUS_UPLOAD_FAILED,
                            message) == 0) {
                        db.putFileForLater(mCurrentUpload.getOriginalStoragePath(), mCurrentUpload.getAccount().name, message);
                    }
                } finally {
                    if (db != null) {
                        db.close();
                    }
                }
            }
        }
    }

    
    /**
     * Saves a OC File after a successful upload.
     * 
     * A PROPFIND is necessary to keep the props in the local database
     * synchronized with the server, specially the modification time and Etag
     * (where available)
     * 
     * TODO refactor this ugly thing
     */
    private void saveUploadedFile() {
        OCFile file = mCurrentUpload.getFile();
        long syncDate = System.currentTimeMillis();
        file.setLastSyncDateForData(syncDate);
        file.setUploading(false);

        // / new PROPFIND to keep data consistent with server in theory, should
        // return the same we already have
        PropFindMethod propfind = null;
        RemoteOperationResult result = null;
        try {
            propfind = new PropFindMethod(mUploadClient.getBaseUri()
                    + WebdavUtils.encodePath(mCurrentUpload.getRemotePath()));
            int status = mUploadClient.executeMethod(propfind);
            boolean isMultiStatus = (status == HttpStatus.SC_MULTI_STATUS);
            if (isMultiStatus) {
                MultiStatus resp = propfind.getResponseBodyAsMultiStatus();
                WebdavEntry we = new WebdavEntry(resp.getResponses()[0], mUploadClient.getBaseUri().getPath());
                updateOCFile(file, we);
                file.setLastSyncDateForProperties(syncDate);

            } else {
                mUploadClient.exhaustResponse(propfind.getResponseBodyAsStream());
            }

            result = new RemoteOperationResult(isMultiStatus, status);
            Log_OC.i(TAG, "Update: synchronizing properties for uploaded " + mCurrentUpload.getRemotePath() + ": "
                    + result.getLogMessage());

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Update: synchronizing properties for uploaded " + mCurrentUpload.getRemotePath() + ": "
                    + result.getLogMessage(), e);

        } finally {
            if (propfind != null)
                propfind.releaseConnection();
        }

        // / maybe this would be better as part of UploadFileOperation... or
        // maybe all this method
        if (mCurrentUpload.wasRenamed()) {
            OCFile oldFile = mCurrentUpload.getOldFile();
            if (oldFile.fileExists()) {
                oldFile.setStoragePath(null);
                oldFile.setUploading(false);
                mStorageManager.saveFile(oldFile);

            } // else: it was just an automatic renaming due to a name
            // coincidence; nothing else is needed, the storagePath is right
            // in the instance returned by mCurrentUpload.getFile()
        }

        mStorageManager.saveFile(file);
    }

    
    /**
     * Checks the result of an upload operation an decides if needs that the user
     * performs any action before it can be successfully retried.
     * 
     * @param uploadResult      Result of a finished upload operation.
     * @return                  'True' when some action from the user is needed before the upload can be retried successfully.
     */
    private boolean uploadNeedsUserInputBeforeRetry(RemoteOperationResult uploadResult) {
        ResultCode code = uploadResult.getCode();
        return (!uploadResult.isSuccess()) &&
                    (uploadResult.isClientFail() ||
                     uploadResult.isSslRecoverableException() ||
                     code == ResultCode.ACCOUNT_NOT_FOUND ||
                     code == ResultCode.ACCOUNT_EXCEPTION ||
                     code == ResultCode.UNAUTHORIZED ||
                     code == ResultCode.FILE_NOT_FOUND ||
                     code == ResultCode.INVALID_LOCAL_FILE_NAME ||
                     code == ResultCode.INVALID_OVERWRITE ||
                     code == ResultCode.CONFLICT ||
                     code == ResultCode.OAUTH2_ERROR ||
                     code == ResultCode.OAUTH2_ERROR_ACCESS_DENIED
                     );
    }

    
    private void updateOCFile(OCFile file, WebdavEntry we) {
        file.setCreationTimestamp(we.createTimestamp());
        file.setFileLength(we.contentLength());
        file.setMimetype(we.contentType());
        file.setModificationTimestamp(we.modifiedTimestamp());
        file.setModificationTimestampAtLastSyncForData(we.modifiedTimestamp());
        // file.setEtag(mCurrentUpload.getEtag());    // TODO Etag, where available
    }

    private boolean checkAndFixInstantUploadDirectory(FileDataStorageManager storageManager) {
        String instantUploadDirPath = FileStorageUtils.getInstantUploadFilePath(this, "");
        OCFile instantUploadDir = storageManager.getFileByPath(instantUploadDirPath);
        if (instantUploadDir == null) {
            // first instant upload in the account. never account not
            // synchronized after the remote InstantUpload folder was created
            OCFile newDir = new OCFile(instantUploadDirPath);
            newDir.setMimetype("DIR");
            OCFile path = storageManager.getFileByPath(OCFile.PATH_SEPARATOR);

            if (path != null) {
                newDir.setParentId(path.getFileId());
                storageManager.saveFile(newDir);
                return true;
            } else {    // this should not happen anymore
                return false;
            }

        }
        return false;
    }

    private OCFile obtainNewOCFileToUpload(String remotePath, String localPath, String mimeType,
            FileDataStorageManager storageManager) {
        OCFile newFile = new OCFile(remotePath);
        newFile.setStoragePath(localPath);
        newFile.setLastSyncDateForProperties(0);
        newFile.setLastSyncDateForData(0);

        // size
        if (localPath != null && localPath.length() > 0) {
            File localFile = new File(localPath);
            newFile.setFileLength(localFile.length());
        } // else : don't worry about not assigning size, the problems with localPath
          //        are checked when the UploadFileOperation instance is created

        // MIME type
        if (mimeType == null || mimeType.length() <= 0) {
            try {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        remotePath.substring(remotePath.lastIndexOf('.') + 1));
            } catch (IndexOutOfBoundsException e) {
                Log_OC.e(TAG, "Trying to find out MIME type of a file without extension: " + remotePath);
            }
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        newFile.setMimetype(mimeType);

        // parent dir
        String parentPath = new File(remotePath).getParent();
        parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath : parentPath + OCFile.PATH_SEPARATOR;
        OCFile parentDir = storageManager.getFileByPath(parentPath);
        long parentDirId = parentDir.getFileId();
        newFile.setParentId(parentDirId);
        return newFile;
    }

    /**
     * Creates a status notification to show the upload progress
     * 
     * @param upload Upload operation starting.
     */
    private void notifyUploadStart(UploadFileOperation upload) {
        // / create status notification with a progress bar
        mLastPercent = 0;
        mNotification = new Notification(R.drawable.icon, getString(R.string.uploader_upload_in_progress_ticker),
                System.currentTimeMillis());
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mDefaultNotificationContentView = mNotification.contentView;
        mNotification.contentView = new RemoteViews(getApplicationContext().getPackageName(),
                R.layout.progressbar_layout);
        mNotification.contentView.setProgressBar(R.id.status_progress, 100, 0, false);
        mNotification.contentView.setTextViewText(R.id.status_text,
                String.format(getString(R.string.uploader_upload_in_progress_content), 0, upload.getFileName()));
        mNotification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);

        /// includes a pending intent in the notification showing the details view of the file
        Intent showDetailsIntent = null;
        if (PreviewImageFragment.canBePreviewed(upload.getFile())) {
            showDetailsIntent = new Intent(this, PreviewImageActivity.class);
        } else {
            showDetailsIntent = new Intent(this, FileDisplayActivity.class);
        }
        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, upload.getFile());
        showDetailsIntent.putExtra(FileActivity.EXTRA_ACCOUNT, upload.getAccount());
        showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(),
                (int) System.currentTimeMillis(), showDetailsIntent, 0);

        mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification);
    }

    /**
     * Callback method to update the progress bar in the status notification
     */
    @Override
    public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String fileName) {
        int percent = (int) (100.0 * ((double) totalTransferredSoFar) / ((double) totalToTransfer));
        if (percent != mLastPercent) {
            mNotification.contentView.setProgressBar(R.id.status_progress, 100, percent, false);
            String text = String.format(getString(R.string.uploader_upload_in_progress_content), percent, fileName);
            mNotification.contentView.setTextViewText(R.id.status_text, text);
            mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification);
        }
        mLastPercent = percent;
    }

    /**
     * Callback method to update the progress bar in the status notification
     * (old version)
     */
    @Override
    public void onTransferProgress(long progressRate) {
        // NOTHING TO DO HERE ANYMORE
    }

    /**
     * Updates the status notification with the result of an upload operation.
     * 
     * @param uploadResult Result of the upload operation.
     * @param upload Finished upload operation
     */
    private void notifyUploadResult(RemoteOperationResult uploadResult, UploadFileOperation upload) {
        Log_OC.d(TAG, "NotifyUploadResult with resultCode: " + uploadResult.getCode());

        if (uploadResult.isCancelled()) {
            // / cancelled operation -> silent removal of progress notification
            mNotificationManager.cancel(R.string.uploader_upload_in_progress_ticker);

        } else if (uploadResult.isSuccess()) {
            // / success -> silent update of progress notification to success
            // message
            mNotification.flags ^= Notification.FLAG_ONGOING_EVENT; // remove
            // the
            // ongoing
            // flag
            mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
            mNotification.contentView = mDefaultNotificationContentView;

            /// includes a pending intent in the notification showing the details view of the file
            Intent showDetailsIntent = null;
            if (PreviewImageFragment.canBePreviewed(upload.getFile())) {
                showDetailsIntent = new Intent(this, PreviewImageActivity.class); 
            } else {
                showDetailsIntent = new Intent(this, FileDisplayActivity.class); 
            }
            showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, upload.getFile());
            showDetailsIntent.putExtra(FileActivity.EXTRA_ACCOUNT, upload.getAccount());
            showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(),
                    (int) System.currentTimeMillis(), showDetailsIntent, 0);

            mNotification.setLatestEventInfo(getApplicationContext(),
                    getString(R.string.uploader_upload_succeeded_ticker),
                    String.format(getString(R.string.uploader_upload_succeeded_content_single), upload.getFileName()),
                    mNotification.contentIntent);

            mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification); 

        } else {

            // / fail -> explicit failure notification
            mNotificationManager.cancel(R.string.uploader_upload_in_progress_ticker);

            if (uploadNeedsUserInputBeforeRetry(uploadResult)) {            
            
                if (uploadResult.getCode() != ResultCode.NO_NETWORK_CONNECTION && uploadResult.getCode() != ResultCode.UNKNOWN_ERROR &&
                        uploadResult.getCode() != ResultCode.TIMEOUT){
                    Notification finalNotification = new Notification(R.drawable.icon,
                            getString(R.string.uploader_upload_failed_ticker), System.currentTimeMillis());
                    finalNotification.flags |= Notification.FLAG_AUTO_CANCEL;
                    if (uploadResult.getCode() == ResultCode.UNAUTHORIZED) {
                        // let the user update credentials with one click
                        Intent updateAccountCredentials = new Intent(this, AuthenticatorActivity.class);
                        updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, upload.getAccount());
                        updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACTION, AuthenticatorActivity.ACTION_UPDATE_TOKEN);
                        updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        updateAccountCredentials.addFlags(Intent.FLAG_FROM_BACKGROUND);
                        finalNotification.contentIntent = PendingIntent.getActivity(this, (int)System.currentTimeMillis(), updateAccountCredentials, PendingIntent.FLAG_ONE_SHOT);
                        mUploadClient = null;   // grant that future retries on the same account will get the fresh credentials
                    } else {
                        // TODO put something smart in the contentIntent below
                        finalNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), (int)System.currentTimeMillis(), new Intent(), 0);
                    }
    
                    String content = null;
                    if (uploadResult.getCode() == ResultCode.LOCAL_STORAGE_FULL
                            || uploadResult.getCode() == ResultCode.LOCAL_STORAGE_NOT_COPIED) {
                        // TODO we need a class to provide error messages for the users
                        // from a RemoteOperationResult and a RemoteOperation
                        content = String.format(getString(R.string.error__upload__local_file_not_copied), upload.getFileName(),
                                getString(R.string.app_name));
                    } else if (uploadResult.getCode() == ResultCode.QUOTA_EXCEEDED) {
                        content = getString(R.string.failed_upload_quota_exceeded_text);
    
                    } else {
                        content = String
                                .format(getString(R.string.uploader_upload_failed_content_single), upload.getFileName());
                    }
    
                    // we add only for instant-uploads the InstantUploadActivity and the
                    // db entry
                    Intent detailUploadIntent = null;
                    if (upload.isInstant() && InstantUploadActivity.IS_ENABLED) {
                        detailUploadIntent = new Intent(this, InstantUploadActivity.class);
                        detailUploadIntent.putExtra(FileUploader.EXTRA_ACCOUNT, upload.getAccount());
                    } else {
                        detailUploadIntent = new Intent(this, FailedUploadActivity.class);
                        detailUploadIntent.putExtra(FailedUploadActivity.MESSAGE, content);
                    }
                    finalNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(),
                            (int) System.currentTimeMillis(), detailUploadIntent, PendingIntent.FLAG_UPDATE_CURRENT
                            | PendingIntent.FLAG_ONE_SHOT);
    
                    finalNotification.setLatestEventInfo(getApplicationContext(), getString(R.string.uploader_upload_failed_ticker), content, finalNotification.contentIntent);
    
                    mNotificationManager.notify(R.string.uploader_upload_failed_ticker, finalNotification);
                }
                
            } else {
                Log_OC.d(TAG, "Upload Offline. File: " + mCurrentUpload.getRemotePath() + " account: " + mCurrentUpload.getAccount().name
                        + ". The upload will be retried in the future ");               
            }
        }

    }

    /**
     * Sends a broadcast when a new upload is added to the queue.
     * 
     * @param upload            Added upload operation
     */
    private void sendBroadcastNewUploader(UploadFileOperation upload) {
        Intent added = new Intent(ACTION_UPLOAD_ADDED);
        added.putExtra(EXTRA_ACCOUNT_NAME, upload.getAccount().name);
        added.putExtra(EXTRA_REMOTE_PATH, upload.getRemotePath());
        added.putExtra(EXTRA_LOCAL_PATH, upload.getOriginalStoragePath());
        sendStickyBroadcast(added);
    }
    
    /**
     * Sends a broadcast in order to the interested activities can update their
     * view
     * 
     * @param upload Finished upload operation
     * @param uploadResult Result of the upload operation
     */
    private void sendFinalBroadcast(UploadFileOperation upload, RemoteOperationResult uploadResult) {
        Intent end = new Intent(ACTION_UPLOAD_FINISHED);
        end.putExtra(EXTRA_REMOTE_PATH, upload.getRemotePath()); // real remote
        // path, after
        // possible
        // automatic
        // renaming
        if (upload.wasRenamed()) {
            end.putExtra(EXTRA_OLD_REMOTE_PATH, upload.getOldFile().getRemotePath());
        }
        end.putExtra(EXTRA_OLD_LOCAL_PATH, upload.getOriginalStoragePath());
        end.putExtra(EXTRA_ACCOUNT_NAME, upload.getAccount().name);
        end.putExtra(EXTRA_UPLOAD_RESULT, uploadResult.isSuccess());
        sendStickyBroadcast(end);
    }


}
