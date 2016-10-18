package com.owncloud.android.datamodel;

import android.support.annotation.Nullable;

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
public class UserProfile {

    private long mId;
    private String mAccountName;

    private String mUserId;
    private String mDisplayName = "";
    private String mEmail = "";

    private UserAvatar mAvatar;

    public UserProfile(String accountName, String userId, String displayName, String email) {
        mAccountName = accountName;
        mUserId = userId;
        mDisplayName = displayName;
        mEmail = email;

        mAvatar = null;
    }

    public String getAccountName() {
        return mAccountName;
    }

    public String getUserId() {
        return mUserId;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public String getEmail() {
        return mEmail;
    }

    @Nullable
    public UserAvatar getAvatar() {
        return mAvatar;
    }

    public void setAvatar(UserAvatar avatar) {
        mAvatar = avatar;
    }

    public static class UserAvatar {

        private String mCacheKey;
        private String mMimeType;
        private String mEtag;

        public UserAvatar(String cacheKey, String mimeType, String etag) {
            mCacheKey = cacheKey;
            mMimeType = mimeType;
            mEtag = etag;
        }

        public String getCacheKey() {
            return mCacheKey;
        }

        public String getMimeType() {
            return mMimeType;
        }

        public String getEtag() {
            return mEtag;
        }
    }
}
