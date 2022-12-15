/*
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * Copyright (C) 2021 ownCloud GmbH.
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
 *
 */

package com.owncloud.android.data.extensions

import java.io.File
import java.io.IOException

/**
 * It's basically a copy of copyRecursively() but deleting the source file after copying it
 * to the target location
 */
fun File.moveRecursively(
    target: File,
    overwrite: Boolean = false,
    onError: (File, IOException) -> OnErrorAction = { _, exception -> throw exception }
): Boolean {
    if (!exists()) {
        return onError(this, NoSuchFileException(file = this, reason = "The source file doesn't exist.")) !=
                OnErrorAction.TERMINATE
    }
    try {
        // We cannot break for loop from inside a lambda, so we have to use an exception here
        for (src in walkTopDown().onFail { f, e -> if (onError(f, e) == OnErrorAction.TERMINATE) throw TerminateException(f) }) {
            if (!src.exists()) {
                if (onError(src, NoSuchFileException(file = src, reason = "The source file doesn't exist.")) ==
                    OnErrorAction.TERMINATE
                )
                    return false
            } else {
                val relPath = src.toRelativeString(this)
                val dstFile = File(target, relPath)
                if (dstFile.exists() && !(src.isDirectory && dstFile.isDirectory)) {
                    val stillExists = if (!overwrite) true else {
                        if (dstFile.isDirectory)
                            !dstFile.deleteRecursively()
                        else
                            !dstFile.delete()
                    }

                    if (stillExists) {
                        if (onError(
                                dstFile, FileAlreadyExistsException(
                                    file = src,
                                    other = dstFile,
                                    reason = "The destination file already exists."
                                )
                            ) == OnErrorAction.TERMINATE
                        )
                            return false

                        continue
                    }
                }

                if (src.isDirectory) {
                    dstFile.mkdirs()
                } else {
                    try {
                        if (src.copyTo(dstFile, overwrite).length() != src.length()) {
                            if (onError(
                                    src,
                                    IOException("Source file wasn't copied completely, length of destination file differs.")
                                ) == OnErrorAction.TERMINATE
                            )
                                return false
                        } else {
                            src.delete()
                        }
                    } catch (e: IOException) {
                        src.delete()
                        dstFile.delete()
                    }
                }
            }
        }

        return true

    } catch (e: TerminateException) {
        return false
    }
}

//Private exception class, used to terminate recursive copying.
private class TerminateException(file: File) : FileSystemException(file)
