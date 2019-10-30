package com.owncloud.android.lib.common.utils

import info.hannes.timber.FileLoggingTree
import info.hannes.timber.fileLoggingTree
import timber.log.Timber
import java.io.File

object LoggingHelper {

    fun startLogging(directory: File, storagePath: String) {
        Timber.forest().fileLoggingTree()?.let {
            Timber.forest().drop(Timber.forest().indexOf(it))
        }
        if (!directory.exists())
            directory.mkdirs()
        Timber.plant(FileLoggingTree(directory, filename = storagePath, delegator = Log_OC::class.java))
    }

    fun stopLogging() {
        Timber.forest().fileLoggingTree()?.let {
            Timber.forest().drop(Timber.forest().indexOf(it))
        }
    }
}
