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

package com.owncloud.android.domain.validator

import com.owncloud.android.domain.exceptions.validation.FileNameException
import com.owncloud.android.domain.exceptions.validation.FileNameException.FileNameExceptionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNameValidatorTest {

    private val validator = FileNameValidator()

    @Test
    fun `validate name - ok`() {
        val result = runCatching { validator.validateOrThrowException("Photos") }
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `validate name - ko - empty`() {
        val result = runCatching { validator.validateOrThrowException("    ") }

        validateExceptionAndType(result, FileNameExceptionType.FILE_NAME_EMPTY)
    }

    @Test
    fun `validate name - ko - back slash`() {
        val result = runCatching { validator.validateOrThrowException("/Photos") }

        validateExceptionAndType(result, FileNameExceptionType.FILE_NAME_FORBIDDEN_CHARACTERS)
    }

    @Test
    fun `validate name - ko - forward slash`() {
        val result = runCatching { validator.validateOrThrowException("\\Photos") }

        validateExceptionAndType(result, FileNameExceptionType.FILE_NAME_FORBIDDEN_CHARACTERS)
    }

    @Test
    fun `validate name - ko - both slashes()`() {
        val result = runCatching { validator.validateOrThrowException("\\Photos/") }

        validateExceptionAndType(result, FileNameExceptionType.FILE_NAME_FORBIDDEN_CHARACTERS)
    }
}

private fun validateExceptionAndType(
    result: Result<Unit>,
    type: FileNameExceptionType
) {
    with(result.exceptionOrNull()) {
        assertTrue(this is FileNameException)
        assertEquals(type, (this as FileNameException).type)
    }
}
