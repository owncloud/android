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

package com.owncloud.android.data.file.datasources.mapper

import com.owncloud.android.data.files.datasources.mapper.OCFileMapper
import com.owncloud.android.testutil.OC_FILE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OCFileMapperTest {

    private val ocFileMapper = OCFileMapper()

    @Test
    fun `file mapper complete - ok `() {

        val entity = ocFileMapper.toEntity(OC_FILE)
        val model = ocFileMapper.toModel(entity)

        assertNotNull(model)
        assertEquals(OC_FILE, model)
    }
}
