/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2020 ownCloud GmbH.
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
import android.app.DatePickerDialog;
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
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.BiometricManager;
import com.owncloud.android.datamodel.CameraUploadsSyncStorageManager;
import com.owncloud.android.datamodel.OCCameraUploadSync;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.PreferenceManager.CameraUploadsConfiguration;
import com.owncloud.android.files.services.CameraUploadsHandler;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PreferenceUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import timber.log.Timber;

import static com.owncloud.android.db.PreferenceManager.PREF__CAMERA_UPLOADS_DEFAULT_PATH;

/**
 * An Activity that allows the user to change the application's settings.
 * <p>
 * It proxies the necessary calls via {@link androidx.appcompat.app.AppCompatDelegate} to be used
 * with AppCompat.
 */
public class Preferences extends PreferenceActivity {

    private static final int ACTION_SELECT_UPLOAD_PATH = 1;
    private static final int ACTION_SELECT_UPLOAD_VIDEO_PATH = 2;
    private static final int ACTION_SELECT_SOURCE_PATH = 3;
    private static final int ACTION_REQUEST_PASSCODE = 5;
    private static final int ACTION_CONFIRM_PASSCODE = 6;
    private static final int ACTION_REQUEST_PATTERN = 7;
    private static final int ACTION_CONFIRM_PATTERN = 8;

    private static final String PREFERENCE_CAMERA_PICTURE_UPLOADS = "camera_picture_uploads";
    private static final String PREFERENCE_CAMERA_PICTURE_UPLOADS_PATH = "camera_picture_uploads_path";
    private static final String PREFERENCE_CAMERA_UPLOADS_CATEGORY = "camera_uploads_category";
    private static final String PREFERENCE_CAMERA_PICTURE_UPLOADS_ON_WIFI = "camera_picture_uploads_on_wifi";
    private static final String PREFERENCE_CAMERA_VIDEO_UPLOADS = "camera_video_uploads";
    private static final String PREFERENCE_CAMERA_VIDEO_UPLOADS_PATH = "camera_video_uploads_path";
    private static final String PREFERENCE_CAMERA_VIDEO_UPLOADS_ON_WIFI = "camera_video_uploads_on_wifi";
    private static final String PREFERENCE_CAMERA_UPLOADS_SOURCE_PATH = "camera_uploads_source_path";
    private static final String PREFERENCE_CAMERA_UPLOADS_BEHAVIOUR = "camera_uploads_behaviour";
    private static final String PREFERENCE_SECURITY_CATEGORY = "security_category";
    private static final String PREFERENCE_MORE_CATEGORY = "more";
    private static final String PREFERENCE_HELP = "help";
    private static final String PREFERENCE_SYNC_CALENDAR_CONTACTS = "syncCalendarContacts";
    private static final String PREFERENCE_RECOMMEND = "recommend";
    private static final String PREFERENCE_FEEDBACK = "feedback";
    private static final String PREFERENCE_PRIVACY_POLICY = "privacyPolicy";
    private static final String PREFERENCE_LOGGER = "logger";
    private static final String PREFERENCE_SYNCSTART = "sync_start";
    private static final String PREFERENCE_IMPRINT = "imprint";
    private static final String PREFERENCE_ABOUT_APP = "about_app";

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
    private CheckBoxPreference mBiometric;
    private BiometricManager mBiometricManager;
    private boolean patternSet;
    private boolean passcodeSet;
    private CheckBoxPreference mPrefTouchesWithOtherVisibleWindows;

    private Preference mAboutApp;
    private AppCompatDelegate mDelegate;

    private SharedPreferences mAppPrefs;
    private Preference mLogger;
    private Preference mSyncStart;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        mAppPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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
            Timber.e(e, "Error while showing about dialog");
        }
        final String appVersion = temp + " " + BuildConfig.BUILD_TYPE + " " + BuildConfig.COMMIT_SHA1;

        // Register context menu for list of preferences.
        registerForContextMenu(getListView());

        getListView().setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getApplicationContext())
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBiometricManager = BiometricManager.getBiometricManager(this);
        }

        /*
         * Camera uploads
         */

        // Pictures
        mPrefCameraPictureUploads = (CheckBoxPreference) findPreference(PREFERENCE_CAMERA_PICTURE_UPLOADS);

        mPrefCameraPictureUploadsPath = findPreference(PREFERENCE_CAMERA_PICTURE_UPLOADS_PATH);
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

        mPrefCameraUploadsCategory = (PreferenceCategory) findPreference(PREFERENCE_CAMERA_UPLOADS_CATEGORY);

        mPrefCameraPictureUploadsWiFi = findPreference(PREFERENCE_CAMERA_PICTURE_UPLOADS_ON_WIFI);

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
        mPrefCameraVideoUploads = (CheckBoxPreference) findPreference(PREFERENCE_CAMERA_VIDEO_UPLOADS);

        mPrefCameraVideoUploadsPath = findPreference(PREFERENCE_CAMERA_VIDEO_UPLOADS_PATH);
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

        mPrefCameraVideoUploadsWiFi = findPreference(PREFERENCE_CAMERA_VIDEO_UPLOADS_ON_WIFI);
        toggleCameraUploadsVideoOptions(true, mPrefCameraVideoUploads.isChecked());

        mPrefCameraVideoUploads.setOnPreferenceChangeListener((preference, newValue) -> {
            toggleCameraUploadsVideoOptions(false, (Boolean) newValue);
            toggleCameraUploadsCommonOptions(
                    (Boolean) newValue,
                    ((CheckBoxPreference) mPrefCameraPictureUploads).isChecked());
            return true;
        });

        mPrefCameraUploadsSourcePath = findPreference(PREFERENCE_CAMERA_UPLOADS_SOURCE_PATH);
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
            Timber.e("Lost preference PREFERENCE_CAMERA_UPLOADS_SOURCE_PATH");
        }

        mPrefCameraUploadsBehaviour = findPreference(PREFERENCE_CAMERA_UPLOADS_BEHAVIOUR);
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

        mPrefSecurityCategory = (PreferenceCategory) findPreference(PREFERENCE_SECURITY_CATEGORY);
        mPasscode = (CheckBoxPreference) findPreference(PassCodeActivity.PREFERENCE_SET_PASSCODE);
        mPattern = (CheckBoxPreference) findPreference(PatternLockActivity.PREFERENCE_SET_PATTERN);
        mBiometric = (CheckBoxPreference) findPreference(BiometricActivity.PREFERENCE_SET_BIOMETRIC);
        mPrefTouchesWithOtherVisibleWindows =
                (CheckBoxPreference) findPreference(PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS);

        // Passcode lock
        if (mPasscode != null) {
            mPasscode.setOnPreferenceChangeListener((preference, newValue) -> {
                Intent i = new Intent(getApplicationContext(), PassCodeActivity.class);
                Boolean incoming = (Boolean) newValue;
                patternSet = mAppPrefs.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false);
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
                passcodeSet = mAppPrefs.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false);
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

        // Biometric lock
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mPrefSecurityCategory.removePreference(mBiometric);
        } else if (mBiometric != null) {
            // Disable biometric lock if Passcode or Pattern locks are disabled
            if (mPasscode != null && mPattern != null && !mPasscode.isChecked() && !mPattern.isChecked()) {
                mBiometric.setEnabled(false);
                mBiometric.setSummary(R.string.prefs_biometric_summary);
            }

            mBiometric.setOnPreferenceChangeListener((preference, newValue) -> {
                Boolean incoming = (Boolean) newValue;

                // Biometric not supported
                if (incoming && mBiometricManager != null && !mBiometricManager.isHardwareDetected()) {

                    showSnackMessage(R.string.biometric_not_hardware_detected);

                    return false;
                }

                // No biometric enrolled yet
                if (incoming && mBiometricManager != null && !mBiometricManager.hasEnrolledBiometric()) {

                    showSnackMessage(R.string.biometric_not_enrolled);

                    return false;
                }

                return true;
            });
        }

        if (mPrefTouchesWithOtherVisibleWindows != null) {
            mPrefTouchesWithOtherVisibleWindows.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        SharedPreferences.Editor editor = mAppPrefs.edit();
                        if ((Boolean) newValue) {
                            new AlertDialog.Builder(this)
                                    .setTitle(getString(R.string.confirmation_touches_with_other_windows_title))
                                    .setMessage(getString(R.string.confirmation_touches_with_other_windows_message))
                                    .setNegativeButton(getString(R.string.common_no), null)
                                    .setPositiveButton(getString(R.string.common_yes), (dialog, which) -> {
                                        editor.putBoolean(PREFERENCE_TOUCHES_WITH_OTHER_VISIBLE_WINDOWS, true).apply();
                                        mPrefTouchesWithOtherVisibleWindows.setChecked(true);
                                    })
                                    .show();
                            return false;
                        }
                        return true;
                    }
            );
        }

        /**
         * More
         */
        PreferenceCategory pCategoryMore = (PreferenceCategory) findPreference(PREFERENCE_MORE_CATEGORY);

        boolean helpEnabled = getResources().getBoolean(R.bool.help_enabled);
        Preference pHelp = findPreference(PREFERENCE_HELP);
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

        Preference pSyncCalendarContacts = findPreference(PREFERENCE_SYNC_CALENDAR_CONTACTS);

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
        Preference pRecommend = findPreference(PREFERENCE_RECOMMEND);
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
        Preference pFeedback = findPreference(PREFERENCE_FEEDBACK);
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
        Preference pPrivacyPolicy = findPreference(PREFERENCE_PRIVACY_POLICY);
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

        // show item(s) only when you are developer
        mLogger = findPreference(PREFERENCE_LOGGER);
        mLogger.setOnPreferenceClickListener(preference -> {
            Intent loggerIntent = new Intent(getApplicationContext(), LogHistoryActivity.class);
            startActivity(loggerIntent);

            return true;
        });

        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd", Locale.getDefault());
        CameraUploadsSyncStorageManager cameraUploadsSyncStorageManager =
                new CameraUploadsSyncStorageManager(getApplicationContext().getContentResolver());
        OCCameraUploadSync cameraUploadSync =
                cameraUploadsSyncStorageManager.getCameraUploadSync(null, null, null);
        mSyncStart = findPreference(PREFERENCE_SYNCSTART);

        if (cameraUploadSync != null) {
            mSyncStart.setTitle("Pictures sync: " + sdf.format(new Date(cameraUploadSync.getPicturesLastSync())));
            mSyncStart.setSummary("Video sync: " + sdf.format(new Date(cameraUploadSync.getPicturesLastSync())));
        } else {
            mSyncStart.setTitle("Pictures sync: not set");
            mSyncStart.setSummary("Video sync: not set");
        }
        mSyncStart.setOnPreferenceClickListener(preference -> {
            final Calendar newCalendar = Calendar.getInstance();
            DatePickerDialog StartTime = new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                if (cameraUploadSync != null) {
                    cameraUploadSync.setPicturesLastSync(newDate.getTimeInMillis());
                    cameraUploadSync.setVideosLastSync(newDate.getTimeInMillis());
                    cameraUploadsSyncStorageManager.updateCameraUploadSync(cameraUploadSync);

                    mSyncStart.setTitle("Pictures sync: " + sdf.format(new Date(cameraUploadSync.getPicturesLastSync())));
                    mSyncStart.setSummary("Video sync: " + sdf.format(new Date(cameraUploadSync.getPicturesLastSync())));
                }
            }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

            StartTime.show();

            return true;
        });
        showDeveloperItems(pCategoryMore);

        boolean imprintEnabled = getResources().getBoolean(R.bool.imprint_enabled);
        Preference pImprint = findPreference(PREFERENCE_IMPRINT);
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
        mAboutApp = findPreference(PREFERENCE_ABOUT_APP);
        if (mAboutApp != null) {
            mAboutApp.setTitle(String.format(
                    getString(R.string.about_android),
                    getString(R.string.app_name)
            ));
            mAboutApp.setSummary(String.format(getString(R.string.about_version), appVersion));
            mAboutApp.setOnPreferenceClickListener(preference -> {
                int clickCount = mAppPrefs.getInt(MainApp.CLICK_DEV_MENU, 0);
                if (mAppPrefs.getInt(MainApp.CLICK_DEV_MENU, 0) > MainApp.CLICKS_NEEDED_TO_BE_DEVELOPER) {
                    return true;
                } else if (mAppPrefs.getInt(MainApp.CLICK_DEV_MENU, 0) ==
                        MainApp.CLICKS_NEEDED_TO_BE_DEVELOPER) {
                    showDeveloperItems(pCategoryMore);
                } else if (clickCount > 0) {
                    Toast.makeText(this,
                            getString(R.string.clicks_to_be_developer,
                                    MainApp.CLICKS_NEEDED_TO_BE_DEVELOPER - clickCount),
                            Toast.LENGTH_SHORT).show();
                }
                mAppPrefs.edit().putInt(MainApp.CLICK_DEV_MENU, clickCount + 1).apply();
                ((MainApp) getApplication()).startLogIfDeveloper(); // read value to global variable

                return true;
            });
        }
    }

    private void showDeveloperItems(PreferenceCategory preferenceCategory) {
        Preference pLogger = findPreference(PREFERENCE_LOGGER);
        if (mAppPrefs.getInt(MainApp.CLICK_DEV_MENU, 0) >= MainApp.CLICKS_NEEDED_TO_BE_DEVELOPER && pLogger == null) {
            preferenceCategory.addPreference(mLogger);
            preferenceCategory.addPreference(mSyncStart);
        } else if (!MainApp.Companion.isDeveloper() && pLogger != null) {
            preferenceCategory.removePreference(mLogger);
            preferenceCategory.removePreference(mSyncStart);
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
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.confirmation_disable_camera_uploads_title))
                        .setMessage(getString(R.string.confirmation_disable_pictures_upload_message))
                        .setNegativeButton(getString(R.string.common_no), (dialog, which) -> {
                            mPrefCameraPictureUploads.setChecked(true);
                            mPrefCameraUploadsCategory.addPreference(mPrefCameraPictureUploadsWiFi);
                            mPrefCameraUploadsCategory.addPreference(mPrefCameraPictureUploadsPath);
                        })
                        .setOnCancelListener(dialog -> {
                            mPrefCameraPictureUploads.setChecked(true);
                            mPrefCameraUploadsCategory.addPreference(mPrefCameraPictureUploadsWiFi);
                            mPrefCameraUploadsCategory.addPreference(mPrefCameraPictureUploadsPath);
                        })
                        .setPositiveButton(getString(R.string.common_yes), (dialog, which) -> {
                            mPrefCameraPictureUploads.setChecked(false);
                            mPrefCameraUploadsCategory.removePreference(mPrefCameraPictureUploadsWiFi);
                            mPrefCameraUploadsCategory.removePreference(mPrefCameraPictureUploadsPath);
                            mCameraUploadsHandler.updatePicturesLastSync(getApplicationContext(), 0);
                        })
                        .show();
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
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.confirmation_disable_camera_uploads_title))
                        .setMessage(getString(R.string.confirmation_disable_videos_upload_message))
                        .setNegativeButton(getString(R.string.common_no), (dialog, which) -> {
                            mPrefCameraVideoUploads.setChecked(true);
                            mPrefCameraUploadsCategory.addPreference(mPrefCameraVideoUploadsWiFi);
                            mPrefCameraUploadsCategory.addPreference(mPrefCameraVideoUploadsPath);
                        })
                        .setOnCancelListener(dialog -> {
                            mPrefCameraVideoUploads.setChecked(true);
                            mPrefCameraUploadsCategory.addPreference(mPrefCameraVideoUploadsWiFi);
                            mPrefCameraUploadsCategory.addPreference(mPrefCameraVideoUploadsPath);
                        })
                        .setPositiveButton(getString(R.string.common_yes), (dialog, which) -> {
                            mPrefCameraVideoUploads.setChecked(false);
                            mPrefCameraUploadsCategory.removePreference(mPrefCameraVideoUploadsWiFi);
                            mPrefCameraUploadsCategory.removePreference(mPrefCameraVideoUploadsPath);
                            mCameraUploadsHandler.updateVideosLastSync(getApplicationContext(), 0);
                        })
                        .show();

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
        boolean passCodeState = mAppPrefs.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false);
        mPasscode.setChecked(passCodeState);
        boolean patternState = mAppPrefs.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false);
        mPattern.setChecked(patternState);
        boolean biometricState = mAppPrefs.getBoolean(BiometricActivity.PREFERENCE_SET_BIOMETRIC, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mBiometricManager != null &&
                !mBiometricManager.hasEnrolledBiometric()) {
            biometricState = false;
        }

        mBiometric.setChecked(biometricState);
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
                Timber.w("Unknown menu item triggered");
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
                SharedPreferences.Editor editor = mAppPrefs.edit();

                for (int i = 1; i <= 4; ++i) {
                    editor.putString(PassCodeActivity.PREFERENCE_PASSCODE_D + i, passcode.substring(i - 1, i));
                }
                editor.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, true);
                editor.apply();

                showSnackMessage(R.string.pass_code_stored);

                // Allow to use biometric lock since Passcode lock has been enabled
                enableBiometric();
            }

        } else if (requestCode == ACTION_CONFIRM_PASSCODE && resultCode == RESULT_OK) { // Disable passcode

            if (data.getBooleanExtra(PassCodeActivity.KEY_CHECK_RESULT, false)) {
                mAppPrefs.edit().putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false).apply();
                showSnackMessage(R.string.pass_code_removed);

                // Do not allow to use biometric lock since Passcode lock has been disabled
                disableBiometric(getString(R.string.prefs_biometric_summary));
            }
        } else if (requestCode == ACTION_REQUEST_PATTERN && resultCode == RESULT_OK) { // Enable pattern
            String patternValue = data.getStringExtra(PatternLockActivity.KEY_PATTERN);
            if (patternValue != null) {
                SharedPreferences.Editor editor = mAppPrefs.edit();
                editor.putString(PatternLockActivity.KEY_PATTERN, patternValue);
                editor.putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, true);
                editor.apply();
                showSnackMessage(R.string.pattern_stored);

                // Allow to use biometric lock since Pattern lock has been enabled
                enableBiometric();
            }
        } else if (requestCode == ACTION_CONFIRM_PATTERN && resultCode == RESULT_OK) { // Disable pattern
            if (data.getBooleanExtra(PatternLockActivity.KEY_CHECK_RESULT, false)) {
                mAppPrefs.edit().putBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false).apply();
                showSnackMessage(R.string.pattern_removed);

                // Do not allow to use biometric lock since Pattern lock has been disabled
                disableBiometric(getString(R.string.prefs_biometric_summary));
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
        mUploadPath = mAppPrefs.getString(PREFERENCE_CAMERA_PICTURE_UPLOADS_PATH, PREF__CAMERA_UPLOADS_DEFAULT_PATH);
        mPrefCameraPictureUploadsPath.setSummary(
                DisplayUtils.getPathWithoutLastSlash(mUploadPath)
        );
    }

    /**
     * Save the "Picture upload path" on preferences
     */
    private void saveCameraUploadsPicturePathOnPreferences() {
        mAppPrefs.edit().putString(PREFERENCE_CAMERA_PICTURE_UPLOADS_PATH, mUploadPath).apply();
    }

    /**
     * Load video upload path set on preferences
     */
    private void loadCameraUploadsVideoPath() {
        mUploadVideoPath = mAppPrefs.getString(PREFERENCE_CAMERA_VIDEO_UPLOADS_PATH, PREF__CAMERA_UPLOADS_DEFAULT_PATH);
        mPrefCameraVideoUploadsPath.setSummary(DisplayUtils.getPathWithoutLastSlash(mUploadVideoPath));
    }

    /**
     * Save the "Video upload path" on preferences
     */
    private void saveCameraUploadsVideoPathOnPreferences() {
        mAppPrefs.edit().putString(PREFERENCE_CAMERA_VIDEO_UPLOADS_PATH, mUploadVideoPath).apply();
    }

    /**
     * Load source path set on preferences
     */
    private void loadCameraUploadsSourcePath() {
        mSourcePath = mAppPrefs.getString(
                PREFERENCE_CAMERA_UPLOADS_SOURCE_PATH,
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
        mAppPrefs.edit().putString(PREFERENCE_CAMERA_UPLOADS_SOURCE_PATH, mSourcePath).apply();
    }

    private void enableBiometric() {
        mBiometric.setEnabled(true);
        mBiometric.setSummary(null);
    }

    private void disableBiometric(String summary) {
        if (mBiometric.isChecked()) {
            mBiometric.setChecked(false);
        }
        mBiometric.setEnabled(false);
        mBiometric.setSummary(summary);
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
     * Show a simple dialog with a message
     *
     * @param message message to show in the dialog
     */
    private void showSimpleDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.common_important)
                .setMessage(message)
                .setPositiveButton(getString(android.R.string.ok), null)
                .show();
    }

}
