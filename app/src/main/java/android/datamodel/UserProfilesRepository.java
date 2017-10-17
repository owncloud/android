package com.owncloud.android.datamodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.owncloud.android.MainApp;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.File;

/**
 *   ownCloud Android client application
 *
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

/**
 * Minimum to get things working.
 *
 * Working around FileContentProvider, we have no interest in exporting user profiles to other apps.
 */
public class UserProfilesRepository {

    private static final String TAG = UserProfilesRepository.class.getName();

    private SQLiteDatabase mDb;

    public UserProfilesRepository() {
        File dbFile = MainApp.getAppContext().getDatabasePath(ProviderMeta.DB_NAME);
        mDb = SQLiteDatabase.openDatabase(
            dbFile.getAbsolutePath(),
            null,
            SQLiteDatabase.OPEN_READWRITE
        );
    }

    /**
     * Persist a user profile.
     *
     * Minimum to get things working: only storing info about avatar.
     *
     * Working around ContentProvider
     *
     * @param userProfile           User profile.
     */
    public void update(UserProfile userProfile) {

        if (userProfile == null) {
            throw new IllegalArgumentException("Received userProfile with NULL value");
        }

        if (userProfile.getAvatar() != null) {
            // map avatar properties to columns
            ContentValues avatarValues = new ContentValues();
            avatarValues.put(
                ProviderMeta.ProviderTableMeta.USER_AVATARS__ACCOUNT_NAME,
                userProfile.getAccountName()
            );
            avatarValues.put(
                ProviderMeta.ProviderTableMeta.USER_AVATARS__CACHE_KEY,
                userProfile.getAvatar().getCacheKey()
            );
            avatarValues.put(
                ProviderMeta.ProviderTableMeta.USER_AVATARS__ETAG,
                userProfile.getAvatar().getEtag()
            );
            avatarValues.put(
                ProviderMeta.ProviderTableMeta.USER_AVATARS__MIME_TYPE,
                userProfile.getAvatar().getMimeType()
            );

            mDb.beginTransaction();
            try {
                if (avatarExists(userProfile)) {
                    // not new, UPDATE
                    int count = mDb.update(
                        ProviderMeta.ProviderTableMeta.USER_AVATARS__TABLE_NAME,
                        avatarValues,
                        ProviderMeta.ProviderTableMeta.USER_AVATARS__ACCOUNT_NAME + "=?",
                        new String[]{String.valueOf(userProfile.getAccountName())}
                    );
                    Log_OC.d(TAG, "Avatar updated");

                } else {
                    // new, CREATE
                    mDb.insert(
                        ProviderMeta.ProviderTableMeta.USER_AVATARS__TABLE_NAME,
                        null,
                        avatarValues
                    );
                    Log_OC.d(TAG, "Avatar inserted");
                }
                mDb.setTransactionSuccessful();

            } finally {
                mDb.endTransaction();
            }
        }
    }

    /**
     * Gets the information about a user avatar bound to an OC account.
     *
     * Shortcut method prevent retrieving a full {@link UserProfile},
     * specially now that {@link UserProfile}s are not really stored. Naughty trick.
     *
     * @param   accountName         Name of an OC account.
     * @return                      Information about a user avatar bound to an OC account, or NULL if
     *                              there is no avatar for the given account.
     */
    public UserProfile.UserAvatar getAvatar(String accountName) {
        UserProfile.UserAvatar avatar = null;
        Cursor c = null;
        try {
             c = mDb.query(
                ProviderMeta.ProviderTableMeta.USER_AVATARS__TABLE_NAME,
                null,
                ProviderMeta.ProviderTableMeta.USER_AVATARS__ACCOUNT_NAME + "=?",
                new String[]{accountName},
                null, null, null
            );
            if (c != null && c.moveToFirst()) {
                avatar = new UserProfile.UserAvatar(
                    c.getString(c.getColumnIndex(
                        ProviderMeta.ProviderTableMeta.USER_AVATARS__CACHE_KEY
                    )),
                    c.getString(c.getColumnIndex(
                        ProviderMeta.ProviderTableMeta.USER_AVATARS__MIME_TYPE
                    )),
                    c.getString(
                        c.getColumnIndex(ProviderMeta.ProviderTableMeta.USER_AVATARS__ETAG
                    ))
                );
            }   // else, no avatar to return
        } catch (Exception e) {
            Log_OC.e(TAG, "Exception while querying avatar", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return avatar;
    }

    public void deleteAvatar(String accountName) {
        try {
            mDb.delete(
                ProviderMeta.ProviderTableMeta.USER_AVATARS__TABLE_NAME,
                ProviderMeta.ProviderTableMeta.USER_AVATARS__ACCOUNT_NAME + "=?",
                new String[]{String.valueOf(accountName)}
            );
            Log_OC.d(TAG, "Avatar deleted");

        } catch (Exception e) {
            Log_OC.e(TAG, "Exception while deleting avatar", e);
        }
    }

    private boolean avatarExists(UserProfile userProfile) {
        boolean exists = false;
        Cursor c = null;
        try {
            c = mDb.query(
                ProviderMeta.ProviderTableMeta.USER_AVATARS__TABLE_NAME,
                null,
                ProviderMeta.ProviderTableMeta.USER_AVATARS__ACCOUNT_NAME + "=?",
                new String[]{userProfile.getAccountName()},
                null, null, null
            );
            exists = (c != null && c.moveToFirst());
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return exists;
    }

}
