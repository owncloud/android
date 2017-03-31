package com.owncloud.android.files.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RetryUploadJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        UploadsStorageManager uploadsStorageManager = new UploadsStorageManager(getContentResolver());

        String fileRemotePath = jobParameters.getExtras().getString(FileUploader.EXTRA_REMOTE_PATH);

        String accountName = jobParameters.getExtras().getString(FileUploader.EXTRA_ACCOUNT_NAME);

        // Get upload to be retried
        OCUpload ocUpload = uploadsStorageManager.getLastUploadFor(new OCFile(fileRemotePath), accountName);

        FileUploader.UploadRequester uploadRequester = new FileUploader.UploadRequester();

        // Retry the upload
        uploadRequester.retry(this, ocUpload);

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

}
