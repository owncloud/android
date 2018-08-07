/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author David González Verdugo
 *   Copyright (C) 2018 ownCloud GmbH.
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

import android.accounts.Account;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.Extras;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RetryDownloadJobService extends JobService {

    private static final String TAG = RetryDownloadJobService.class.getName();

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        String accountName = jobParameters.getExtras().getString(Extras.EXTRA_ACCOUNT_NAME);

        Account account = AccountUtils.getOwnCloudAccountByName(this, accountName);

        // Check if the account has been deleted after downloading the file and before
        // retrying the download
        if (account != null) {
            FileDataStorageManager fileDataStorageManager = new FileDataStorageManager(
                    this, account,
                getContentResolver()
            );

            String fileRemotePath = jobParameters.getExtras().getString(
                Extras.EXTRA_REMOTE_PATH
            );

            Log_OC.d(TAG, String.format("Retrying download of %1s in %2s", fileRemotePath,
                    accountName));

            // Get download file from database
            OCFile ocFile = fileDataStorageManager.getFileByPath(fileRemotePath);

            if (ocFile != null) {
                // Retry download
                Intent i = new Intent(this, FileDownloader.class);
                i.putExtra(FileDownloader.EXTRA_ACCOUNT, account);
                i.putExtra(FileDownloader.EXTRA_FILE, ocFile);
                this.startService(i);
            } else {
                Log_OC.w(
                    TAG,
                    String.format(
                        "File %1s in %2s not found in database",
                        fileRemotePath, accountName
                    )
                );
            }

        } else {
            Log_OC.w(
                TAG,
                String.format(
                    "Account %1s was deleted, no retry will be done",
                    accountName
                )
            );
        }

        jobFinished(jobParameters, false);  // done here, real job was delegated to another castle
        return true;    // TODO or false? what is the real effect, Google!?!?!?!?
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }
}