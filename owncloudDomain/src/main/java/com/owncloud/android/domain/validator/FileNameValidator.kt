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
import java.util.regex.Pattern

class FileNameValidator {

    @Throws(FileNameException::class)
    fun validateOrThrowException(string: String) {
        if (string.trim().isBlank()) {
            throw FileNameException(type = FileNameException.FileNameExceptionType.FILE_NAME_EMPTY)
        } else if (string.length >= FILE_NAME_MAX_LENGTH_ALLOWED) {
            throw FileNameException(type = FileNameException.FileNameExceptionType.FILE_NAME_TOO_LONG)
        } else if (FILE_NAME_REGEX.containsMatchIn(string)) {
            throw FileNameException(type = FileNameException.FileNameExceptionType.FILE_NAME_FORBIDDEN_CHARACTERS)
        }
    }

    companion object {
        // Regex to check both slashes '/' and '\'
        private val FILE_NAME_REGEX = Pattern.compile(".*[/\\\\].*").toRegex()
        private const val FILE_NAME_MAX_LENGTH_ALLOWED = 250
    }
}
