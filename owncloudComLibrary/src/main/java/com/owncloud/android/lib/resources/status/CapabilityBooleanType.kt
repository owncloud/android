/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2019 ownCloud GmbH.
 *   @author masensio
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */
package com.owncloud.android.lib.resources.status

/**
 * Enum for Boolean Type in RemoteCapability parameters, with values:
 * -1 - Unknown
 * 0 - False
 * 1 - True
 */
enum class CapabilityBooleanType private constructor(val value: Int) {
    UNKNOWN(-1),
    FALSE(0),
    TRUE(1);

    val isUnknown: Boolean
        get() = value == -1

    val isFalse: Boolean
        get() = value == 0

    val isTrue: Boolean
        get() = value == 1

    companion object {
        fun fromValue(value: Int): CapabilityBooleanType? {
            when (value) {
                -1 -> return UNKNOWN
                0 -> return FALSE
                1 -> return TRUE
            }
            return null
        }

        fun fromBooleanValue(boolValue: Boolean): CapabilityBooleanType {
            return if (boolValue) {
                TRUE
            } else {
                FALSE
            }
        }
    }
}
