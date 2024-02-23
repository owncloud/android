/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.services;

import android.accounts.Account;
import android.accounts.AccountsException;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Pair;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.SingleSessionManager;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.CheckCurrentCredentialsOperation;
import com.owncloud.android.operations.common.SyncOperation;
import timber.log.Timber;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class OperationsService extends Service {

    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    public static final String EXTRA_SERVER_URL = "SERVER_URL";
    public static final String EXTRA_FILE = "FILE";

    public static final String ACTION_CHECK_CURRENT_CREDENTIALS = "CHECK_CURRENT_CREDENTIALS";

    private ConcurrentMap<Integer, Pair<RemoteOperation, RemoteOperationResult>>
            mUndispatchedFinishedOperations = new ConcurrentHashMap<>();

    private static class Target {
        public Uri mServerUrl;
        public Account mAccount;

        public Target(Account account, Uri serverUrl) {
            mAccount = account;
            mServerUrl = serverUrl;
        }
    }

    private ServiceHandler mOperationsHandler;
    private OperationsServiceBinder mOperationsBinder;

    /**
     * Service initialization
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("Creating service");

        /// First worker thread for most of operations
        HandlerThread thread = new HandlerThread("Operations thread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mOperationsHandler = new ServiceHandler(thread.getLooper(), this);
        mOperationsBinder = new OperationsServiceBinder(mOperationsHandler);
    }

    /**
     * Entry point to add a new operation to the queue of operations.
     * <p>
     * New operations are added calling to startService(), resulting in a call to this method.
     * This ensures the service will keep on working although the caller activity goes away.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("Starting command with id %s", startId);

        Message msg = mOperationsHandler.obtainMessage();
        msg.arg1 = startId;
        mOperationsHandler.sendMessage(msg);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Timber.v("Destroying service");

        mUndispatchedFinishedOperations.clear();

        mOperationsBinder = null;

        mOperationsHandler.getLooper().quit();
        mOperationsHandler = null;

        super.onDestroy();
    }

    /**
     * Provides a binder object that clients can use to perform actions on the queue of operations,
     * except the addition of new operations.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mOperationsBinder;
    }

    /**
     * Called when ALL the bound clients were unbound.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        mOperationsBinder.clearListeners();
        return false;   // not accepting rebinding (default behaviour)
    }

    /**
     * Binder to let client components to perform actions on the queue of operations.
     * <p/>
     * It provides by itself the available operations.
     */
    public class OperationsServiceBinder extends Binder {

        /**
         * Map of listeners that will be reported about the end of operations from a
         * {@link OperationsServiceBinder} instance
         */
        private final ConcurrentMap<OnRemoteOperationListener, Handler> mBoundListeners = new ConcurrentHashMap<>();

        private ServiceHandler mServiceHandler;

        OperationsServiceBinder(ServiceHandler serviceHandler) {
            mServiceHandler = serviceHandler;
        }

        void clearListeners() {
            mBoundListeners.clear();
        }

        /**
         * Adds a listener interested in being reported about the end of operations.
         *
         * @param listener        Object to notify about the end of operations.
         * @param callbackHandler {@link Handler} to access the listener without
         *                        breaking Android threading protection.
         */
        public void addOperationListener(OnRemoteOperationListener listener, Handler callbackHandler) {
            synchronized (mBoundListeners) {
                mBoundListeners.put(listener, callbackHandler);
            }
        }

        /**
         * Removes a listener from the list of objects interested in the being reported about
         * the end of operations.
         *
         * @param listener Object to notify about progress of transfer.
         */
        public void removeOperationListener(OnRemoteOperationListener listener) {
            synchronized (mBoundListeners) {
                mBoundListeners.remove(listener);
            }
        }

        /**
         * Creates and adds to the queue a new operation, as described by operationIntent.
         * <p>
         * Calls startService to make the operation is processed by the ServiceHandler.
         *
         * @param operationIntent Intent describing a new operation to queue and execute.
         * @return Identifier of the operation created, or null if failed.
         */
        public long queueNewOperation(Intent operationIntent) {
            Pair<Target, RemoteOperation> itemToQueue = newOperation(operationIntent);
            if (itemToQueue != null) {
                mServiceHandler.mPendingOperations.add(itemToQueue);
                Intent executeOperation = new Intent(OperationsService.this, OperationsService.class);
                startService(executeOperation);
                return itemToQueue.second.hashCode();
            } else {
                return Long.MAX_VALUE;
            }
        }

        public boolean dispatchResultIfFinished(int operationId, OnRemoteOperationListener listener) {
            Pair<RemoteOperation, RemoteOperationResult> undispatched =
                    mUndispatchedFinishedOperations.remove(operationId);
            if (undispatched != null) {
                listener.onRemoteOperationFinish(undispatched.first, undispatched.second);
                return true;
            } else {
                return !mServiceHandler.mPendingOperations.isEmpty();
            }
        }
    }

    /**
     * Operations worker. Performs the pending operations in the order they were requested.
     * <p>
     * Created with the Looper of a new thread, started in {@link OperationsService#onCreate()}.
     */
    private static class ServiceHandler extends Handler {
        // don't make it a final class, and don't remove the static ; lint will warn about a p
        // possible memory leak

        OperationsService mService;

        private final ConcurrentLinkedQueue<Pair<Target, RemoteOperation>> mPendingOperations =
                new ConcurrentLinkedQueue<>();
        private Target mLastTarget = null;
        private OwnCloudClient mOwnCloudClient = null;
        private FileDataStorageManager mStorageManager;

        ServiceHandler(Looper looper, OperationsService service) {
            super(looper);
            if (service == null) {
                throw new IllegalArgumentException("Received invalid NULL in parameter 'service'");
            }
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            nextOperation();
            Timber.d("Stopping after command with id %s", msg.arg1);
            mService.stopSelf(msg.arg1);
        }

        /**
         * Performs the next operation in the queue
         */
        private void nextOperation() {
            Pair<Target, RemoteOperation> next;
            synchronized (mPendingOperations) {
                next = mPendingOperations.peek();
            }

            if (next != null) {

                RemoteOperation currentOperation = next.second;
                RemoteOperationResult result;
                try {
                    /// prepare client object to send the request to the ownCloud server
                    if (mLastTarget == null || !mLastTarget.equals(next.first)) {
                        mLastTarget = next.first;
                        OwnCloudAccount ocAccount;
                        if (mLastTarget.mAccount != null) {
                            ocAccount = new OwnCloudAccount(mLastTarget.mAccount, mService);
                            mOwnCloudClient = SingleSessionManager.getDefaultSingleton().
                                    getClientFor(ocAccount, mService);

                            mStorageManager = new FileDataStorageManager(mLastTarget.mAccount);
                        } else {
                            OwnCloudCredentials credentials = null;
                            ocAccount = new OwnCloudAccount(mLastTarget.mServerUrl, credentials);

                            mOwnCloudClient = SingleSessionManager.getDefaultSingleton().
                                    getClientFor(ocAccount, mService);

                            mStorageManager = null;
                        }
                    }

                    /// perform the operation
                    if (currentOperation instanceof SyncOperation) {
                        result = ((SyncOperation) currentOperation).execute(mOwnCloudClient, mStorageManager);
                    } else {
                        result = currentOperation.execute(mOwnCloudClient);
                    }

                } catch (AccountsException | IOException e) {
                    if (mLastTarget.mAccount == null) {
                        Timber.e(e, "Error while trying to get authorization for a NULL account");
                    } else {
                        Timber.e(e, "Error while trying to get authorization for %s", mLastTarget.mAccount.name);
                    }
                    result = new RemoteOperationResult(e);

                } catch (Exception e) {
                    if (mLastTarget.mAccount == null) {
                        Timber.e(e, "Unexpected error for a NULL account");
                    } else {
                        Timber.e(e, "Unexpected error for %s", mLastTarget.mAccount.name);
                    }
                    result = new RemoteOperationResult(e);

                } finally {
                    synchronized (mPendingOperations) {
                        mPendingOperations.poll();
                    }
                }

                mService.dispatchResultToOperationListeners(currentOperation, result);
            }
        }
    }

    /**
     * Creates a new operation, as described by operationIntent.
     * <p>
     * TODO - move to ServiceHandler (probably)
     *
     * @param operationIntent Intent describing a new operation to queue and execute.
     * @return Pair with the new operation object and the information about its
     * target server.
     */
    private Pair<Target, RemoteOperation> newOperation(Intent operationIntent) {
        RemoteOperation operation = null;
        Target target = null;
        try {
            if (!operationIntent.hasExtra(EXTRA_ACCOUNT) &&
                    !operationIntent.hasExtra(EXTRA_SERVER_URL)) {
                Timber.e("Not enough information provided in intent");

            } else {
                Account account = operationIntent.getParcelableExtra(EXTRA_ACCOUNT);
                String serverUrl = operationIntent.getStringExtra(EXTRA_SERVER_URL);
                target = new Target(
                        account,
                        (serverUrl == null) ? null : Uri.parse(serverUrl)
                );

                String action = operationIntent.getAction();
                if (action != null) {
                    switch (action) {
                        case ACTION_CHECK_CURRENT_CREDENTIALS:
                            // Check validity of currently stored credentials for a given account
                            operation = new CheckCurrentCredentialsOperation(account);

                            break;
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            Timber.e(e, "Bad information provided in intent: %s", e.getMessage());
            operation = null;
        }

        if (operation != null) {
            return new Pair<>(target, operation);
        } else {
            return null;
        }
    }

    /**
     * Notifies the currently subscribed listeners about the end of an operation.
     *
     * @param operation Finished operation.
     * @param result    Result of the operation.
     */
    protected void dispatchResultToOperationListeners(
            final RemoteOperation operation, final RemoteOperationResult result
    ) {
        int count = 0;
        for (OnRemoteOperationListener listener : mOperationsBinder.mBoundListeners.keySet()) {
            final Handler handler = mOperationsBinder.mBoundListeners.get(listener);
            if (handler != null) {
                handler.post(() -> listener.onRemoteOperationFinish(operation, result));
                count += 1;
            }
        }
        if (count == 0) {
            Pair<RemoteOperation, RemoteOperationResult> undispatched = new Pair<>(operation, result);
            mUndispatchedFinishedOperations.put(operation.hashCode(), undispatched);
        }
        Timber.d("Called " + count + " listeners");
    }
}
