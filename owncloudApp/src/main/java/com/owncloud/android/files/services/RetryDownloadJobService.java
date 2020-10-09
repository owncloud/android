/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
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
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;

import androidx.core.content.ContextCompat;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.utils.Extras;
import timber.log.Timber;

public class RetryDownloadJobService extends JobService {

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

            Timber.d("Retrying download of %1s in %2s", fileRemotePath, accountName);

            // Get download file from database
            OCFile ocFile = fileDataStorageManager.getFileByPath(fileRemotePath);

            if (ocFile != null) {
                // Retry download
                Intent intent = new Intent(this, FileDownloader.class);
                intent.putExtra(FileDownloader.KEY_ACCOUNT, account);
                intent.putExtra(FileDownloader.KEY_FILE, ocFile);
                intent.putExtra(FileDownloader.KEY_RETRY_DOWNLOAD, true);
                Timber.d("Retry download from foreground/background, startForeground() will be called soon");
                ContextCompat.startForegroundService(this, intent);
            } else {
                Timber.w("File %1s in %2s not found in database", fileRemotePath, accountName);
            }

        } else {
            Timber.w("Account %1s was deleted, no retry will be done", accountName);
        }

        jobFinished(jobParameters, false);  // done here, real job was delegated to another castle
        return true;    // TODO or false? what is the real effect, Google!?!?!?!?
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }
}