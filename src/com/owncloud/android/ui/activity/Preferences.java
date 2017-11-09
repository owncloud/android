/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2017 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.CameraUploadsSyncStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.OCCameraUploadSync;
import com.owncloud.android.db.PreferenceManager.CameraUploadsConfiguration;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.CheckBoxPreferenceWithLongTitle;
import com.owncloud.android.utils.DisplayUtils;

import java.io.File;


/**
 * An Activity that allows the user to change the application's settings.
 *
 * It proxies the necessary calls via {@link android.support.v7.app.AppCompatDelegate} to be used
 * with AppCompat.
 */
public class Preferences extends PreferenceActivity {

    private static final String TAG = Preferences.class.getSimpleName();

    private static final int ACTION_SELECT_UPLOAD_PATH = 1;
    private static final int ACTION_SELECT_UPLOAD_VIDEO_PATH = 2;
    private static final int ACTION_SELECT_SOURCE_PATH = 3;
    private static final int ACTION_REQUEST_PASSCODE = 5;
    private static final int ACTION_CONFIRM_PASSCODE = 6;

    private CheckBoxPreference pCode;
    private Preference pAboutApp;
    private AppCompatDelegate mDelegate;

    private String mUploadPath;
    private String mUploadVideoPath;
    private String mSourcePath;

    private PreferenceCategory mPrefCameraUploadsCategory;
    private Preference mPrefCameraPictureUploads;
    private Preference mPrefCameraPictureUploadsPath;
    private Preference mPrefCameraPictureUploadsWiFi;
    private Preference mPrefCameraVideoUploads;
    private Preference mPrefCameraVideoUploadsPath;
    private Preference mPrefCameraVideoUploadsWiFi;
    private Preference mPrefCameraUploadsSourcePath;
    private Preference mPrefCameraUploadsBehaviour;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.actionbar_settings);

        // For adding content description tag to a title field in the action bar
        int actionBarTitleId = getResources().getIdentifier("action_bar_title", "id", "android");
        View actionBarTitleView = getWindow().getDecorView().findViewById(actionBarTitleId);
        if (actionBarTitleView != null) {    // it's null in Android 2.x
            getWindow().getDecorView().findViewById(actionBarTitleId).
                    setContentDescription(getString(R.string.actionbar_settings));
        }

        // Load package info
        String temp;
        try {
            PackageInfo pkg = getPackageManager().getPackageInfo(getPackageName(), 0);
            temp = pkg.versionName;
        } catch (NameNotFoundException e) {
            temp = "";
            Log_OC.e(TAG, "Error while showing about dialog", e);
        }
        final String appVersion = temp;

        // Register context menu for list of preferences.
        registerForContextMenu(getListView());

        pCode = (CheckBoxPreference) findPreference(PassCodeActivity.PREFERENCE_SET_PASSCODE);
        if (pCode != null) {
            pCode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Intent i = new Intent(getApplicationContext(), PassCodeActivity.class);
                    Boolean incoming = (Boolean) newValue;

                    i.setAction(
                            incoming ? PassCodeActivity.ACTION_REQUEST_WITH_RESULT :
                                    PassCodeActivity.ACTION_CHECK_WITH_RESULT
                    );

                    startActivityForResult(i, incoming ? ACTION_REQUEST_PASSCODE :
                            ACTION_CONFIRM_PASSCODE);

                    // Don't update just yet, we will decide on it in onActivityResult
                    return false;
                }
            });
        }

        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("more");

        boolean helpEnabled = getResources().getBoolean(R.bool.help_enabled);
        Preference pHelp = findPreference("help");
        if (pHelp != null) {
            if (helpEnabled) {
                pHelp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        String helpWeb = (String) getText(R.string.url_help);
                        if (helpWeb != null && helpWeb.length() > 0) {
                            Uri uriUrl = Uri.parse(helpWeb);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                            startActivity(intent);
                        }
                        return true;
                    }
                });
            } else {
                preferenceCategory.removePreference(pHelp);
            }
        }

        boolean recommendEnabled = getResources().getBoolean(R.bool.recommend_enabled);
        Preference pRecommend = findPreference("recommend");
        if (pRecommend != null) {
            if (recommendEnabled) {
                pRecommend.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {

                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setType("text/plain");
                        intent.setData(Uri.parse(getString(R.string.mail_recommend)));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        String appName = getString(R.string.app_name);
                        String downloadUrl = getString(R.string.url_app_download);

                        String recommendSubject =
                                String.format(getString(R.string.recommend_subject),
                                        appName);
                        String recommendText = String.format(getString(R.string.recommend_text),
                                appName, downloadUrl);

                        intent.putExtra(Intent.EXTRA_SUBJECT, recommendSubject);
                        intent.putExtra(Intent.EXTRA_TEXT, recommendText);
                        startActivity(intent);

                        return (true);

                    }
                });
            } else {
                preferenceCategory.removePreference(pRecommend);
            }
        }

        boolean feedbackEnabled = getResources().getBoolean(R.bool.feedback_enabled);
        Preference pFeedback = findPreference("feedback");
        if (pFeedback != null) {
            if (feedbackEnabled) {
                pFeedback.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        String feedbackMail = (String) getText(R.string.mail_feedback);
                        String feedback = (String) getText(R.string.prefs_feedback) +
                                " - android v" + appVersion;
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_SUBJECT, feedback);

                        intent.setData(Uri.parse(feedbackMail));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                        return true;
                    }
                });
            } else {
                preferenceCategory.removePreference(pFeedback);
            }
        }

        boolean privacyPolicyEnabled = getResources().getBoolean(R.bool.privacy_policy_enabled);
        Preference pPrivacyPolicy = findPreference("privacyPolicy");
        if (pPrivacyPolicy != null) {
            if (privacyPolicyEnabled) {
                pPrivacyPolicy.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent privacyPolicyIntent = new Intent(getApplicationContext(), PrivacyPolicyActivity.class);
                        startActivity(privacyPolicyIntent);

                        return true;
                    }
                });
            } else {
                preferenceCategory.removePreference(pPrivacyPolicy);
            }
        }

        boolean loggerEnabled = getResources().getBoolean(R.bool.logger_enabled) ||
                BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.equals(MainApp.BUILD_TYPE_BETA);
        Preference pLogger = findPreference("logger");
        if (pLogger != null) {
            if (loggerEnabled) {
                pLogger.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent loggerIntent = new Intent(getApplicationContext(), LogHistoryActivity.class);
                        startActivity(loggerIntent);

                        return true;
                    }
                });
            } else {
                preferenceCategory.removePreference(pLogger);
            }
        }

        boolean imprintEnabled = getResources().getBoolean(R.bool.imprint_enabled);
        Preference pImprint = findPreference("imprint");
        if (pImprint != null) {
            if (imprintEnabled) {
                pImprint.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        String imprintWeb = (String) getText(R.string.url_imprint);
                        if (imprintWeb != null && imprintWeb.length() > 0) {
                            Uri uriUrl = Uri.parse(imprintWeb);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                            startActivity(intent);
                        }
                        return true;
                    }
                });
            } else {
                preferenceCategory.removePreference(pImprint);
            }
        }

        mPrefCameraPictureUploadsPath = findPreference("camera_picture_uploads_path");
        if (mPrefCameraPictureUploadsPath != null) {

            mPrefCameraPictureUploadsPath.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (!mUploadPath.endsWith(OCFile.PATH_SEPARATOR)) {
                        mUploadPath += OCFile.PATH_SEPARATOR;
                    }
                    Intent intent = new Intent(Preferences.this, UploadPathActivity.class);
                    intent.putExtra(UploadPathActivity.KEY_CAMERA_UPLOAD_PATH, mUploadPath);
                    startActivityForResult(intent, ACTION_SELECT_UPLOAD_PATH);
                    return true;
                }
            });
        }

        mPrefCameraUploadsCategory =
                (PreferenceCategory) findPreference("camera_uploads_category");

        mPrefCameraPictureUploadsWiFi = findPreference("camera_picture_uploads_on_wifi");
        mPrefCameraPictureUploads = findPreference("camera_picture_uploads");

        toggleCameraUploadsPictureOptions(((CheckBoxPreference) mPrefCameraPictureUploads).isChecked());

        mPrefCameraPictureUploads.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean enableCameraUploadsPicture = (Boolean) newValue;
                toggleCameraUploadsPictureOptions(enableCameraUploadsPicture);
                toggleCameraUploadsCommonOptions(
                        ((CheckBoxPreference) mPrefCameraVideoUploads).isChecked(),
                        enableCameraUploadsPicture
                );
                return true;
            }
        });

        mPrefCameraVideoUploadsPath = findPreference("camera_video_uploads_path");
        if (mPrefCameraVideoUploadsPath != null) {

            mPrefCameraVideoUploadsPath.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (!mUploadVideoPath.endsWith(OCFile.PATH_SEPARATOR)) {
                        mUploadVideoPath += OCFile.PATH_SEPARATOR;
                    }
                    Intent intent = new Intent(Preferences.this, UploadPathActivity.class);
                    intent.putExtra(UploadPathActivity.KEY_CAMERA_UPLOAD_PATH,
                            mUploadVideoPath);
                    startActivityForResult(intent, ACTION_SELECT_UPLOAD_VIDEO_PATH);
                    return true;
                }
            });
        }

        mPrefCameraVideoUploadsWiFi = findPreference("camera_video_uploads_on_wifi");
        mPrefCameraVideoUploads = findPreference("camera_video_uploads");
        toggleCameraUploadsVideoOptions(((CheckBoxPreference) mPrefCameraVideoUploads).isChecked());

        mPrefCameraVideoUploads.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                toggleCameraUploadsVideoOptions((Boolean) newValue);
                toggleCameraUploadsCommonOptions(
                        (Boolean) newValue,
                        ((CheckBoxPreference) mPrefCameraPictureUploads).isChecked());
                return true;
            }
        });

        mPrefCameraUploadsSourcePath = findPreference("camera_uploads_source_path");
        if (mPrefCameraUploadsSourcePath != null) {
            mPrefCameraUploadsSourcePath.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (!mSourcePath.endsWith(File.separator)) {
                        mSourcePath += File.separator;
                    }
                    LocalFolderPickerActivity.startLocalFolderPickerActivityForResult(
                            Preferences.this,
                            mSourcePath,
                            ACTION_SELECT_SOURCE_PATH
                    );
                    return true;
                }
            });
        } else {
            Log_OC.e(TAG, "Lost preference camera_uploads_source_path");
        }

        mPrefCameraUploadsBehaviour = findPreference("camera_uploads_behaviour");
        toggleCameraUploadsCommonOptions(
                ((CheckBoxPreference) mPrefCameraVideoUploads).isChecked(),
                ((CheckBoxPreference) mPrefCameraPictureUploads).isChecked());

        /* About App */
        pAboutApp = (Preference) findPreference("about_app");
        if (pAboutApp != null) {
            pAboutApp.setTitle(String.format(
                    getString(R.string.about_android),
                    getString(R.string.app_name)
            ));
            pAboutApp.setSummary(String.format(getString(R.string.about_version), appVersion));
        }

        loadCameraUploadsPicturePath();
        loadCameraUploadsVideoPath();
        loadCameraUploadsSourcePath();

    }

    private void toggleCameraUploadsPictureOptions(Boolean value) {
        if (value) {
            mPrefCameraUploadsCategory.addPreference(mPrefCameraPictureUploadsWiFi);
            mPrefCameraUploadsCategory.addPreference(mPrefCameraPictureUploadsPath);
        } else {
            mPrefCameraUploadsCategory.removePreference(mPrefCameraPictureUploadsWiFi);
            mPrefCameraUploadsCategory.removePreference(mPrefCameraPictureUploadsPath);
        }
    }

    private void toggleCameraUploadsVideoOptions(Boolean value) {
        if (value) {
            mPrefCameraUploadsCategory.addPreference(mPrefCameraVideoUploadsWiFi);
            mPrefCameraUploadsCategory.addPreference(mPrefCameraVideoUploadsPath);
        } else {
            mPrefCameraUploadsCategory.removePreference(mPrefCameraVideoUploadsWiFi);
            mPrefCameraUploadsCategory.removePreference(mPrefCameraVideoUploadsPath);
        }
    }

    private void toggleCameraUploadsCommonOptions(Boolean video, Boolean picture) {
        if (picture || video) {
            mPrefCameraUploadsCategory.addPreference(mPrefCameraUploadsSourcePath);
            mPrefCameraUploadsCategory.addPreference(mPrefCameraUploadsBehaviour);
        } else {
            mPrefCameraUploadsCategory.removePreference(mPrefCameraUploadsSourcePath);
            mPrefCameraUploadsCategory.removePreference(mPrefCameraUploadsBehaviour);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean state = appPrefs.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false);
        pCode.setChecked(state);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        super.onMenuItemSelected(featureId, item);
        Intent intent;

        switch (item.getItemId()) {
            case android.R.id.home:
                intent = new Intent(getBaseContext(), FileDisplayActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            default:
                Log_OC.w(TAG, "Unknown menu item triggered");
                return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTION_SELECT_UPLOAD_PATH && resultCode == RESULT_OK) {

            OCFile folderToUpload = data.getParcelableExtra(UploadPathActivity.EXTRA_FOLDER);
            mUploadPath = folderToUpload.getRemotePath();
            mPrefCameraPictureUploadsPath.setSummary(
                    DisplayUtils.getPathWithoutLastSlash(mUploadPath)
            );
            saveCameraUploadsPicturePathOnPreferences();

        } else if (requestCode == ACTION_SELECT_UPLOAD_VIDEO_PATH && resultCode == RESULT_OK) {

            OCFile folderToUploadVideo = data.getParcelableExtra(UploadPathActivity.EXTRA_FOLDER);
            mUploadVideoPath = folderToUploadVideo.getRemotePath();
            mPrefCameraVideoUploadsPath.setSummary(
                    DisplayUtils.getPathWithoutLastSlash(mUploadVideoPath)
            );
            saveCameraUploadsVideoPathOnPreferences();

        } else if (requestCode == ACTION_SELECT_SOURCE_PATH && resultCode == RESULT_OK) {

            mSourcePath = data.getStringExtra(LocalFolderPickerActivity.EXTRA_PATH);
            mPrefCameraUploadsSourcePath.setSummary(
                    DisplayUtils.getPathWithoutLastSlash(mSourcePath)
            );
            saveCameraUploadsSourcePathOnPreferences();

        } else if (requestCode == ACTION_REQUEST_PASSCODE && resultCode == RESULT_OK) {

            String passcode = data.getStringExtra(PassCodeActivity.KEY_PASSCODE);
            if (passcode != null && passcode.length() == 4) {
                SharedPreferences.Editor appPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext()).edit();

                for (int i = 1; i <= 4; ++i) {
                    appPrefs.putString(PassCodeActivity.PREFERENCE_PASSCODE_D + i,
                            passcode.substring(i - 1, i));
                }
                appPrefs.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true);
                appPrefs.commit();
                showSnackMessage(R.string.pass_code_stored);
            }

        } else if (requestCode == ACTION_CONFIRM_PASSCODE && resultCode == RESULT_OK) {

            if (data.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false)) {
                SharedPreferences.Editor appPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext()).edit();
                appPrefs.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false);
                appPrefs.commit();
                showSnackMessage(R.string.pass_code_removed);
            }
        }
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    @Override
    protected void onStop() {

        super.onStop();
        getDelegate().onStop();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    /**
     * Load upload picture path set on preferences
     */
    private void loadCameraUploadsPicturePath() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mUploadPath = appPrefs.getString("camera_picture_uploads_path", getString(R.string.camera_upload_path));
        mPrefCameraPictureUploadsPath.setSummary(
                DisplayUtils.getPathWithoutLastSlash(mUploadPath)
        );
    }

    /**
     * Save the "Upload picture path" on preferences
     */
    private void saveCameraUploadsPicturePathOnPreferences() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString("camera_picture_uploads_path", mUploadPath);
        editor.commit();
    }

    /**
     * Load upload video path set on preferences
     */
    private void loadCameraUploadsVideoPath() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mUploadVideoPath = appPrefs.getString("camera_video_uploads_path", getString(R.string.camera_upload_path));
        mPrefCameraVideoUploadsPath.setSummary(
                DisplayUtils.getPathWithoutLastSlash(mUploadVideoPath)
        );
    }

    /**
     * Save the "Upload video path" on preferences
     */
    private void saveCameraUploadsVideoPathOnPreferences() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString("camera_video_uploads_path", mUploadVideoPath);
        editor.commit();
    }

    /**
     * Load source path set on preferences
     */
    private void loadCameraUploadsSourcePath() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        mSourcePath = appPrefs.getString(
                "camera_uploads_source_path",
                CameraUploadsConfiguration.DEFAULT_SOURCE_PATH
        );
        if (mPrefCameraUploadsSourcePath != null) {
            mPrefCameraUploadsSourcePath.setSummary(
                    DisplayUtils.getPathWithoutLastSlash(mSourcePath)
            );
            String comment;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                comment = getString(R.string.prefs_camera_upload_source_path_title_optional);
            } else {
                comment = getString(R.string.prefs_camera_upload_source_path_title_required);
            }
            mPrefCameraUploadsSourcePath.setTitle(
                    String.format(mPrefCameraUploadsSourcePath.getTitle().toString(), comment)
            );
        }
    }

    /**
     * Save the "Camera folder" path on preferences
     */
    private void saveCameraUploadsSourcePathOnPreferences() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString("camera_uploads_source_path", mSourcePath);
        editor.commit();
    }


    /**
     * Show a temporary message in a Snackbar bound to the content view
     *
     * @param messageResource       Message to show.
     */
    private void showSnackMessage(int messageResource) {
        Snackbar snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                messageResource,
                Snackbar.LENGTH_LONG
        );
        snackbar.show();
    }

}
