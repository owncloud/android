/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.owncloud.android.utils

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation

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
        fun getAvailableRemotePath(ownCloudClient: OwnCloudClient, remotePath: String): String? {
            var checkExistsFile = existsFile(ownCloudClient, remotePath)
            if (!checkExistsFile) {
                return remotePath
            }
            val pos = remotePath.lastIndexOf(".")
            var suffix = ""
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
                    existsFile(ownCloudClient, "${remotePath.substringBeforeLast('.', "")}$suffix.$extension")
                } else {
                    existsFile(ownCloudClient, remotePath + suffix)
                }
                count++
            } while (checkExistsFile)
            return if (pos >= 0) {
                "${remotePath.substringBeforeLast('.', "")}$suffix.$extension"
            } else {
                remotePath + suffix
            }
        }

        private fun existsFile(client: OwnCloudClient, remotePath: String): Boolean {
            val existsOperation = ExistenceCheckRemoteOperation(remotePath, false, false)
            return existsOperation.execute(client).isSuccess
        }
    }
}
