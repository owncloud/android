package com.owncloud.android.files.services;

import android.accounts.Account;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RetryDownloadJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        String accountName = jobParameters.getExtras().getString(FileDownloader.
                EXTRA_ACCOUNT_NAME);

        Account account = AccountUtils.getOwnCloudAccountByName(this, accountName);

        // Check if the account has been deleted after downloading the file and before
        // retrying the download
        if (account != null) {

            FileDataStorageManager fileDataStorageManager = new FileDataStorageManager(account,
                    getContentResolver());

            String fileRemotePath = jobParameters.getExtras().getString(FileDownloader.
                    EXTRA_REMOTE_PATH);

            // Get download file from database
            OCFile ocFile= fileDataStorageManager.getFileByPath(fileRemotePath);

            // Retry download
            Intent i = new Intent(this, FileDownloader.class);
            i.putExtra(FileDownloader.EXTRA_ACCOUNT, account);
            i.putExtra(FileDownloader.EXTRA_FILE, ocFile);
            this.startService(i);
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

}
