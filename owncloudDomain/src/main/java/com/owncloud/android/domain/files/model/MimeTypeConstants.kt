/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.owncloud.android.domain.files.model

const val MIME_DIR= "DIR"
const val MIME_DIR_UNIX= "httpd/unix-directory"
val LIST_MIME_DIR = listOf(MIME_DIR, MIME_DIR_UNIX)

const val MIME_PREFIX_AUDIO = "audio/"
const val MIME_PREFIX_VIDEO= "video/"
const val MIME_PREFIX_IMAGE = "image/"
const val MIME_PREFIX_TEXT = "text/"
