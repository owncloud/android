package com.owncloud.android.lib.common.utils

import info.hannes.timber.FileLoggingTree
import info.hannes.timber.fileLoggingTree
import timber.log.Timber
import java.io.File

object LoggingHelper {

    fun startLogging(directory: File, storagePath: String) {
        fileLoggingTree()?.let {
            Timber.uproot(it)
        }
        if (!directory.exists())
            directory.mkdirs()
        Timber.plant(FileLoggingTree(directory, filename = storagePath))
    }

    fun stopLogging() {
        fileLoggingTree()?.let {
            Timber.uproot(it)
        }
    }
}
