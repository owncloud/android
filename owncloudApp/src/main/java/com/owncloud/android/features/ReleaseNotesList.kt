/**
 * ownCloud Android client application
 *
 * @author David Crespo Ríos
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

package com.owncloud.android.features

import com.owncloud.android.datamodel.ReleaseNote
import com.owncloud.android.enums.ReleaseNoteType

class ReleaseNotesList {

    private val releaseNotesList = listOf(
        ReleaseNote("Account List", "There is a fresh look for the single account overview.", ReleaseNoteType.BUGFIX),
        ReleaseNote(
            "Biometrical Unlock",
            "When protection the app with a pin code, biometrical unlock will be suggested as a default",
            ReleaseNoteType.CHANGE
        ),
        ReleaseNote("Account List", "There is a fresh look for the single account overview.", ReleaseNoteType.ENHANCEMENT),
        ReleaseNote(
            "Biometrical Unlock",
            "When protection the app with a pin code, biometrical unlock will be suggested as a default",
            ReleaseNoteType.SECURITY
        ),
        ReleaseNote("Account List", "There is a fresh look for the single account overview.", ReleaseNoteType.SECURITY),
        ReleaseNote(
            "Biometrical Unlock",
            "When protection the app with a pin code, biometrical unlock will be suggested as a default",
            ReleaseNoteType.BUGFIX
        )
    )

    fun getReleaseNotes(): List<ReleaseNote> {
        return releaseNotesList
    }
}