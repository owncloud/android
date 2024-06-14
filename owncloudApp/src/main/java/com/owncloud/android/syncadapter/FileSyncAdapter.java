/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2024 ownCloud GmbH.
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.owncloud.android.R;
import com.owncloud.android.domain.UseCaseResult;
import com.owncloud.android.domain.capabilities.usecases.RefreshCapabilitiesFromServerAsyncUseCase;
import com.owncloud.android.domain.exceptions.UnauthorizedException;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.domain.files.usecases.GetPersonalRootFolderForAccountUseCase;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.usecases.synchronization.SynchronizeFolderUseCase;
import com.owncloud.android.utils.NotificationUtils;
import kotlin.Lazy;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

import java.io.IOException;

import static com.owncloud.android.utils.NotificationConstantsKt.FILE_SYNC_NOTIFICATION_CHANNEL_ID;
import static org.koin.java.KoinJavaComponent.inject;

/**
 * Implementation of {@link AbstractThreadedSyncAdapter} responsible for synchronizing
 * ownCloud files.
 * <p>
 * Performs a full synchronization of the account received in {@link #onPerformSync(Account, Bundle,
 * String, ContentProviderClient, SyncResult)}.
 */
public class FileSyncAdapter extends AbstractOwnCloudSyncAdapter {

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

    /**
     * Flag made 'true' when a request to cancel the synchronization is received
     */
    private boolean mCancellation;

    /**
     * Counter for failed operations in the synchronization process
     */
    private int mFailedResultsCounter;

    /**
     * Result of the last failed operation
     */
    private Throwable mLastFailedThrowable;

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
     * {@inheritDoc}
     */
    @Override
    public synchronized void onPerformSync(Account account, Bundle extras,
                                           String authority, ContentProviderClient providerClient,
                                           SyncResult syncResult) {

        mCancellation = false;
        /*
         * When 'true' the process was requested by the user through the user interface;
         * when 'false', it was requested automatically by the system
         */
        boolean isManualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        mFailedResultsCounter = 0;
        mSyncResult = syncResult;
        mSyncResult.fullSyncRequested = false;
        mSyncResult.delayUntil = (System.currentTimeMillis() / 1000) + 3 * 60 * 60; // avoid too many automatic
        // synchronizations

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getContext());

        this.setAccount(account);

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
            if (!mCancellation) {
                @NotNull Lazy<GetPersonalRootFolderForAccountUseCase> getRootFolderPersonalUseCaseLazy =
                        inject(GetPersonalRootFolderForAccountUseCase.class);
                GetPersonalRootFolderForAccountUseCase.Params params = new GetPersonalRootFolderForAccountUseCase.Params(account.name);

                OCFile rootFolder = getRootFolderPersonalUseCaseLazy.getValue().invoke(params);
                if (rootFolder != null) {
                    synchronizeFolder(rootFolder);
                }

            } else {
                Timber.d("Leaving synchronization before synchronizing the root folder because cancelation request");
            }

        } finally {
            // it's important making this although very unexpected errors occur;
            // that's the reason for the finally

            if (mFailedResultsCounter > 0 && isManualSync) {
                /// don't let the system synchronization manager retries MANUAL synchronizations
                //      (be careful: "MANUAL" currently includes the synchronization requested when
                //      a new account is created and when the user changes the current account)
                mSyncResult.tooManyRetries = true;

                /// notify the user about the failure of MANUAL synchronization
                notifyFailedSynchronization();
            }
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
     * and {@link #synchronizeFolder(OCFile)}.
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
        @NotNull Lazy<RefreshCapabilitiesFromServerAsyncUseCase> refreshCapabilitiesFromServerAsyncUseCase =
                inject(RefreshCapabilitiesFromServerAsyncUseCase.class);
        RefreshCapabilitiesFromServerAsyncUseCase.Params params = new RefreshCapabilitiesFromServerAsyncUseCase.Params(getAccount().name);
        UseCaseResult<Unit> useCaseResult = refreshCapabilitiesFromServerAsyncUseCase.getValue().invoke(params);

        if (useCaseResult.isError()) {
            mLastFailedThrowable = useCaseResult.getThrowableOrNull();
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
     * @param folder Folder to synchronize.
     */
    private void synchronizeFolder(OCFile folder) {

        // Discover full account
        @NotNull Lazy<SynchronizeFolderUseCase> synchronizeFolderUseCase =
                inject(SynchronizeFolderUseCase.class);
        SynchronizeFolderUseCase.Params params = new SynchronizeFolderUseCase.Params(
                folder.getRemotePath(),
                folder.getOwner(),
                folder.getSpaceId(),
                SynchronizeFolderUseCase.SyncFolderMode.REFRESH_FOLDER_RECURSIVELY,
                false);
        UseCaseResult<Unit> useCaseResult;

        useCaseResult = synchronizeFolderUseCase.getValue().invoke(params);

        // in failures, the statistics for the global result are updated
        if (useCaseResult.getThrowableOrNull() != null) {
            if (useCaseResult.getThrowableOrNull() instanceof UnauthorizedException) {
                mSyncResult.stats.numAuthExceptions++;
            }
            mFailedResultsCounter++;
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
    private void sendLocalBroadcast(String event, String dirRemotePath, RemoteOperationResult result) {
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
                mLastFailedThrowable != null &&
                        mLastFailedThrowable instanceof UnauthorizedException
        );
        if (needsToUpdateCredentials) {
            // let the user update credentials with one click
            PendingIntent pendingIntentToRefreshCredentials =
                    NotificationUtils.INSTANCE.composePendingIntentToRefreshCredentials(getContext(), getAccount());

            notificationBuilder
                    .setTicker(i18n(R.string.sync_fail_ticker_unauthorized))
                    .setContentTitle(i18n(R.string.sync_fail_ticker_unauthorized))
                    .setContentIntent(pendingIntentToRefreshCredentials)
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
     */
    private String i18n(int key, Object... args) {
        return getContext().getString(key, args);
    }
}
