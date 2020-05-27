/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
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
import android.accounts.AccountManager;
import android.content.res.Resources;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.domain.UseCaseResult;
import com.owncloud.android.domain.exceptions.FileNotFoundException;
import com.owncloud.android.domain.user.model.UserAvatar;
import com.owncloud.android.domain.user.model.UserInfo;
import com.owncloud.android.domain.user.model.UserQuota;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.operations.common.UseCaseHelper;
import timber.log.Timber;

/**
 * Get and save user's profile from the server.
 * <p>
 * Currently only retrieves the display name.
 */
public class GetUserProfileOperation extends SyncOperation {
    /**
     * Constructor
     */
    GetUserProfileOperation() {
    }

    /**
     * Performs the operation.
     * <p>
     * Target user account is implicit in 'client'.
     * <p>
     * Stored account is implicit in {@link #getStorageManager()}.
     *
     * @return Result of the operation. If successful, includes an instance of
     * {@link String} with the display name retrieved from the server.
     * Call {@link RemoteOperationResult#getData()}.get(0) to get it.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        try {
            /// get display name
            UseCaseHelper useCaseHelper = new UseCaseHelper();
            UseCaseResult<UserInfo> useCaseResult = useCaseHelper.getUserInfo();
            if (useCaseResult.getDataOrNull() != null) {
                // store display name with account data
                AccountManager accountManager = AccountManager.get(MainApp.Companion.getAppContext());
                UserInfo userInfo = useCaseResult.getDataOrNull();
                Timber.d("User info recovered from UseCaseHelper:GetUserInfo -> %s", userInfo.toString());
                Account storedAccount = getStorageManager().getAccount();
                accountManager.setUserData(
                        storedAccount,
                        AccountUtils.Constants.KEY_DISPLAY_NAME,    // keep also there, for the moment
                        userInfo.getDisplayName()
                );
                accountManager.setUserData(
                        storedAccount,
                        AccountUtils.Constants.KEY_ID,
                        userInfo.getId()
                );

                /// get quota
                UseCaseResult<UserQuota> quotaUseCaseResult = useCaseHelper.getUserQuota(storedAccount.name);
                if (quotaUseCaseResult.getDataOrNull() != null) {
                    // store display name with account data
                    UserQuota userQuotaResult = quotaUseCaseResult.getDataOrNull();
                    Timber.d("User quota recovered from UseCaseHelper:GetUserQuota -> %s", userQuotaResult.toString());

                    int dimension = getAvatarDimension();

                    /// get avatar (optional for success)
                    UseCaseResult<UserAvatar> avatarUseCaseResult = useCaseHelper.getUserAvatar(storedAccount.name);
                    if (avatarUseCaseResult.getDataOrNull() != null) {
                        // store display name with account data
                        UserAvatar userAvatar = avatarUseCaseResult.getDataOrNull();

                        byte[] avatarData = userAvatar.getAvatarData();
                        String avatarKey = ThumbnailsCacheManager.addAvatarToCache(
                                storedAccount.name,
                                avatarData,
                                dimension
                        );

                        Timber.d("User avatar saved into cache -> %s", avatarKey);

                    } else if (avatarUseCaseResult.getThrowableOrNull() instanceof FileNotFoundException) {
                        Timber.i("No avatar available, removing cached copy");
                        ThumbnailsCacheManager.removeAvatarFromCache(storedAccount.name);

                    }   // others are ignored, including 304 (not modified), so the avatar is only stored
                    // if changed in the server :D

                    return new RemoteOperationResult<>(RemoteOperationResult.ResultCode.OK);

                } else {
                    return new RemoteOperationResult<>(RemoteOperationResult.ResultCode.UNKNOWN_ERROR);
                }
            } else {
                return new RemoteOperationResult<>(RemoteOperationResult.ResultCode.UNKNOWN_ERROR);
            }
        } catch (Exception e) {
            Timber.e(e, "Exception while getting user profile: ");
            return new RemoteOperationResult(e);
        }
    }

    /**
     * Converts size of file icon from dp to pixel
     *
     * @return int
     */
    private int getAvatarDimension() {
        // Converts dp to pixel
        Resources r = MainApp.Companion.getAppContext().getResources();
        return Math.round(r.getDimension(R.dimen.file_avatar_size));
    }
}