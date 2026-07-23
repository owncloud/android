/**
 * ownCloud Android client application
 *
 * @author Matt Van Horn
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

package com.owncloud.android.presentation.spaces

import com.owncloud.android.testutil.OC_SPACE_PERSONAL
import org.junit.Assert.assertEquals
import org.junit.Test

class SpacesListFragmentTest {
    @Test
    fun `filterSpaces filters multi-personal names and preserves single-personal pinning`() {
        val personalSpaces = listOf(OC_SPACE_PERSONAL.copy(name = "Alice Personal"), OC_SPACE_PERSONAL.copy(name = "Bob Personal"))
        val singlePersonalSpaces = listOf(OC_SPACE_PERSONAL.copy(name = "Project Personal"))
        assertEquals(personalSpaces, personalSpaces.filterSpaces("personal", true) { true })
        assertEquals(listOf(personalSpaces[0]), personalSpaces.filterSpaces("alice", true) { true })
        assertEquals(singlePersonalSpaces, singlePersonalSpaces.filterSpaces("project", false) { true })
    }
}
