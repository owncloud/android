package com.owncloud.android.services.observer;

import android.accounts.Account;
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
import android.util.Log;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.GetFolderFilesOperation;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.utils.Extras;

import java.io.File;
import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SyncCameraFolderJobService extends JobService implements OnRemoteOperationListener {

    private static final String TAG = SyncCameraFolderJobService.class.getName();

    // To enqueue an action to be performed on a different thread than the current one
    private final Handler mHandler = new Handler();
    private ServiceConnection mOperationsServiceConnection = null;
    private OperationsService.OperationsServiceBinder mOperationsServiceBinder = null;

    // Identifier of operation in progress which result shouldn't be lost
    private long mWaitingForOpId = Long.MAX_VALUE;

    private JobParameters mJobParameters;

    private String mUploadedPicturesPath;
    private String mUploadedVideosPath;

    private boolean mGetRemotePicturesCompleted;
    private boolean mGetRemoteVideosCompleted;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        Log.d(TAG, "Starting job to sync camera folder");

        mJobParameters = jobParameters;

        mGetRemotePicturesCompleted = false;
        mGetRemoteVideosCompleted = false;

        // bind to Operations Service
        mOperationsServiceConnection = new OperationsServiceConnection();
        bindService(new Intent(this, OperationsService.class), mOperationsServiceConnection,
                Context.BIND_AUTO_CREATE);

        return true; // True because we have a thread still running requesting stuff to the server
    }

    @Override
    /**
     * Called by the system if the job is cancelled before being finished
     */
    public boolean onStopJob(JobParameters jobParameters) {

        if (mOperationsServiceConnection != null) {
            unbindService(mOperationsServiceConnection);
            mOperationsServiceBinder = null;
        }

        return true;
    }

    /**
     * Get remote pictures and videos contained in upload folders
     */
    private void getUploadedPicturesAndVideos() {

        // Registering to listen for operation callbacks
        mOperationsServiceBinder.addOperationListener(this, mHandler);

        if (mWaitingForOpId <= Integer.MAX_VALUE) {
            mOperationsServiceBinder.dispatchResultIfFinished((int)mWaitingForOpId, this);
        }

        String accountName = mJobParameters.getExtras().getString(Extras.EXTRA_ACCOUNT_NAME);

        Account account = AccountUtils.getOwnCloudAccountByName(this, accountName);

        mUploadedPicturesPath = mJobParameters.getExtras().getString(Extras.
                EXTRA_UPLOAD_PICTURES_PATH);

        mUploadedVideosPath = mJobParameters.getExtras().getString(Extras.
                EXTRA_UPLOAD_VIDEOS_PATH);

        if (mUploadedPicturesPath != null) {
            // Get remote pictures
            Intent getUploadedPicturesIntent = new Intent();
            getUploadedPicturesIntent.setAction(OperationsService.ACTION_GET_FOLDER_FILES);
            getUploadedPicturesIntent.putExtra(OperationsService.EXTRA_REMOTE_PATH, mUploadedPicturesPath);
            getUploadedPicturesIntent.putExtra(OperationsService.EXTRA_ACCOUNT, account);
            mWaitingForOpId = mOperationsServiceBinder.queueNewOperation(getUploadedPicturesIntent);
        }

        if (mUploadedVideosPath != null) {
            // Get remote videos
            Intent getUploadedVideosIntent = new Intent();
            getUploadedVideosIntent.setAction(OperationsService.ACTION_GET_FOLDER_FILES);
            getUploadedVideosIntent.putExtra(OperationsService.EXTRA_REMOTE_PATH, mUploadedVideosPath);
            getUploadedVideosIntent.putExtra(OperationsService.EXTRA_ACCOUNT, account);
            mWaitingForOpId = mOperationsServiceBinder.queueNewOperation(getUploadedVideosIntent);
        }
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

                getUploadedPicturesAndVideos();
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
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {

        String remotePath = ((GetFolderFilesOperation) operation).getRemotePath();

        // Result contains remote pictures
        if (mUploadedPicturesPath != null && mUploadedPicturesPath.equals(remotePath)) {

            Log.d(TAG, "Receiving pictures uploaded");

            uploadNewFiles(result.getData());

            mGetRemotePicturesCompleted = true;
        }

        // Result contains remote videos
        if (mUploadedVideosPath != null && mUploadedVideosPath.equals(remotePath)) {

            Log.d(TAG, "Receiving videos uploaded");

            mGetRemoteVideosCompleted = true;
        }

        // We have to unbind the service to get remote images/videos and finish the job when
        // requested operations finish

        // User only requests pictures upload
        boolean mOnlyGetPicturesFinished = mGetRemotePicturesCompleted && mUploadedVideosPath == null;

        // User only requests videos upload
        boolean mOnlyGetVideosFinished = mGetRemoteVideosCompleted && mUploadedPicturesPath == null ;

        // User requests pictures & videos upload
        boolean mGetPicturesVideosFinished = mGetRemotePicturesCompleted && mGetRemoteVideosCompleted;

        if (mOnlyGetPicturesFinished || mOnlyGetVideosFinished || mGetPicturesVideosFinished) {

            Log.d(TAG, "Finishing camera folder sync job");

            if (mOperationsServiceBinder != null) {
                mOperationsServiceBinder.removeOperationListener(this);
            }

            if (mOperationsServiceConnection != null) {
                unbindService(mOperationsServiceConnection);
                mOperationsServiceBinder = null;
            }

            jobFinished(mJobParameters, false);
        }
    }

    private void uploadNewFiles(ArrayList<Object> remoteFiles) {

        //Get local folder images
        String localCameraPath = mJobParameters.getExtras().getString(Extras.EXTRA_LOCAL_CAMERA_PATH);

        File cameraFolderFiles[] = new File[0];

        if (localCameraPath != null) {
            File cameraFolder = new File(localCameraPath);
            cameraFolderFiles = cameraFolder.listFiles();
        }
    }
}