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

package com.owncloud.android.domain.spaces.model

import com.owncloud.android.testutil.OC_SPACE_PERSONAL
import com.owncloud.android.testutil.OC_SPACE_PROJECT_DISABLED
import com.owncloud.android.testutil.OC_SPACE_PROJECT_WITHOUT_IMAGE
import com.owncloud.android.testutil.OC_SPACE_PROJECT_WITH_IMAGE
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OCSpaceTest {

    @Test
    fun `test space is personal - ok - true`() {
        val ocSpace = OC_SPACE_PERSONAL
        assertTrue(ocSpace.isPersonal)
    }

    @Test
    fun `test space is personal - ok - false`() {
        val ocSpace = OC_SPACE_PROJECT_WITH_IMAGE
        assertFalse(ocSpace.isPersonal)
    }

    @Test
    fun `test space is project - ok - true`() {
        val ocSpace = OC_SPACE_PROJECT_WITH_IMAGE
        assertTrue(ocSpace.isProject)
    }

    @Test
    fun `test space is project - ok - false`() {
        val ocSpace = OC_SPACE_PERSONAL
        assertFalse(ocSpace.isProject)
    }

    @Test
    fun `test space is disabled - ok - true`() {
        val ocSpace = OC_SPACE_PROJECT_DISABLED
        assertTrue(ocSpace.isDisabled)
    }

    @Test
    fun `test space is disabled - ok - false`() {
        val ocSpace = OC_SPACE_PROJECT_WITH_IMAGE
        assertFalse(ocSpace.isDisabled)
    }

    @Test
    fun `test get space special image - ok - has image`() {
        val ocSpace = OC_SPACE_PROJECT_WITH_IMAGE
        assertNotNull(ocSpace.getSpaceSpecialImage())
    }

    @Test
    fun `test get space special image - ok - does not have image`() {
        val ocSpace = OC_SPACE_PROJECT_WITHOUT_IMAGE
        assertNull(ocSpace.getSpaceSpecialImage())
    }

}
