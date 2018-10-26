/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Shashvat Kedia
 * Copyright (C) 2018 ownCloud GmbH.
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

package com.owncloud.android.db;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;

/**
 * Helper to simplify reading of Preferences all around the app
 */
public abstract class PreferenceManager {
    /**
     * Constant to access value of last path selected by the user to upload a file shared from other app.
     * Value handled by the app without direct access in the UI.
     */
    private static final String AUTO_PREF__LAST_UPLOAD_PATH = "last_upload_path";
    private static final String AUTO_PREF__SORT_ORDER_FILE_DISP = "sortOrderFileDisp";
    private static final String AUTO_PREF__SORT_ASCENDING_FILE_DISP = "sortAscendingFileDisp";
    private static final String AUTO_PREF__SORT_ORDER_UPLOAD = "sortOrderUpload";
    private static final String AUTO_PREF__SORT_ASCENDING_UPLOAD = "sortAscendingUpload";

    private static final String PREF__CAMERA_PICTURE_UPLOADS_ENABLED = "camera_picture_uploads";
    private static final String PREF__CAMERA_VIDEO_UPLOADS_ENABLED = "camera_video_uploads";
    private static final String PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY = "camera_picture_uploads_on_wifi";
    private static final String PREF__CAMERA_VIDEO_UPLOADS_WIFI_ONLY = "camera_video_uploads_on_wifi";
    private static final String PREF__CAMERA_UPLOADS_ACCOUNT_NAME = "camera_uploads_account_name";  // NEW - not saved yet
    private static final String PREF__CAMERA_PICTURE_UPLOADS_PATH = "camera_picture_uploads_path";
    private static final String PREF__CAMERA_VIDEO_UPLOADS_PATH = "camera_video_uploads_path";
    private static final String PREF__CAMERA_UPLOADS_BEHAVIOUR = "camera_uploads_behaviour";
    private static final String PREF__CAMERA_UPLOADS_SOURCE = "camera_uploads_source_path";

    public static boolean cameraPictureUploadEnabled(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__CAMERA_PICTURE_UPLOADS_ENABLED, false);
    }

    public static boolean cameraVideoUploadEnabled(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__CAMERA_VIDEO_UPLOADS_ENABLED, false);
    }

    public static boolean cameraPictureUploadViaWiFiOnly(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY, false);
    }

    public static boolean cameraVideoUploadViaWiFiOnly(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(PREF__CAMERA_VIDEO_UPLOADS_WIFI_ONLY, false);
    }

    public static CameraUploadsConfiguration getCameraUploadsConfiguration(Context context) {
        CameraUploadsConfiguration result = new CameraUploadsConfiguration();
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        result.setEnabledForPictures(
                prefs.getBoolean(PREF__CAMERA_PICTURE_UPLOADS_ENABLED, false)
        );
        result.setEnabledForVideos(
                prefs.getBoolean(PREF__CAMERA_VIDEO_UPLOADS_ENABLED, false)
        );
        result.setWifiOnlyForPictures(
                prefs.getBoolean(PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY, false)
        );
        result.setWifiOnlyForVideos(
                prefs.getBoolean(PREF__CAMERA_VIDEO_UPLOADS_WIFI_ONLY, false)
        );
        Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(context);
        result.setUploadAccountName(
                prefs.getString(
                        PREF__CAMERA_UPLOADS_ACCOUNT_NAME,
                        (currentAccount == null) ? "" : currentAccount.name
                )
        );
        String uploadPath = prefs.getString(
                PREF__CAMERA_PICTURE_UPLOADS_PATH,
                context.getString(R.string.camera_upload_path) + OCFile.PATH_SEPARATOR
        );
        result.setUploadPathForPictures(
                uploadPath.endsWith(File.separator) ? uploadPath : uploadPath + File.separator
        );
        uploadPath = prefs.getString(
                PREF__CAMERA_VIDEO_UPLOADS_PATH,
                context.getString(R.string.camera_upload_path) + OCFile.PATH_SEPARATOR
        );
        result.setUploadPathForVideos(
                uploadPath.endsWith(File.separator) ? uploadPath : uploadPath + File.separator
        );
        result.setBehaviourAfterUpload(
                prefs.getString(
                        PREF__CAMERA_UPLOADS_BEHAVIOUR,
                        context.getResources().getStringArray(R.array.pref_behaviour_entryValues)[0]
                )
        );
        result.setSourcePath(
                prefs.getString(
                        PREF__CAMERA_UPLOADS_SOURCE,
                        CameraUploadsConfiguration.DEFAULT_SOURCE_PATH
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
    public static int getSortOrder(Context context, int flag) {
        if (flag == FileStorageUtils.FILE_DISPLAY_SORT) {
            return getDefaultSharedPreferences(context)
                    .getInt(AUTO_PREF__SORT_ORDER_FILE_DISP, FileStorageUtils.SORT_NAME);
        } else {
            return getDefaultSharedPreferences(context)
                    .getInt(AUTO_PREF__SORT_ORDER_UPLOAD, FileStorageUtils.SORT_DATE);
        }
    }

    /**
     * Save the sort order which the user has set last.
     *
     * @param order   the sort order
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     */
    public static void setSortOrder(int order, Context context, int flag) {
        if (flag == FileStorageUtils.FILE_DISPLAY_SORT) {
            saveIntPreference(AUTO_PREF__SORT_ORDER_FILE_DISP, order, context);
        } else {
            saveIntPreference(AUTO_PREF__SORT_ORDER_UPLOAD, order, context);
        }
    }

    /**
     * Gets the ascending order flag which the user has set last.
     *
     * @param context Caller {@link Context}, used to access to shared preferences manager.
     * @return ascending order     the ascending order, default is true
     */
    public static boolean getSortAscending(Context context, int flag) {
        if (flag == FileStorageUtils.FILE_DISPLAY_SORT) {
            return getDefaultSharedPreferences(context)
                    .getBoolean(AUTO_PREF__SORT_ASCENDING_FILE_DISP, true);
        } else {
            return getDefaultSharedPreferences(context)
                    .getBoolean(AUTO_PREF__SORT_ASCENDING_UPLOAD, true);
        }
    }

    /**
     * Saves the ascending order flag which the user has set last.
     *
     * @param ascending flag if sorting is ascending or descending
     * @param context   Caller {@link Context}, used to access to shared preferences manager.
     */
    public static void setSortAscending(boolean ascending, Context context, int flag) {
        if (flag == FileStorageUtils.FILE_DISPLAY_SORT) {
            saveBooleanPreference(AUTO_PREF__SORT_ASCENDING_FILE_DISP, ascending, context);
        } else {
            saveBooleanPreference(AUTO_PREF__SORT_ASCENDING_UPLOAD, ascending, context);
        }
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
     * Aggregates preferences related to camera uploads in a single object.
     */
    public static class CameraUploadsConfiguration {

        public static final String DEFAULT_SOURCE_PATH = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
        ).getAbsolutePath() + "/Camera";

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
