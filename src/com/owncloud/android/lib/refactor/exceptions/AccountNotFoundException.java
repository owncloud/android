package com.owncloud.android.lib.refactor.exceptions;

import android.accounts.Account;
import android.accounts.AccountsException;

public class AccountNotFoundException extends AccountsException {

    /**
     * Generated - should be refreshed every time the class changes!!
     */
    private static final long serialVersionUID = -1684392454778508693L;

    private Account mFailedAccount;

    public AccountNotFoundException(Account failedAccount, String message, Throwable cause) {
        super(message, cause);
        mFailedAccount = failedAccount;
    }

    public Account getFailedAccount() {
        return mFailedAccount;
    }
}