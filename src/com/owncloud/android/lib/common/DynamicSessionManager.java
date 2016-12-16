package com.owncloud.android.lib.common;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;

import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import java.io.IOException;

/**
 * Dynamic implementation of {@link OwnCloudClientManager}.
 *
 * Wraps instances of {@link SingleSessionManager} and {@link SimpleFactoryManager} and delegates on one
 * or the other depending on the known version of the server corresponding to the {@link OwnCloudAccount}
 *
 * @author David A. Velasco
 */

public class DynamicSessionManager implements OwnCloudClientManager {

    private SimpleFactoryManager mSimpleFactoryManager = new SimpleFactoryManager();

    private SingleSessionManager mSingleSessionManager = new SingleSessionManager();

    @Override
    public OwnCloudClient getClientFor(OwnCloudAccount account, Context context)
        throws AccountUtils.AccountNotFoundException,
                OperationCanceledException, AuthenticatorException, IOException {

        OwnCloudVersion ownCloudVersion = null;
        if (account.getSavedAccount() != null) {
            ownCloudVersion = AccountUtils.getServerVersionForAccount(
                account.getSavedAccount(), context
            );
        }

        if (ownCloudVersion !=  null && ownCloudVersion.isSessionMonitoringSupported()) {
            return mSingleSessionManager.getClientFor(account, context);
        } else {
            return mSimpleFactoryManager.getClientFor(account, context);
        }
    }

    @Override
    public OwnCloudClient removeClientFor(OwnCloudAccount account) {
        OwnCloudClient clientRemovedFromFactoryManager = mSimpleFactoryManager.removeClientFor(account);
        OwnCloudClient clientRemovedFromSingleSessionManager = mSingleSessionManager.removeClientFor(account);
        if (clientRemovedFromSingleSessionManager != null) {
            return clientRemovedFromSingleSessionManager;
        } else {
            return clientRemovedFromFactoryManager;
        }
        // clientRemoved and clientRemoved2 should not be != null at the same time
    }

    @Override
    public void saveAllClients(Context context, String accountType)
        throws AccountUtils.AccountNotFoundException,
                AuthenticatorException, IOException, OperationCanceledException {
        mSimpleFactoryManager.saveAllClients(context, accountType);
        mSingleSessionManager.saveAllClients(context, accountType);
    }

}
