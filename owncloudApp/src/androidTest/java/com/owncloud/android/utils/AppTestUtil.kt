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

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.domain.files.model.MIME_DIR

object AppTestUtil {
    /**
     * Files
     * Move to owncloudTestUtil module when OCFile is migrated to owncloudDomain
     */
    val OC_FILE = OCFile(
        "/Images/img.png"
    ).apply {
        fileId = 1
        fileName =  "img.png"
        mimetype = ".png"
        privateLink = "privateLink"
    }

    val OC_FOLDER = OCFile(
        "/Images/img.png"
    ).apply {
        fileName =  "/Documents/"
        mimetype = MIME_DIR
    }
}
