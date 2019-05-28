/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2019 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android

import android.accounts.Account
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment

import com.owncloud.android.authentication.FingerprintManager
import com.owncloud.android.authentication.PassCodeManager
import com.owncloud.android.authentication.PatternManager
import com.owncloud.android.capabilities.viewmodel.OCCapabilityViewModel
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory.Policy
import com.owncloud.android.lib.common.authentication.oauth.OAuth2ClientConfiguration
import com.owncloud.android.lib.common.authentication.oauth.OAuth2ProvidersRegistry
import com.owncloud.android.lib.common.authentication.oauth.OwnCloudOAuth2Provider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.shares.viewmodel.OCShareViewModel
import com.owncloud.android.ui.activity.FingerprintActivity
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
import com.owncloud.android.ui.activity.WhatsNewActivity
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Main Application of the project
 *
 * Contains methods to build the "static" strings. These strings were before constants in different
 * classes
 */
class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext

        OwnCloudClient.setContext(appContext)

        val isSamlAuth = AUTH_ON == getString(R.string.auth_method_saml_web_sso)

        OwnCloudClientManagerFactory.setUserAgent(userAgent)
        if (isSamlAuth) {
            OwnCloudClientManagerFactory.setDefaultPolicy(Policy.SINGLE_SESSION_PER_ACCOUNT)
        } else {
            OwnCloudClientManagerFactory.setDefaultPolicy(
                Policy.SINGLE_SESSION_PER_ACCOUNT_IF_SERVER_SUPPORTS_SERVER_MONITORING
            )
        }

        val oauth2Provider = OwnCloudOAuth2Provider()
        oauth2Provider.authorizationCodeEndpointPath = getString(R.string.oauth2_url_endpoint_auth)
        oauth2Provider.accessTokenEndpointPath = getString(R.string.oauth2_url_endpoint_access)
        oauth2Provider.clientConfiguration = OAuth2ClientConfiguration(
            getString(R.string.oauth2_client_id),
            getString(R.string.oauth2_client_secret),
            getString(R.string.oauth2_redirect_uri)
        )

        OAuth2ProvidersRegistry.getInstance().registerProvider(
            OwnCloudOAuth2Provider.NAME,
            oauth2Provider
        )

        // initialise thumbnails cache on background thread
        ThumbnailsCacheManager.InitDiskCacheTask().execute()

        if (BuildConfig.DEBUG || isBeta) {

            val dataFolder = dataFolder

            // Set folder for store logs
            Log_OC.setLogDataFolder(dataFolder)

            Log_OC.startLogging(Environment.getExternalStorageDirectory().absolutePath)
            Log_OC.d(BuildConfig.BUILD_TYPE, "start logging " + BuildConfig.VERSION_NAME)
        }

        // register global protection with pass code, pattern lock and fingerprint lock
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                Log_OC.d(activity.javaClass.simpleName, "onCreate(Bundle) starting")
                PassCodeManager.getPassCodeManager().onActivityCreated(activity)
                PatternManager.getPatternManager().onActivityCreated(activity)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    FingerprintManager.getFingerprintManager(activity).onActivityCreated(activity)
                }
                // If there's any lock protection, don't show wizard at this point, show it when lock activities
                // have finished
                if (activity !is PassCodeActivity &&
                    activity !is PatternLockActivity &&
                    activity !is FingerprintActivity
                ) {
                    WhatsNewActivity.runIfNeeded(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {
                Log_OC.d(activity.javaClass.simpleName, "onStart() starting")
                PassCodeManager.getPassCodeManager().onActivityStarted(activity)
                PatternManager.getPatternManager().onActivityStarted(activity)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    FingerprintManager.getFingerprintManager(activity).onActivityStarted(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) {
                Log_OC.d(activity.javaClass.simpleName, "onResume() starting")
            }

            override fun onActivityPaused(activity: Activity) {
                Log_OC.d(activity.javaClass.simpleName, "onPause() ending")
            }

            override fun onActivityStopped(activity: Activity) {
                Log_OC.d(activity.javaClass.simpleName, "onStop() ending")
                PassCodeManager.getPassCodeManager().onActivityStopped(activity)
                PatternManager.getPatternManager().onActivityStopped(activity)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    FingerprintManager.getFingerprintManager(activity).onActivityStopped(activity)
                }
                if (activity is PassCodeActivity ||
                    activity is PatternLockActivity ||
                    activity is FingerprintActivity
                ) {
                    WhatsNewActivity.runIfNeeded(activity)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                Log_OC.d(activity.javaClass.simpleName, "onSaveInstanceState(Bundle) starting")
            }

            override fun onActivityDestroyed(activity: Activity) {
                Log_OC.d(activity.javaClass.simpleName, "onDestroy() ending")
            }
        })

        val newArchModule = module {
            viewModel { (filePath: String, account: Account, shareTypes: List<ShareType>) ->
                OCShareViewModel(filePath, account, shareTypes)
            }

            viewModel { (account: Account) ->
                OCCapabilityViewModel(account)
            }
        }

        startKoin {
            modules(newArchModule)
        }
    }

    companion object {

        private val TAG = MainApp::class.java.simpleName

        private val AUTH_ON = "on"

        private val POLICY_SINGLE_SESSION_PER_ACCOUNT = "single session per account"
        private val POLICY_ALWAYS_NEW_CLIENT = "always new client"

        var appContext: Context? = null
            private set

        var BETA_VERSION = "beta"

        /**
         * Next methods give access in code to some constants that need to be defined in string resources to be referred
         * in AndroidManifest.xml file or other xml resource files; or that need to be easy to modify in build time.
         */

        val accountType: String
            get() = appContext!!.resources.getString(R.string.account_type)

        val versionCode: Int
            get() {
                try {
                    val thisPackageName = appContext!!.packageName
                    return appContext!!.packageManager.getPackageInfo(thisPackageName, 0).versionCode
                } catch (e: PackageManager.NameNotFoundException) {
                    return 0
                }

            }

        val authority: String
            get() = appContext!!.resources.getString(R.string.authority)

        val authTokenType: String
            get() = appContext!!.resources.getString(R.string.authority)

        val dbFile: String
            get() = appContext!!.resources.getString(R.string.db_file)

        val dbName: String
            get() = appContext!!.resources.getString(R.string.db_name)

        val dataFolder: String
            get() = appContext!!.resources.getString(R.string.data_folder)

        val logName: String
            get() = appContext!!.resources.getString(R.string.log_name)

        // user agent
        // Mozilla/5.0 (Android) ownCloud-android/1.7.0
        val userAgent: String
            get() {
                val appString = appContext!!.resources.getString(R.string.user_agent)
                val packageName = appContext!!.packageName
                var version = ""

                val pInfo: PackageInfo?
                try {
                    pInfo = appContext!!.packageManager.getPackageInfo(packageName, 0)
                    if (pInfo != null) {
                        version = pInfo.versionName
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    Log_OC.e(TAG, "Trying to get packageName", e.cause)
                }

                return String.format(appString, version)
            }

        val isBeta: Boolean
            get() {
                var isBeta = false
                try {
                    val packageName = appContext!!.packageName
                    val packageInfo = appContext!!.packageManager.getPackageInfo(packageName, 0)
                    val versionName = packageInfo.versionName
                    if (versionName.contains(BETA_VERSION)) {
                        isBeta = true
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }

                return isBeta
            }
    }
}
