/**
 * ownCloud Android client application
 *
 * @author Hannes Achleitner
 * @author Manuel Plazas Palacio
 *
 * Copyright (C) 2023 ownCloud GmbH.
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.lib.common.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class OCFileLoggingTree(
    externalCacheDir: File,
    context: Context? = null,
    filename: String = UUID.randomUUID().toString(),
    private val newLogcat: Boolean = true,
) : Timber.DebugTree() {

    private var file: File
    private var logImpossible = false
    private var codeIdentifier = ""
    private var method = ""

    init {
        externalCacheDir.let {
            if (!it.exists()) {
                if (!it.mkdirs())
                    Log.e(LOG_TAG, "couldn't create ${it.absoluteFile}")
            }

            var fileNameTimestamp = SimpleDateFormat(LOG_FILE_TIME_FORMAT, Locale.getDefault()).format(Date())

            it.list()?.let { logFiles ->
                if (logFiles.isNotEmpty()) {

                    var lastDateLogFileString = if (context != null) {
                        logFiles.last().substringAfterLast("${context.packageName}.").substringBeforeLast(".log")
                    } else {
                        logFiles.last().substringAfterLast("$filename.").substringBeforeLast(".log")
                    }

                    val dateFormat = SimpleDateFormat(LOG_FILE_TIME_FORMAT)
                    if (lastDateLogFileString.matches("^\\d{4}-\\d{2}-\\d{2}$".toRegex())) {
                        lastDateLogFileString = "${lastDateLogFileString}_00.00.00"
                    }
                    val lastDayLogFileDate = dateFormat.parse(lastDateLogFileString)
                    val newDayLogFileDate = dateFormat.parse(fileNameTimestamp)

                    val lastDayDateLogFileString = SimpleDateFormat(LOG_FILE_DAY_FORMAT, Locale.getDefault()).format(lastDayLogFileDate!!)
                    val newDayDateLogFileString = SimpleDateFormat(LOG_FILE_DAY_FORMAT, Locale.getDefault()).format(newDayLogFileDate!!)

                    if (lastDayDateLogFileString == newDayDateLogFileString) {
                        fileNameTimestamp = lastDateLogFileString
                    }
                }
            }

            file = if (context != null) {
                File(it, "${context.packageName}.$fileNameTimestamp.log")
            } else {
                File(it, "$filename.$fileNameTimestamp.log")
            }
        }
    }

    override fun createStackElementTag(element: StackTraceElement): String {
        if (newLogcat) {
            method = String.format(
                "%s.%s()",
                // method is fully qualified only when class differs on filename otherwise it can be cropped on long lambda expressions
                super.createStackElementTag(element)?.replaceFirst(element.fileName.takeWhile { it != '.' }, ""),
                element.methodName
            )

            codeIdentifier = String.format(
                "(%s:%d)",
                element.fileName,
                element.lineNumber // format ensures line numbers have at least 3 places to align consecutive output from the same file
            )
            return "(${element.fileName}:${element.lineNumber})"
        } else
            return String.format(
                "(%s:%d) %s.%s()",
                element.fileName,
                element.lineNumber, // format ensures line numbers have at least 3 places to align consecutive output from the same file
                // method is fully qualified only when class differs on filename otherwise it can be cropped on long lambda expressions
                super.createStackElementTag(element)?.replaceFirst(element.fileName.takeWhile { it != '.' }, ""),
                element.methodName
            )
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

            Log.d(tag, "$priorityText $message")

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

        private val LOG_TAG = OCFileLoggingTree::class.java.simpleName
        private const val LOG_FILE_TIME_FORMAT = "yyyy-MM-dd_HH.mm.ss"
        private const val LOG_FILE_DAY_FORMAT = "dd"
        private const val LOG_MESSAGE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS"

    }

}

fun ocFileLoggingTree() = Timber.forest().filterIsInstance<OCFileLoggingTree>().firstOrNull()
