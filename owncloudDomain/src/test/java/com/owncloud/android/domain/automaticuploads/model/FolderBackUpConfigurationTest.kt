/**
 * ownCloud Android client application
 *
 * Copyright (C) 2026 ownCloud GmbH.
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
package com.owncloud.android.domain.automaticuploads.model

import com.owncloud.android.testutil.OC_BACKUP
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderBackUpConfigurationTest {

    @Test
    fun `getRemotePathForFile - ok - upload path without trailing separator`() {
        val folderBackUpConfiguration = OC_BACKUP.copy(uploadPath = "/Photos")

        assertEquals("/Photos/image.jpg", folderBackUpConfiguration.getRemotePathForFile("image.jpg"))
    }

    @Test
    fun `getRemotePathForFile - ok - upload path with trailing separator`() {
        val folderBackUpConfiguration = OC_BACKUP.copy(uploadPath = "/Photos/")

        assertEquals("/Photos/image.jpg", folderBackUpConfiguration.getRemotePathForFile("image.jpg"))
    }

    @Test
    fun `getRemotePathForFile - ok - root upload path`() {
        val folderBackUpConfiguration = OC_BACKUP.copy(uploadPath = "/")

        assertEquals("/image.jpg", folderBackUpConfiguration.getRemotePathForFile("image.jpg"))
    }
}
