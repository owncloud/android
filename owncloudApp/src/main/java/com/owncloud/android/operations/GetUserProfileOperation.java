/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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
import com.owncloud.android.datamodel.UserProfile;
import com.owncloud.android.datamodel.UserProfilesRepository;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.GetRemoteUserAvatarOperation;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation.UserInfo;
import com.owncloud.android.lib.resources.users.GetRemoteUserQuotaOperation;
import com.owncloud.android.lib.resources.users.GetRemoteUserQuotaOperation.RemoteQuota;
import com.owncloud.android.operations.common.SyncOperation;

/**
 * Get and save user's profile from the server.
 *
 * Currently only retrieves the display name.
 */
public class GetUserProfileOperation extends SyncOperation {

    private static final String TAG = GetUserProfileOperation.class.getName();

    private String mRemotePath;

    /**
     * Constructor
     *
     * @param remotePath Remote path of the file.
     */
    public GetUserProfileOperation(String remotePath) {
        mRemotePath = remotePath;
    }

    /**
     * Performs the operation.
     *
     * Target user account is implicit in 'client'.
     *
     * Stored account is implicit in {@link #getStorageManager()}.
     *
     * @return Result of the operation. If successful, includes an instance of
     *              {@link String} with the display name retrieved from the server.
     *              Call {@link RemoteOperationResult#getData()}.get(0) to get it.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        UserProfile userProfile;

        try {
            /// get display name
            GetRemoteUserInfoOperation getDisplayName = new GetRemoteUserInfoOperation();
            final RemoteOperationResult<UserInfo> userInfoOperationResult = getDisplayName.execute(client);

            UserProfilesRepository userProfilesRepository = UserProfilesRepository.getUserProfilesRepository();

            if (userInfoOperationResult.isSuccess()) {
                // store display name with account data
                AccountManager accountManager = AccountManager.get(MainApp.Companion.getAppContext());
                UserInfo userInfo = userInfoOperationResult.getData();
                Account storedAccount = getStorageManager().getAccount();
                accountManager.setUserData(
                        storedAccount,
                        AccountUtils.Constants.KEY_DISPLAY_NAME,    // keep also there, for the moment
                        userInfo.mDisplayName
                );
                accountManager.setUserData(
                        storedAccount,
                        AccountUtils.Constants.KEY_ID,
                        userInfo.mId
                );

                // map user info into UserProfile instance
                userProfile = new UserProfile(
                        storedAccount.name,
                        userInfo.mId,
                        userInfo.mDisplayName,
                        userInfo.mEmail
                );

                /// get quota
                GetRemoteUserQuotaOperation getRemoteUserQuotaOperation = new GetRemoteUserQuotaOperation(mRemotePath);

                final RemoteOperationResult<RemoteQuota> quotaOperationResult =
                        getRemoteUserQuotaOperation.execute(client);

                if (quotaOperationResult.isSuccess()) {

                    RemoteQuota remoteQuota = quotaOperationResult.getData();

                    UserProfile.UserQuota userQuota = new UserProfile.UserQuota(
                            remoteQuota.getFree(),
                            remoteQuota.getRelative(),
                            remoteQuota.getTotal(),
                            remoteQuota.getUsed()
                    );

                    userProfile.setQuota(userQuota);

                    /// get avatar (optional for success)
                    int dimension = getAvatarDimension();
                    UserProfile.UserAvatar currentUserAvatar = userProfilesRepository.getAvatar(storedAccount.name);

                    GetRemoteUserAvatarOperation getAvatarOperation = new GetRemoteUserAvatarOperation(
                            dimension,
                            (currentUserAvatar == null)
                                    ? ""
                                    : currentUserAvatar.getEtag());

                    RemoteOperationResult<GetRemoteUserAvatarOperation.ResultData> avatarOperationResult =
                            getAvatarOperation.execute(client);

                    if (avatarOperationResult.isSuccess()) {
                        GetRemoteUserAvatarOperation.ResultData avatar = avatarOperationResult.getData();

                        byte[] avatarData = avatar.getAvatarData();
                        String avatarKey = ThumbnailsCacheManager.addAvatarToCache(
                                storedAccount.name,
                                avatarData,
                                dimension
                        );

                        UserProfile.UserAvatar userAvatar = new UserProfile.UserAvatar(
                                avatarKey, avatar.getMimeType(), avatar.getEtag()
                        );
                        userProfile.setAvatar(userAvatar);

                    } else if (quotaOperationResult.getCode().equals(RemoteOperationResult.ResultCode.FILE_NOT_FOUND)) {
                        Log_OC.i(TAG, "No avatar available, removing cached copy");
                        userProfilesRepository.deleteAvatar(storedAccount.name);
                        ThumbnailsCacheManager.removeAvatarFromCache(storedAccount.name);

                    }   // others are ignored, including 304 (not modified), so the avatar is only stored
                    // if changed in the server :D

                    /// store userProfile
                    userProfilesRepository.update(userProfile);

                    RemoteOperationResult<UserProfile> result =
                            new RemoteOperationResult<>(RemoteOperationResult.ResultCode.OK);
                    result.setData(userProfile);

                    return result;

                } else {
                    return quotaOperationResult;
                }
            } else {
                return userInfoOperationResult;
            }
        } catch (Exception e) {
            Log_OC.e(TAG, "Exception while getting user profile: ", e);
            return new RemoteOperationResult(e);
        }
    }

    /**
     * Converts size of file icon from dp to pixel
     * @return int
     */
    private int getAvatarDimension() {
        // Converts dp to pixel
        Resources r = MainApp.Companion.getAppContext().getResources();
        return Math.round(r.getDimension(R.dimen.file_avatar_size));
    }
}