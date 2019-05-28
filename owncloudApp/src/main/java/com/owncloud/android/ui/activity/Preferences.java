/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2019 ownCloud GmbH.
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

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.FingerprintManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.PreferenceManager.CameraUploadsConfiguration;
import com.owncloud.android.files.services.CameraUploadsHandler;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PreferenceUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * An Activity that allows the user to change the application's settings.
 * <p>
 * It proxies the necessary calls via {@link androidx.appcompat.app.AppCompatDelegate} to be used
 * with AppCompat.
 */
public class Preferences extends PreferenceActivity {

    private static final String TAG = Preferences.class.getSimpleName();

    private static final int ACTION_SELECT_UPLOAD_PATH = 1;
    private static final int ACTION_SELECT_UPLOAD_VIDEO_PATH = 2;
    private static final int ACTION_SELECT_SOURCE_PATH = 3;
    private static final int ACTION_REQUEST_PASSCODE = 5;
    private static final int ACTION_CONFIRM_PASSCODE = 6;
    private static final int ACTION_REQUEST_PATTERN = 7;
    private static final int ACTION_CONFIRM_PATTERN = 8;

    public static final String PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS = "touches_with_other_visible_windows";

    private PreferenceCategory mPrefCameraUploadsCategory;
    private CheckBoxPreference mPrefCameraPictureUploads;
    private Preference mPrefCameraPictureUploadsPath;
    private Preference mPrefCameraPictureUploadsWiFi;
    private CheckBoxPreference mPrefCameraVideoUploads;
    private Preference mPrefCameraVideoUploadsPath;
    private Preference mPrefCameraVideoUploadsWiFi;
    private Preference mPrefCameraUploadsSourcePath;
    private Preference mPrefCameraUploadsBehaviour;

    private String mUploadPath;
    private String mUploadVideoPath;
    private String mSourcePath;

    private CameraUploadsHandler mCameraUploadsHandler;

    private PreferenceCategory mPrefSecurityCategory;
    private CheckBoxPreference mPasscode;
    private CheckBoxPreference mPattern;
    private CheckBoxPreference mFingerprint;
    private FingerprintManager mFingerprintManager;
    private boolean patternSet;
    private boolean passcodeSet;
    private CheckBoxPreference mPrefTouchesWithOtherVisibleWindows;

    private Preference mAboutApp;
    private AppCompatDelegate mDelegate;

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
        final String appVersion = temp + " " + BuildConfig.BUILD_TYPE + " " + BuildConfig.COMMIT_SHA1;

        // Register context menu for list of preferences.
        registerForContextMenu(getListView());

        getListView().setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getApplicationContext())
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFingerprintManager = FingerprintManager.getFingerprintManager(this);
        }

        /*
         * Camera uploads
         */

        // Pictures
        mPrefCameraPictureUploads = (CheckBoxPreference) findPreference("camera_picture_uploads");

        mPrefCameraPictureUploadsPath = findPreference("camera_picture_uploads_path");
        if (mPrefCameraPictureUploadsPath != null) {

            mPrefCameraPictureUploadsPath.setOnPreferenceClickListener(preference -> {
                if (!mUploadPath.endsWith(OCFile.PATH_SEPARATOR)) {
                    mUploadPath += OCFile.PATH_SEPARATOR;
                }
                Intent intent = new Intent(Preferences.this, UploadPathActivity.class);
                intent.putExtra(UploadPathActivity.KEY_CAMERA_UPLOAD_PATH, mUploadPath);
                startActivityForResult(intent, ACTION_SELECT_UPLOAD_PATH);
                return true;
            });
        }

        mPrefCameraUploadsCategory = (PreferenceCategory) findPreference("camera_uploads_category");

        mPrefCameraPictureUploadsWiFi = findPreference("camera_picture_uploads_on_wifi");

        toggleCameraUploadsPictureOptions(true, mPrefCameraPictureUploads.isChecked());

        mPrefCameraPictureUploads.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enableCameraUploadsPicture = (Boolean) newValue;
            toggleCameraUploadsPictureOptions(false, enableCameraUploadsPicture);
            toggleCameraUploadsCommonOptions(
                    mPrefCameraVideoUploads.isChecked(),
                    enableCameraUploadsPicture
            );
            return true;
        });

        // Videos
        mPrefCameraVideoUploads = (CheckBoxPreference) findPreference("camera_video_uploads");

        mPrefCameraVideoUploadsPath = findPreference("camera_video_uploads_path");
        if (mPrefCameraVideoUploadsPath != null) {
            mPrefCameraVideoUploadsPath.setOnPreferenceClickListener(preference -> {
                if (!mUploadVideoPath.endsWith(OCFile.PATH_SEPARATOR)) {
                    mUploadVideoPath += OCFile.PATH_SEPARATOR;
                }
                Intent intent = new Intent(Preferences.this, UploadPathActivity.class);
                intent.putExtra(UploadPathActivity.KEY_CAMERA_UPLOAD_PATH, mUploadVideoPath);
                startActivityForResult(intent, ACTION_SELECT_UPLOAD_VIDEO_PATH);
                return true;
            });
        }

        mPrefCameraVideoUploadsWiFi = findPreference("camera_video_uploads_on_wifi");
        toggleCameraUploadsVideoOptions(true, mPrefCameraVideoUploads.isChecked());

        mPrefCameraVideoUploads.setOnPreferenceChangeListener((preference, newValue) -> {
            toggleCameraUploadsVideoOptions(false, (Boolean) newValue);
            toggleCameraUploadsCommonOptions(
                    (Boolean) newValue,
                    ((CheckBoxPreference) mPrefCameraPictureUploads).isChecked());
            return true;
        });

        mPrefCameraUploadsSourcePath = findPreference("camera_uploads_source_path");
        if (mPrefCameraUploadsSourcePath != null) {
            mPrefCameraUploadsSourcePath.setOnPreferenceClickListener(preference -> {
                if (!mSourcePath.endsWith(File.separator)) {
                    mSourcePath += File.separator;
                }
                LocalFolderPickerActivity.startLocalFolderPickerActivityForResult(
                        Preferences.this,
                        mSourcePath,
                        ACTION_SELECT_SOURCE_PATH
                );
                return true;
            });
        } else {
            Log_OC.e(TAG, "Lost preference camera_uploads_source_path");
        }

        mPrefCameraUploadsBehaviour = findPreference("camera_uploads_behaviour");
        toggleCameraUploadsCommonOptions(
                mPrefCameraVideoUploads.isChecked(),
                mPrefCameraPictureUploads.isChecked()
        );

        loadCameraUploadsPicturePath();
        loadCameraUploadsVideoPath();
        loadCameraUploadsSourcePath();

        CameraUploadsConfiguration configuration = com.owncloud.android.db.PreferenceManager.
                getCameraUploadsConfiguration(this);

        mCameraUploadsHandler = new CameraUploadsHandler(configuration);

        /*
         * Security
         */

        mPrefSecurityCategory = (PreferenceCategory) findPreference("security_category");
        mPasscode = (CheckBoxPreference) findPreference(PassCodeActivity.PREFERENCE_SET_PASSCODE);
        mPattern = (CheckBoxPreference) findPreference(PatternLockActivity.PREFERENCE_SET_PATTERN);
        mFingerprint = (CheckBoxPreference) findPreference(FingerprintActivity.PREFERENCE_SET_FINGERPRINT);
        mPrefTouchesWithOtherVisibleWindows =
                (CheckBoxPreference) findPreference(PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS);

        // Passcode lock
        if (mPasscode != null) {
            mPasscode.setOnPreferenceChangeListener((preference, newValue) -> {
                Intent i = new Intent(getApplicationContext(), PassCodeActivity.class);
                Boolean incoming = (Boolean) newValue;
                SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                patternSet = appPrefs.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false);
                if (patternSet) {
                    showSnackMessage(R.string.pattern_already_set);
                } else {
                    i.setAction(incoming ? PassCodeActivity.ACTION_REQUEST_WITH_RESULT :
                            PassCodeActivity.ACTION_CHECK_WITH_RESULT);

                    startActivityForResult(i, incoming ? ACTION_REQUEST_PASSCODE : ACTION_CONFIRM_PASSCODE);
                }
                // Don't update this yet, we will decide it on onActivityResult
                return false;
            });
        }

        // Pattern lock
        if (mPattern != null) {
            mPattern.setOnPreferenceChangeListener((preference, newValue) -> {
                Intent intent = new Intent(getApplicationContext(), PatternLockActivity.class);
                Boolean state = (Boolean) newValue;
                SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                passcodeSet = appPrefs.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false);
                if (passcodeSet) {
                    showSnackMessage(R.string.passcode_already_set);
                } else {
                    intent.setAction(state ? PatternLockActivity.ACTION_REQUEST_WITH_RESULT :
                            PatternLockActivity.ACTION_CHECK_WITH_RESULT);
                    startActivityForResult(intent, state ? ACTION_REQUEST_PATTERN : ACTION_CONFIRM_PATTERN);
                }
                return false;
            });
        }

        // Fingerprint lock
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mPrefSecurityCategory.removePreference(mFingerprint);
        } else if (mFingerprint != null) {
            // Disable Fingerprint lock if Passcode or Pattern locks are disabled
            if (mPasscode != null && mPattern != null && !mPasscode.isChecked() && !mPattern.isChecked()) {
                mFingerprint.setEnabled(false);
                mFingerprint.setSummary(R.string.prefs_fingerprint_summary);
            }

            mFingerprint.setOnPreferenceChangeListener((preference, newValue) -> {
                Boolean incoming = (Boolean) newValue;

                // Fingerprint not supported
                if (incoming && mFingerprintManager != null && !mFingerprintManager.isHardwareDetected()) {

                    showSnackMessage(R.string.fingerprint_not_hardware_detected);

                    return false;
                }

                // No fingerprints enrolled yet
                if (incoming && mFingerprintManager != null && !mFingerprintManager.hasEnrolledFingerprints()) {

                    showSnackMessage(R.string.fingerprint_not_enrolled_fingerprints);

                    return false;
                }

                return true;
            });
        }

        if (mPrefTouchesWithOtherVisibleWindows != null) {
            mPrefTouchesWithOtherVisibleWindows.setOnPreferenceChangeListener((preference, newValue) -> {
                        SharedPreferences.Editor appPrefs = PreferenceManager.
                                getDefaultSharedPreferences(getApplicationContext()).edit();

                        if ((Boolean) newValue) {
                            showConfirmationDialog(
                                    getString(R.string.confirmation_touches_with_other_windows_title),
                                    getString(R.string.confirmation_touches_with_other_windows_message),
                                    (dialog, which) -> {
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            appPrefs.putBoolean(
                                                    PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS,
                                                    true
                                            );
                                        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                                            mPrefTouchesWithOtherVisibleWindows.setChecked(false);
                                        }
                                        dialog.dismiss();
                                    });
                        } else {
                            appPrefs.putBoolean(PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS, false);
                        }

                        appPrefs.apply();

                        return true;
                    }
            );
        }

        /**
         * More
         */
        PreferenceCategory pCategoryMore = (PreferenceCategory) findPreference("more");

        boolean helpEnabled = getResources().getBoolean(R.bool.help_enabled);
        Preference pHelp = findPreference("help");
        if (pHelp != null) {
            if (helpEnabled) {
                pHelp.setOnPreferenceClickListener(preference -> {
                    String helpWeb = (String) getText(R.string.url_help);
                    if (helpWeb.length() > 0) {
                        Uri uriUrl = Uri.parse(helpWeb);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                        startActivity(intent);
                    }
                    return true;
                });
            } else {
                pCategoryMore.removePreference(pHelp);
            }
        }

        Preference pSyncCalendarContacts = findPreference("syncCalendarContacts");

        boolean syncCalendarContactsEnabled = getResources().getBoolean(R.bool.sync_calendar_contacts_enabled);
        if (pSyncCalendarContacts != null) {
            if (syncCalendarContactsEnabled) {
                pSyncCalendarContacts.setOnPreferenceClickListener(preference -> {
                    String syncCalendarContactsUrl = (String) getText(R.string.url_sync_calendar_contacts);
                    if (syncCalendarContactsUrl.length() > 0) {
                        Uri uriUrl = Uri.parse(syncCalendarContactsUrl);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                        startActivity(intent);
                    }
                    return true;
                });
            } else {
                pCategoryMore.removePreference(pSyncCalendarContacts);
            }
        }

        boolean recommendEnabled = getResources().getBoolean(R.bool.recommend_enabled);
        Preference pRecommend = findPreference("recommend");
        if (pRecommend != null) {
            if (recommendEnabled) {
                pRecommend.setOnPreferenceClickListener(preference -> {

                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse(getString(R.string.mail_recommend)));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    String appName = getString(R.string.app_name);
                    String downloadUrl = getString(R.string.url_app_download);

                    String recommendSubject =
                            String.format(getString(R.string.recommend_subject), appName);
                    String recommendText = String.format(getString(R.string.recommend_text),
                            appName, downloadUrl);

                    intent.putExtra(Intent.EXTRA_SUBJECT, recommendSubject);
                    intent.putExtra(Intent.EXTRA_TEXT, recommendText);
                    startActivity(intent);

                    return (true);

                });
            } else {
                pCategoryMore.removePreference(pRecommend);
            }
        }

        boolean feedbackEnabled = getResources().getBoolean(R.bool.feedback_enabled);
        Preference pFeedback = findPreference("feedback");
        if (pFeedback != null) {
            if (feedbackEnabled) {
                pFeedback.setOnPreferenceClickListener(preference -> {
                    String feedbackMail = (String) getText(R.string.mail_feedback);
                    String feedback = "Android v" + BuildConfig.VERSION_NAME + " - " + getText(R.string.prefs_feedback);
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.putExtra(Intent.EXTRA_SUBJECT, feedback);

                    intent.setData(Uri.parse(feedbackMail));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    return true;
                });
            } else {
                pCategoryMore.removePreference(pFeedback);
            }
        }

        boolean privacyPolicyEnabled = getResources().getBoolean(R.bool.privacy_policy_enabled);
        Preference pPrivacyPolicy = findPreference("privacyPolicy");
        if (pPrivacyPolicy != null) {
            if (privacyPolicyEnabled) {
                pPrivacyPolicy.setOnPreferenceClickListener(preference -> {
                    Intent privacyPolicyIntent = new Intent(getApplicationContext(), PrivacyPolicyActivity.class);
                    startActivity(privacyPolicyIntent);

                    return true;
                });
            } else {
                pCategoryMore.removePreference(pPrivacyPolicy);
            }
        }

        boolean loggerEnabled = getResources().getBoolean(R.bool.logger_enabled) ||
                BuildConfig.DEBUG || MainApp.Companion.isBeta();
        Preference pLogger = findPreference("logger");
        if (pLogger != null) {
            if (loggerEnabled) {
                pLogger.setOnPreferenceClickListener(preference -> {
                    Intent loggerIntent = new Intent(getApplicationContext(), LogHistoryActivity.class);
                    startActivity(loggerIntent);

                    return true;
                });
            } else {
                pCategoryMore.removePreference(pLogger);
            }
        }

        boolean imprintEnabled = getResources().getBoolean(R.bool.imprint_enabled);
        Preference pImprint = findPreference("imprint");
        if (pImprint != null) {
            if (imprintEnabled) {
                pImprint.setOnPreferenceClickListener(preference -> {
                    String imprintWeb = (String) getText(R.string.url_imprint);
                    if (imprintWeb.length() > 0) {
                        Uri uriUrl = Uri.parse(imprintWeb);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                        startActivity(intent);
                    }
                    return true;
                });
            } else {
                pCategoryMore.removePreference(pImprint);
            }
        }

        /*
         * About App
         */
        mAboutApp = findPreference("about_app");
        if (mAboutApp != null) {
            mAboutApp.setTitle(String.format(
                    getString(R.string.about_android),
                    getString(R.string.app_name)
            ));
            mAboutApp.setSummary(String.format(getString(R.string.about_version), appVersion));
            mAboutApp.setOnPreferenceClickListener(preference -> {
                String commitUrl = BuildConfig.GIT_REMOTE + "/commit/" + BuildConfig.COMMIT_SHA1;
                Uri uriUrl = Uri.parse(commitUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                startActivity(intent);
                return true;
            });
        }
    }

    /**
     * Handle the toggles from the different camera uploads for pictures options
     *
     * @param initializing avoid showing the dialog to confirm camera uploads by disabling it in the first load of the
     *                     view and showing it when the user just unchecked the feature checkbox
     * @param isChecked    camera uploads for pictures is checked
     */
    private void toggleCameraUploadsPictureOptions(Boolean initializing, Boolean isChecked) {
        if (isChecked) {
            mPrefCameraUploadsCategory.addPreference(mPrefCameraPictureUploadsWiFi);
            mPrefCameraUploadsCategory.addPreference(mPrefCameraPictureUploadsPath);
            if (!initializing) {
                showSimpleDialog(getString(R.string.proper_pics_folder_warning_camera_upload));
            }
        } else {
            if (!initializing) {
                showConfirmationDialog(
                        getString(R.string.confirmation_disable_camera_uploads_title),
                        getString(R.string.confirmation_disable_pictures_upload_message),
                        (dialog, which) -> {
                            if (which == DialogInterface.BUTTON_NEGATIVE) {
                                mPrefCameraPictureUploads.setChecked(true);
                                mPrefCameraUploadsCategory.addPreference(mPrefCameraPictureUploadsWiFi);
                                mPrefCameraUploadsCategory.addPreference(mPrefCameraPictureUploadsPath);

                            } else if (which == DialogInterface.BUTTON_POSITIVE) {
                                mPrefCameraUploadsCategory.removePreference(mPrefCameraPictureUploadsWiFi);
                                mPrefCameraUploadsCategory.removePreference(mPrefCameraPictureUploadsPath);
                                mCameraUploadsHandler.updatePicturesLastSync(getApplicationContext(), 0);
                            }
                            dialog.dismiss();
                        });
            } else {
                mPrefCameraUploadsCategory.removePreference(mPrefCameraPictureUploadsWiFi);
                mPrefCameraUploadsCategory.removePreference(mPrefCameraPictureUploadsPath);
            }
        }
    }

    /**
     * Handle the toggles from the different camera uploads for videos options
     *
     * @param initializing to avoid showing the dialog to confirm camera uploads disabling in the first load of the
     *                     view and showing it when the user just unchecked the feature checkbox
     * @param isChecked    camera uploads for videos is checked
     */
    private void toggleCameraUploadsVideoOptions(Boolean initializing, Boolean isChecked) {
        if (isChecked) {
            mPrefCameraUploadsCategory.addPreference(mPrefCameraVideoUploadsWiFi);
            mPrefCameraUploadsCategory.addPreference(mPrefCameraVideoUploadsPath);
            if (!initializing) {
                showSimpleDialog(getString(R.string.proper_videos_folder_warning_camera_upload));
            }
        } else {
            if (!initializing) {
                showConfirmationDialog(
                        getString(R.string.confirmation_disable_camera_uploads_title),
                        getString(R.string.confirmation_disable_videos_upload_message),
                        (dialog, which) -> {
                            if (which == DialogInterface.BUTTON_NEGATIVE) {
                                mPrefCameraVideoUploads.setChecked(true);
                                mPrefCameraUploadsCategory.addPreference(mPrefCameraVideoUploadsWiFi);
                                mPrefCameraUploadsCategory.addPreference(mPrefCameraVideoUploadsPath);
                            } else if (which == DialogInterface.BUTTON_POSITIVE) {
                                mPrefCameraUploadsCategory.removePreference(mPrefCameraVideoUploadsWiFi);
                                mPrefCameraUploadsCategory.removePreference(mPrefCameraVideoUploadsPath);
                                mCameraUploadsHandler.updateVideosLastSync(getApplicationContext(), 0);
                            }
                            dialog.dismiss();
                        });

            } else {
                mPrefCameraUploadsCategory.removePreference(mPrefCameraVideoUploadsWiFi);
                mPrefCameraUploadsCategory.removePreference(mPrefCameraVideoUploadsPath);
            }
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
        boolean passCodeState = appPrefs.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false);
        mPasscode.setChecked(passCodeState);
        boolean patternState = appPrefs.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false);
        mPattern.setChecked(patternState);
        boolean fingerprintState = appPrefs.getBoolean(FingerprintActivity.PREFERENCE_SET_FINGERPRINT, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mFingerprintManager != null &&
                !mFingerprintManager.hasEnrolledFingerprints()) {
            fingerprintState = false;
        }

        mFingerprint.setChecked(fingerprintState);
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

            // If the source path has changed, update camera uploads last sync
            String previousSourcePath = mSourcePath;

            if (previousSourcePath.endsWith(File.separator)) {
                previousSourcePath = previousSourcePath.substring(0, previousSourcePath.length() - 1);
            }

            if (!previousSourcePath.equals(data.getStringExtra(LocalFolderPickerActivity.EXTRA_PATH))) {
                long currentTimeStamp = System.currentTimeMillis();
                mCameraUploadsHandler.updatePicturesLastSync(getApplicationContext(), currentTimeStamp);
                mCameraUploadsHandler.updateVideosLastSync(getApplicationContext(), currentTimeStamp);
            }

            mSourcePath = data.getStringExtra(LocalFolderPickerActivity.EXTRA_PATH);
            mPrefCameraUploadsSourcePath.setSummary(
                    DisplayUtils.getPathWithoutLastSlash(mSourcePath)
            );
            saveCameraUploadsSourcePathOnPreferences();

        } else if (requestCode == ACTION_REQUEST_PASSCODE && resultCode == RESULT_OK) { // Enable passcode

            String passcode = data.getStringExtra(PassCodeActivity.KEY_PASSCODE);
            if (passcode != null && passcode.length() == 4) {
                SharedPreferences.Editor appPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext()).edit();

                for (int i = 1; i <= 4; ++i) {
                    appPrefs.putString(PassCodeActivity.PREFERENCE_PASSCODE_D + i,
                            passcode.substring(i - 1, i));
                }
                appPrefs.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true);
                appPrefs.apply();

                showSnackMessage(R.string.pass_code_stored);

                // Allow to use Fingerprint lock since Passcode lock has been enabled
                enableFingerprint();
            }

        } else if (requestCode == ACTION_CONFIRM_PASSCODE && resultCode == RESULT_OK) { // Disable passcode

            if (data.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false)) {
                SharedPreferences.Editor appPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext()).edit();
                appPrefs.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false);
                appPrefs.apply();
                showSnackMessage(R.string.pass_code_removed);

                // Do not allow to use Fingerprint lock since Passcode lock has been disabled
                disableFingerprint(getString(R.string.prefs_fingerprint_summary));
            }
        } else if (requestCode == ACTION_REQUEST_PATTERN && resultCode == RESULT_OK) { // Enable pattern
            String patternValue = data.getStringExtra(PatternLockActivity.KEY_PATTERN);
            if (patternValue != null) {
                SharedPreferences.Editor appPrefs = PreferenceManager.
                        getDefaultSharedPreferences(getApplicationContext()).edit();
                appPrefs.putString(PatternLockActivity.KEY_PATTERN, patternValue);
                appPrefs.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, true);
                appPrefs.apply();
                showSnackMessage(R.string.pattern_stored);

                // Allow to use Fingerprint lock since Pattern lock has been enabled
                enableFingerprint();
            }
        } else if (requestCode == ACTION_CONFIRM_PATTERN && resultCode == RESULT_OK) { // Disable pattern
            if (data.getBooleanExtra(PatternLockActivity.KEY_CHECK_RESULT, false)) {
                SharedPreferences.Editor appPrefs = PreferenceManager.
                        getDefaultSharedPreferences(getApplicationContext()).edit();
                appPrefs.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false);
                appPrefs.apply();
                showSnackMessage(R.string.pattern_removed);

                // Do not allow to use Fingerprint lock since Pattern lock has been disabled
                disableFingerprint(getString(R.string.prefs_fingerprint_summary));
            }
        }
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    @NotNull
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

        CameraUploadsConfiguration configuration = com.owncloud.android.db.PreferenceManager.
                getCameraUploadsConfiguration(this);

        if (configuration.isEnabledForPictures() || configuration.isEnabledForVideos()) {

            mCameraUploadsHandler.setCameraUploadsConfig(configuration);

            mCameraUploadsHandler.scheduleCameraUploadsSyncJob(getApplicationContext());
        }

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
     * Load picture upload path set on preferences
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
     * Save the "Picture upload path" on preferences
     */
    private void saveCameraUploadsPicturePathOnPreferences() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString("camera_picture_uploads_path", mUploadPath);
        editor.apply();
    }

    /**
     * Load video upload path set on preferences
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
     * Save the "Video upload path" on preferences
     */
    private void saveCameraUploadsVideoPathOnPreferences() {
        SharedPreferences appPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString("camera_video_uploads_path", mUploadVideoPath);
        editor.apply();
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
        editor.apply();
    }

    private void enableFingerprint() {
        mFingerprint.setEnabled(true);
        mFingerprint.setSummary(null);
    }

    private void disableFingerprint(String summary) {
        if (mFingerprint.isChecked()) {
            mFingerprint.setChecked(false);
        }
        mFingerprint.setEnabled(false);
        mFingerprint.setSummary(summary);
    }

    /**
     * Show a temporary message in a Snackbar bound to the content view
     *
     * @param messageResource Message to show.
     */
    private void showSnackMessage(int messageResource) {
        Snackbar snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                messageResource,
                Snackbar.LENGTH_LONG
        );
        snackbar.show();
    }

    /**
     * Show a confirmation dialog to disable camera uploads
     *
     * @param message  message to show in the dialog
     * @param listener to handle button clicks
     */
    private void showConfirmationDialog(String title, String message, DialogInterface.OnClickListener listener) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.common_no), listener);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.common_yes), listener);
        alertDialog.show();
    }

    /**
     * Show a simple dialog with a message
     *
     * @param message message to show in the dialog
     */
    private void showSimpleDialog(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(R.string.common_important);
        alertDialog.setMessage(message);
        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(android.R.string.ok),
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }
}
