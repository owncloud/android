/**
 * ownCloud Android client application
 *
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
 *
 */
package com.owncloud.android.extensions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.FileListOption

@StringRes
fun FileListOption.toTitleStringRes(): Int = when (this) {
    FileListOption.ALL_FILES -> R.string.file_list_empty_title_all_files
    FileListOption.SPACES_LIST -> R.string.spaces_list_empty_title
    FileListOption.SHARED_BY_LINK -> R.string.file_list_empty_title_shared_by_links
    FileListOption.AV_OFFLINE -> R.string.file_list_empty_title_available_offline
}

@StringRes
fun FileListOption.toSubtitleStringRes(): Int = when (this) {
    FileListOption.ALL_FILES -> R.string.file_list_empty_subtitle_all_files
    FileListOption.SPACES_LIST -> R.string.spaces_list_empty_subtitle
    FileListOption.SHARED_BY_LINK -> R.string.file_list_empty_subtitle_shared_by_links
    FileListOption.AV_OFFLINE -> R.string.file_list_empty_subtitle_available_offline
}

@DrawableRes
fun FileListOption.toDrawableRes(): Int = when (this) {
    FileListOption.ALL_FILES -> R.drawable.ic_folder
    FileListOption.SPACES_LIST -> R.drawable.ic_spaces
    FileListOption.SHARED_BY_LINK -> R.drawable.ic_shared_by_link
    FileListOption.AV_OFFLINE -> R.drawable.ic_available_offline
}
