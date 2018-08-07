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

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.Extras;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RetryUploadJobService extends JobService {

    private static final String TAG = RetryUploadJobService.class.getName();

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        UploadsStorageManager uploadsStorageManager = new UploadsStorageManager(getContentResolver());

        String fileRemotePath = jobParameters.getExtras().getString(Extras.EXTRA_REMOTE_PATH);

        String accountName = jobParameters.getExtras().getString(Extras.EXTRA_ACCOUNT_NAME);

        Log_OC.d(TAG, String.format("Retrying upload of %1s in %2s", fileRemotePath, accountName));

        // Get upload to be retried
        OCUpload ocUpload = uploadsStorageManager.getLastUploadFor(new OCFile(fileRemotePath), accountName);

        if (ocUpload != null) {
            // Retry the upload
            TransferRequester requester = new TransferRequester();
            requester.retry(this, ocUpload);

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

        jobFinished(jobParameters, false);  // done here, real job was delegated to another castle
        return true;    // TODO or false? what is the real effect, Google!?!?!?!?
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }
}