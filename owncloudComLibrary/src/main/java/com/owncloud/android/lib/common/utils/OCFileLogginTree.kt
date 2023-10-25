package com.owncloud.android.lib.common.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import info.hannes.timber.DebugFormatTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class OCFileLogginTree(externalCacheDir: File, context: Context? = null, filename: String = UUID.randomUUID().toString()) : DebugFormatTree() {

    private var file: File

    private var logImpossible = false

    init {
        externalCacheDir.let {
            if (!it.exists()) {
                if (!it.mkdirs())
                    Log.e(LOG_TAG, "couldn't create ${it.absoluteFile}")
            }
            val fileNameTimeStamp = SimpleDateFormat(LOG_FILE_TIME_FORMAT, Locale.getDefault()).format(Date())
            file = if (context != null) {
                File(it, "${context.packageName}.$fileNameTimeStamp.log")
            } else {
                File(it, "$filename.$fileNameTimeStamp.log")
            }
        }
    }

    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val logTimeStamp = SimpleDateFormat(LOG_MESSAGE_TIME_FORMAT, Locale.getDefault()).format(Date())

            val priorityText = when (priority) {
                2 -> "V:"
                3 -> "D:"
                4 -> "I:"
                5 -> "W:"
                6 -> "E:"
                7 -> "A:"
                else -> "$priority"
            }

            val textLine = "$priorityText $logTimeStamp$tag$message\n"
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    val writer = FileWriter(file, true)
                    writer.append(textLine)
                    writer.flush()
                    writer.close()
                }
            }

        } catch (e: Exception) {
            // Log to prevent an endless loop
            if (!logImpossible) {
                // log this output just once
                Log.w(LOG_TAG, "Can't log into file : $e")
                logImpossible = true
            }
        }
        // Don't call super, otherwise it logs twice
        // super.log(priority, tag, message, t)
    }

    companion object {

        private val LOG_TAG = OCFileLogginTree::class.java.simpleName
        private const val LOG_FILE_TIME_FORMAT = "yyyy-MM-dd_HH.mm.ss"
        private const val LOG_MESSAGE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS"

    }

}
