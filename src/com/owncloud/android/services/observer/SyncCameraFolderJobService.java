package com.owncloud.android.services.observer;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.files.services.RetryUploadJobService;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.GetFolderFilesOperation;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.utils.Extras;

import java.io.File;
import java.io.IOException;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SyncCameraFolderJobService extends JobService implements OnRemoteOperationListener {

    private static final String TAG = RetryUploadJobService.class.getName();

    // To enqueue an action to be performed on a different thread than the current one
    private final Handler mHandler = new Handler();
    private ServiceConnection mOperationsServiceConnection = null;
    private OperationsService.OperationsServiceBinder mOperationsServiceBinder = null;

    // Identifier of operation in progress which result shouldn't be lost
    private long mWaitingForOpId = Long.MAX_VALUE;

    private Account mAccount;
    private String mRemotePath;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        //Get local folder images
        String localCameraPath = jobParameters.getExtras().getString(Extras.EXTRA_LOCAL_CAMERA_PATH);

        File cameraFolderFiles[] = new File[0];

        if (localCameraPath != null) {
            File cameraFolder = new File(localCameraPath);
            cameraFolderFiles = cameraFolder.listFiles();
        }

        //Get existing images in server
        String accountName = jobParameters.getExtras().getString(Extras.EXTRA_ACCOUNT_NAME);

        Account account = AccountUtils.getOwnCloudAccountByName(this, accountName);

        String uploadPicturesPath = jobParameters.getExtras().getString(Extras.
                EXTRA_UPLOAD_PICTURES_PATH);

        // bind to Operations Service
        mOperationsServiceConnection = new OperationsServiceConnection();
        bindService(new Intent(this, OperationsService.class), mOperationsServiceConnection,
                Context.BIND_AUTO_CREATE);

        //Get existing videos in server


//        String accountName = jobParameters.getExtras().getString(Extras.EXTRA_ACCOUNT_NAME);
//
//        Account account = AccountUtils.getOwnCloudAccountByName(this, accountName);
//
//        try {
//            OwnCloudAccount ocAccount = new OwnCloudAccount(
//                    account,
//                    this
//            );
//
//            OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton()
//                    .getClientFor(ocAccount, this);
//
//            GetFolderFilesOperation getFolderFilesOperation = new GetFolderFilesOperation("/");
//            getFolderFilesOperation.execute(client);
//
//        } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException |
//                AuthenticatorException | IOException | OperationCanceledException e) {
//            e.printStackTrace();
//        }
//
//        for (File file : cameraFolderFiles) {
//
//        }

        /// check file type
//        String mimeType = MimetypeIconUtil.getBestMimeTypeByFilename(fileName);
//        boolean isImage = mimeType.startsWith("image/");
//        boolean isVideo = mimeType.startsWith("video/");


        // Check upload path for images

        jobFinished(jobParameters, false);  // done here, real job was delegated to another castle
        return true;    // TODO or false? what is the real effect, Google!?!?!?!?
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {

        if (mOperationsServiceConnection != null) {
            unbindService(mOperationsServiceConnection);
            mOperationsServiceBinder = null;
        }

        return true;
    }

    private void doOnResumeAndBound() {

        // Registering to listen for operation callbacks
        mOperationsServiceBinder.addOperationListener(this, mHandler);

        if (mWaitingForOpId <= Integer.MAX_VALUE) {
            mOperationsServiceBinder.dispatchResultIfFinished((int)mWaitingForOpId, this);
        }

        Intent getFolderFilesIntent = new Intent();
        getFolderFilesIntent.setAction(OperationsService.ACTION_GET_FOLDER_FILES);
        getFolderFilesIntent.putExtra(OperationsService.EXTRA_REMOTE_PATH, mRemotePath);
        getFolderFilesIntent.putExtra(OperationsService.EXTRA_ACCOUNT, mAccount);
        mWaitingForOpId = mOperationsServiceBinder.queueNewOperation(getFolderFilesIntent);
    }

    /**
     * Implements callback methods for service binding.
     */
    private class OperationsServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (component.equals(
                    new ComponentName(SyncCameraFolderJobService.this, OperationsService.class)
            )) {
                mOperationsServiceBinder = (OperationsService.OperationsServiceBinder) service;

                doOnResumeAndBound();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(
                    new ComponentName(SyncCameraFolderJobService.this, OperationsService.class)
            )) {
                Log_OC.e(TAG, "Operations service crashed");
                mOperationsServiceBinder = null;
            }
        }
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation caller, RemoteOperationResult result) {

        String a = "";

    }
}