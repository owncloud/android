package com.owncloud.android.utils

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.test.runner.AndroidJUnitRunner
import com.github.tmurakami.dexopener.DexOpener

/**
 * We need to use DexOpener for executing instrumented tests on <P Android devices,
 * as Mockk documentation suggests https://mockk.io/ANDROID.html
 */
class OCTestAndroidJUnitRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            DexOpener.install(this)
        }
        return super.newApplication(cl, className, context)
    }
}
