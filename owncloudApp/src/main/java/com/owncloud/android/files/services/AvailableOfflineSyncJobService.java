/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.util.Pair;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.notifications.NotificationUtils;
import com.owncloud.android.utils.Extras;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.util.List;

/**
 * Job to watch for local changes in available offline files (formerly known as kept-in-sync files) and try to
 * synchronize them with the OC server.
 * This job should be executed every 15 minutes since a file is set as available offline for the first time and stopped
 * when there's no available offline files
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AvailableOfflineSyncJobService extends JobService {
    private static final String TAG = "AvOfflineSyncJobService";
    private static final String FILE_SYNC_CONFLICT_CHANNEL_ID = "FILE_SYNC_CONFLICT_CHANNEL_ID";

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log_OC.d(TAG, "Starting job to sync available offline files");

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

            String accountName = jobParams[0].getExtras().getString(Extras.EXTRA_ACCOUNT_NAME);

            Account account = AccountUtils.getOwnCloudAccountByName(mAvailableOfflineJobService, accountName);

            FileDataStorageManager fileDataStorageManager = new FileDataStorageManager(
                    mAvailableOfflineJobService, account, mAvailableOfflineJobService.getContentResolver()
            );

            List<Pair<OCFile, String>> availableOfflineFilesFromEveryAccount = fileDataStorageManager.
                    getAvailableOfflineFilesFromEveryAccount();

            // Cancel periodic job if there's no available offline files to watch for local changes
            if (availableOfflineFilesFromEveryAccount.isEmpty()) {
                cancelPeriodicJob(jobParams[0].getJobId());
                return jobParams[0];
            } else {
                syncAvailableOfflineFiles(availableOfflineFilesFromEveryAccount);
            }

            return jobParams[0];
        }

        private void syncAvailableOfflineFiles(List<Pair<OCFile, String>> availableOfflineFilesForAccount) {
            for (Pair<OCFile, String> fileForAccount : availableOfflineFilesForAccount) {

                String localPath = fileForAccount.first.getStoragePath();

                if (localPath == null) {
                    localPath = FileStorageUtils.getDefaultSavePathFor(
                            fileForAccount.second, // Account name
                            fileForAccount.first   // OCFile
                    );
                }

                File localFile = new File(localPath);

                if (localFile.lastModified() <= fileForAccount.first.getLastSyncDateForData() &&
                        (BuildConfig.DEBUG || MainApp.Companion.isBeta())) {
                    Log_OC.i(TAG, "File " + fileForAccount.first.getRemotePath() + " already synchronized " +
                            "in account " + fileForAccount.second + ", ignoring");
                    continue;
                }

                startSyncOperation(fileForAccount.first, fileForAccount.second);
            }
        }

        /**
         * Triggers an operation to synchronize the contents of a recently modified available offline file with
         * its remote counterpart in the associated ownCloud account.
         * @param availableOfflineFile file to synchronize
         * @param accountName account to synchronize the available offline file with
         */
        private void startSyncOperation(OCFile availableOfflineFile, String accountName) {
            if (BuildConfig.DEBUG || MainApp.Companion.isBeta()) {
                Log_OC.i(
                        TAG,
                        String.format(
                                "Requested synchronization for file %1s in account %2s",
                                availableOfflineFile.getRemotePath(),
                                accountName
                        )
                );
            }

            Account account = AccountUtils.getOwnCloudAccountByName(mAvailableOfflineJobService, accountName);

            FileDataStorageManager storageManager =
                    new FileDataStorageManager(
                            mAvailableOfflineJobService, account, mAvailableOfflineJobService.getContentResolver()
                    );

            SynchronizeFileOperation synchronizeFileOperation =
                    new SynchronizeFileOperation(availableOfflineFile, null, account, false,
                            mAvailableOfflineJobService, true);

            RemoteOperationResult result = synchronizeFileOperation.
                    execute(storageManager, mAvailableOfflineJobService);

            if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                notifyConflict(availableOfflineFile, account);
            }
        }

        /**
         * Show a notification with file conflict information, that will open a dialog to solve it when tapping it
         * @param availableOfflineFile file in conflict
         * @param account account which the file in conflict belongs to
         */
        private void notifyConflict(OCFile availableOfflineFile, Account account) {
            NotificationManager notificationManager = (NotificationManager) mAvailableOfflineJobService.
                    getSystemService(NOTIFICATION_SERVICE);
            NotificationCompat.Builder notificationBuilder = NotificationUtils.
                    newNotificationBuilder(mAvailableOfflineJobService);

            // Configure notification channel
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel;
                CharSequence name = mAvailableOfflineJobService.
                        getString(R.string.file_sync_notification_channel_name);
                String description = mAvailableOfflineJobService.
                        getString(R.string.file_sync_notification_channel_description);
                int importance = NotificationManager.IMPORTANCE_LOW;
                notificationChannel = new NotificationChannel(FILE_SYNC_CONFLICT_CHANNEL_ID,
                        name, importance);
                notificationChannel.setDescription(description);
                notificationManager.createNotificationChannel(notificationChannel);
            }

            notificationBuilder
                    .setChannelId(FILE_SYNC_CONFLICT_CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setTicker(mAvailableOfflineJobService.getString(R.string.conflict_title))
                    .setContentTitle(mAvailableOfflineJobService.getString(R.string.conflict_title))
                    .setContentText(String.format(
                            mAvailableOfflineJobService.getString(R.string.conflict_description),
                            availableOfflineFile.getRemotePath())
                    )
                    .setAutoCancel(true);

            Intent showConflictActivityIntent = new Intent(mAvailableOfflineJobService, ConflictsResolveActivity.class);
            showConflictActivityIntent.setFlags(showConflictActivityIntent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_FROM_BACKGROUND);
            showConflictActivityIntent.putExtra(ConflictsResolveActivity.EXTRA_FILE, availableOfflineFile);
            showConflictActivityIntent.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, account);

            notificationBuilder.setContentIntent(
                    PendingIntent.getActivity(mAvailableOfflineJobService, (int) System.currentTimeMillis(),
                            showConflictActivityIntent, 0)
            );

            int notificationId = 0;

            // We need a notification id for each file in conflict, let's use the file id but in a safe way
            if ((int) availableOfflineFile.getFileId() >= Integer.MIN_VALUE && (int) availableOfflineFile.getFileId() <=
                    Integer.MAX_VALUE) {
                notificationId = (int) availableOfflineFile.getFileId();
            }

            notificationManager.notify(notificationId, notificationBuilder.build());
        }

        /**
         * Cancel the periodic job
         * @param jobId id of the job to cancel
         */
        private void cancelPeriodicJob(int jobId) {
            JobScheduler jobScheduler = (JobScheduler) mAvailableOfflineJobService.getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);

            jobScheduler.cancel(jobId);

            Log_OC.d(TAG, "No available offline files to check, cancelling the periodic job");
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
