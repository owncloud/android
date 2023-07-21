/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 *
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
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.data.providers

import android.os.Environment
import java.io.File

@Deprecated("Do not use this anymore. We have moved to Scoped Storage")
class LegacyStorageProvider(
    rootFolderName: String
) : LocalStorageProvider(rootFolderName) {

    override fun getPrimaryStorageDirectory(): File = Environment.getExternalStorageDirectory()
}
