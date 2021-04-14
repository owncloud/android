/*
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
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
package com.owncloud.android

import android.app.Activity
import android.app.Application
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.WindowManager
import com.owncloud.android.authentication.BiometricManager
import com.owncloud.android.authentication.PassCodeManager
import com.owncloud.android.authentication.PatternManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.dependecyinjection.commonModule
import com.owncloud.android.dependecyinjection.localDataSourceModule
import com.owncloud.android.dependecyinjection.remoteDataSourceModule
import com.owncloud.android.dependecyinjection.repositoryModule
import com.owncloud.android.dependecyinjection.useCaseModule
import com.owncloud.android.dependecyinjection.viewModelModule
import com.owncloud.android.extensions.createNotificationChannel
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.SingleSessionManager
import com.owncloud.android.lib.common.utils.LoggingHelper
import com.owncloud.android.providers.LogsProvider
import com.owncloud.android.ui.activity.BiometricActivity
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.PatternLockActivity
import com.owncloud.android.ui.activity.WhatsNewActivity
import com.owncloud.android.utils.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import com.owncloud.android.utils.FILE_SYNC_CONFLICT_CHANNEL_ID
import com.owncloud.android.utils.FILE_SYNC_NOTIFICATION_CHANNEL_ID
import com.owncloud.android.utils.MEDIA_SERVICE_NOTIFICATION_CHANNEL_ID
import com.owncloud.android.utils.UPLOAD_NOTIFICATION_CHANNEL_ID
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import timber.log.Timber
import java.io.File

/**
 * Main Application of the project
 *
 *
 * Contains methods to build the "static" strings. These strings were before constants in different
 * classes
 */
class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext

        startLogIfDeveloper()

        OwnCloudClient.setContext(appContext)

        createNotificationChannels()

        SingleSessionManager.setUserAgent(userAgent)

        // initialise thumbnails cache on background thread
        ThumbnailsCacheManager.InitDiskCacheTask().execute()

        // register global protection with pass code, pattern lock and biometric lock
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                Timber.d("${activity.javaClass.simpleName} onCreate(Bundle) starting")
                val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val passCodeEnabled = preferences.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
                val patternCodeEnabled = preferences.getBoolean(PatternLockActivity.PREFERENCE_SET_PATTERN, false)
                if (!isDeveloper) {
                    // To enable biometric you need to enable passCode or pattern, so no need to add check to if
                    if (passCodeEnabled || patternCodeEnabled) {
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                } // else, let it go, or taking screenshots & testing will not be possible

                // If there's any lock protection, don't show wizard at this point, show it when lock activities
                // have finished
                if (activity !is PassCodeActivity &&
                    activity !is PatternLockActivity &&
                    activity !is BiometricActivity
                ) {
                    WhatsNewActivity.runIfNeeded(activity)
                }

                PreferenceManager.migrateFingerprintToBiometricKey(applicationContext)
            }

            override fun onActivityStarted(activity: Activity) {
                Timber.v("${activity.javaClass.simpleName} onStart() starting")
                PassCodeManager.getPassCodeManager().onActivityStarted(activity)
                PatternManager.getPatternManager().onActivityStarted(activity)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    BiometricManager.getBiometricManager(activity).onActivityStarted(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) {
                Timber.v("${activity.javaClass.simpleName} onResume() starting")
            }

            override fun onActivityPaused(activity: Activity) {
                Timber.v("${activity.javaClass.simpleName} onPause() ending")
            }

            override fun onActivityStopped(activity: Activity) {
                Timber.v("${activity.javaClass.simpleName} onStop() ending")
                PassCodeManager.getPassCodeManager().onActivityStopped(activity)
                PatternManager.getPatternManager().onActivityStopped(activity)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    BiometricManager.getBiometricManager(activity).onActivityStopped(activity)
                }
                if (activity is PassCodeActivity ||
                    activity is PatternLockActivity ||
                    activity is BiometricActivity
                ) {
                    WhatsNewActivity.runIfNeeded(activity)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                Timber.v("${activity.javaClass.simpleName} onSaveInstanceState(Bundle) starting")
            }

            override fun onActivityDestroyed(activity: Activity) {
                Timber.v("${activity.javaClass.simpleName} onDestroy() ending")
            }
        })

        initDependencyInjection()
    }

    fun startLogIfDeveloper() {
        isDeveloper =
            BuildConfig.DEBUG || PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getInt(CLICK_DEV_MENU, CLICKS_DEFAULT) > CLICKS_NEEDED_TO_BE_DEVELOPER

        if (isDeveloper) {
            val dataFolder = dataFolder

            // Set folder for store logs
            LoggingHelper.startLogging(
                File(Environment.getExternalStorageDirectory().absolutePath + File.separator + dataFolder), dataFolder
            )
            Timber.d("${BuildConfig.BUILD_TYPE} start logging ${BuildConfig.VERSION_NAME} ${BuildConfig.COMMIT_SHA1}")

            LogsProvider(applicationContext).initHttpLogs()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        createNotificationChannel(
            id = DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            name = getString(R.string.download_notification_channel_name),
            description = getString(R.string.download_notification_channel_description),
            importance = IMPORTANCE_LOW
        )

        createNotificationChannel(
            id = UPLOAD_NOTIFICATION_CHANNEL_ID,
            name = getString(R.string.upload_notification_channel_name),
            description = getString(R.string.upload_notification_channel_description),
            importance = IMPORTANCE_LOW
        )

        createNotificationChannel(
            id = MEDIA_SERVICE_NOTIFICATION_CHANNEL_ID,
            name = getString(R.string.media_service_notification_channel_name),
            description = getString(R.string.media_service_notification_channel_description),
            importance = IMPORTANCE_LOW
        )

        createNotificationChannel(
            id = FILE_SYNC_CONFLICT_CHANNEL_ID,
            name = getString(R.string.file_sync_notification_channel_name),
            description = getString(R.string.file_sync_notification_channel_description),
            importance = IMPORTANCE_LOW
        )

        createNotificationChannel(
            id = FILE_SYNC_NOTIFICATION_CHANNEL_ID,
            name = getString(R.string.file_sync_notification_channel_name),
            description = getString(R.string.file_sync_notification_channel_description),
            importance = IMPORTANCE_LOW
        )
    }

    companion object {
        const val CLICK_DEV_MENU = "clickDeveloperMenu"
        const val CLICKS_NEEDED_TO_BE_DEVELOPER = 5
        private const val BETA_VERSION = "beta"
        private const val CLICKS_DEFAULT = 0

        lateinit var appContext: Context
            private set
        var isDeveloper: Boolean = false
            private set

        /**
         * Next methods give access in code to some constants that need to be defined in string resources to be referred
         * in AndroidManifest.xml file or other xml resource files; or that need to be easy to modify in build time.
         */

        val accountType: String
            get() = appContext.resources.getString(R.string.account_type)

        val versionCode: Int
            get() {
                return try {
                    val thisPackageName = appContext.packageName
                    appContext.packageManager.getPackageInfo(thisPackageName, 0).versionCode
                } catch (e: PackageManager.NameNotFoundException) {
                    0
                }

            }

        val authority: String
            get() = appContext.resources.getString(R.string.authority)

        val authTokenType: String
            get() = appContext.resources.getString(R.string.authority)

        val dataFolder: String
            get() = appContext.resources.getString(R.string.data_folder)

        // user agent
        // Mozilla/5.0 (Android) ownCloud-android/1.7.0
        val userAgent: String
            get() {
                val appString = appContext.resources.getString(R.string.user_agent)
                val packageName = appContext.packageName
                var version = ""

                val pInfo: PackageInfo?
                try {
                    pInfo = appContext.packageManager.getPackageInfo(packageName, 0)
                    if (pInfo != null) {
                        version = pInfo.versionName
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    Timber.e(e, "Trying to get packageName")
                }

                return String.format(appString, version)
            }

        val isBeta: Boolean
            get() {
                var isBeta = false
                try {
                    val packageName = appContext.packageName
                    val packageInfo = appContext.packageManager.getPackageInfo(packageName, 0)
                    val versionName = packageInfo.versionName
                    if (versionName.contains(BETA_VERSION)) {
                        isBeta = true
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    Timber.e(e)
                }

                return isBeta
            }

        fun initDependencyInjection() {
            stopKoin()
            startKoin {
                androidContext(appContext)
                modules(
                    listOf(
                        commonModule,
                        viewModelModule,
                        useCaseModule,
                        repositoryModule,
                        localDataSourceModule,
                        remoteDataSourceModule
                    )
                )
            }
        }
    }
}
