package com.owncloud.android.lib.common;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;

public class SimpleFactoryManager implements OwnCloudClientManager {
    
	private static final String TAG = OwnCloudClientManager.class.getSimpleName();

	@Override
	public OwnCloudClient getClientFor(OwnCloudAccount account, Context context) {
		Log.d(TAG, "getClientFor(OwnCloudAccount ... : ");
		OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(
				account.getBaseUri(), 
				context.getApplicationContext(),
				true);

		Log.d(TAG, "    new client " + client.hashCode());
		client.setCredentials(account.getCredentials());
		return client;
	}

	
	@Override
	public OwnCloudClient getClientFor(Account savedAccount, Context context)
			throws OperationCanceledException, AuthenticatorException, AccountNotFoundException,
			IOException {
		Log.d(TAG, "getClientFor(Account ... : ");

		OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(
				savedAccount, 
				context.getApplicationContext());
		
		Log.d(TAG, "    new client " + client.hashCode());
		return client;
	}

	@Override
	public OwnCloudClient getClientFor(Uri serverBaseUri, OwnCloudCredentials credentials,
			Context context) {
		Log.d(TAG, "getClientFor(Uri ... : ");
		
		OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(
				serverBaseUri, 
				context.getApplicationContext(),
				true);

		client.setCredentials(credentials);
		Log.d(TAG, "    new client " + client.hashCode());
		return client;
	}

	@Override
	public void saveAllClients(Context context, String accountType) {
		// TODO Auto-generated method stub

	}

	@Override
	public OwnCloudClient removeClientFor(Account account, Context context) {
		// TODO Auto-generated method stub
		return null;
	}

}
