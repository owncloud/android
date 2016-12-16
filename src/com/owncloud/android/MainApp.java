/**
 *   ownCloud Android client application
 *
 *   @author masensio
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
package com.owncloud.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;

import com.owncloud.android.authentication.PassCodeManager;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory.Policy;
import com.owncloud.android.lib.common.utils.Log_OC;


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

    
    public void onCreate(){
        super.onCreate();
        MainApp.mContext = getApplicationContext();
        
        boolean isSamlAuth = AUTH_ON.equals(getString(R.string.auth_method_saml_web_sso));

        OwnCloudClientManagerFactory.setUserAgent(getUserAgent());
        if (isSamlAuth) {
            OwnCloudClientManagerFactory.setDefaultPolicy(Policy.SINGLE_SESSION_PER_ACCOUNT);
        } else {
            OwnCloudClientManagerFactory.setDefaultPolicy(
                Policy.SINGLE_SESSION_PER_ACCOUNT_IF_SERVER_SUPPORTS_SERVER_MONITORING
            );
        }

        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();
        
        if (BuildConfig.DEBUG) {

            String dataFolder = getDataFolder();

            // Set folder for store logs
            Log_OC.setLogDataFolder(dataFolder);

            Log_OC.startLogging(Environment.getExternalStorageDirectory().getAbsolutePath());
            Log_OC.d("Debug", "start logging");
        }

        // register global protection with pass code
        registerActivityLifecycleCallbacks( new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Log_OC.d(activity.getClass().getSimpleName(),  "onCreate(Bundle) starting" );
                PassCodeManager.getPassCodeManager().onActivityCreated(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {
                Log_OC.d(activity.getClass().getSimpleName(),  "onStart() starting" );
                PassCodeManager.getPassCodeManager().onActivityStarted(activity);
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

        PackageInfo pInfo = null;
        try {
            pInfo = getAppContext().getPackageManager().getPackageInfo(packageName, 0);
            if (pInfo != null) {
                version = pInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log_OC.e(TAG, "Trying to get packageName", e.getCause());
        }

        // Mozilla/5.0 (Android) ownCloud-android/1.7.0
        String userAgent = String.format(appString, version);

        return userAgent;
    }
}
