package com.owncloud.android.utils

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import com.owncloud.android.BuildConfig
import timber.log.Timber

object DeviceInfoLogger {

    fun logDeviceInfo(context: Context) {
        // Device model and OS version
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        val version = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT

        // Screen size in pixels and dp
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val widthPx = metrics.widthPixels
        val heightPx = metrics.heightPixels

        val density = metrics.density
        val widthDp = (widthPx / density).toInt()
        val heightDp = (heightPx / density).toInt()

        val appVersion =  "${BuildConfig.VERSION_NAME} (${ BuildConfig.COMMIT_SHA1})"

        Timber.d("\n==================\n" +
                "App version: $appVersion\n" +
                "Model: $manufacturer $model\n" +
                "OS Version: $version (SDK $sdkInt)\n" +
                "Screen size: ${widthPx}x${heightPx} px\n" +
                "Screen size: ${widthDp}x${heightDp} dp\n" +
                "Density: $density")
    }
}