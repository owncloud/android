package com.owncloud.android.lib.common;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;

import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;

public class SimpleFactoryManager implements OwnCloudClientManager {

    
	@Override
	public OwnCloudClient getClientFor(OwnCloudAccount account, Context context) {
		OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(
				account.getBaseUri(), 
				context.getApplicationContext(),
				true);

		client.setCredentials(account.getCredentials());
		return client;
	}

	
	@Override
	public OwnCloudClient getClientFor(Account savedAccount, Context context)
			throws OperationCanceledException, AuthenticatorException, AccountNotFoundException,
			IOException {
		
		return OwnCloudClientFactory.createOwnCloudClient(
				savedAccount, 
				context.getApplicationContext());
	}

	@Override
	public OwnCloudClient getClientFor(Uri serverBaseUri, OwnCloudCredentials credentials,
			Context context) {
		
		OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(
				serverBaseUri, 
				context.getApplicationContext(),
				true);

		client.setCredentials(credentials);
		return client;
	}

	@Override
	public void saveClient(Account savedAccount, Context context) {
		// TODO Auto-generated method stub
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
