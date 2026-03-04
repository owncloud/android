/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
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

package com.owncloud.android.extensions

import com.owncloud.android.R
import com.owncloud.android.domain.links.model.OCLinkType

fun OCLinkType.toStringResId() =
    when (this) {
        OCLinkType.CAN_VIEW -> R.string.public_link_view
        OCLinkType.CAN_EDIT -> R.string.public_link_edit
        OCLinkType.CREATE_ONLY -> R.string.public_link_create_only
        OCLinkType.CAN_UPLOAD -> R.string.public_link_upload
        OCLinkType.INTERNAL -> R.string.public_link_internal
    }
