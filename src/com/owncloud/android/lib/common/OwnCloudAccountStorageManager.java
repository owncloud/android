package com.owncloud.android.lib.common;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.net.Uri;

import com.owncloud.android.lib.common.accounts.AccountUtils;

import java.io.IOException;

/**
 * OwnCloud Account
 *
 * @author David A. Velasco
 */
public class OwnCloudAccountStorageManager {

    /**
     * Constructor for already saved OC accounts.
     *
     * Do not use for anonymous credentials.
     */
    public static OwnCloudAccount getOwnCloudAccount(Account savedAccount, Context context)
        throws AccountUtils.AccountNotFoundException {

        if (savedAccount == null) {
            throw new IllegalArgumentException("Parameter 'savedAccount' cannot be null");
        }

        if (context == null) {
            throw new IllegalArgumentException("Parameter 'context' cannot be null");
        }

        OwnCloudAccount account = new OwnCloudAccount(
            Uri.parse(AccountUtils.getBaseUrlForAccount(context, savedAccount))
        );

        AccountManager ama = AccountManager.get(context.getApplicationContext());
        String displayName = ama.getUserData(savedAccount, AccountUtils.Constants.KEY_DISPLAY_NAME);
        if (displayName != null && displayName.length() > 0) {
            account.setDisplayName(displayName);
        }

        return account;
    }

}
