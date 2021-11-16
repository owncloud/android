/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author Juan Carlos Garrote Gascón
 * <p>
 * Copyright (C) 2021 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.data.storage

import android.accounts.Account
import android.annotation.SuppressLint
import android.net.Uri
import com.owncloud.android.data.extension.moveRecursively
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

sealed class LocalStorageProvider(private val rootFolderName: String) {

    abstract fun getPrimaryStorageDirectory(): File

    /**
     * Return the root path of primary shared/external storage directory for this application.
     * For example: /storage/emulated/0/owncloud
     */
    fun getRootFolderPath(): String = getPrimaryStorageDirectory().absolutePath + File.separator + rootFolderName

    /**
     * Get local storage path for accountName.
     */
    fun getAccountDirectoryPath(
        accountName: String?
    ): String = getRootFolderPath() + File.separator + getEncodedAccountName(accountName)

    /**
     * Get local path where OCFile file is to be stored after upload. That is,
     * corresponding local path (in local owncloud storage) to remote uploaded
     * file.
     */
    fun getDefaultSavePathFor(
        accountName: String?,
        remotePath: String
    ): String = getAccountDirectoryPath(accountName) + remotePath

    /**
     * Get absolute path to tmp folder inside datafolder in sd-card for given accountName.
     */
    fun getTemporalPath(
        accountName: String?
    ): String = getRootFolderPath() + File.separator + TEMPORAL_FOLDER_NAME + File.separator + getEncodedAccountName(accountName)

    fun getLogsPath(): String = getRootFolderPath() + LOGS_FOLDER_NAME

    /**
     * Optimistic number of bytes available on sd-card.
     *
     * @return Optimistic number of available bytes (can be less)
     */
    @SuppressLint("UsableSpace")
    fun getUsableSpace(): Long = getPrimaryStorageDirectory().usableSpace

    /**
     * Checks if there is user data which does not have a corresponding account in the Account manager.
     */
    private fun getDanglingAccountDirs(remainingAccounts: Array<Account>): List<File> {
        val rootFolder = File(getRootFolderPath())
        val danglingDirs = mutableListOf<File>()
        rootFolder.listFiles()?.forEach { dir ->
            var dirIsOk = false
            if (dir.name.equals(TEMPORAL_FOLDER_NAME)) {
                dirIsOk = true
            } else {
                remainingAccounts.forEach { account ->
                    if (dir.name.equals(getEncodedAccountName(account.name))) {
                        dirIsOk = true
                    }
                }
            }
            if (!dirIsOk) {
                danglingDirs.add(dir)
            }
        }
        return danglingDirs
    }

    open fun deleteUnusedUserDirs(remainingAccounts: Array<Account>) {
        val danglingDirs = getDanglingAccountDirs(remainingAccounts)
        danglingDirs.forEach { dd ->
            dd.deleteRecursively()
        }
    }

    /**
     * URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names,
     * that can be in the accountName since 0.1.190B
     */
    private fun getEncodedAccountName(accountName: String?): String = Uri.encode(accountName, "@")

    fun moveLegacyToScopedStorage() {
        val timeInMillis = measureTimeMillis {
            moveFileOrFolderToScopedStorage(retrieveRootLegacyStorage())
        }
        Timber.d("MIGRATED FILES IN ${TimeUnit.SECONDS.convert(timeInMillis, TimeUnit.MILLISECONDS)} seconds")
    }

    private fun retrieveRootLegacyStorage(): File {
        val legacyStorageProvider = LegacyStorageProvider(rootFolderName)
        val rootLegacyStorage = File(legacyStorageProvider.getRootFolderPath())

        val legacyStorageUsedBytes = sizeOfDirectory(rootLegacyStorage)
        Timber.d(
            "Root ${rootLegacyStorage.absolutePath} has ${rootLegacyStorage.listFiles()?.size} files and its size is $legacyStorageUsedBytes Bytes"
        )

        return rootLegacyStorage
    }

    private fun moveFileOrFolderToScopedStorage(rootLegacyDirectory: File) {
        Timber.d("Let's move ${rootLegacyDirectory.absolutePath} to scoped storage")
        rootLegacyDirectory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                file.moveRecursively(File(getRootFolderPath(), file.name), overwrite = true)
            }
        }
        rootLegacyDirectory.deleteRecursively()
    }

    fun sizeOfDirectory(dir: File): Long {
        if (dir.exists()) {
            var result: Long = 0
            val fileList = dir.listFiles() ?: arrayOf()
            fileList.forEach { file ->
                // Recursive call if it's a directory
                result += if (file.isDirectory) {
                    sizeOfDirectory(file)
                } else {
                    // Sum the file size in bytes
                    file.length()
                }
            }
            return result // return the file size
        }
        return 0
    }

    companion object {
        private const val LOGS_FOLDER_NAME = "/logs/"
        private const val TEMPORAL_FOLDER_NAME = "tmp"
    }
}
