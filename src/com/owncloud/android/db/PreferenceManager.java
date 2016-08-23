/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2016 ownCloud GmbH.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.utils.FileStorageUtils;

/**
 * Helper to simplify reading of Preferences all around the app
 */
public abstract class PreferenceManager {
    /**
     * Constant to access value of last path selected by the user to upload a file shared from other app.
     * Value handled by the app without direct access in the UI.
     */
    private static final String AUTO_PREF__LAST_UPLOAD_PATH = "last_upload_path";
    private static final String AUTO_PREF__SORT_ORDER = "sortOrder";
    private static final String AUTO_PREF__SORT_ASCENDING = "sortAscending";

    private static final String PREF__INSTANT_PICTURE_ENABLED = "instant_uploading";
    private static final String PREF__INSTANT_VIDEO_ENABLED = "instant_video_uploading";
    private static final String PREF__INSTANT_PICTURE_WIFI_ONLY = "instant_upload_on_wifi";
    private static final String PREF__INSTANT_VIDEO_WIFI_ONLY = "instant_video_upload_on_wifi";
    private static final String PREF__INSTANT_UPLOAD_ACCOUNT_NAME = "instant_upload_account_name";  // NEW - not saved yet
    private static final String PREF__INSTANT_PICTURE_UPLOAD_PATH = "instant_upload_path";
    private static final String PREF__INSTANT_VIDEO_UPLOAD_PATH = "instant_video_upload_path";
    private static final String PREF__INSTANT_UPLOAD_BEHAVIOUR = "prefs_instant_behaviour";
    private static final String PREF__INSTANT_UPLOAD_SOURCE = "instant_upload_source_path";   // NEW - not saved yet

    private static final String PREF__INSTANT_UPLOAD_SOURCE_DEFAULT_FOLDER = "/Camera";

    public static boolean instantPictureUploadEnabled(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__INSTANT_PICTURE_ENABLED, false);
    }

    public static boolean instantVideoUploadEnabled(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__INSTANT_VIDEO_ENABLED, false);
    }

    public static boolean instantPictureUploadViaWiFiOnly(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__INSTANT_PICTURE_WIFI_ONLY, false);
    }

    public static boolean instantVideoUploadViaWiFiOnly(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__INSTANT_VIDEO_WIFI_ONLY, false);
    }

    public static InstantUploadsConfiguration getInstantUploadsConfiguration(Context context) {
        InstantUploadsConfiguration result = new InstantUploadsConfiguration();
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        result.setEnabledForPictures(
            prefs.getBoolean(PREF__INSTANT_PICTURE_ENABLED, false)
        );
        result.setEnabledForVideos(
            prefs.getBoolean(PREF__INSTANT_VIDEO_ENABLED, false)
        );
        result.setWifiOnlyForPictures(
            prefs.getBoolean(PREF__INSTANT_PICTURE_WIFI_ONLY, false)
        );
        result.setWifiOnlyForVideos(
            prefs.getBoolean(PREF__INSTANT_VIDEO_WIFI_ONLY, false)
        );
        result.setUploadAccountName(
            prefs.getString(
                PREF__INSTANT_UPLOAD_ACCOUNT_NAME,
                AccountUtils.getCurrentOwnCloudAccount(context).name
            )
        );
        result.setUploadPathForPictures(
            prefs.getString(
                PREF__INSTANT_PICTURE_UPLOAD_PATH,
                context.getString(R.string.instant_upload_path)
            )
        );
        result.setUploadPathForVideos(
            prefs.getString(
                PREF__INSTANT_VIDEO_UPLOAD_PATH,
                context.getString(R.string.instant_upload_path)
            )
        );
        result.setBehaviourAfterUpload(
            prefs.getString(
                PREF__INSTANT_UPLOAD_BEHAVIOUR,
                context.getResources().getStringArray(R.array.pref_behaviour_entryValues)[0]
            )
        );
        result.setSourcePath(
            prefs.getString(
                PREF__INSTANT_UPLOAD_SOURCE,
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM
                ).getAbsolutePath() + PREF__INSTANT_UPLOAD_SOURCE_DEFAULT_FOLDER
            )
        );
        return result;
    }

    /**
     * Gets the path where the user selected to do the last upload of a file shared from other app.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return path     Absolute path to a folder, as previously stored by {@link #setLastUploadPath(String, Context)},
     * or empty String if never saved before.
     */
    public static String getLastUploadPath(Context context) {
        return getDefaultSharedPreferences(context).getString(AUTO_PREF__LAST_UPLOAD_PATH, "");
    }

    /**
     * Saves the path where the user selected to do the last upload of a file shared from other app.
     *
     * @param path    Absolute path to a folder.
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     */
    public static void setLastUploadPath(String path, Context context) {
        saveStringPreference(AUTO_PREF__LAST_UPLOAD_PATH, path, context);
    }

    /**
     * Gets the sort order which the user has set last.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return sort order     the sort order, default is {@link FileStorageUtils#SORT_NAME} (sort by name)
     */
    public static int getSortOrder(Context context) {
        return getDefaultSharedPreferences(context).getInt(AUTO_PREF__SORT_ORDER, FileStorageUtils.SORT_NAME);
    }

    /**
     * Save the sort order which the user has set last.
     *
     * @param order   the sort order
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     */
    public static void setSortOrder(int order, Context context) {
        saveIntPreference(AUTO_PREF__SORT_ORDER, order, context);
    }

    /**
     * Gets the ascending order flag which the user has set last.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return ascending order     the ascending order, default is true
     */
    public static boolean getSortAscending(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(AUTO_PREF__SORT_ASCENDING, true);
    }

    /**
     * Saves the ascending order flag which the user has set last.
     *
     * @param ascending flag if sorting is ascending or descending
     * @param context   Caller {@link Context}, used to access to shared preferences manager.
     */
    public static void setSortAscending(boolean ascending, Context context) {
        saveBooleanPreference(AUTO_PREF__SORT_ASCENDING, ascending, context);
    }

    private static void saveBooleanPreference(String key, boolean value, Context context) {
        SharedPreferences.Editor appPreferences = getDefaultSharedPreferences(context.getApplicationContext()).edit();
        appPreferences.putBoolean(key, value);
        appPreferences.apply();
    }

    private static void saveStringPreference(String key, String value, Context context) {
        SharedPreferences.Editor appPreferences = getDefaultSharedPreferences(context.getApplicationContext()).edit();
        appPreferences.putString(key, value);
        appPreferences.apply();
    }

    private static void saveIntPreference(String key, int value, Context context) {
        SharedPreferences.Editor appPreferences = getDefaultSharedPreferences(context.getApplicationContext()).edit();
        appPreferences.putInt(key, value);
        appPreferences.apply();
    }

    private static SharedPreferences getDefaultSharedPreferences(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    /**
     * Aggregates preferences related to instant uploads in a single object.
     */
    public static class InstantUploadsConfiguration {

        private boolean mEnabledForPictures;
        private boolean mEnabledForVideos;
        private boolean mWifiOnlyForPictures;
        private boolean mWifiOnlyForVideos;
        private String mUploadAccountName;      // same for both audio & video
        private String mUploadPathForPictures;
        private String mUploadPathForVideos;
        private String mBehaviourAfterUpload;
        private String mSourcePath;             // same for both audio & video

        public boolean isEnabledForPictures() {
            return mEnabledForPictures;
        }

        public void setEnabledForPictures(boolean uploadPictures) {
            mEnabledForPictures = uploadPictures;
        }

        public boolean isEnabledForVideos() {
            return mEnabledForVideos;
        }

        public void setEnabledForVideos(boolean uploadVideos) {
            mEnabledForVideos = uploadVideos;
        }

        public boolean isWifiOnlyForPictures() {
            return mWifiOnlyForPictures;
        }

        public void setWifiOnlyForPictures(boolean wifiOnlyForPictures) {
            mWifiOnlyForPictures = wifiOnlyForPictures;
        }

        public boolean isWifiOnlyForVideos() {
            return mWifiOnlyForVideos;
        }

        public void setWifiOnlyForVideos(boolean wifiOnlyForVideos) {
            mWifiOnlyForVideos = wifiOnlyForVideos;
        }

        public String getUploadAccountName() {
            return mUploadAccountName;
        }

        public void setUploadAccountName(String uploadAccountName) {
            mUploadAccountName = uploadAccountName;
        }

        public String getUploadPathForPictures() {
            return mUploadPathForPictures;
        }

        public void setUploadPathForPictures(String uploadPathForPictures) {
            mUploadPathForPictures = uploadPathForPictures;
        }

        public String getUploadPathForVideos() {
            return mUploadPathForVideos;
        }

        public void setUploadPathForVideos(String uploadPathForVideos) {
            mUploadPathForVideos = uploadPathForVideos;
        }

        public int getBehaviourAfterUpload() {
            if (mBehaviourAfterUpload.equalsIgnoreCase("MOVE")) {
                return FileUploader.LOCAL_BEHAVIOUR_MOVE;
            }
            return FileUploader.LOCAL_BEHAVIOUR_FORGET; // "NOTHING
        }

        public void setBehaviourAfterUpload(String behaviourAfterUpload) {
            mBehaviourAfterUpload = behaviourAfterUpload;
        }

        public String getSourcePath() {
            return mSourcePath;
        }

        public void setSourcePath(String sourcePath) {
            mSourcePath = sourcePath;
        }

    }
}
