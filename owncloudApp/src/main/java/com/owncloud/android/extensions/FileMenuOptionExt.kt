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
