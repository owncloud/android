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

import java.security.SecureRandom

private val charsetLowercase = ('a'..'z').toList()
private val charsetUppercase = ('A'..'Z').toList()
private val charsetDigits = ('0'..'9').toList()
private val charsetSpecial = listOf('!', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']','^', '_', '`', '{', '|', '}' ,'~')

fun generatePassword(
    minCharacters: Int = 8,
    maxCharacters: Int = 72,
    minDigits: Int = 1,
    minLowercaseCharacters: Int = 1,
    minUppercaseCharacters: Int = 1,
    minSpecialCharacters: Int = 1,
): String {
    val secureRandom = SecureRandom()

    // Determine the number of characters to generate randomly within the provided range
    val length = secureRandom.nextInt(maxCharacters - minCharacters + 1) + minCharacters

    // First, store all the chars that will be part of the password unordered
    val passwordChars = mutableListOf<Char>()

    // Include the minimum number of digits established by the policy
    for (i in 1..minDigits) {
        passwordChars.add(charsetDigits[secureRandom.nextInt(charsetDigits.size)])
    }

    // Include the minimum number of lowercase chars established by the policy
    for (i in 1..minLowercaseCharacters) {
        passwordChars.add(charsetLowercase[secureRandom.nextInt(charsetLowercase.size)])
    }

    // Include the minimum number of uppercase chars established by the policy
    for (i in 1..minUppercaseCharacters) {
        passwordChars.add(charsetUppercase[secureRandom.nextInt(charsetUppercase.size)])
    }

    // Include the minimum number of special chars established by the policy
    for (i in 1..minSpecialCharacters) {
        passwordChars.add(charsetSpecial[secureRandom.nextInt(charsetSpecial.size)])
    }

    // Fill with random characters from every charset until determined length is reached
    val allCharsets = charsetLowercase + charsetUppercase + charsetDigits + charsetSpecial
    while (passwordChars.size < length) {
        passwordChars.add(allCharsets[secureRandom.nextInt(allCharsets.size)])
    }

    // Shuffle chars in the unordered list and convert it to String
    passwordChars.shuffle(secureRandom)

    return passwordChars.joinToString("")
}
