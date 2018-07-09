/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud GmbH.
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

package com.owncloud.android.operations.common;

import android.content.Context;
import android.os.Handler;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;


/**
 * Operation which execution involves both interactions with an ownCloud server and
 * with local data in the device.
 * 
 * Provides methods to onExecute the operation both synchronously or asynchronously.
 */
public abstract class SyncOperation<T> extends RemoteOperation<T> {

    //private static final String TAG = SyncOperation.class.getSimpleName();

    private FileDataStorageManager mStorageManager;
    
    public FileDataStorageManager getStorageManager() {
        return mStorageManager;
    }


    /**
     * Synchronously executes the operation on the received ownCloud account.
     * 
     * Do not call this method from the main thread.
     * 
     * This method should be used whenever an ownCloud account is available, instead of
     * {@link #execute(OwnCloudClient, com.owncloud.android.datamodel.FileDataStorageManager)}.
     * 
     * @param storageManager
     * @param context           Android context for the component calling the method.
     * @return                  Result of the operation.
     */
    public RemoteOperationResult<T> execute(FileDataStorageManager storageManager, Context context) {
        if (storageManager == null) {
            throw new IllegalArgumentException("Trying to onExecute a sync operation with a " +
                    "NULL storage manager");
        }
        if (storageManager.getAccount() == null) {
            throw new IllegalArgumentException("Trying to onExecute a sync operation with a " +
                    "storage manager for a NULL account");
        }
        mStorageManager = storageManager;
        return super.execute(mStorageManager.getAccount(), context);
    }
    

    /**
     * Synchronously executes the remote operation
     *
     * Do not call this method from the main thread.
     * 
     * @param client            Client object to reach an ownCloud server during the execution of the operation.
     * @param storageManager    Instance of local repository to sync with remote.
     * @return                  Result of the operation.
     */
    public RemoteOperationResult<T> execute(OwnCloudClient client,
                                         FileDataStorageManager storageManager) {
        if (storageManager == null)
            throw new IllegalArgumentException("Trying to onExecute a sync operation with a " +
                    "NULL storage manager");
        mStorageManager = storageManager;
        return super.execute(client);
    }


    /**
     * Asynchronously executes the remote operation
     *
     * This method should be used whenever an ownCloud account is available,
     * instead of {@link #execute(OwnCloudClient, OnRemoteOperationListener, Handler))}.
     *
     * @param storageManager    Instance of local repository to sync with remote.
     * @param context           Android context for the component calling the method.
     * @param listener          Listener to be notified about the execution of the operation.
     * @param listenerHandler   Handler associated to the thread where the methods of the listener
     *                          objects must be called.
     * @return                  Thread were the remote operation is executed.
     */
    public Thread execute(FileDataStorageManager storageManager, Context context,
                          OnRemoteOperationListener listener, Handler listenerHandler) {
        if (storageManager == null) {
            throw new IllegalArgumentException("Trying to onExecute a sync operation " +
                "with a NULL storage manager");
        }
        if (storageManager.getAccount() == null) {
            throw new IllegalArgumentException("Trying to onExecute a sync operation with a" +
                " storage manager for a NULL account");
        }
        mStorageManager = storageManager;
        return super.execute(mStorageManager.getAccount(), context, listener, listenerHandler);
    }

    /**
     * Asynchronously executes the remote operation
     *
     * @param client            Client object to reach an ownCloud server during the
     *                          execution of the operation.
     * @param storageManager    Instance of local repository to sync with remote.
     * @param listener          Listener to be notified about the execution of the operation.
     * @param listenerHandler   Handler associated to the thread where the methods of
     *                          the listener objects must be called.
     * @return                  Thread were the remote operation is executed.
     */
    public Thread execute(OwnCloudClient client, FileDataStorageManager storageManager,
                          OnRemoteOperationListener listener, Handler listenerHandler) {
        if (storageManager == null) {
            throw new IllegalArgumentException("Trying to onExecute a sync operation " +
                    "with a NULL storage manager");
        }
        mStorageManager = storageManager;
        return super.execute(client, listener, listenerHandler);
    }

}
