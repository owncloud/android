/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
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

package com.owncloud.android.operations;

import android.accounts.Account;

import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CheckPathExistenceRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;

/**
 * Checks validity of currently stored credentials for a given OC account
 */
public class CheckCurrentCredentialsOperation extends SyncOperation<Account> {

    private Account mAccount;

    public CheckCurrentCredentialsOperation(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("NULL account");
        }
        mAccount = account;
    }

    @Override
    protected RemoteOperationResult<Account> run(OwnCloudClient client) {
        if (!getStorageManager().getAccount().name.equals(mAccount.name)) {
            return new RemoteOperationResult<>(new IllegalStateException(
                    "Account to validate is not the account connected to!"));
        } else {
            RemoteOperation checkPathExistenceOperation = new CheckPathExistenceRemoteOperation(OCFile.ROOT_PATH, false);
            final RemoteOperationResult existenceCheckResult = checkPathExistenceOperation.execute(client);
            final RemoteOperationResult<Account> result
                    = new RemoteOperationResult<>(existenceCheckResult.getCode());
            result.setData(mAccount);
            return result;
        }
    }
}