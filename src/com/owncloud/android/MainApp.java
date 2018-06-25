/**
 *   ownCloud Android client application
 *
 *   @author masensio
 *   @author David A. Velasco
 *   @author David González Verdugo
 *   @author Christian Schabesberger
 *   Copyright (C) 2018 ownCloud GmbH.
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
package com.owncloud.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import com.facebook.stetho.Stetho;
import com.owncloud.android.authentication.FingerprintManager;
import com.owncloud.android.authentication.PassCodeManager;
import com.owncloud.android.authentication.PatternManager;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory.Policy;
import com.owncloud.android.lib.common.authentication.oauth.OAuth2ClientConfiguration;
import com.owncloud.android.lib.common.authentication.oauth.OAuth2ProvidersRegistry;
import com.owncloud.android.lib.common.authentication.oauth.OwnCloudOAuth2Provider;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FingerprintActivity;
import com.owncloud.android.ui.activity.PassCodeActivity;
import com.owncloud.android.ui.activity.PatternLockActivity;
import com.owncloud.android.ui.activity.WhatsNewActivity;


/**
 * Main Application of the project
 *
 * Contains methods to build the "static" strings. These strings were before constants in different
 * classes
 */
public class MainApp extends Application {

    private static final String TAG = MainApp.class.getSimpleName();

    private static final String AUTH_ON = "on";

    @SuppressWarnings("unused")
    private static final String POLICY_SINGLE_SESSION_PER_ACCOUNT = "single session per account";
    @SuppressWarnings("unused")
    private static final String POLICY_ALWAYS_NEW_CLIENT = "always new client";

    private static Context mContext;

    // TODO Enable when "On Device" is recovered?
    // TODO better place
    // private static boolean mOnlyOnDevice = false;

    public static String BETA_VERSION = "beta";

    public void onCreate(){
        super.onCreate();
        MainApp.mContext = getApplicationContext();

        boolean isSamlAuth = AUTH_ON.equals(getString(R.string.auth_method_saml_web_sso));

        if(BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this);
        }

        OwnCloudClientManagerFactory.setUserAgent(getUserAgent());
        if (isSamlAuth) {
            OwnCloudClientManagerFactory.setDefaultPolicy(Policy.SINGLE_SESSION_PER_ACCOUNT);
        } else {
            OwnCloudClientManagerFactory.setDefaultPolicy(
                    Policy.SINGLE_SESSION_PER_ACCOUNT_IF_SERVER_SUPPORTS_SERVER_MONITORING
            );
        }

        OwnCloudOAuth2Provider oauth2Provider = new OwnCloudOAuth2Provider();
        oauth2Provider.setAuthorizationCodeEndpointPath(
                getString(R.string.oauth2_url_endpoint_auth)
        );
        oauth2Provider.setAccessTokenEndpointPath(
                getString(R.string.oauth2_url_endpoint_access)
        );
        oauth2Provider.setClientConfiguration(
                new OAuth2ClientConfiguration(
                        getString(R.string.oauth2_client_id),
                        getString(R.string.oauth2_client_secret),
                        getString(R.string.oauth2_redirect_uri)
                )
        );

        OAuth2ProvidersRegistry.getInstance().registerProvider(
                OwnCloudOAuth2Provider.NAME,
                oauth2Provider
        );

        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();

        if (BuildConfig.DEBUG || isBeta()) {

            String dataFolder = getDataFolder();

            // Set folder for store logs
            Log_OC.setLogDataFolder(dataFolder);

            Log_OC.startLogging(Environment.getExternalStorageDirectory().getAbsolutePath());
            Log_OC.d("Debug", "start logging");
        }

        // register global protection with pass code, pattern lock and fingerprint lock
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Log_OC.d(activity.getClass().getSimpleName(),  "onCreate(Bundle) starting" );
                PassCodeManager.getPassCodeManager().onActivityCreated(activity);
                PatternManager.getPatternManager().onActivityCreated(activity);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    FingerprintManager.getFingerprintManager(activity).onActivityCreated(activity);
                }
                // If there's any lock protection, don't show wizard at this point, show it when lock activities
                // have finished
                if (!(activity instanceof PassCodeActivity) &&
                        !(activity instanceof PatternLockActivity) &&
                        !(activity instanceof FingerprintActivity)) {
                    WhatsNewActivity.runIfNeeded(activity);
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(),  "onStart() starting" );
                PassCodeManager.getPassCodeManager().onActivityStarted(activity);
                PatternManager.getPatternManager().onActivityStarted(activity);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    FingerprintManager.getFingerprintManager(activity).onActivityStarted(activity);
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(), "onResume() starting" );
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(), "onPause() ending");
            }

            @Override
            public void onActivityStopped(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(), "onStop() ending" );
                PassCodeManager.getPassCodeManager().onActivityStopped(activity);
                PatternManager.getPatternManager().onActivityStopped(activity);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    FingerprintManager.getFingerprintManager(activity).onActivityStopped(activity);
                }
                if (activity instanceof PassCodeActivity ||
                        activity instanceof PatternLockActivity ||
                        activity instanceof FingerprintActivity) {
                    WhatsNewActivity.runIfNeeded(activity);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                Log_OC.d(activity.getClass().getSimpleName(), "onSaveInstanceState(Bundle) starting" );
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(), "onDestroy() ending" );
            }
        });
    }

    public static Context getAppContext() {
        return MainApp.mContext;
    }

    /**
     * Next methods give access in code to some constants that need to be defined in string resources to be referred
     * in AndroidManifest.xml file or other xml resource files; or that need to be easy to modify in build time.
     */

    public static String getAccountType() {
        return getAppContext().getResources().getString(R.string.account_type);
    }
    
    public static int getVersionCode() {
        try {
            String thisPackageName = getAppContext().getPackageName();
            return getAppContext().getPackageManager().getPackageInfo(thisPackageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    public static String getAuthority() {
        return getAppContext().getResources().getString(R.string.authority);
    }

    public static String getAuthTokenType() {
        return getAppContext().getResources().getString(R.string.authority);
    }

    public static String getDBFile() {
        return getAppContext().getResources().getString(R.string.db_file);
    }

    public static String getDBName() {
        return getAppContext().getResources().getString(R.string.db_name);
    }

    public static String getDataFolder() {
        return getAppContext().getResources().getString(R.string.data_folder);
    }

    public static String getLogName() {
        return getAppContext().getResources().getString(R.string.log_name);
    }

    // TODO Enable when "On Device" is recovered ?
//    public static void showOnlyFilesOnDevice(boolean state){
//        mOnlyOnDevice = state;
//    }
//
//    public static boolean getOnlyOnDevice(){
//        return mOnlyOnDevice;
//    }

    // user agent
    public static String getUserAgent() {
        String appString = getAppContext().getResources().getString(R.string.user_agent);
        String packageName = getAppContext().getPackageName();
        String version = "";

        PackageInfo pInfo;
        try {
            pInfo = getAppContext().getPackageManager().getPackageInfo(packageName, 0);
            if (pInfo != null) {
                version = pInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log_OC.e(TAG, "Trying to get packageName", e.getCause());
        }

        // Mozilla/5.0 (Android) ownCloud-android/1.7.0
        return String.format(appString, version);
    }

    public static boolean isBeta() {
        boolean isBeta = false;
        try {
            String packageName = getAppContext().getPackageName();
            PackageInfo packageInfo = getAppContext().getPackageManager().getPackageInfo(packageName, 0);
            String versionName = packageInfo.versionName;
            if (versionName.contains(BETA_VERSION)){
                isBeta = true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return isBeta;
    }
}
