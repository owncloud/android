/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.extensions

import androidx.work.WorkInfo
import com.owncloud.android.domain.extensions.isOneOf
import com.owncloud.android.workers.DownloadFileWorker
import com.owncloud.android.workers.UploadFileFromContentUriWorker
import com.owncloud.android.workers.UploadFileFromFileSystemWorker

fun WorkInfo.isUpload() =
    tags.any { it.isOneOf(UploadFileFromContentUriWorker::class.java.name, UploadFileFromFileSystemWorker::class.java.name) }

fun WorkInfo.isDownload() =
    tags.any { it.isOneOf(DownloadFileWorker::class.java.name) }
