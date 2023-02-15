/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Juan Carlos Garrote Gascón
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.resources.files.CheckPathExistenceRemoteOperation

class RemoteFileUtils {
    companion object {
        /**
         * Checks if remotePath does not exist in the server and returns it, or adds
         * a suffix to it in order to avoid the server file is overwritten.
         *
         * @param ownCloudClient
         * @param remotePath
         * @return
         */
        fun getAvailableRemotePath(
            ownCloudClient: OwnCloudClient,
            remotePath: String,
            spaceWebDavUrl: String? = null
        ): String {
            var checkExistsFile = existsFile(ownCloudClient, remotePath, spaceWebDavUrl)
            if (!checkExistsFile) {
                return remotePath
            }
            val pos = remotePath.lastIndexOf(".")
            var suffix: String
            var extension = ""
            if (pos >= 0) {
                extension = remotePath.substring(pos + 1)
                remotePath.apply {
                    substring(0, pos)
                }
            }
            var count = 2
            do {
                suffix = " ($count)"
                checkExistsFile = if (pos >= 0) {
                    existsFile(ownCloudClient, "${remotePath.substringBeforeLast('.', "")}$suffix.$extension", spaceWebDavUrl)
                } else {
                    existsFile(ownCloudClient, remotePath + suffix, spaceWebDavUrl)
                }
                count++
            } while (checkExistsFile)
            return if (pos >= 0) {
                "${remotePath.substringBeforeLast('.', "")}$suffix.$extension"
            } else {
                remotePath + suffix
            }
        }

        private fun existsFile(
            ownCloudClient: OwnCloudClient,
            remotePath: String,
            spaceWebDavUrl: String?,
        ): Boolean {
            val existsOperation =
                CheckPathExistenceRemoteOperation(
                    remotePath = remotePath,
                    isUserLoggedIn = false,
                    spaceWebDavUrl = spaceWebDavUrl,
                )
            return existsOperation.execute(ownCloudClient).isSuccess
        }
    }
}
