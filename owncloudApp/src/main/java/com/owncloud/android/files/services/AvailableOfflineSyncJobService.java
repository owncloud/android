/**
 * ownCloud Android client application
 *
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
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.Context;
import android.os.AsyncTask;

import androidx.core.util.Pair;
import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.utils.FileStorageUtils;
import timber.log.Timber;

import java.io.File;
import java.util.List;

import static com.owncloud.android.utils.NotificationUtils.notifyConflict;

/**
 * Job to watch for local changes in available offline files (formerly known as kept-in-sync files) and try to
 * synchronize them with the OC server.
 * This job should be executed every 15 minutes since a file is set as available offline for the first time and stopped
 * when there's no available offline files
 */
public class AvailableOfflineSyncJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Timber.d("Starting job to sync available offline files");

        new AvailableOfflineJobTask(this).execute(jobParameters);

        return true; // True because we have a thread still running in background
    }

    private static class AvailableOfflineJobTask extends AsyncTask<JobParameters, Void, JobParameters> {

        private final JobService mAvailableOfflineJobService;

        public AvailableOfflineJobTask(JobService mAvailableOfflineJobService) {
            this.mAvailableOfflineJobService = mAvailableOfflineJobService;
        }

        @Override
        protected JobParameters doInBackground(JobParameters... jobParams) {
//
//            Account account =
//                    AccountUtils.getCurrentOwnCloudAccount(mAvailableOfflineJobService.getApplicationContext());
//
//            if (mAvailableOfflineJobService.getContentResolver() == null) {
//                Timber.w("Content resolver is null, do not sync av. offline files.");
//                return jobParams[0];
//            }
//
//            FileDataStorageManager fileDataStorageManager = new FileDataStorageManager(
//                    mAvailableOfflineJobService, account, mAvailableOfflineJobService.getContentResolver()
//            );
//
//            List<Pair<OCFile, String>> availableOfflineFilesFromEveryAccount = fileDataStorageManager.
//                    getAvailableOfflineFilesFromEveryAccount();
//
//            // Cancel periodic job if there's no available offline files to watch for local changes
//            if (availableOfflineFilesFromEveryAccount.isEmpty()) {
//                cancelPeriodicJob(jobParams[0].getJobId());
//                return jobParams[0];
//            } else {
//                syncAvailableOfflineFiles(availableOfflineFilesFromEveryAccount);
//            }

            return jobParams[0];
        }

        private void syncAvailableOfflineFiles(List<Pair<OCFile, String>> availableOfflineFilesForAccount) {
//            for (Pair<OCFile, String> fileForAccount : availableOfflineFilesForAccount) {
//
//                String localPath = fileForAccount.first.getStoragePath();
//
//                if (localPath == null) {
//                    localPath = FileStorageUtils.getDefaultSavePathFor(
//                            fileForAccount.second, // Account name
//                            fileForAccount.first   // OCFile
//                    );
//                }
//
//                File localFile = new File(localPath);
//
//                if (localFile.lastModified() <= fileForAccount.first.getLastSyncDateForData() &&
//                        MainApp.Companion.isDeveloper()) {
//                    Timber.i("File " + fileForAccount.first.getRemotePath() + " already synchronized " +
//                            "in account " + fileForAccount.second + ", ignoring");
//                    continue;
//                }
//
//                startSyncOperation(fileForAccount.first, fileForAccount.second);
//            }
        }

        /**
         * Triggers an operation to synchronize the contents of a recently modified available offline file with
         * its remote counterpart in the associated ownCloud account.
         *
         * @param availableOfflineFile file to synchronize
         * @param accountName          account to synchronize the available offline file with
         */
        private void startSyncOperation(OCFile availableOfflineFile, String accountName) {
//            if (MainApp.Companion.isDeveloper()) {
//                Timber.i("Requested synchronization for file %1s in account %2s",
//                        availableOfflineFile.getRemotePath(), accountName);
//            }
//
//            Account account = AccountUtils.getOwnCloudAccountByName(mAvailableOfflineJobService, accountName);
//            if (account == null) {
//                Timber.w("Account '" + accountName + "' not found in account manager. Aborting Sync operation...");
//                return;
//            }
//
//            FileDataStorageManager storageManager =
//                    new FileDataStorageManager(
//                            mAvailableOfflineJobService, account, mAvailableOfflineJobService.getContentResolver()
//                    );
//
//            SynchronizeFileOperation synchronizeFileOperation =
//                    new SynchronizeFileOperation(availableOfflineFile, null, account, false,
//                            mAvailableOfflineJobService, true);
//
//            RemoteOperationResult result = synchronizeFileOperation.
//                    execute(storageManager, mAvailableOfflineJobService);
//
//            if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
//                notifyConflict(availableOfflineFile, account, mAvailableOfflineJobService);
//            }
        }

        /**
         * Cancel the periodic job
         *
         * @param jobId id of the job to cancel
         */
        private void cancelPeriodicJob(int jobId) {
            JobScheduler jobScheduler = (JobScheduler) mAvailableOfflineJobService.getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);

            jobScheduler.cancel(jobId);

            Timber.d("No available offline files to check, cancelling the periodic job");
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            mAvailableOfflineJobService.jobFinished(jobParameters, false);
        }
    }

    @Override
    /*
     * Called by the system if the job is cancelled before being finished
     */
    public boolean onStopJob(JobParameters jobParameters) {
        Timber.d("Job was cancelled before finishing.");
        return true;
    }
}
