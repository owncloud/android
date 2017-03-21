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

        String file = jobParameters.getExtras().getString(FileUploader.KEY_FILE);

        String account = jobParameters.getExtras().getString(FileUploader.KEY_ACCOUNT);

        UploadsStorageManager uploadsStorageManager = new UploadsStorageManager(getContentResolver());

        // Get upload to be retried
        OCUpload ocUpload = uploadsStorageManager.getLastUploadFor(new OCFile(file), account);

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
