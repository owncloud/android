/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

import com.owncloud.android.shares.db.OCShare

class ShareUtils {

    companion object {
        fun shareWithNameAndLink(
            path: String,
            isFolder: Boolean,
            name: String,
            shareLink: String
        ): OCShare {
            return OCShare(
                7,
                7,
                3,
                "",
                path,
                1,
                1542628397,
                0,
                "pwdasd12dasdWZ",
                "",
                isFolder,
                -1,
                1,
                "admin@server",
                name,
                shareLink
            )
        }
    }
}
