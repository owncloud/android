/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2011  Bartek Przybylski
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

package com.owncloud.android.syncadapter;

import android.accounts.Account;
import android.accounts.AccountsException;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.util.Pair;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import at.bitfire.dav4jvm.exception.DavException;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.SyncCapabilitiesOperation;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.presentation.ui.authentication.AuthenticatorConstants;
import com.owncloud.android.presentation.ui.authentication.LoginActivity;
import com.owncloud.android.ui.activity.ErrorsWhileCopyingHandlerActivity;
import com.owncloud.android.utils.NotificationUtils;
import timber.log.Timber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.owncloud.android.utils.NotificationConstantsKt.FILE_SYNC_NOTIFICATION_CHANNEL_ID;

/**
 * Implementation of {@link AbstractThreadedSyncAdapter} responsible for synchronizing
 * ownCloud files.
 * <p>
 * Performs a full synchronization of the account received in {@link #onPerformSync(Account, Bundle,
 * String, ContentProviderClient, SyncResult)}.
 */
public class FileSyncAdapter extends AbstractOwnCloudSyncAdapter {

    /**
     * Maximum number of failed folder synchronizations that are supported before finishing
     * the synchronization operation
     */
    private static final int MAX_FAILED_RESULTS = 3;

    public static final String EVENT_FULL_SYNC_START = FileSyncAdapter.class.getName() +
            ".EVENT_FULL_SYNC_START";
    public static final String EVENT_FULL_SYNC_END = FileSyncAdapter.class.getName() +
            ".EVENT_FULL_SYNC_END";
    public static final String EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED =
            FileSyncAdapter.class.getName() + ".EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED";

    public static final String EXTRA_ACCOUNT_NAME = FileSyncAdapter.class.getName() + ".EXTRA_ACCOUNT_NAME";
    public static final String EXTRA_FOLDER_PATH = FileSyncAdapter.class.getName() + ".EXTRA_FOLDER_PATH";
    public static final String EXTRA_SERVER_VERSION = FileSyncAdapter.class.getName() + ".EXTRA_SERVER_VERSION";
    public static final String EXTRA_RESULT = FileSyncAdapter.class.getName() + ".EXTRA_RESULT";

    private static final int MAX_REPEAT_COUNTER = 1;

    /**
     * Time stamp for the current synchronization process, used to distinguish fresh data
     */
    private long mCurrentSyncTime;

    /**
     * Flag made 'true' when a request to cancel the synchronization is received
     */
    private boolean mCancellation;

    /**
     * When 'true' the process was requested by the user through the user interface;
     * when 'false', it was requested automatically by the system
     */
    private boolean mIsManualSync;

    /**
     * Counter for failed operations in the synchronization process
     */
    private int mFailedResultsCounter;

    /**
     * Result of the last failed operation
     */
    private RemoteOperationResult mLastFailedResult;

    /**
     * Counter of conflicts found between local and remote files
     */
    private int mConflictsFound;

    /**
     * Counter of failed operations in synchronization of kept-in-sync files
     */
    private int mFailsInFavouritesFound;

    /**
     * Map of remote and local paths to files that where locally stored in a location out
     * of the ownCloud folder and couldn't be copied automatically into it
     */
    private Map<String, String> mForgottenLocalFiles;

    /**
     * {@link SyncResult} instance to return to the system when the synchronization finish
     */
    private SyncResult mSyncResult;

    /**
     * To send broadcast messages not visible out of the app
     */
    private LocalBroadcastManager mLocalBroadcastManager;

    /**
     * Creates a {@link FileSyncAdapter}
     * <p>
     * {@inheritDoc}
     */
    public FileSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    /**
     * Creates a {@link FileSyncAdapter}
     * <p>
     * {@inheritDoc}
     */
    public FileSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void onPerformSync(Account account, Bundle extras,
                                           String authority, ContentProviderClient providerClient,
                                           SyncResult syncResult) {

        mCancellation = false;
        mIsManualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        mFailedResultsCounter = 0;
        mLastFailedResult = null;
        mConflictsFound = 0;
        mFailsInFavouritesFound = 0;
        mForgottenLocalFiles = new HashMap<>();
        mSyncResult = syncResult;
        mSyncResult.fullSyncRequested = false;
        mSyncResult.delayUntil = (System.currentTimeMillis() / 1000) + 3 * 60 * 60; // avoid too many automatic
        // synchronizations

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getContext());

        this.setAccount(account);
        this.setContentProviderClient(providerClient);
        this.setStorageManager(new FileDataStorageManager(getContext(), account, providerClient));

        try {
            this.initClientForCurrentAccount();
        } catch (IOException | AccountsException e) {
            /// the account is unknown for the Synchronization Manager, unreachable this context,
            // or can not be authenticated; don't try this again
            mSyncResult.tooManyRetries = true;
            notifyFailedSynchronization();
            return;
        }

        Timber.d("Synchronization of ownCloud account " + account.name + " starting");
        sendLocalBroadcast(EVENT_FULL_SYNC_START, null, null);  // message to signal the start
        // of the synchronization to the UI
        try {
            updateCapabilities();
            mCurrentSyncTime = System.currentTimeMillis();
            if (!mCancellation) {
                synchronizeFolder(getStorageManager().getFileByPath(OCFile.ROOT_PATH), false);

            } else {
                Timber.d("Leaving synchronization before synchronizing the root folder because cancelation request");
            }

        } finally {
            // it's important making this although very unexpected errors occur;
            // that's the reason for the finally

            if (mFailedResultsCounter > 0 && mIsManualSync) {
                /// don't let the system synchronization manager retries MANUAL synchronizations
                //      (be careful: "MANUAL" currently includes the synchronization requested when
                //      a new account is created and when the user changes the current account)
                mSyncResult.tooManyRetries = true;

                /// notify the user about the failure of MANUAL synchronization
                notifyFailedSynchronization();
            }
            if (mConflictsFound > 0 || mFailsInFavouritesFound > 0) {
                notifyFailsInFavourites();
            }
            if (mForgottenLocalFiles.size() > 0) {
                notifyForgottenLocalFiles();
            }
            sendLocalBroadcast(EVENT_FULL_SYNC_END, null, mLastFailedResult);   // message to signal
            // the end to the UI
        }

    }

    /**
     * Called by system SyncManager when a synchronization is required to be cancelled.
     * <p>
     * Sets the mCancellation flag to 'true'. THe synchronization will be stopped later,
     * before a new folder is fetched. Data of the last folder synchronized will be still
     * locally saved.
     * <p>
     * See {@link #onPerformSync(Account, Bundle, String, ContentProviderClient, SyncResult)}
     * and {@link #synchronizeFolder(OCFile, boolean)}.
     */
    @Override
    public void onSyncCanceled() {
        Timber.d("Synchronization of " + getAccount().name + " has been requested to cancel");
        mCancellation = true;
        super.onSyncCanceled();
    }

    /**
     * Updates the local copy of capabilities information of the ownCloud server
     */
    private void updateCapabilities() {
        SyncCapabilitiesOperation getCapabilities = new SyncCapabilitiesOperation();
        RemoteOperationResult result = getCapabilities.execute(getStorageManager(), getContext());
        if (!result.isSuccess()) {
            mLastFailedResult = result;
        }
    }

    /**
     * Synchronizes the list of files contained in a folder identified with its remote path.
     * <p>
     * Fetches the list and properties of the files contained in the given folder, including their
     * properties, and updates the local database with them.
     * <p>
     * Enters in the child folders to synchronize their contents also, following a recursive
     * depth first strategy.
     *
     * @param folder   Folder to synchronize.
     * @param pushOnly When 'true', it's assumed that the folder did not change in the
     *                 server, so data will not be fetched. Only local changes of
     *                 available offline files will be pushed.
     */
    private void synchronizeFolder(OCFile folder, boolean pushOnly) {

        if (mFailedResultsCounter > MAX_FAILED_RESULTS || isFinisher(mLastFailedResult)) {
            return;
        }

        // folder synchronization
        SynchronizeFolderOperation synchFolderOp = new SynchronizeFolderOperation(
                getContext(),
                folder.getRemotePath(),
                getAccount(),
                mCurrentSyncTime,
                pushOnly,
                true,       // sync full account
                false       // only sync contents of available offline files
        );

        RemoteOperationResult result;
        boolean repeat;
        do {
            repeat = false;

            result = synchFolderOp.execute(getClient(), getStorageManager());

        } while (repeat);

        // synchronized folder -> notice to UI - ALWAYS, although !result.isSuccess
        sendLocalBroadcast(EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED, folder.getRemotePath(), result);

        // check the result of synchronizing the folder
        if (result.isSuccess() || result.getCode() == ResultCode.SYNC_CONFLICT) {

            if (result.getCode() == ResultCode.SYNC_CONFLICT) {
                mConflictsFound += synchFolderOp.getConflictsFound();
                mFailsInFavouritesFound += synchFolderOp.getFailsInFileSyncsFound();
            }
            if (synchFolderOp.getForgottenLocalFiles().size() > 0) {
                mForgottenLocalFiles.putAll(synchFolderOp.getForgottenLocalFiles());
            }
            if (result.isSuccess()) {
                // synchronize children folders 
                List<Pair<OCFile, Boolean>> children = synchFolderOp.getFoldersToVisit();
                // beware of the 'hidden' recursion here!
                syncSubfolders(children);
            }

        } else if (result.getCode() != ResultCode.FILE_NOT_FOUND) {
            // in failures, the statistics for the global result are updated
            if (RemoteOperationResult.ResultCode.UNAUTHORIZED.equals(result.getCode())) {
                mSyncResult.stats.numAuthExceptions++;

            } else if (result.getException() instanceof DavException) {
                mSyncResult.stats.numParseExceptions++;

            } else if (result.getException() instanceof IOException) {
                mSyncResult.stats.numIoExceptions++;
            }
            mFailedResultsCounter++;
            mLastFailedResult = result;

        } // else, ResultCode.FILE_NOT_FOUND is ignored, remote folder was
        // removed from other thread or other client during the synchronization,
        // before this thread fetched its contents

    }

    /**
     * Checks if a failed result should terminate the synchronization process immediately,
     * according to OUR OWN POLICY
     *
     * @param failedResult Remote operation result to check.
     * @return 'True' if the result should immediately finish the
     * synchronization
     */
    private boolean isFinisher(RemoteOperationResult failedResult) {
        if (failedResult != null) {
            RemoteOperationResult.ResultCode code = failedResult.getCode();
            return (code.equals(RemoteOperationResult.ResultCode.SSL_ERROR) ||
                    code.equals(RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) ||
                    code.equals(RemoteOperationResult.ResultCode.BAD_OC_VERSION) ||
                    code.equals(RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED) ||
                    code.equals(ResultCode.ACCOUNT_NOT_FOUND) ||
                    code.equals(ResultCode.ACCOUNT_EXCEPTION)
            );
        }
        return false;
    }

    /**
     * Triggers the synchronization of any folder contained in the list of received files.
     * <p>
     * Every subfolder comes with a boolean flag, set to true if the previous sync operation detected
     * that there are pending changes in the file.
     * <p>
     * Only folders that have pending changes in the server will be sync'd here.
     *
     * @param folders Subfolders to recursively synchronize, with boolean value signaling if there are pending
     *                changes to sync in the server.
     */
    private void syncSubfolders(List<Pair<OCFile, Boolean>> folders) {
        int i;
        Pair<OCFile, Boolean> pair;
        for (i = 0; i < folders.size() && !mCancellation; i++) {
            pair = folders.get(i);
            if (pair.first.isFolder()) {
                synchronizeFolder(pair.first, !pair.second);
            }
        }

        if (mCancellation && i < folders.size()) {
            Timber.d("Leaving synchronization before synchronizing " + folders.get(i).first.getRemotePath() + " due " +
                    "to cancelation request");
        }
    }

    /**
     * Sends a message to any application component interested in the progress of the
     * synchronization.
     *
     * @param event         Event in the process of synchronization to be notified.
     * @param dirRemotePath Remote path of the folder target of the event occurred.
     * @param result        Result of an individual folder synchronization,
     *                      if completed; may be null.
     */
    private void sendLocalBroadcast(String event, String dirRemotePath,
                                    RemoteOperationResult result) {
        Timber.d("Send broadcast %s", event);
        Intent intent = new Intent(event);
        intent.putExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME, getAccount().name);
        if (dirRemotePath != null) {
            intent.putExtra(FileSyncAdapter.EXTRA_FOLDER_PATH, dirRemotePath);
        }
        if (result != null) {
            intent.putExtra(FileSyncAdapter.EXTRA_RESULT, result);
        }
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Notifies the user about a failed synchronization through the status notification bar
     */
    private void notifyFailedSynchronization() {
        NotificationCompat.Builder notificationBuilder = createNotificationBuilder();
        boolean needsToUpdateCredentials = (
                mLastFailedResult != null &&
                        ResultCode.UNAUTHORIZED.equals(mLastFailedResult.getCode())
        );
        if (needsToUpdateCredentials) {
            // let the user update credentials with one click
            Intent updateAccountCredentials = new Intent(getContext(), LoginActivity.class);
            updateAccountCredentials.putExtra(AuthenticatorConstants.EXTRA_ACCOUNT, getAccount());
            updateAccountCredentials.putExtra(AuthenticatorConstants.EXTRA_ACTION,
                    AuthenticatorConstants.ACTION_UPDATE_EXPIRED_TOKEN);
            updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            updateAccountCredentials.addFlags(Intent.FLAG_FROM_BACKGROUND);
            notificationBuilder
                    .setTicker(i18n(R.string.sync_fail_ticker_unauthorized))
                    .setContentTitle(i18n(R.string.sync_fail_ticker_unauthorized))
                    .setContentIntent(PendingIntent.getActivity(
                            getContext(), (int) System.currentTimeMillis(), updateAccountCredentials,
                            PendingIntent.FLAG_ONE_SHOT
                    ))
                    .setContentText(i18n(R.string.sync_fail_content_unauthorized, getAccount().name));
        } else {
            notificationBuilder
                    .setTicker(i18n(R.string.sync_fail_ticker))
                    .setContentTitle(i18n(R.string.sync_fail_ticker))
                    .setContentText(i18n(R.string.sync_fail_content, getAccount().name));
        }

        showNotification(R.string.sync_fail_ticker, notificationBuilder);
    }

    /**
     * Notifies the user about conflicts and strange fails when trying to synchronize the contents
     * of kept-in-sync files.
     * <p>
     * By now, we won't consider a failed synchronization.
     */
    private void notifyFailsInFavourites() {
        if (mFailedResultsCounter > 0) {
            NotificationCompat.Builder notificationBuilder = createNotificationBuilder();
            notificationBuilder.setTicker(i18n(R.string.sync_fail_in_favourites_ticker));

            // TODO put something smart in the contentIntent below
            notificationBuilder
                    .setContentIntent(PendingIntent.getActivity(
                            getContext(), (int) System.currentTimeMillis(), new Intent(), 0
                    ))
                    .setContentTitle(i18n(R.string.sync_fail_in_favourites_ticker))
                    .setContentText(i18n(R.string.sync_fail_in_favourites_content,
                            mFailedResultsCounter + mConflictsFound, mConflictsFound));

            showNotification(R.string.sync_fail_in_favourites_ticker, notificationBuilder);
        } else {
            NotificationCompat.Builder notificationBuilder = createNotificationBuilder();
            notificationBuilder.setTicker(i18n(R.string.sync_conflicts_in_favourites_ticker));

            // TODO put something smart in the contentIntent below
            notificationBuilder
                    .setContentIntent(PendingIntent.getActivity(
                            getContext(), (int) System.currentTimeMillis(), new Intent(), 0
                    ))
                    .setContentTitle(i18n(R.string.sync_conflicts_in_favourites_ticker))
                    .setContentText(i18n(R.string.sync_conflicts_in_favourites_ticker, mConflictsFound));

            showNotification(R.string.sync_conflicts_in_favourites_ticker, notificationBuilder);
        }
    }

    /**
     * Notifies the user about local copies of files out of the ownCloud local directory that
     * were 'forgotten' because copying them inside the ownCloud local directory was not possible.
     * <p>
     * We don't want links to files out of the ownCloud local directory (foreign files) anymore.
     * It's easy to have synchronization problems if a local file is linked to more than one
     * remote file.
     * <p>
     * We won't consider a synchronization as failed when foreign files can not be copied to
     * the ownCloud local directory.
     */
    private void notifyForgottenLocalFiles() {
        NotificationCompat.Builder notificationBuilder = createNotificationBuilder();
        notificationBuilder.setTicker(i18n(R.string.sync_foreign_files_forgotten_ticker));

        /// includes a pending intent in the notification showing a more detailed explanation
        Intent explanationIntent = new Intent(getContext(), ErrorsWhileCopyingHandlerActivity.class);
        explanationIntent.putExtra(ErrorsWhileCopyingHandlerActivity.EXTRA_ACCOUNT, getAccount());
        ArrayList<String> remotePaths = new ArrayList<>();
        ArrayList<String> localPaths = new ArrayList<>();
        remotePaths.addAll(mForgottenLocalFiles.keySet());
        localPaths.addAll(mForgottenLocalFiles.values());
        explanationIntent.putExtra(ErrorsWhileCopyingHandlerActivity.EXTRA_LOCAL_PATHS, localPaths);
        explanationIntent.putExtra(ErrorsWhileCopyingHandlerActivity.EXTRA_REMOTE_PATHS, remotePaths);
        explanationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        notificationBuilder
                .setContentIntent(PendingIntent.getActivity(
                        getContext(), (int) System.currentTimeMillis(), explanationIntent, 0
                ))
                .setContentTitle(i18n(R.string.sync_foreign_files_forgotten_ticker))
                .setContentText(i18n(R.string.sync_foreign_files_forgotten_content,
                        mForgottenLocalFiles.size(), i18n(R.string.app_name)));

        showNotification(R.string.sync_foreign_files_forgotten_ticker, notificationBuilder);
    }

    /**
     * Creates a notification builder with some commonly used settings
     *
     * @return a notification builder with some commonly used settings.
     */
    private NotificationCompat.Builder createNotificationBuilder() {
        NotificationCompat.Builder notificationBuilder = NotificationUtils.newNotificationBuilder(getContext(),
                FILE_SYNC_NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true);
        return notificationBuilder;
    }

    /**
     * Builds and shows the notification
     *
     * @param id      Id for the notification to build.
     * @param builder Notification builder, already set up.
     */
    private void showNotification(int id, NotificationCompat.Builder builder) {

        NotificationManager mNotificationManager = ((NotificationManager) getContext().
                getSystemService(Context.NOTIFICATION_SERVICE));

        mNotificationManager.notify(id, builder.build());
    }

    /**
     * Shorthand translation
     *
     * @param key  String key.
     * @param args Arguments to replace in a formatted string.
     * @return
     */
    private String i18n(int key, Object... args) {
        return getContext().getString(key, args);
    }
}
