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

package com.owncloud.android.extensions

import java.io.File

fun File.sizeInBytes(): Int = if (!exists()) 0 else length().toInt()
fun File.sizeInKb() = sizeInBytes() / 1024
fun File.sizeInMb() = sizeInKb() / 1024

fun File.getSize(): String {
    return if (sizeInBytes() < 1024) {
        "${sizeInBytes()} bytes"
    } else if (sizeInBytes() > 1024 && sizeInKb() < 1024) {
        "${sizeInKb()} Kb"
    } else {
        "${sizeInMb()} Mb"
    }
}

