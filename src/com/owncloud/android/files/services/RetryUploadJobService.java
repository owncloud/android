package com.owncloud.android.files.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.lib.common.utils.Log_OC;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RetryUploadJobService extends JobService {

    private static final String TAG = RetryUploadJobService.class.getName();

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        UploadsStorageManager uploadsStorageManager = new UploadsStorageManager(getContentResolver());

        String fileRemotePath = jobParameters.getExtras().getString(FileUploader.EXTRA_REMOTE_PATH);

        String accountName = jobParameters.getExtras().getString(FileUploader.EXTRA_ACCOUNT_NAME);

        Log_OC.d(TAG, String.format("Retrying upload of %1s in %2s", fileRemotePath, accountName));

        // Get upload to be retried
        OCUpload ocUpload = uploadsStorageManager.getLastUploadFor(new OCFile(fileRemotePath), accountName);

        if (ocUpload != null) {
            // Retry the upload
            FileUploader.UploadRequester uploadRequester = new FileUploader.UploadRequester();
            uploadRequester.retry(this, ocUpload);
        } else {
            // easy if the user deletes the upload in uploads view before recovering network
            Log_OC.w(
                TAG,
                String.format(
                    "No upload found in database for %1s in %2s",
                    fileRemotePath, accountName
                )
            );
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

}
