/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2018 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.files.services;

import android.accounts.Account;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.google.gson.Gson;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.AvailableOfflineSyncStorageManager;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCAvailableOfflineSync;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.OCFilesForAccount;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.utils.Extras;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;

/**
 * Job to watch for local changes in available offline files (formerly known as kept-in-sync files) and try to
 * synchronize them with the OC server.
 * This job should be executed every 15 minutes since a file is set as available offline for the first time
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AvailableOfflineSyncJobService extends JobService {
    private static final String TAG = AvailableOfflineSyncJobService.class.getName();

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log_OC.d(TAG, "Starting job to sync available offline files");

        new AvailableOfflineJobTask(this).execute(jobParameters);

        return true; // True because we have a thread still running in background
    }

    private static class AvailableOfflineJobTask extends AsyncTask<JobParameters, Void, JobParameters> {

        private final JobService mAvailableOfflineJobService;

        private AvailableOfflineSyncStorageManager mAvailableOfflineSyncStorageManager;
        private OCAvailableOfflineSync mOcAvailableOfflineSync;

        public AvailableOfflineJobTask(JobService mAvailableOfflineJobService) {
            this.mAvailableOfflineJobService = mAvailableOfflineJobService;
        }

        @Override
        protected JobParameters doInBackground(JobParameters... jobParams) {

            mAvailableOfflineSyncStorageManager = new AvailableOfflineSyncStorageManager(
                    mAvailableOfflineJobService.getContentResolver()
            );

            String availableOfflineFilesJson = jobParams[0].getExtras()
                    .getString(Extras.EXTRA_AVAILABLE_OFFLINE_FILES_FOR_ACCOUNT);
            Gson gson = new Gson();

            OCFilesForAccount availableOfflineFilesForAccount = gson.fromJson(
                    availableOfflineFilesJson,
                    OCFilesForAccount.class
            );

            // Cancel periodic job if there's no available offline files to watch for local changes
            if (availableOfflineFilesForAccount.getFilesForAccount().isEmpty()) {
                cancelPeriodicJob(jobParams[0].getJobId());
                return jobParams[0];

            } else {
                syncAvailableOfflineFiles(availableOfflineFilesForAccount);
            }

            return jobParams[0];
        }

        private void syncAvailableOfflineFiles(OCFilesForAccount availableOfflineFilesForAccount) {
            mOcAvailableOfflineSync = mAvailableOfflineSyncStorageManager.getAvailableOfflineSync(
                    null, null, null);

            for (OCFilesForAccount.OCFileForAccount fileForAccount :
                    availableOfflineFilesForAccount.getFilesForAccount()) {

                String localPath = fileForAccount.getFile().getStoragePath();

                if (localPath == null) {
                    localPath = FileStorageUtils.getDefaultSavePathFor(
                            fileForAccount.getAccountName(),
                            fileForAccount.getFile()
                    );
                }

                File availableOfflineFile = new File(localPath);

                if (availableOfflineFile.lastModified() <= mOcAvailableOfflineSync.getAvailableOfflineLastSync()) {
                    Log_OC.i(TAG, "File " + localPath + " modified before period to check, ignoring");
                    continue;
                }

                startSyncOperation(availableOfflineFile, fileForAccount.getAccountName());
            }
        }

        /**
         * Triggers an operation to synchronize the contents of a recently modified available offline file with
         * its remote counterpart in the associated ownCloud account.
         * @param availableOffline file to synchronize
         * @param accountName account to which upload the available offline file
         */
        private void startSyncOperation(File availableOffline, String accountName) {
            Account mAccount = AccountUtils.getOwnCloudAccountByName(mAvailableOfflineJobService, accountName);

            FileDataStorageManager storageManager =
                    new FileDataStorageManager(mAccount, mAvailableOfflineJobService.getContentResolver());
            // a fresh object is needed; many things could have occurred to the file
            // since it was registered to observe again, assuming that local files
            // are linked to a remote file AT MOST, SOMETHING TO BE DONE;
            OCFile file = storageManager.getFileByLocalPath(
                    availableOffline.getPath() + File.separator + availableOffline.getName()
            );
            if (file == null) {
                Log_OC.w(TAG, "Could not find OC file for " + availableOffline.getPath() +
                        File.separator + availableOffline.getName());
            } else {
                SynchronizeFileOperation sfo =
                        new SynchronizeFileOperation(file, null, mAccount, false,
                                mAvailableOfflineJobService);
                RemoteOperationResult result = sfo.execute(storageManager, mAvailableOfflineJobService);
                if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                    // ISSUE 5: if the user is not running the app (this is a service!),
                    // this can be very intrusive; a notification should be preferred
                    Intent i = new Intent(mAvailableOfflineJobService, ConflictsResolveActivity.class);
                    i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.putExtra(ConflictsResolveActivity.EXTRA_FILE, file);
                    i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, mAccount);
                    mAvailableOfflineJobService.startActivity(i);
                }
            }
        }

        /**
         * Cancel the periodic job
         * @param jobId id of the job to cancel
         */
        private void cancelPeriodicJob(int jobId) {
            JobScheduler jobScheduler = (JobScheduler) mAvailableOfflineJobService.getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);

            jobScheduler.cancel(jobId);

            Log_OC.d(TAG, "No available offline to check, cancelling the periodic job");
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            mAvailableOfflineJobService.jobFinished(jobParameters, false);
        }
    }

    @Override
    /**
     * Called by the system if the job is cancelled before being finished
     */
    public boolean onStopJob(JobParameters jobParameters) {
        Log_OC.d(TAG, "Job " + TAG + " was cancelled before finishing.");

        return true;
    }
}