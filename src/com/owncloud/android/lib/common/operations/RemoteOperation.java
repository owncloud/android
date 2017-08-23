/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2017 ownCloud GmbH.
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.common.operations;

import android.accounts.Account;
import android.accounts.AccountsException;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.Handler;

import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.IOException;


/**
 * Operation which execution involves one or several interactions with an ownCloud server.
 *
 * Provides methods to execute the operation both synchronously or asynchronously.
 *
 * @author David A. Velasco
 * @author David González Verdugo
 */
public abstract class RemoteOperation implements Runnable {

    private static final String TAG = RemoteOperation.class.getSimpleName();

    /**
     * OCS API header name
     */
    public static final String OCS_API_HEADER = "OCS-APIREQUEST";

    /**
     * OCS API header value
     */
    public static final String OCS_API_HEADER_VALUE = "true";

    /**
     * ownCloud account in the remote ownCloud server to operate
     */
    private Account mAccount = null;

    /**
     * Android Application context
     */
    private Context mContext = null;

    /**
     * Object to interact with the remote server
     */
    private OwnCloudClient mClient = null;

    /**
     * Callback object to notify about the execution of the remote operation
     */
    private OnRemoteOperationListener mListener = null;

    /**
     * Handler to the thread where mListener methods will be called
     */
    private Handler mListenerHandler = null;

    /**
     * Abstract method to implement the operation in derived classes.
     */
    protected abstract RemoteOperationResult run(OwnCloudClient client);


    /**
     * Synchronously executes the remote operation on the received ownCloud account.
     *
     * Do not call this method from the main thread.
     *
     * This method should be used whenever an ownCloud account is available, instead of
     * {@link #execute(OwnCloudClient)}.
     *
     * @param account ownCloud account in remote ownCloud server to reach during the
     *                execution of the operation.
     * @param context Android context for the component calling the method.
     * @return Result of the operation.
     */
    public RemoteOperationResult execute(Account account, Context context) {
        if (account == null)
            throw new IllegalArgumentException("Trying to execute a remote operation with a NULL " +
                    "Account");
        if (context == null)
            throw new IllegalArgumentException("Trying to execute a remote operation with a NULL " +
                    "Context");
        mAccount = account;
        mContext = context.getApplicationContext();

        return runOperation();
    }


    /**
     * Synchronously executes the remote operation
     *
     * Do not call this method from the main thread.
     *
     * @param client Client object to reach an ownCloud server during the execution of
     *               the operation.
     * @return Result of the operation.
     */
    public RemoteOperationResult execute(OwnCloudClient client) {
        if (client == null)
            throw new IllegalArgumentException("Trying to execute a remote operation with a NULL " +
                    "OwnCloudClient");
        mClient = client;
        if (client.getAccount() != null) {
            mAccount = client.getAccount().getSavedAccount();
        }
        mContext = client.getContext();

        return runOperation();
    }


    /**
     * Asynchronously executes the remote operation
     *
     * This method should be used whenever an ownCloud account is available,
     * instead of {@link #execute(OwnCloudClient, OnRemoteOperationListener, Handler))}.
     *
     * @param account         ownCloud account in remote ownCloud server to reach during the
     *                        execution of the operation.
     * @param context         Android context for the component calling the method.
     * @param listener        Listener to be notified about the execution of the operation.
     * @param listenerHandler Handler associated to the thread where the methods of the listener
     *                        objects must be called.
     * @return Thread were the remote operation is executed.
     */
    public Thread execute(Account account, Context context,
                          OnRemoteOperationListener listener, Handler listenerHandler) {

        if (account == null)
            throw new IllegalArgumentException
                    ("Trying to execute a remote operation with a NULL Account");
        if (context == null)
            throw new IllegalArgumentException
                    ("Trying to execute a remote operation with a NULL Context");
        // mAccount and mContext in the runnerThread to create below
        mAccount = account;
        mContext = context.getApplicationContext();
        mClient = null;     // the client instance will be created from

        mListener = listener;

        mListenerHandler = listenerHandler;

        Thread runnerThread = new Thread(this);
        runnerThread.start();
        return runnerThread;
    }


    /**
     * Asynchronously executes the remote operation
     *
     * @param client          Client object to reach an ownCloud server
     *                        during the execution of the operation.
     * @param listener        Listener to be notified about the execution of the operation.
     * @param listenerHandler Handler, if passed in, associated to the thread where the methods of
     *                        the listener objects must be called.
     * @return Thread were the remote operation is executed.
     */
    public Thread execute(OwnCloudClient client,
                          OnRemoteOperationListener listener, Handler listenerHandler) {
        if (client == null) {
            throw new IllegalArgumentException
                    ("Trying to execute a remote operation with a NULL OwnCloudClient");
        }
        mClient = client;
        if (client.getAccount() != null) {
            mAccount = client.getAccount().getSavedAccount();
        }
        mContext = client.getContext();

        if (listener == null) {
            throw new IllegalArgumentException
                    ("Trying to execute a remote operation asynchronously " +
                            "without a listener to notiy the result");
        }
        mListener = listener;

        if (listenerHandler != null) {
            mListenerHandler = listenerHandler;
        }

        Thread runnerThread = new Thread(this);
        runnerThread.start();
        return runnerThread;
    }


    /**
     * Asynchronous execution of the operation
     * started by {@link RemoteOperation#execute(OwnCloudClient,
     * OnRemoteOperationListener, Handler)},
     * and result posting.
     */
    @Override
    public final void run() {

        final RemoteOperationResult resultToSend = runOperation();

        if (mAccount != null && mContext != null) {
            // Save Client Cookies
            AccountUtils.saveClient(mClient, mAccount, mContext);
        }

        if (mListenerHandler != null && mListener != null) {
            mListenerHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onRemoteOperationFinish(RemoteOperation.this, resultToSend);
                }
            });
        } else if (mListener != null) {
            mListener.onRemoteOperationFinish(RemoteOperation.this, resultToSend);
        }
    }

    /**
     * Run operation for asynchronous or synchronous 'execute' method.
     *
     * Considers and performs silent refresh of account credentials if possible, and if
     * {@link RemoteOperation#setSilentRefreshOfAccountCredentials(boolean)} was called with
     * parameter 'true' before the execution.
     *
     * @return      Remote operation result
     */
    private RemoteOperationResult runOperation() {

        RemoteOperationResult result;

        try {
            grantOwnCloudClient();
            result = run(mClient);

        } catch (AccountsException | IOException e) {
            Log_OC.e(TAG, "Error while trying to access to " + mAccount.name, e);
            result = new RemoteOperationResult(e);
        }

        return result;
    }


    private void grantOwnCloudClient() throws
        AccountUtils.AccountNotFoundException, OperationCanceledException, AuthenticatorException, IOException {
        if (mClient == null) {
            if (mAccount != null && mContext != null) {
                OwnCloudAccount ocAccount = new OwnCloudAccount(mAccount, mContext);
                mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                    getClientFor(ocAccount, mContext);

            } else {
                throw new IllegalStateException("Trying to run a remote operation " +
                    "asynchronously with no client and no chance to create one (no account)");
            }
        }
    }

    /**
     * Returns the current client instance to access the remote server.
     *
     * @return Current client instance to access the remote server.
     */
    public final OwnCloudClient getClient() {
        return mClient;
    }

}