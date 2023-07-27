/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.extensions

import com.owncloud.android.R
import com.owncloud.android.domain.files.model.FileMenuOption

fun FileMenuOption.toResId() =
    when (this) {
        FileMenuOption.SELECT_ALL -> R.id.file_action_select_all
        FileMenuOption.SELECT_INVERSE -> R.id.action_select_inverse
        FileMenuOption.DOWNLOAD -> R.id.action_download_file
        FileMenuOption.RENAME -> R.id.action_rename_file
        FileMenuOption.MOVE -> R.id.action_move
        FileMenuOption.COPY -> R.id.action_copy
        FileMenuOption.REMOVE -> R.id.action_remove_file
        FileMenuOption.OPEN_WITH -> R.id.action_open_file_with
        FileMenuOption.SYNC -> R.id.action_sync_file
        FileMenuOption.CANCEL_SYNC -> R.id.action_cancel_sync
        FileMenuOption.SHARE -> R.id.action_share_file
        FileMenuOption.DETAILS -> R.id.action_see_details
        FileMenuOption.SEND -> R.id.action_send_file
        FileMenuOption.SET_AV_OFFLINE -> R.id.action_set_available_offline
        FileMenuOption.UNSET_AV_OFFLINE -> R.id.action_unset_available_offline
    }

fun FileMenuOption.toStringResId() =
    when (this) {
        FileMenuOption.SELECT_ALL -> R.string.actionbar_select_all
        FileMenuOption.SELECT_INVERSE -> R.string.actionbar_select_inverse
        FileMenuOption.DOWNLOAD -> R.string.filedetails_download
        FileMenuOption.RENAME -> R.string.common_rename
        FileMenuOption.MOVE -> R.string.actionbar_move
        FileMenuOption.COPY -> android.R.string.copy
        FileMenuOption.REMOVE -> R.string.common_remove
        FileMenuOption.OPEN_WITH -> R.string.actionbar_open_with
        FileMenuOption.SYNC -> R.string.filedetails_sync_file
        FileMenuOption.CANCEL_SYNC -> R.string.common_cancel_sync
        FileMenuOption.SHARE -> R.string.action_share
        FileMenuOption.DETAILS -> R.string.actionbar_see_details
        FileMenuOption.SEND -> R.string.actionbar_send_file
        FileMenuOption.SET_AV_OFFLINE -> R.string.set_available_offline
        FileMenuOption.UNSET_AV_OFFLINE -> R.string.unset_available_offline
    }

fun FileMenuOption.toDrawableResId() =
    when (this) {
        FileMenuOption.SELECT_ALL -> R.drawable.ic_select_all
        FileMenuOption.SELECT_INVERSE -> R.drawable.ic_select_inverse
        FileMenuOption.DOWNLOAD -> R.drawable.ic_action_download
        FileMenuOption.RENAME -> R.drawable.ic_pencil
        FileMenuOption.MOVE -> R.drawable.ic_action_move
        FileMenuOption.COPY -> R.drawable.ic_action_copy
        FileMenuOption.REMOVE -> R.drawable.ic_action_delete_white
        FileMenuOption.OPEN_WITH -> R.drawable.ic_open_in_app
        FileMenuOption.SYNC -> R.drawable.ic_action_refresh
        FileMenuOption.CANCEL_SYNC -> R.drawable.ic_action_cancel_white
        FileMenuOption.SHARE -> R.drawable.ic_share_generic_white
        FileMenuOption.DETAILS -> R.drawable.ic_info_white
        FileMenuOption.SEND -> R.drawable.ic_send_white
        FileMenuOption.SET_AV_OFFLINE -> R.drawable.ic_action_set_available_offline
        FileMenuOption.UNSET_AV_OFFLINE -> R.drawable.ic_action_unset_available_offline
    }
