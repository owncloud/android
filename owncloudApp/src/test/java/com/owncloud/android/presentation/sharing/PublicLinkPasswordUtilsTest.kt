/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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

package com.owncloud.android.presentation.sharing

import org.junit.Assert.assertTrue
import org.junit.Test

class PublicLinkPasswordUtilsTest {

    private val charsetLowercase = ('a'..'z').toList()
    private val charsetUppercase = ('A'..'Z').toList()
    private val charsetDigits = ('0'..'9').toList()
    private val charsetSpecial = listOf('!', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~')

    private val minCharacters = 10
    private val maxCharacters = 100
    private val minDigits = 2
    private val minLowercaseCharacters = 2
    private val minUppercaseCharacters = 2
    private val minSpecialCharacters = 2

    @Test
    fun `generatePassword creates password fulfilling all policies`() {
        for (i in 1..1000) {
            val password = generatePassword(
                minChars = minCharacters,
                maxChars = maxCharacters,
                minDigitsChars = minDigits,
                minLowercaseChars = minLowercaseCharacters,
                minUppercaseChars = minUppercaseCharacters,
                minSpecialChars = minSpecialCharacters,
            )

            assertTrue(password.length >= minCharacters)
            assertTrue(password.length <= maxCharacters)

            val digitsInPassword = password.filter { charsetDigits.contains(it) }
            assertTrue(digitsInPassword.length >= minDigits)

            val lowercaseCharsInPassword = password.filter { charsetLowercase.contains(it) }
            assertTrue(lowercaseCharsInPassword.length >= minLowercaseCharacters)

            val uppercaseCharsInPassword = password.filter { charsetUppercase.contains(it) }
            assertTrue(uppercaseCharsInPassword.length >= minUppercaseCharacters)

            val specialCharsInPassword = password.filter { charsetSpecial.contains(it) }
            assertTrue(specialCharsInPassword.length >= minSpecialCharacters)
        }
    }

    @Test
    fun `generatePassword creates password using the default value for a null parameter`() {
        val minDigitsByDefault = 1

        val password = generatePassword(
            minChars = minCharacters,
            maxChars = maxCharacters,
            minDigitsChars = null,
            minLowercaseChars = minLowercaseCharacters,
            minUppercaseChars = minUppercaseCharacters,
            minSpecialChars = minSpecialCharacters,
        )

        val digitsInPassword = password.filter { charsetDigits.contains(it) }
        assertTrue(digitsInPassword.length >= minDigitsByDefault)
    }

    @Test
    fun `generatePassword creates password using the default value for a parameter with value 0`() {
        val minSpecialCharsByDefault = 1

        val password = generatePassword(
            minChars = minCharacters,
            maxChars = maxCharacters,
            minDigitsChars = minDigits,
            minLowercaseChars = minLowercaseCharacters,
            minUppercaseChars = minUppercaseCharacters,
            minSpecialChars = 0
        )

        val specialCharsInPassword = password.filter { charsetSpecial.contains(it) }
        assertTrue(specialCharsInPassword.length >= minSpecialCharsByDefault)
    }
}
