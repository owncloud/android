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

enum class Permissions(val value: Int) {
    READ_PERMISSIONS(1),
    EDIT_PERMISSIONS(3),
    SHARE_PERMISSIONS(17),
    ALL_PERMISSIONS(19),
    // FOLDERS
    EDIT_CREATE_PERMISSIONS(5),
    EDIT_CREATE_CHANGE_PERMISSIONS(7),
    EDIT_CREATE_DELETE_PERMISSIONS(13),
    EDIT_CREATE_CHANGE_DELETE_PERMISSIONS(15),
    EDIT_CHANGE_PERMISSIONS(3),
    EDIT_CHANGE_DELETE_PERMISSIONS(11),
    EDIT_DELETE_PERMISSIONS(9),
}
