/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2015 ownCloud Inc.
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
package com.owncloud.android.lib.resources.status;

/**
 * Enum for Boolean Type in OCCapability parameters, with values:
 * -1 - Unknown
 *  0 - False
 *  1 - True
 */
public enum CapabilityBooleanType {
    UNKNOWN (-1),
    FALSE (0),
    TRUE (1);

    private int value;

    CapabilityBooleanType(int value)
    {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static CapabilityBooleanType fromValue(int value)
    {
        switch (value)
        {
            case -1:
                return UNKNOWN;
            case 0:
                return FALSE;
            case 1:
                return TRUE;
        }
        return null;
    }

    public static CapabilityBooleanType fromBooleanValue(boolean boolValue){
        if (boolValue){
            return TRUE;
        } else {
            return FALSE;
        }
    }

    public boolean isUnknown(){
        return getValue() == -1;
    }

    public boolean isFalse(){
        return getValue() == 0;
    }

    public boolean isTrue(){
        return getValue() == 1;
    }

};